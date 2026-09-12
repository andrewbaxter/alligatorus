package compilerxyz.main.eval;

import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

@BuiltinFull(uniqueId = 6024L)
public final class Scope {

    public final EvalContext vx;
    public final TList<ScopeLevel> levels = new TList<>();

    public Scope(EvalContext vx) {
        this.vx = vx;
    }

    public int levelCount() {
        return levels.size();
    }

    public ScopeLevel.Entry pushNamed(ConstKey name, Binding binding) {
        var id = vx.dropStateCounter.next();
        var entry = new ScopeLevel.Entry(
                Opt.some(name), binding, false, id, id, new ScopeLevel.Emission(vx.emissionCounter.next()));
        levels.add(entry);
        return entry;
    }

    public void pushAll(TMap<ConstKey, Binding> named) {
        for (var entry : named.entrySet()) {
            pushNamed(entry.getKey(), entry.getValue());
        }
    }

    public ScopeLevel.Temp pushTemp(Binding binding) {
        var id = vx.dropStateCounter.next();
        var level = new ScopeLevel.Temp(binding, id, id, new ScopeLevel.Emission(vx.emissionCounter.next()));
        levels.add(level);
        return level;
    }

    public ScopeLevel.Labeled pushLabeled(Opt<String> label) {
        var level = new ScopeLevel.Labeled(
                label,
                true,
                new JumpKey(),
                new ScopeLevel.Landing(),
                new ScopeLevel.Emission(vx.emissionCounter.next()));
        levels.add(level);
        return level;
    }

    public ScopeLevel.Labeled pushMergeLabeled() {
        var level = new ScopeLevel.Labeled(
                Opt.none(),
                false,
                new JumpKey(),
                new ScopeLevel.Landing(),
                new ScopeLevel.Emission(vx.emissionCounter.next()));
        levels.add(level);
        return level;
    }

    public void popToLevelCount(int targetCount) {
        while (levels.size() > targetCount) {
            levels.remove(levels.size() - 1);
        }
    }

    public Scope fork() {
        var out = new Scope(vx);
        for (var level : levels) {
            out.levels.add(
                    switch (level) {
                        case ScopeLevel.Entry entry ->
                            new ScopeLevel.Entry(
                                    entry.name,
                                    entry.binding.bindingFork(),
                                    entry.stackOnly,
                                    entry.state,
                                    entry.dropState,
                                    entry.emission);
                        case ScopeLevel.Temp temp ->
                            new ScopeLevel.Temp(temp.binding.bindingFork(), temp.state, temp.dropState, temp.emission);
                        case ScopeLevel.Labeled labeled ->
                            new ScopeLevel.Labeled(
                                    labeled.label, labeled.isBlock, labeled.end, labeled.landing, labeled.emission);
                        case ScopeLevel.Catch catch_ -> new ScopeLevel.Catch(catch_.handler, catch_.emission);
                        case ScopeLevel.Finally finally_ -> {
                            var f = new ScopeLevel.Finally(finally_.pending, finally_.emission);
                            f.body = finally_.body;
                            yield f;
                        }
                    });
        }
        return out;
    }

    public EResult<Void> merge(ExprContext xx, QExprId loc, TList<Scope> others) {
        for (var other : others) {
            if (other.levels.size() != levels.size()) {
                return EResult.err(
                        loc,
                        "Scope.merge: branch produced a scope of different depth (" + other.levels.size() + " vs "
                                + levels.size() + ")");
            }
        }
        var merged = new TList<Binding>(levels.size());
        var mergedStates = new TList<Long>(levels.size());
        var mergedDropStates = new TList<Long>(levels.size());
        for (int i = 0; i < levels.size(); i++) {
            switch (levels.get(i)) {
                case ScopeLevel.Temp t -> {
                    merged.add(null);
                    mergedStates.add(null);
                    mergedDropStates.add(null);
                }
                case ScopeLevel.Labeled l -> {
                    merged.add(null);
                    mergedStates.add(null);
                    mergedDropStates.add(null);
                }
                case ScopeLevel.Catch c -> {
                    merged.add(null);
                    mergedStates.add(null);
                    mergedDropStates.add(null);
                }
                case ScopeLevel.Finally f -> {
                    merged.add(null);
                    mergedStates.add(null);
                    mergedDropStates.add(null);
                }
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
                            case ScopeLevel.Temp t -> {
                                return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i);
                            }
                            case ScopeLevel.Labeled l -> {
                                return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i);
                            }
                            case ScopeLevel.Catch c -> {
                                return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i);
                            }
                            case ScopeLevel.Finally f -> {
                                return EResult.err(loc, "Scope.merge: branch level kind mismatch at " + i);
                            }
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
                        mergedDropStates.add(anyDropStateDiffers ? vx.dropStateCounter.next() : entry.dropState);
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

    public void adopt(Scope other) {
        var src = TList.from(other.levels);
        levels.clear();
        levels.addAll(src);
    }
}
