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


  static long levelDropState(ScopeLevel level) {
    return switch (level) {
      case ScopeLevel.Entry entry -> entry.dropState;
      case ScopeLevel.Temp temp -> temp.dropState;
      case ScopeLevel.Finally finally_ -> 0L;
      case ScopeLevel.Labeled labeled -> throw new AssertionError();
      case ScopeLevel.Catch catch_ -> throw new AssertionError();
    };
  }

  /**
   * Get or create the chain node for {@code level} under {@code key}.
   * Returns the resolved dest: the node's own key, or the continuation when
   * the level's cleanup is empty at the current state (pass-through).
   * Finally nodes always materialize, with code generation deferred to the
   * try's close (the body is const-evaled after the covered code).
   */
  static EResult<Opt<JumpKey>> node(
      ExprContext xx, QExprId loc, ScopeLevel level, ScopeLevel.DestKey key) {
    var emission = level.scopeLevelEmission();
    var existing = emission.dests.getOrNull(key);
    if (existing != null) {
      return EResult.ok(existing);
    }
    Binding binding;
    switch (level) {
      case ScopeLevel.Finally finally_ -> {
        var self = new JumpKey();
        finally_.pending.add(new ScopeLevel.Finally.Pending(self, key));
        var resolved = Opt.some(self);
        emission.dests.put(key, resolved);
        return EResult.ok(resolved);
      }
      case ScopeLevel.Entry entry -> binding = entry.binding;
      case ScopeLevel.Temp temp -> binding = temp.binding;
      case ScopeLevel.Labeled labeled -> throw new AssertionError();
      case ScopeLevel.Catch catch_ -> throw new AssertionError();
    }
    emission.dests.put(key, key.next());
    var dropRes = binding.bindingDrop(xx, loc);
    if (dropRes.hasErrors()) return dropRes.reerr();
    var cleanup = dropRes.requireValue("ScopeChains node drop");
    Opt<JumpKey> resolved;
    if (cleanup.codeIsNone()) {
      resolved = key.next();
    } else {
      var self = new JumpKey();
      EResult<Code> nodeRes;
      if (key.label().optIsSome()) {
        nodeRes =
            xx.target.targetExitTrampoline(
                xx, loc, self, key.carryType(), cleanup,
                key.next().optUnwrap());
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

}
