package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynException;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeLocTable;
import compilerxyz.main.eval.dynamic.JavaBytecodeRenderRefs;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
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

public final class EvalRoot {
    private EvalRoot() {}

    private static final int MAX_METHOD_CODE = 65535;

    private static final int CHUNK_BUDGET = 32000;

    public static String newBinaryName(ExprContext xx) {
        var binaryName = "compilerxyz.main.eval.gen.EvalRoot$" + xx.vx.ix.evalRootCounter.next();
        xx.vx.evalRootInternalName = Opt.some(EvalRootEmitter.toInternalName(binaryName));
        return binaryName;
    }

    public static EResult<Object> run(ExprContext xx, QExprId loc, String binaryName, CodeDynamicReg code) {
        var vx = xx.vx;
        var internalName = EvalRootEmitter.toInternalName(binaryName);
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
                    case IincInsnNode n -> n.var < 256 && n.incr >= Byte.MIN_VALUE && n.incr <= Byte.MAX_VALUE ? 3 : 6;
                    case TableSwitchInsnNode n -> 16 + 4 * n.labels.size();
                    case LookupSwitchInsnNode n -> 12 + 8 * n.keys.size();
                    case MultiANewArrayInsnNode n -> 4;
                    default ->
                        throw new IllegalStateException(
                                "unsized instruction node " + insn.getClass().getName());
                };
            }
            offsets[i] = at;
        }
        if (offsets[offsets.length - 1] <= MAX_METHOD_CODE) {
            JavaBytecodeUtils.renderMethodNode(cw, "run", mdescRun, whole.node(), context);
        } else {
            var itemBytes = new int[whole.itemInsnIndex().length];
            for (int i = 0; i < itemBytes.length; i++) itemBytes[i] = offsets[whole.itemInsnIndex()[i]];
            var cuts = seq.chunkCuts(layout, itemBytes, CHUNK_BUDGET);
            var fields = seq.chunkFields(layout, cuts, refs);
            var starts = new TList<Integer>();
            starts.add(0);
            starts.addAll(cuts);
            var driver = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", mdescRun.value(), null, null);
            for (int k = 0; k < starts.size(); k++) {
                boolean last = k == starts.size() - 1;
                int from = starts.get(k);
                int to = last ? layout.size() : starts.get(k + 1);
                var part = seq.emit(layout, from, to, TList.of(), refs, Opt.some(fields));
                if (!last) part.node().instructions.add(new InsnNode(Opcodes.RETURN));
                var chunkName = "run$" + k;
                var chunkDesc = last ? mdescRun : JavaMethodDescriptor.VOID_NOARGS;
                JavaBytecodeUtils.renderMethodNode(cw, chunkName, chunkDesc, part.node(), context + "$" + k);
                driver.visitMethodInsn(Opcodes.INVOKESTATIC, internalName, chunkName, chunkDesc.value(), false);
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
            var installRes = ds.install(xx, loc);
            if (installRes.hasErrors()) return installRes.reerr();
            dependencyLoaders.add(installRes.requireValue("EvalRoot::run install"));
        }
        if (vx.ix.classLoader.optIsNone()) {
            vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                    vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
        }
        var loader = vx.ix.classLoader.optUnwrap();
        loader.registerClass(binaryName, bytecode);
        loader.registerLocs(binaryName, locTable.locs());
        rootStaticFields.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, xx);
        loader.registerExternals(binaryName, rootStaticFields);
        Class<?> rootClass;
        try {
            rootClass = Class.forName(binaryName, true, loader);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("Generated root class " + binaryName + " failed to load", e);
        }
        Object result;
        try {
            result = rootClass.getDeclaredMethod("run").invoke(null);
        } catch (InvocationTargetException e) {
            return thrownErrors(loader, loc, e, "Generated root class " + binaryName);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Generated root class " + binaryName + " invoke failed", e);
        }
        return EResult.ok(result);
    }

    public static <T> EResult<T> thrownErrors(
            DefinitionSetClassLoader loader, QExprId loc, InvocationTargetException e, String what) {
        var cause = e.getCause();
        if (cause == null) {
            throw new AssertionError(what + " threw without a cause", e);
        }
        if (cause instanceof DynCompileErrors compileErrs) {
            return EResult.errs(compileErrs.errors);
        }
        var at = loc;
        for (var frame : cause.getStackTrace()) {
            var found = loader.locate(frame);
            if (found.optIsSome()) {
                at = found.optUnwrap();
                break;
            }
        }
        if (cause instanceof DynPanic panic) {
            return EResult.err(at, "panic: " + panic.getMessage());
        }
        if (cause instanceof DynException dynEx) {
            return EResult.err(at, dynEx.payload == null ? "null" : dynEx.payload.toString());
        }
        var trace = new StringBuilder("uncaught ").append(cause);
        for (var frame : cause.getStackTrace()) trace.append("\n    at ").append(frame);
        return EResult.err(at, trace.toString());
    }
}
