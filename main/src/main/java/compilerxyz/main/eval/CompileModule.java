package compilerxyz.main.eval;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeLocTable;
import compilerxyz.main.eval.dynamic.JavaBytecodeRenderRefs;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenBuiltinTree;
import compilerxyz.main.gen.Value;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.InvocationTargetException;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.IincInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.InvokeDynamicInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LookupSwitchInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MultiANewArrayInsnNode;
import org.objectweb.asm.tree.TableSwitchInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class CompileModule {
    private CompileModule() {}

    public record CompileModuleResult(
            CompileRes res, TList<CompileId> localDeps, TList<Compiles.LocalMeta> localData) {}

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

        var startMs = System.currentTimeMillis();
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
                        case Expr.Loop lp -> false;
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
                evalXx.vx.evalRootInternalName = Opt.some(EvalRootEmitter.toInternalName(evalBinaryName));
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
                if (stage1Code.optIsNone()) {
                    evalResult = new EvalOutcome.Ok(fallbackValue, new TList<>());
                    break evalBlock;
                }
                Object rawResult;
                {
                    var code = stage1Code.optUnwrap();
                    var internalName = EvalRootEmitter.toInternalName(evalBinaryName);
                    var locTable = new JavaBytecodeLocTable();
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
                    cw.visitField(
                                    Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
                                    EvalRootEmitter.EXPR_CONTEXT_FIELD,
                                    EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
                                    null,
                                    null)
                            .visitEnd();
                    var mdescRun = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of());
                    var seq = new JavaBytecodeSequence();
                    var ownerInternal = JavaInternalName.of(internalName);
                    var refs = new JavaBytecodeRenderRefs(ownerInternal, "");
                    seq.add(code.code());
                    {
                        var rb = new JavaBytecodeBuilder(CodeLoc.Infallible.INSTANCE);
                        var result = code.reg();
                        if (!result.desc.isReference()) {
                            throw new IllegalStateException("eval root: result is not a reference");
                        }
                        rb.ret(result);
                        seq.add(rb.code());
                    }
                    var context = ownerInternal.value() + ".run";
                    var layout = seq.layout(refs, locTable);
                    var whole = seq.emit(layout, 0, layout.size(), TList.of(), refs, Opt.none());
                    var offsets = new int[whole.node().instructions.size() + 1];
                    {
                        int at = 0;
                        int i = 0;
                        for (var insn : whole.node().instructions) {
                            offsets[i++] = at;
                            at += switch (insn) {
                                case LabelNode l -> 0;
                                case LineNumberNode l -> 0;
                                case FrameNode f -> 0;
                                case InsnNode n -> 1;
                                case IntInsnNode n -> n.getOpcode() == Opcodes.SIPUSH ? 3 : 2;
                                case VarInsnNode n -> n.var < 256 ? 2 : 4;
                                case TypeInsnNode n -> 3;
                                case FieldInsnNode n -> 3;
                                case MethodInsnNode n -> n.getOpcode() == Opcodes.INVOKEINTERFACE ? 5 : 3;
                                case InvokeDynamicInsnNode n -> 5;
                                case JumpInsnNode n -> 3;
                                case LdcInsnNode n -> 3;
                                case IincInsnNode n ->
                                    n.var < 256 && n.incr >= Byte.MIN_VALUE && n.incr <= Byte.MAX_VALUE ? 3 : 6;
                                case TableSwitchInsnNode n -> 16 + 4 * n.labels.size();
                                case LookupSwitchInsnNode n -> 12 + 8 * n.keys.size();
                                case MultiANewArrayInsnNode n -> 4;
                                default ->
                                    throw new IllegalStateException("unsized instruction node "
                                            + insn.getClass().getName());
                            };
                        }
                        offsets[i] = at;
                    }
                    if (offsets[offsets.length - 1] <= 65535) {
                        JavaBytecodeUtils.renderMethodNode(cw, "run", mdescRun, whole.node(), context);
                    } else {
                        var itemBytes = new int[whole.itemInsnIndex().length];
                        for (int i = 0; i < itemBytes.length; i++) itemBytes[i] = offsets[whole.itemInsnIndex()[i]];
                        var cuts = seq.chunkCuts(layout, itemBytes, 32000);
                        var fields = seq.chunkFields(layout, cuts, refs);
                        var starts = new TList<Integer>();
                        starts.add(0);
                        starts.addAll(cuts);
                        var driver = cw.visitMethod(
                                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", mdescRun.value(), null, null);
                        for (int k = 0; k < starts.size(); k++) {
                            boolean last = k == starts.size() - 1;
                            int from = starts.get(k);
                            int to = last ? layout.size() : starts.get(k + 1);
                            var part = seq.emit(layout, from, to, TList.of(), refs, Opt.some(fields));
                            if (!last) part.node().instructions.add(new InsnNode(Opcodes.RETURN));
                            var chunkName = "run$" + k;
                            var chunkDesc = last ? mdescRun : JavaMethodDescriptor.VOID_NOARGS;
                            JavaBytecodeUtils.renderMethodNode(
                                    cw, chunkName, chunkDesc, part.node(), context + "$" + k);
                            driver.visitMethodInsn(
                                    Opcodes.INVOKESTATIC, internalName, chunkName, chunkDesc.value(), false);
                        }
                        driver.visitInsn(Opcodes.ARETURN);
                        driver.visitMaxs(0, 0);
                        driver.visitEnd();
                    }
                    var rootStaticFields = new TOrderedMap<String, Object>();
                    refs.declareFields(cw, rootStaticFields);
                    cw.visitEnd();
                    var bytecode = cw.toByteArray();
                    var dependencyLoaders = new TList<DefinitionSetClassLoader>();
                    for (var ds : refs.defSets().values()) {
                        var installRes = ds.install(evalXx, evalRootLoc);
                        if (installRes.hasErrors()) {
                            evalResult =
                                    new EvalOutcome.Err(installRes.errorsOpt().optUnwrap());
                            break evalBlock;
                        }
                        dependencyLoaders.add(installRes.requireValue("EvalRoot::run install"));
                    }
                    if (evalXx.vx.ix.classLoader.optIsNone()) {
                        evalXx.vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                                evalXx.vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
                    }
                    var loader = evalXx.vx.ix.classLoader.optUnwrap();
                    loader.registerClass(evalBinaryName, bytecode);
                    loader.registerLocs(evalBinaryName, locTable.locs());
                    rootStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, evalXx);
                    loader.registerExternals(evalBinaryName, rootStaticFields);
                    Class<?> rootClass;
                    try {
                        rootClass = Class.forName(evalBinaryName, true, loader);
                    } catch (ClassNotFoundException e) {
                        throw new AssertionError("Generated root class " + evalBinaryName + " failed to load", e);
                    }
                    Object result;
                    try {
                        result = rootClass.getDeclaredMethod("run").invoke(null);
                    } catch (InvocationTargetException e) {
                        var thrown =
                                EvalRoot.thrownErrors(loader, evalRootLoc, e, "Generated root class " + evalBinaryName);
                        evalResult = new EvalOutcome.Err(thrown.errorsOpt().optUnwrap());
                        break evalBlock;
                    } catch (ReflectiveOperationException e) {
                        throw new AssertionError("Generated root class " + evalBinaryName + " invoke failed", e);
                    }
                    rawResult = result;
                }
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
                evalResult = new EvalOutcome.Ok(finalValue, evalVx.logs);
            }
            switch (evalResult) {
                case EvalOutcome.Err err -> {
                    return new CompileModuleResult(
                            new CompileRes(
                                    startMs,
                                    System.currentTimeMillis(),
                                    allLogs,
                                    new CompileResResult.Err(err.errors())),
                            cix.localDependencies,
                            cix.localData);
                }
                case EvalOutcome.Ok ok -> {
                    allLogs.addAll(ok.logs());
                    if (!isStage) {
                        var vx = new EvalContext(cix);
                        var rootScope = new Scope(vx);
                        var target = new TargetDynamic();
                        var xx = new ExprContext(rootScope, target, vx, importState, Opt.some(new TypeStringDynamic()));
                        if (ok.value().optIsNone()) {
                            return new CompileModuleResult(
                                    new CompileRes(
                                            startMs,
                                            System.currentTimeMillis(),
                                            allLogs,
                                            new CompileResResult.Ok(Opt.none())),
                                    cix.localDependencies,
                                    cix.localData);
                        }
                        var exportErrors = new TList<ExprErr>();
                        var exportArt =
                                Exporters.exportValue(xx, rootLoc, ok.value().optUnwrap());
                        if (exportArt.hasErrors()) {
                            exportArt.drainInto(exportErrors);
                            return new CompileModuleResult(
                                    new CompileRes(
                                            startMs,
                                            System.currentTimeMillis(),
                                            allLogs,
                                            new CompileResResult.Err(exportErrors)),
                                    cix.localDependencies,
                                    cix.localData);
                        }
                        var artifact = exportArt.requireValue("ArtifactSupport::exportArtifact artifact");
                        return new CompileModuleResult(
                                new CompileRes(
                                        startMs,
                                        System.currentTimeMillis(),
                                        allLogs,
                                        new CompileResResult.Ok(Opt.some(artifact))),
                                cix.localDependencies,
                                cix.localData);
                    }
                    if (ok.value().optIsNone()) {
                        return new CompileModuleResult(
                                new CompileRes(
                                        startMs,
                                        System.currentTimeMillis(),
                                        allLogs,
                                        new CompileResResult.Err(TList.of(new ExprErr(
                                                rootLoc,
                                                TList.of(new ExprSubErr.Opaque("Stage yields no expression")))))),
                                cix.localDependencies,
                                cix.localData);
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
            case Expr.Loop lp -> lowerExcess(lp.body(), sd, ld);
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

        record Ok(Opt<Value> value, TList<LogEntry> logs) implements EvalOutcome {}

        record Err(TList<ExprErr> errors) implements EvalOutcome {}
    }
}
