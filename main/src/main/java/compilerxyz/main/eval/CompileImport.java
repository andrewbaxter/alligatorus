package compilerxyz.main.eval;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynException;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EmitCtx;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.Prefixes;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenBuiltinTree;
import compilerxyz.main.gen.Value;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import compilerxyz.prelude.util.TSet;
import java.util.ArrayDeque;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class CompileImport {
    private CompileImport() {}

    public record CompileImportResult(CompileRes res, TList<CompileId> localDeps) {}

    public static CompileImportResult compileImportId(
            GlobalContext gx,
            ImportStateId importState,
            Expr.Module source,
            TList<CompileId> compilePath,
            SourceSpecAbsolute currentSpec) {
        Expr rootExpr =
                switch (source) {
                    case Expr.Module.V1 v1 -> v1.expr();
                };
        var rootLoc = rootExpr.loc();
        var sourceId = rootLoc.source();
        var extendedPath = new TList<CompileId>(compilePath.size() + 1);
        extendedPath.addAll(compilePath);
        extendedPath.add(new CompileId(importState, sourceId));
        var cix = new CompileImportContext(gx, importState, sourceId, extendedPath, currentSpec);

        var excess = maxLowerExcess(rootExpr, 0, 0);
        for (var i = 0; i < excess; i++) {
            rootExpr = new Expr(rootLoc, new Expr.Stage(rootExpr));
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
                var evalBinaryName = "compilerxyz.main.eval.gen.EvalRoot$" + evalXx.vx.ix.evalRootCounter.next();
                evalVx.evalRootInternalName = EvalRootEmitter.toInternalName(evalBinaryName);
                var cvRes = ConstEval.constEval(evalXx, rootExpr);
                if (cvRes.hasErrors()) {
                    evalResult = new EvalOutcome.Err(cvRes.errorsOpt().optUnwrap());
                    break evalBlock;
                }
                var evalRes = cvRes.requireValue("Eval::eval constEval");
                var cv = evalRes.cv();
                Opt<CodeDynamicReg> stage1Code;
                Opt<Value> fallbackValue;
                Type valueType;
                if (cv.value().optIsNone()) {
                    var codeDynCast = CodeDynamic.castOpt(evalRootLoc, cv.code());
                    if (codeDynCast.hasErrors()) {
                        evalResult = new EvalOutcome.Err(codeDynCast.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    if (codeDynCast.requireValue("codeDyn").optIsNone()) {
                        evalResult = new EvalOutcome.Err(TList.of(new ExprErr(
                                evalRootLoc,
                                TList.of(new ExprSubErr.Opaque(
                                        "eval: const_eval produced no compile-time value and no dynamic code")))));
                        break evalBlock;
                    }
                    var codeDyn = codeDynCast.requireValue("codeDyn").optUnwrap();
                    var nullType = new TypeNullDynamic();
                    var nb = new JavaBytecodeBuilder(evalRootLoc);
                    nb.add(codeDyn);
                    stage1Code = Opt.some(nb.code(nb.constNull()));
                    fallbackValue = Opt.none();
                    valueType = nullType;
                } else {
                    var cvValue = cv.value().optUnwrap();
                    valueType = cvValue.valueType();
                    if (!DynUtil.dynamicHasCode(cv)) {
                        stage1Code = Opt.none();
                        fallbackValue = Opt.some(cvValue);
                    } else {
                        var dynamicRes = DynUtil.materialize(evalXx, evalRootLoc, cv);
                        if (dynamicRes.hasErrors()) {
                            evalResult =
                                    new EvalOutcome.Err(dynamicRes.errorsOpt().optUnwrap());
                            break evalBlock;
                        }
                        stage1Code = Opt.some(dynamicRes.requireValue("Eval::eval materialize"));
                        fallbackValue = Opt.none();
                    }
                }
                if (stage1Code.optIsNone() && evalVx.definitionSets.isEmpty()) {
                    if (fallbackValue.optIsNone()) {
                        throw new IllegalStateException("eval: no dynamic code but also no fallback value");
                    }
                    evalResult = new EvalOutcome.Ok(fallbackValue.optUnwrap(), new TList<>(), false);
                    break evalBlock;
                }
                var dependencyLoaders = new TList<DefinitionSetClassLoader>();
                var seenSets = new TSet<DefSetId>();
                var dss = new TList<DefinitionSet>();
                var toVisit = new ArrayDeque<DefinitionSet>();
                for (var ds : evalVx.definitionSets.values()) {
                    toVisit.push(ds);
                }
                defSetLoop:
                while (true) {
                    while (!toVisit.isEmpty()) {
                        var ds = toVisit.pop();
                        if (!seenSets.add(ds.defSetId)) {
                            continue;
                        }
                        var installRes = ds.install(evalXx, evalRootLoc);
                        if (installRes.hasErrors()) {
                            evalResult =
                                    new EvalOutcome.Err(installRes.errorsOpt().optUnwrap());
                            break evalBlock;
                        }
                        var installedLoader = installRes.requireValue("Eval::eval installDefSetToDynamic");
                        dependencyLoaders.add(installedLoader);
                        for (var dep : ds.dependencies.values()) {
                            if (!seenSets.contains(dep.defSetId)) {
                                toVisit.push(dep);
                            }
                        }
                        dss.add(ds);
                    }
                    boolean foundNew = false;
                    for (var ds : evalVx.definitionSets.values()) {
                        if (!seenSets.contains(ds.defSetId)) {
                            toVisit.push(ds);
                            foundNew = true;
                        }
                    }
                    if (!foundNew) {
                        break defSetLoop;
                    }
                }
                Object rawResult;
                try {
                    if (stage1Code.optIsNone()) {
                        if (fallbackValue.optIsNone()) {
                            throw new IllegalStateException("eval: no stage1 code and no fallback value");
                        }
                        evalResult = new EvalOutcome.Ok(fallbackValue.optUnwrap(), evalVx.logs, false);
                        break evalBlock;
                    }
                    var internalName = EvalRootEmitter.toInternalName(evalBinaryName);
                    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS) {
                        @Override
                        protected String getCommonSuperClass(String type1, String type2) {
                            return JvmConstants.INTNAME_OBJECT.value();
                        }
                    };
                    cw.visit(
                            Opcodes.V11,
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                            internalName,
                            null,
                            JvmConstants.INTNAME_OBJECT.value(),
                            null);
                    var fieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
                    fieldDescriptors.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, EvalRootEmitter.DESC_EXPR_CONTEXT);
                    var externalFieldNames = new TList<String>(evalVx.externals.size());
                    var externalFieldDescs = new TList<JavaDataDescriptor>(evalVx.externals.size());
                    for (int ereI = 0; ereI < evalVx.externals.size(); ereI++) {
                        var ereEntry = evalVx.externals.get(ereI);
                        var fieldName = Prefixes.EXTERNAL_PREFIX + ereI;
                        externalFieldNames.add(fieldName);
                        var desc =
                                JavaDataDescriptor.fromClass(ereEntry.external().externalHostClass());
                        externalFieldDescs.add(desc);
                        fieldDescriptors.put(fieldName, desc);
                    }
                    for (var fieldEntry : fieldDescriptors.entrySet()) {
                        cw.visitField(
                                        Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                        fieldEntry.getKey(),
                                        fieldEntry.getValue().value(),
                                        null,
                                        null)
                                .visitEnd();
                    }
                    var mdescRun = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of());
                    var mv = cw.visitMethod(
                            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", mdescRun.value(), null, null);
                    var ctx = new EmitCtx(internalName, 0);
                    var ereSeq = new JavaBytecodeSequence();
                    var ownerInternal = JavaInternalName.of(internalName);
                    {
                        var pb = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                        for (int i = 0; i < evalVx.externals.size(); i++) {
                            var entry = evalVx.externals.get(i);
                            pb.store(
                                    entry.key(),
                                    pb.getStatic(ownerInternal, externalFieldNames.get(i), externalFieldDescs.get(i)));
                        }
                        ereSeq.add(pb.code());
                    }
                    var stage1 = stage1Code.optUnwrap();
                    evalTarget.emit(ereSeq, ctx, stage1.code());
                    {
                        var rb = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                        var result = stage1.reg();
                        if (!result.desc.isReference()) {
                            throw new IllegalStateException("eval root: stage-1 result is not a reference");
                        }
                        rb.ret(result);
                        ereSeq.add(rb.code());
                    }
                    JavaBytecodeUtils.renderMethodTyped(
                            ereSeq,
                            mv,
                            TList.of(),
                            ownerInternal.value() + ".run",
                            JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of()));
                    cw.visitEnd();
                    var bytecode = cw.toByteArray();
                    var staticFields = new TOrderedMap<String, Object>();
                    for (int i = 0; i < evalVx.externals.size(); i++) {
                        staticFields.put(
                                externalFieldNames.get(i),
                                evalVx.externals.get(i).external().externalHostValue());
                    }
                    if (evalXx.vx.ix.classLoader.optIsNone()) {
                        evalXx.vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                                evalXx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
                    }
                    var loader = evalXx.vx.ix.classLoader.optUnwrap();
                    loader.registerClass(evalBinaryName, bytecode);
                    var rootStaticFields = new TOrderedMap<String, Object>();
                    rootStaticFields.putAll(staticFields);
                    rootStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, evalXx);
                    loader.registerExternals(evalBinaryName, rootStaticFields);
                    Class<?> rootClass;
                    try {
                        rootClass = Class.forName(evalBinaryName, true, loader);
                    } catch (ClassNotFoundException e) {
                        throw new AssertionError("Generated root class " + evalBinaryName + " failed to load", e);
                    }
                    try {
                        var runMethod = rootClass.getDeclaredMethod("run");
                        rawResult = runMethod.invoke(null);
                    } catch (ReflectiveOperationException e) {
                        var cause = e.getCause();
                        if (cause instanceof DynPanic panic) {
                            evalResult = new EvalOutcome.Err(TList.of(new ExprErr(
                                    evalRootLoc, TList.of(new ExprSubErr.Opaque("panic: " + panic.getMessage())))));
                            break evalBlock;
                        }
                        if (cause instanceof DynCompileErrors compileErrs) {
                            evalResult = new EvalOutcome.Err(compileErrs.errors);
                            break evalBlock;
                        }
                        if (cause instanceof DynException dynEx) {
                            var rendered = dynEx.payload == null ? "null" : dynEx.payload.toString();
                            evalResult = new EvalOutcome.Err(
                                    TList.of(new ExprErr(evalRootLoc, TList.of(new ExprSubErr.Opaque(rendered)))));
                            break evalBlock;
                        }
                        if (cause instanceof RuntimeException re) {
                            throw re;
                        }
                        var causeMsg =
                                cause == null ? e.toString() : (cause.getClass().getName() + ": " + cause.getMessage());
                        throw new AssertionError(
                                "Generated root class " + evalBinaryName + " invoke failed: " + causeMsg, e);
                    }
                } finally {
                }
                Value finalValue;
                if (fallbackValue.optIsSome()) {
                    finalValue = fallbackValue.optUnwrap();
                } else {
                    var liftRes = GenAspectLift.get(evalXx, evalRootLoc, valueType);
                    if (liftRes.hasErrors()) {
                        evalResult = new EvalOutcome.Err(liftRes.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    var liftAspect = liftRes.requireValue("Eval::eval lift aspect");
                    var liftedRes = liftAspect.aspectLiftLift(evalXx, evalRootLoc, Opt.ofNullable(rawResult));
                    if (liftedRes.hasErrors()) {
                        evalResult = new EvalOutcome.Err(liftedRes.errorsOpt().optUnwrap());
                        break evalBlock;
                    }
                    finalValue = liftedRes.requireValue("Eval::eval lift result");
                }
                evalResult = new EvalOutcome.Ok(finalValue, evalVx.logs, true);
            }
            switch (evalResult) {
                case EvalOutcome.Err err -> {
                    return new CompileImportResult(
                            new CompileRes(allLogs, new CompileResResult.Err(err.errors())), cix.localDependencies);
                }
                case EvalOutcome.Ok ok -> {
                    allLogs.addAll(ok.logs());
                    if (!isStage) {
                        if (compilePath.isEmpty() && ok.producedDynamicRoot()) {
                            return new CompileImportResult(
                                    new CompileRes(
                                            allLogs,
                                            new CompileResResult.Ok(new Artifact.Body(new ArtifactBody(
                                                    new JsonValueNull(), new ArtifactBodyExporter.Root())))),
                                    cix.localDependencies);
                        }
                        var vx = new EvalContext(cix);
                        var rootScope = new Scope(vx);
                        var target = new TargetDynamic();
                        var xx = new ExprContext(rootScope, target, vx, importState, Opt.some(new TypeStringDynamic()));
                        var exportErrors = new TList<ExprErr>();
                        var exportArt = Artifactable.exportArtifact(xx, rootLoc, ok.value());
                        if (exportArt.hasErrors()) {
                            exportArt.drainInto(exportErrors);
                            return new CompileImportResult(
                                    new CompileRes(allLogs, new CompileResResult.Err(exportErrors)),
                                    cix.localDependencies);
                        }
                        var artifact = exportArt.requireValue("ArtifactSupport::exportArtifact artifact");
                        return new CompileImportResult(
                                new CompileRes(allLogs, new CompileResResult.Ok(artifact)), cix.localDependencies);
                    }
                    rootExpr = mustCast(Expr.class, ok.value());
                }
            }
        }
    }

    private static int maxLowerExcess(Expr expr, int sd, int ld) {
        return switch (expr.variant()) {
            case Expr.Lower l -> {
                var newLd = ld + 1;
                var excess = Math.max(0, newLd - sd);
                yield Math.max(excess, maxLowerExcess(l.expr(), sd, newLd));
            }
            case Expr.Stage s -> maxLowerExcess(s.expr(), sd + 1, ld);
            case Expr.Graft g ->
                switch (g) {
                    case Expr.Graft.Pending p -> maxLowerExcess(p.expr(), sd, ld);
                    case Expr.Graft.Evaluated e -> 0;
                };
            case Expr.Literal lit -> 0;
            case Expr.ScopeUse u -> 0;
            case Expr.Labeled lb -> maxLowerExcess(lb.inner(), sd, ld);
            case Expr.Return rt ->
                rt.value().optIsSome() ? maxLowerExcess(rt.value().optUnwrap(), sd, ld) : 0;
            case Expr.As a -> Math.max(maxLowerExcess(a.base(), sd, ld), maxLowerExcess(a.target(), sd, ld));
            case Expr.Call c -> Math.max(maxLowerExcess(c.func(), sd, ld), maxLowerExcess(c.arg(), sd, ld));
            case Expr.Bind b -> Math.max(maxLowerExcess(b.base(), sd, ld), maxLowerExcess(b.value(), sd, ld));
            case Expr.Set s -> Math.max(maxLowerExcess(s.base(), sd, ld), maxLowerExcess(s.value(), sd, ld));
            case Expr.Field f -> Math.max(maxLowerExcess(f.base(), sd, ld), maxLowerExcess(f.key(), sd, ld));
            case Expr.Seq s -> {
                var m = 0;
                for (var sub : s.exprs()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
                yield m;
            }
            case Expr.Tuple t -> {
                var m = 0;
                for (var sub : t.elements()) m = Math.max(m, maxLowerExcess(sub, sd, ld));
                yield m;
            }
            case Expr.Record r -> {
                var m = 0;
                for (var entry : r.entries()) {
                    m = Math.max(m, maxLowerExcess(entry.key(), sd, ld));
                    m = Math.max(m, maxLowerExcess(entry.value(), sd, ld));
                }
                yield m;
            }
            case Expr.If i -> {
                var m = 0;
                for (var branch : i.conditions()) {
                    m = Math.max(m, maxLowerExcess(branch.condition(), sd, ld));
                    m = Math.max(m, maxLowerExcess(branch.body(), sd, ld));
                }
                var el = i.else_();
                if (el.optIsSome()) {
                    m = Math.max(m, maxLowerExcess(el.optUnwrap(), sd, ld));
                }
                yield m;
            }
            case Expr.OperatorBinary op ->
                Math.max(maxLowerExcess(op.base(), sd, ld), maxLowerExcess(op.reference(), sd, ld));
            case Expr.OperatorUnary op -> maxLowerExcess(op.base(), sd, ld);
            case Expr.Foreach f -> {
                var m = maxLowerExcess(f.base(), sd, ld);
                m = Math.max(m, maxLowerExcess(f.elementKey(), sd, ld));
                m = Math.max(m, maxLowerExcess(f.body(), sd, ld));
                yield m;
            }
            case Expr.Match m -> {
                var x = maxLowerExcess(m.base(), sd, ld);
                x = Math.max(x, maxLowerExcess(m.elementKey(), sd, ld));
                for (var c : m.cases()) {
                    x = Math.max(x, maxLowerExcess(c.condition(), sd, ld));
                    x = Math.max(x, maxLowerExcess(c.body(), sd, ld));
                }
                var fb = m.fallback();
                if (fb.optIsSome()) {
                    x = Math.max(x, maxLowerExcess(fb.optUnwrap(), sd, ld));
                }
                yield x;
            }
            case Expr.Try t -> {
                var m = maxLowerExcess(t.body(), sd, ld);
                var cc = t.catchClause();
                if (cc.optIsSome()) {
                    var c = cc.optUnwrap();
                    m = Math.max(m, maxLowerExcess(c.bindingName(), sd, ld));
                    m = Math.max(m, maxLowerExcess(c.exceptionType(), sd, ld));
                    m = Math.max(m, maxLowerExcess(c.body(), sd, ld));
                }
                var fb = t.finallyBlock();
                if (fb.optIsSome()) {
                    m = Math.max(m, maxLowerExcess(fb.optUnwrap(), sd, ld));
                }
                yield m;
            }
        };
    }

    public sealed interface EvalOutcome {

        record Ok(Value value, TList<LogEntry> logs, boolean producedDynamicRoot) implements EvalOutcome {}

        record Err(TList<ExprErr> errors) implements EvalOutcome {}
    }
}
