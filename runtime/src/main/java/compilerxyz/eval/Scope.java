package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * The scope stack: a list of {@link ScopeLevel}s. Binding entries carry the
 * lexical names; label, catch, and finally levels mark the control constructs
 * an unwind or labeled return must pass through; temp levels track in-flight
 * droppable values. Chain bookkeeping lives on the levels themselves and is
 * shared across forks by identity.
 */
@BuiltinFull(uniqueId = 6024L)
public final class Scope {


  public final EvalContext vx;
  public final TList<ScopeLevel> levels = new TList<>();

  public Scope(EvalContext vx) {
    this.vx = vx;
  }

  /** Number of binding entries (level kinds other than Entry are not counted). */
  public int depth() {
    int n = 0;
    for (var level : levels) {
      n += switch (level) {
        case ScopeLevel.Entry e -> 1;
        case ScopeLevel.Temp t -> 0;
        case ScopeLevel.Labeled l -> 0;
        case ScopeLevel.Catch c -> 0;
        case ScopeLevel.Finally f -> 0;
      };
    }
    return n;
  }

  /** Total level count, all kinds. Constructs snapshot this to pop back to. */
  public int levelCount() {
    return levels.size();
  }

  public ScopeLevel.Entry pushNamed(ConstKey name, Binding binding) {
    var id = vx.dropStateCounter.next();
    var entry = new ScopeLevel.Entry(Opt.some(name), binding, false, id, id);
    levels.add(entry);
    return entry;
  }

  public ScopeLevel.Entry push(Binding binding) {
    var id = vx.dropStateCounter.next();
    var entry = new ScopeLevel.Entry(Opt.<ConstKey>none(), binding, false, id, id);
    levels.add(entry);
    return entry;
  }

  /** Push a synthetic operand-stack cleanup entry (trampoline-only; see Entry.stackOnly). */
  public ScopeLevel.Entry pushStackCleanup(Binding binding) {
    var id = vx.dropStateCounter.next();
    var entry = new ScopeLevel.Entry(Opt.<ConstKey>none(), binding, true, id, id);
    levels.add(entry);
    return entry;
  }

  public void pushAll(compilerxyz.util.TMap<ConstKey, Binding> named) {
    for (var entry : named.entrySet()) {
      pushNamed(entry.getKey(), entry.getValue());
    }
  }

  public ScopeLevel.Labeled pushLabeled(Opt<String> label) {
    var level = new ScopeLevel.Labeled(label, new JumpKey());
    levels.add(level);
    return level;
  }

  public ScopeLevel.Catch pushCatch(JumpKey handler) {
    var level = new ScopeLevel.Catch(handler);
    levels.add(level);
    return level;
  }

  public ScopeLevel.Finally pushFinally() {
    var level = new ScopeLevel.Finally();
    levels.add(level);
    return level;
  }

  public ScopeLevel.Temp pushTemp(Binding binding) {
    var id = vx.dropStateCounter.next();
    var level = new ScopeLevel.Temp(binding, id, id);
    levels.add(level);
    return level;
  }


  /** Pop the topmost level; it must be the given one. */
  public void pop(ScopeLevel expected) {
    var top = levels.remove(levels.size() - 1);
    if (top != expected) {
      throw new AssertionError("Scope.pop: top level is not the expected level");
    }
  }

  /** Pop levels until {@link #levelCount()} is {@code targetCount}. */
  public void popToLevelCount(int targetCount) {
    while (levels.size() > targetCount) {
      levels.remove(levels.size() - 1);
    }
  }

  public Binding lookup(ConstKey name) {
    var entry = lookupEntry(name);
    return entry == null ? null : entry.binding;
  }

  public ScopeLevel.Entry lookupEntry(ConstKey name) {
    for (int i = levels.size() - 1; i >= 0; i--) {
      switch (levels.get(i)) {
        case ScopeLevel.Entry entry -> {
          if (entry.name.optIsSome() && entry.name.optUnwrap().equals(name)) return entry;
        }
        case ScopeLevel.Temp t -> {}
        case ScopeLevel.Labeled l -> {}
        case ScopeLevel.Catch c -> {}
        case ScopeLevel.Finally f -> {}
      }
    }
    return null;
  }

  /**
   * Clone the scope for a branch — binding entries are forked so mutations in
   * the branch do not leak into the parent or siblings; drop states carry
   * over (the fork is the same state until a path mutates); chain bookkeeping
   * boxes are shared by identity so nodes registered on any path surface at
   * the owning level's close.
   */
  public Scope fork() {
    var out = new Scope(vx);
    for (var level : levels) {
      out.levels.add(
          switch (level) {
            case ScopeLevel.Entry entry -> entry.forkCopy();
            case ScopeLevel.Temp temp -> temp.forkCopy();
            case ScopeLevel.Labeled labeled -> labeled.forkCopy();
            case ScopeLevel.Catch catch_ -> catch_.forkCopy();
            case ScopeLevel.Finally finally_ -> finally_.forkCopy();
          });
    }
    return out;
  }

  /**
   * Merge branch outcomes into this scope. Binding entries merge
   * position-wise; branch scopes must have the same level shape.
   */
  public EResult<Void> merge(ExprContext xx, QExprId loc, TList<Scope> others) {
    for (var other : others) {
      if (other.levels.size() != levels.size()) {
        return EResult.err(loc,
            "Scope.merge: branch produced a scope of different depth ("
                + other.levels.size() + " vs " + levels.size() + ")");
      }
    }
    var merged = new TList<Binding>(levels.size());
    var mergedStates = new TList<Long>(levels.size());
    var mergedDropStates = new TList<Long>(levels.size());
    for (int i = 0; i < levels.size(); i++) {
      switch (levels.get(i)) {
        case ScopeLevel.Temp t -> { merged.add(null); mergedStates.add(null); mergedDropStates.add(null); }
        case ScopeLevel.Labeled l -> { merged.add(null); mergedStates.add(null); mergedDropStates.add(null); }
        case ScopeLevel.Catch c -> { merged.add(null); mergedStates.add(null); mergedDropStates.add(null); }
        case ScopeLevel.Finally f -> { merged.add(null); mergedStates.add(null); mergedDropStates.add(null); }
        case ScopeLevel.Entry entry -> {
          var branchBindings = new TList<Binding>(others.size());
          boolean anyStateDiffers = false;
          boolean anyDropStateDiffers = false;
          for (var other : others) {
            switch (other.levels.get(i)) {
              case ScopeLevel.Entry otherEntry -> {
                branchBindings.add(otherEntry.binding);
                if (otherEntry.state != entry.state) anyStateDiffers = true;
                if (otherEntry.dropState != entry.dropState) anyDropStateDiffers = true;
              }
              case ScopeLevel.Temp t -> { return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i); }
              case ScopeLevel.Labeled l -> { return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i); }
              case ScopeLevel.Catch c -> { return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i); }
              case ScopeLevel.Finally f -> { return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i); }
            }
          }
          if (!anyStateDiffers) {
            merged.add(entry.binding);
            mergedStates.add(entry.state);
            mergedDropStates.add(entry.dropState);
          } else {
            var mergedRes = entry.binding.bindingMerge(xx, loc, branchBindings);
            if (mergedRes.hasErrors()) return mergedRes.reerr();
            merged.add(mergedRes.requireValue("Scope.merge binding"));
            mergedStates.add(vx.dropStateCounter.next());
            mergedDropStates.add(
                anyDropStateDiffers ? vx.dropStateCounter.next() : entry.dropState);
          }
        }
      }
    }
    for (int i = 0; i < levels.size(); i++) {
      switch (levels.get(i)) {
        case ScopeLevel.Entry entry -> {
          if (merged.get(i) != null) {
            entry.binding = merged.get(i);
            entry.state = mergedStates.get(i);
            entry.dropState = mergedDropStates.get(i);
          }
        }
        case ScopeLevel.Temp t -> {}
        case ScopeLevel.Labeled l -> {}
        case ScopeLevel.Catch c -> {}
        case ScopeLevel.Finally f -> {}
      }
    }
    return EResult.ok(null);
  }

  /**
   * Install {@code other}'s contents as this scope's, unless it already is
   * this scope. Used to adopt an expression result's default-exit scope tree
   * before evaluating the next expression.
   */
  public void adopt(Scope other) {
    if (other != this) {
      replace(other);
    }
  }

  /** Replace this scope's contents with {@code other}'s. */
  public void replace(Scope other) {
    levels.clear();
    levels.addAll(other.levels);
  }
}
