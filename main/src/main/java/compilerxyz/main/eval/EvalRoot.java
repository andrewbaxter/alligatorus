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

public final class EvalRoot {
    private EvalRoot() {}

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
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", mdescRun.value(), null, null);
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
        JavaBytecodeUtils.renderMethodTyped(
                seq, mv, TList.of(), ownerInternal.value() + ".run", mdescRun, refs, locTable);
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
