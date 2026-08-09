package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Chain-node construction over the scope stack. A throwing site's exceptions
 * route to the innermost chain node; a return jumps to the innermost
 * trampoline node. Each node runs one level's cleanup at the state current
 * when the site was compiled, then forwards outward. Nodes dedup per (level
 * lineage, state, continuation) via the levels' shared emission boxes; their
 * code is spliced in at the owning level's close.
 */
public final class ScopeChains {
  private ScopeChains() {}

  /**
   * Destination for exceptions thrown at the current position, or none when
   * nothing needs to intervene (no live cleanup, no enclosing catch) and the
   * exception propagates out of the method unobserved.
   */
  public static EResult<Opt<JumpKey>> excDest(ExprContext xx, QExprId loc) {
    var levels = xx.scope.levels;
    // Participating levels, innermost-first, up to an enclosing catch
    // (exclusive); the catch's handler key is the chain terminal.
    var cleanups = new TList<ScopeLevel>();
    Opt<JumpKey> terminal = Opt.none();
    for (int i = levels.size() - 1; i >= 0; i--) {
      var level = levels.get(i);
      boolean done = false;
      switch (level) {
        case ScopeLevel.Catch catch_ -> {
          terminal = Opt.some(catch_.handler);
          done = true;
        }
        case ScopeLevel.Entry entry -> {
          // Stack-only cleanup restores the operand stack, which an
          // exceptional unwind discards anyway.
          if (!entry.stackOnly) cleanups.add(entry);
        }
        case ScopeLevel.Temp temp -> cleanups.add(temp);
        case ScopeLevel.Finally finally_ -> cleanups.add(finally_);
        case ScopeLevel.Labeled labeled -> {}
        case ScopeLevel.Barrier barrier -> {}
      }
      if (done) break;
    }
    // Build outermost-first: each node needs its parent's resolved dest.
    var parent = terminal;
    for (int i = cleanups.size() - 1; i >= 0; i--) {
      var resolved =
          node(
              xx,
              loc,
              cleanups.get(i),
              new ScopeLevel.DestKey(
                  levelDropState(cleanups.get(i)), Opt.<String>none(), parent, Opt.<Type>none()));
      if (resolved.hasErrors()) return resolved.reerr();
      parent = resolved.requireValue("ScopeChains.excDest node");
    }
    return EResult.ok(parent);
  }

  /**
   * Destination for a return targeting {@code target}: the innermost
   * trampoline node of the chain that drops every cleanup level between the
   * site and the target, then lands at the target's end. {@code carryType}
   * is the carried value's type when it rides the stack across the jump.
   */
  public static EResult<JumpKey> exitDest(
      ExprContext xx, QExprId loc, ScopeLevel.Labeled target, Opt<Type> carryType) {
    var levels = xx.scope.levels;
    var cleanups = new TList<ScopeLevel>();
    boolean found = false;
    for (int i = levels.size() - 1; i >= 0; i--) {
      var level = levels.get(i);
      if (level == target) {
        found = true;
        break;
      }
      switch (level) {
        case ScopeLevel.Entry entry -> cleanups.add(entry);
        case ScopeLevel.Temp temp -> cleanups.add(temp);
        case ScopeLevel.Finally finally_ -> cleanups.add(finally_);
        case ScopeLevel.Labeled labeled -> {}
        case ScopeLevel.Catch catch_ -> {}
        case ScopeLevel.Barrier barrier ->
            throw new AssertionError("exitDest: return across an operand barrier");
      }
    }
    if (!found) {
      throw new AssertionError("exitDest: target label level is not in scope");
    }
    var discriminator =
        target.label instanceof Opt.Some<String> someLabel ? someLabel.value() : "";
    var next = target.end;
    for (int i = cleanups.size() - 1; i >= 0; i--) {
      var resolved =
          node(
              xx,
              loc,
              cleanups.get(i),
              new ScopeLevel.DestKey(
                  levelDropState(cleanups.get(i)),
                  Opt.some(discriminator),
                  Opt.some(next),
                  carryType));
      if (resolved.hasErrors()) return resolved.reerr();
      next = resolved.requireValue("ScopeChains.exitDest node").optUnwrap();
    }
    return EResult.ok(next);
  }

  private static long levelDropState(ScopeLevel level) {
    return switch (level) {
      case ScopeLevel.Entry entry -> entry.dropState;
      case ScopeLevel.Temp temp -> temp.dropState;
      case ScopeLevel.Finally finally_ -> 0L;
      case ScopeLevel.Labeled labeled -> throw new AssertionError();
      case ScopeLevel.Catch catch_ -> throw new AssertionError();
      case ScopeLevel.Barrier barrier -> throw new AssertionError();
    };
  }

  /**
   * Get or create the chain node for {@code level} under {@code key}.
   * Returns the resolved dest: the node's own key, or the continuation when
   * the level's cleanup is empty at the current state (pass-through).
   * Finally nodes always materialize, with code generation deferred to the
   * try's close (the body is const-evaled after the covered code).
   */
  private static EResult<Opt<JumpKey>> node(
      ExprContext xx, QExprId loc, ScopeLevel level, ScopeLevel.DestKey key) {
    var emission = level.scopeLevelEmission();
    var existing = emission.dests.getOrNull(key);
    if (existing != null) {
      return EResult.ok(existing);
    }
    if (level instanceof ScopeLevel.Finally finally_) {
      var self = new JumpKey();
      finally_.pending.add(new ScopeLevel.Finally.Pending(self, key));
      var resolved = Opt.some(self);
      emission.dests.put(key, resolved);
      return EResult.ok(resolved);
    }
    Binding binding =
        switch (level) {
          case ScopeLevel.Entry entry -> entry.binding;
          case ScopeLevel.Temp temp -> temp.binding;
          case ScopeLevel.Finally finally_ -> throw new AssertionError();
          case ScopeLevel.Labeled labeled -> throw new AssertionError();
          case ScopeLevel.Catch catch_ -> throw new AssertionError();
          case ScopeLevel.Barrier barrier -> throw new AssertionError();
        };
    // Pre-register as pass-through before generating: drop-code generation
    // runs user code whose nested evaluation may request this same node — a
    // throw during a drop routes outward, never back into the drop.
    emission.dests.put(key, key.next());
    var dropRes = binding.bindingDrop(xx, loc);
    if (dropRes.hasErrors()) return dropRes.reerr();
    var cleanup = dropRes.requireValue("ScopeChains node drop");
    Opt<JumpKey> resolved;
    if (cleanup.isNone()) {
      // Pass-through: the chain continues to the continuation (exceptional
      // parent, next trampoline node, or terminal).
      resolved = key.next();
    } else {
      var self = new JumpKey();
      EResult<Code> nodeRes;
      if (key.label() instanceof Opt.Some<String>) {
        nodeRes =
            xx.target.targetExitTrampoline(
                xx, loc, self, key.carryType(), cleanup,
                ((Opt.Some<JumpKey>) key.next()).value());
      } else {
        nodeRes = xx.target.targetExcCleanup(xx, loc, self, cleanup, key.next());
      }
      if (nodeRes.hasErrors()) return nodeRes.reerr();
      emission.blocks.add(nodeRes.requireValue("ScopeChains node block"));
      resolved = Opt.some(self);
    }
    emission.dests.put(key, resolved);
    return EResult.ok(resolved);
  }

  /**
   * Generate the deferred blocks for a finally level once its body is known.
   * Called at the try's close; every pending node becomes one instance of
   * the body bracketed for its chain kind.
   */
  public static EResult<Void> finallyGenerate(
      ExprContext xx, QExprId loc, ScopeLevel.Finally level) {
    var emission = level.scopeLevelEmission();
    for (var pending : level.pending) {
      var key = pending.key();
      EResult<Code> nodeRes;
      if (key.label() instanceof Opt.Some<String>) {
        nodeRes =
            xx.target.targetExitTrampoline(
                xx, loc, pending.self(), key.carryType(), level.body,
                ((Opt.Some<JumpKey>) key.next()).value());
      } else {
        nodeRes = xx.target.targetExcCleanup(xx, loc, pending.self(), level.body, key.next());
      }
      if (nodeRes.hasErrors()) return nodeRes.reerr();
      emission.blocks.add(nodeRes.requireValue("ScopeChains finally block"));
    }
    level.pending.clear();
    return EResult.ok(null);
  }
}
