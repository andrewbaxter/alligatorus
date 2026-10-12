package compilerxyz.main.eval;

import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        return chain(
                xx,
                loc,
                cleanups,
                terminal,
                level -> new ScopeLevel.DestKey(levelDropState(level), Opt.<String>none(), Opt.<JumpKey>none(), -1));
    }

    public interface KeyFor {
        ScopeLevel.DestKey keyFor(ScopeLevel level);
    }

    public static EResult<Opt<JumpKey>> chain(
            ExprContext xx, QExprId loc, TList<ScopeLevel> cleanups, Opt<JumpKey> terminal, KeyFor keyFor) {
        var drops = new TList<Opt<Code>>(cleanups.size());
        var dropXx = Eval.dropContext(xx);
        for (var level : cleanups) {
            EResult<Opt<Code>> dropRes =
                    switch (level) {
                        case ScopeLevel.Finally finally_ -> EResult.ok(Opt.none());
                        case ScopeLevel.Entry entry -> {
                            var r = entry.binding.bindingDrop(dropXx, loc);
                            if (r.hasErrors()) yield r.reerr();
                            yield EResult.ok(Opt.some(r.requireValue("ScopeChains.chain entry drop")));
                        }
                        case ScopeLevel.Temp temp -> {
                            var r = Eval.dropValue(
                                    xx, loc, new CodeValue(Code.none(loc), EvalOutcome.ofValue(temp.value)));
                            if (r.hasErrors()) yield r.reerr();
                            yield EResult.ok(Opt.some(r.requireValue("ScopeChains.chain temp drop")));
                        }
                        case ScopeLevel.Labeled labeled -> throw new AssertionError();
                        case ScopeLevel.Catch catch_ -> throw new AssertionError();
                    };
            if (dropRes.hasErrors()) return dropRes.reerr();
            drops.add(dropRes.requireValue("ScopeChains.chain drop"));
        }
        var parent = terminal;
        for (int i = cleanups.size() - 1; i >= 0; i--) {
            var level = cleanups.get(i);
            var base = keyFor.keyFor(level);
            var key = new ScopeLevel.DestKey(base.dropState, base.label(), parent, base.argumentIdx);
            var emission = level.scopeLevelEmission();
            var existingOpt = emission.dests.getOpt(key);
            if (existingOpt.optIsSome()) {
                parent = existingOpt.optUnwrap();
                continue;
            }
            var drop = drops.get(i);
            if (drop.optIsNone()) {
                var finally_ = (ScopeLevel.Finally) level;
                var self = new JumpKey();
                finally_.pending.add(new ScopeLevel.Finally.Pending(self, key));
                parent = Opt.some(self);
                emission.dests.put(key, parent);
                continue;
            }
            emission.dests.put(key, key.next());
            var cleanup = drop.optUnwrap();
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
                            key.argument(emission),
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
            parent = resolved;
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
}
