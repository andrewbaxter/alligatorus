package compilerxyz.main.eval;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenBuiltinTree;
import compilerxyz.main.gen.Value;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public final class CompileModule {
    private CompileModule() {}

    public record CompileModuleResult(CompileRes res, TList<CompileId> localDeps) {}

    public static CompileModuleResult compileModuleId(
            GlobalContext gx,
            ImportStateId importState,
            Expr.Module source,
            TList<CompileId> compilePath,
            SourceSpecAbsolute currentSpec) {
        var sourceId =
                switch (source) {
                    case Expr.Module.V1 v1 -> v1.source();
                };
        var rootLoc = new QExprId(sourceId, Opt.none());
        Expr rootExpr =
                switch (source) {
                    case Expr.Module.V1 v1 -> new Expr(rootLoc, new Expr.Seq(v1.exprs()));
                };
        var extendedPath = new TList<CompileId>(compilePath.size() + 1);
        extendedPath.addAll(compilePath);
        extendedPath.add(new CompileId(importState, sourceId));
        var cix = new CompileImportContext(gx, importState, sourceId, extendedPath, currentSpec);

        var excess = lowerExcess(rootExpr, 0, 0);
        if (excess.optIsSome()) {
            for (var i = 0; i < excess.optUnwrap().excess(); i++) {
                rootExpr = new Expr(excess.optUnwrap().loc(), new Expr.Stage(rootExpr));
            }
        }

        var allLogs = new TList<LogEntry>();
        while (true) {
            boolean isStage =
                    switch (rootExpr.variant()) {
                        case Expr.Stage s -> true;
                        case Expr.As a -> false;
                        case Expr.Bind b -> false;
                        case Expr.Call c -> false;
                        case Expr.Field f -> false;
                        case Expr.Foreach f -> false;
                        case Expr.Graft.Pending p -> false;
                        case Expr.Graft.Evaluated g -> false;
                        case Expr.If i -> false;
                        case Expr.Labeled l -> false;
                        case Expr.Literal l -> false;
                        case Expr.Lower l -> false;
                        case Expr.Match m -> false;
                        case Expr.OperatorBinary o -> false;
                        case Expr.OperatorUnary o -> false;
                        case Expr.Record r -> false;
                        case Expr.Return r -> false;
                        case Expr.ScopeUse s -> false;
                        case Expr.Seq s -> false;
                        case Expr.Set s -> false;
                        case Expr.Try t -> false;
                        case Expr.Tuple t -> false;
                    };
            EvalOutcome evalResult;
            evalBlock:
            {
                var evalRootLoc = rootExpr.loc();
                var evalVx = new EvalContext(cix);
                var evalRootScope = new Scope(evalVx);
                var evalTarget = new TargetDynamic();
                var evalXx = new ExprContext(
                        evalRootScope, evalTarget, evalVx, importState, Opt.some(new TypeStringConst()));
                var res = GenBuiltinTree.installRoot(evalXx, evalRootLoc, importState);
                if (res.hasErrors()) {
                    evalResult = new EvalOutcome.Err(res.errorsOpt().optUnwrap());
                    break evalBlock;
                }
                var binding = res.requireValue("Eval::eval installRoot");
                evalXx.vx.ix.builtinScopeRootBinding = Opt.some(binding);
                var scopeEntries = new TMap<ConstKey, Binding>();
                scopeEntries.put(new ConstKey.Str(Eval.KEY_BUILTINS), binding);
                for (var entry : scopeEntries.entrySet()) {
                    evalRootScope.pushNamed(entry.getKey(), entry.getValue());
                }
                var evalBinaryName = EvalRoot.newBinaryName(evalXx);
                var cvRes = ConstEval.constEval(evalXx, rootExpr);
                if (cvRes.hasErrors()) {
                    evalResult = new EvalOutcome.Err(cvRes.errorsOpt().optUnwrap());
                    break evalBlock;
                }
                var evalRes = cvRes.requireValue("Eval::eval constEval");
                var cv = evalRes.cv();
                Opt<CodeDynamicReg> stage1Code;
                Opt<Value> fallbackValue;
                Opt<Type> valueType;
                if (cv.value().optIsNone() || !DynUtil.valueIsRuntime(cv.value().optUnwrap())) {
                    var codeDynCast = CodeDynamic.castOpt(evalRootLoc, cv.code());
                    if (codeDynCast.hasErrors()) {
                        evalResult = new EvalOutcome.Err(codeDynCast.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    var codeDyn = codeDynCast.requireValue("codeDyn");
                    if (codeDyn.optIsNone()) {
                        stage1Code = Opt.none();
                    } else {
                        var nb = new JavaBytecodeBuilder(evalRootLoc);
                        nb.add(codeDyn.optUnwrap());
                        stage1Code = Opt.some(nb.code(nb.constNull()));
                    }
                    fallbackValue = cv.value();
                    valueType = cv.value().optMap(Value::valueType);
                } else {
                    var cvValue = cv.value().optUnwrap();
                    valueType = Opt.some(cvValue.valueType());
                    var dynamicRes = DynUtil.materialize(evalXx, evalRootLoc, cv);
                    if (dynamicRes.hasErrors()) {
                        evalResult = new EvalOutcome.Err(dynamicRes.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    stage1Code = Opt.some(dynamicRes.requireValue("Eval::eval materialize"));
                    fallbackValue = Opt.none();
                }
                if (stage1Code.optIsNone() && evalVx.definitionSets.isEmpty()) {
                    evalResult = new EvalOutcome.Ok(fallbackValue, new TList<>(), false);
                    break evalBlock;
                }
                var installRes = EvalRoot.installDefinitionSets(evalXx, evalRootLoc);
                if (installRes.hasErrors()) {
                    evalResult = new EvalOutcome.Err(installRes.errorsOpt().optUnwrap());
                    break evalBlock;
                }
                if (stage1Code.optIsNone()) {
                    evalResult = new EvalOutcome.Ok(fallbackValue, evalVx.logs, false);
                    break evalBlock;
                }
                var runRes = EvalRoot.run(
                        evalXx,
                        evalRootLoc,
                        evalBinaryName,
                        stage1Code.optUnwrap(),
                        installRes.requireValue("Eval::eval installDefinitionSets"));
                if (runRes.hasErrors()) {
                    evalResult = new EvalOutcome.Err(runRes.errorsOpt().optUnwrap());
                    break evalBlock;
                }
                Object rawResult = runRes.requireValue("Eval::eval run");
                Opt<Value> finalValue;
                if (fallbackValue.optIsSome()) {
                    finalValue = fallbackValue;
                } else if (valueType.optIsNone()) {
                    finalValue = Opt.none();
                } else {
                    var liftRes = GenAspectLift.get(evalXx, evalRootLoc, valueType.optUnwrap());
                    if (liftRes.hasErrors()) {
                        evalResult = new EvalOutcome.Err(liftRes.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    var liftAspect = liftRes.requireValue("Eval::eval lift aspect");
                    var liftedRes = liftAspect.aspectLiftLift(evalXx, evalRootLoc, rawResult);
                    if (liftedRes.hasErrors()) {
                        evalResult = new EvalOutcome.Err(liftedRes.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    finalValue = Opt.some(liftedRes.requireValue("Eval::eval lift result"));
                }
                evalResult = new EvalOutcome.Ok(finalValue, evalVx.logs, fallbackValue.optIsNone());
            }
            switch (evalResult) {
                case EvalOutcome.Err err -> {
                    return new CompileModuleResult(
                            new CompileRes(allLogs, new CompileResResult.Err(err.errors())), cix.localDependencies);
                }
                case EvalOutcome.Ok ok -> {
                    allLogs.addAll(ok.logs());
                    if (!isStage) {
                        if (compilePath.isEmpty()
                                && (ok.producedDynamicRoot() || ok.value().optIsNone())) {
                            return new CompileModuleResult(
                                    new CompileRes(allLogs, new CompileResResult.Ok(Opt.none())),
                                    cix.localDependencies);
                        }
                        var vx = new EvalContext(cix);
                        var rootScope = new Scope(vx);
                        var target = new TargetDynamic();
                        var xx = new ExprContext(rootScope, target, vx, importState, Opt.some(new TypeStringDynamic()));
                        if (ok.value().optIsNone()) {
                            return new CompileModuleResult(
                                    new CompileRes(allLogs, new CompileResResult.Ok(Opt.none())),
                                    cix.localDependencies);
                        }
                        var exportErrors = new TList<ExprErr>();
                        var exportArt = Artifactable.exportArtifact(
                                xx, rootLoc, ok.value().optUnwrap());
                        if (exportArt.hasErrors()) {
                            exportArt.drainInto(exportErrors);
                            return new CompileModuleResult(
                                    new CompileRes(allLogs, new CompileResResult.Err(exportErrors)),
                                    cix.localDependencies);
                        }
                        var artifact = exportArt.requireValue("ArtifactSupport::exportArtifact artifact");
                        return new CompileModuleResult(
                                new CompileRes(allLogs, new CompileResResult.Ok(Opt.some(artifact))),
                                cix.localDependencies);
                    }
                    if (ok.value().optIsNone()) {
                        return new CompileModuleResult(
                                new CompileRes(
                                        allLogs,
                                        new CompileResResult.Err(TList.of(new ExprErr(
                                                rootLoc,
                                                TList.of(new ExprSubErr.Opaque("Stage yields no expression")))))),
                                cix.localDependencies);
                    }
                    rootExpr = mustCast(Expr.class, ok.value().optUnwrap());
                }
            }
        }
    }

    private record LowerExcess(int excess, QExprId loc) {}

    private static Opt<LowerExcess> maxExcess(Opt<LowerExcess> a, Opt<LowerExcess> b) {
        if (a.optIsNone()) return b;
        if (b.optIsNone()) return a;
        return a.optUnwrap().excess() >= b.optUnwrap().excess() ? a : b;
    }

    private static Opt<LowerExcess> lowerExcess(Expr expr, int sd, int ld) {
        return switch (expr.variant()) {
            case Expr.Lower l -> {
                var newLd = ld + 1;
                Opt<LowerExcess> own = newLd > sd ? Opt.some(new LowerExcess(newLd - sd, expr.loc())) : Opt.none();
                yield maxExcess(own, lowerExcess(l.expr(), sd, newLd));
            }
            case Expr.Stage s -> lowerExcess(s.expr(), sd + 1, ld);
            case Expr.Graft g ->
                switch (g) {
                    case Expr.Graft.Pending p -> lowerExcess(p.expr(), sd, ld);
                    case Expr.Graft.Evaluated e -> Opt.none();
                };
            case Expr.Literal lit -> Opt.none();
            case Expr.ScopeUse u -> Opt.none();
            case Expr.Labeled lb -> lowerExcess(lb.inner(), sd, ld);
            case Expr.Return rt ->
                rt.value().optIsSome() ? lowerExcess(rt.value().optUnwrap(), sd, ld) : Opt.none();
            case Expr.As a -> maxExcess(lowerExcess(a.base(), sd, ld), lowerExcess(a.target(), sd, ld));
            case Expr.Call c -> maxExcess(lowerExcess(c.func(), sd, ld), lowerExcess(c.arg(), sd, ld));
            case Expr.Bind b -> maxExcess(lowerExcess(b.base(), sd, ld), lowerExcess(b.value(), sd, ld));
            case Expr.Set s -> maxExcess(lowerExcess(s.base(), sd, ld), lowerExcess(s.value(), sd, ld));
            case Expr.Field f -> maxExcess(lowerExcess(f.base(), sd, ld), lowerExcess(f.key(), sd, ld));
            case Expr.Seq s -> {
                Opt<LowerExcess> m = Opt.none();
                for (var sub : s.exprs()) m = maxExcess(m, lowerExcess(sub, sd, ld));
                yield m;
            }
            case Expr.Tuple t -> {
                Opt<LowerExcess> m = Opt.none();
                for (var sub : t.elements()) m = maxExcess(m, lowerExcess(sub, sd, ld));
                yield m;
            }
            case Expr.Record r -> {
                Opt<LowerExcess> m = Opt.none();
                for (var entry : r.entries()) {
                    m = maxExcess(m, lowerExcess(entry.key(), sd, ld));
                    m = maxExcess(m, lowerExcess(entry.value(), sd, ld));
                }
                yield m;
            }
            case Expr.If i -> {
                Opt<LowerExcess> m = Opt.none();
                for (var branch : i.conditions()) {
                    m = maxExcess(m, lowerExcess(branch.condition(), sd, ld));
                    m = maxExcess(m, lowerExcess(branch.body(), sd, ld));
                }
                var el = i.else_();
                if (el.optIsSome()) {
                    m = maxExcess(m, lowerExcess(el.optUnwrap(), sd, ld));
                }
                yield m;
            }
            case Expr.OperatorBinary op ->
                maxExcess(lowerExcess(op.base(), sd, ld), lowerExcess(op.reference(), sd, ld));
            case Expr.OperatorUnary op -> lowerExcess(op.base(), sd, ld);
            case Expr.Foreach f -> {
                var m = lowerExcess(f.base(), sd, ld);
                m = maxExcess(m, lowerExcess(f.elementKey(), sd, ld));
                m = maxExcess(m, lowerExcess(f.body(), sd, ld));
                yield m;
            }
            case Expr.Match m -> {
                var x = lowerExcess(m.base(), sd, ld);
                x = maxExcess(x, lowerExcess(m.elementKey(), sd, ld));
                for (var c : m.cases()) {
                    x = maxExcess(x, lowerExcess(c.condition(), sd, ld));
                    x = maxExcess(x, lowerExcess(c.body(), sd, ld));
                }
                var fb = m.fallback();
                if (fb.optIsSome()) {
                    x = maxExcess(x, lowerExcess(fb.optUnwrap(), sd, ld));
                }
                yield x;
            }
            case Expr.Try t -> {
                var m = lowerExcess(t.body(), sd, ld);
                var cc = t.catchClause();
                if (cc.optIsSome()) {
                    var c = cc.optUnwrap();
                    m = maxExcess(m, lowerExcess(c.bindingName(), sd, ld));
                    m = maxExcess(m, lowerExcess(c.exceptionType(), sd, ld));
                    m = maxExcess(m, lowerExcess(c.body(), sd, ld));
                }
                var fb = t.finallyBlock();
                if (fb.optIsSome()) {
                    m = maxExcess(m, lowerExcess(fb.optUnwrap(), sd, ld));
                }
                yield m;
            }
        };
    }

    public sealed interface EvalOutcome {

        record Ok(Opt<Value> value, TList<LogEntry> logs, boolean producedDynamicRoot) implements EvalOutcome {}

        record Err(TList<ExprErr> errors) implements EvalOutcome {}
    }
}
