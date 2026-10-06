package compilerxyz.main.eval;

import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynCompileErrors;
import compilerxyz.main.eval.dynamic.DynException;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeLocTable;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.Prefixes;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import compilerxyz.prelude.util.TSet;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayDeque;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class EvalRoot {
    private EvalRoot() {}

    public static String newBinaryName(ExprContext xx) {
        var binaryName = "compilerxyz.main.eval.gen.EvalRoot$" + xx.vx.ix.evalRootCounter.next();
        xx.vx.evalRootInternalName = Opt.some(EvalRootEmitter.toInternalName(binaryName));
        return binaryName;
    }

    public static EResult<TList<DefinitionSetClassLoader>> installDefinitionSets(ExprContext xx, QExprId loc) {
        var dependencyLoaders = new TList<DefinitionSetClassLoader>();
        var seenSets = new TSet<DefSetId>();
        var toVisit = new ArrayDeque<DefinitionSet>();
        for (var ds : xx.vx.definitionSets.values()) {
            toVisit.push(ds);
        }
        while (true) {
            while (!toVisit.isEmpty()) {
                var ds = toVisit.pop();
                if (!seenSets.add(ds.defSetId)) {
                    continue;
                }
                var installRes = ds.install(xx, loc);
                if (installRes.hasErrors()) return installRes.reerr();
                dependencyLoaders.add(installRes.requireValue("EvalRoot::installDefinitionSets install"));
                for (var dep : ds.dependencies.values()) {
                    if (!seenSets.contains(dep.defSetId)) {
                        toVisit.push(dep);
                    }
                }
            }
            boolean foundNew = false;
            for (var ds : xx.vx.definitionSets.values()) {
                if (!seenSets.contains(ds.defSetId)) {
                    toVisit.push(ds);
                    foundNew = true;
                }
            }
            if (!foundNew) {
                return EResult.ok(dependencyLoaders);
            }
        }
    }

    public static EResult<Object> run(
            ExprContext xx,
            QExprId loc,
            String binaryName,
            CodeDynamicReg code,
            TList<DefinitionSetClassLoader> dependencyLoaders) {
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
        var fieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
        fieldDescriptors.put(EvalRootEmitter.EXPR_CONTEXT_FIELD, EvalRootEmitter.DESC_EXPR_CONTEXT);
        var externalFieldNames = new TList<String>(vx.externals.size());
        for (int i = 0; i < vx.externals.size(); i++) {
            var fieldName = Prefixes.EXTERNAL_PREFIX + i;
            externalFieldNames.add(fieldName);
            fieldDescriptors.put(
                    fieldName,
                    JavaDataDescriptor.fromClass(vx.externals.get(i).external().externalHostClass()));
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
        var mv = cw.visitMethod(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "run", mdescRun.value(), null, null);
        var seq = new JavaBytecodeSequence();
        var ownerInternal = JavaInternalName.of(internalName);
        var externalFields = JavaBytecodeUtils.externalFields(ownerInternal, "", vx.externals);
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
                seq, mv, TList.of(), ownerInternal.value() + ".run", mdescRun, externalFields, locTable);
        cw.visitEnd();
        var bytecode = cw.toByteArray();
        if (vx.ix.classLoader.optIsNone()) {
            vx.ix.classLoader = Opt.some(new DefinitionSetClassLoader(
                    vx.ix.gx, DefinitionSetClassLoader.class.getClassLoader(), dependencyLoaders));
        }
        var loader = vx.ix.classLoader.optUnwrap();
        loader.registerClass(binaryName, bytecode);
        loader.registerLocs(binaryName, locTable.locs());
        var rootStaticFields = new TOrderedMap<String, Object>();
        for (int i = 0; i < vx.externals.size(); i++) {
            rootStaticFields.put(
                    externalFieldNames.get(i), vx.externals.get(i).external().externalHostValue());
        }
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
        return EResult.err(at, "uncaught " + cause);
    }
}
