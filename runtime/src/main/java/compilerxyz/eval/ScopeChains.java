package compilerxyz.eval;

import compilerxyz.gen.Binding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class ScopeChains {
    private ScopeChains() {}

    public static EResult<Opt<JumpKey>> excDest(ExprContext xx, QExprId loc) {
        var levels = xx.scope.levels;
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
                    if (!entry.stackOnly) cleanups.add(entry);
                }
                case ScopeLevel.Temp temp -> cleanups.add(temp);
                case ScopeLevel.Finally finally_ -> cleanups.add(finally_);
                case ScopeLevel.Labeled labeled -> {}
            }
            if (done) break;
        }
        var parent = terminal;
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            var resolved = node(
                    xx,
                    loc,
                    cleanups.get(i),
                    new ScopeLevel.DestKey(levelDropState(cleanups.get(i)), Opt.<String>none(), parent, -1));
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

    static EResult<Opt<JumpKey>> node(ExprContext xx, QExprId loc, ScopeLevel level, ScopeLevel.DestKey key) {
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
                nodeRes = xx.target.targetExitTrampoline(
                        xx,
                        loc,
                        self,
                        key.carryType(emission),
                        cleanup,
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
