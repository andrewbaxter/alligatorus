package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.value.source_function.SourceFunctionData;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import compilerxyz.util.TSet;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class DefSetFunctionsEmitter {
  private static final JavaMethodDescriptor MDESC_FUNCTION_METHOD =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT, compilerxyz.util.TList.of(JvmConstants.DESC_OBJECT));

  private DefSetFunctionsEmitter() {}

  public record FunctionSpec(
      long bindingIndex,
      BindingId.Local argId,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals) {}

  public record EmittedFunctions(
      String binaryName,
      String internalName,
      byte[] bytecode,
      TOrderedMap<String, Object> staticFieldValues) {}

  public static String functionsBinaryName(DefSetId defSet) {
    var compileId = defSet.compileId();
    return "compilerxyz.eval.gen.Functions$"
        + compileId.importState().value()
        + "_"
        + compileId.source().value()
        + "_"
        + defSet.index();
  }

  public static String functionsInternalName(DefSetId defSet) {
    return functionsBinaryName(defSet).replace('.', '/');
  }

  public static String functionMethodName(long bindingIndex) {
    return "_" + bindingIndex;
  }

  public static String functionFieldPrefix(long bindingIndex) {
    return "_" + bindingIndex + "_";
  }

  public static EmittedFunctions emit(
      DefSetId defSet,
      TList<FunctionSpec> specs,
      TOrderedMap<String, Object> builtinDynamics,
      TSet<BindingId.Def> staticFunctionDefs) {
    var binaryName = functionsBinaryName(defSet);
    var internalName = functionsInternalName(defSet);

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        internalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);

    var allFieldNames = new TOrderedSet<String>();
    var staticFieldValues = new TOrderedMap<String, Object>();

    for (var spec : specs) {
      var prefix = functionFieldPrefix(spec.bindingIndex());

      for (var builtinId : EvalRootEmitter.referencedBuiltinIds(spec.bodyCode())) {
        var name = prefix + Prefixes.BUILTIN_PREFIX + builtinId;
        if (allFieldNames.add(name)) {
          var v = builtinDynamics.get(Prefixes.BUILTIN_PREFIX + builtinId);
          if (v != null) {
            staticFieldValues.put(name, v);
          }
        }
      }

      for (int i = 0; i < spec.externals().size(); i++) {
        var entry = spec.externals().get(i);
        var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
        if (allFieldNames.add(name)) {
          staticFieldValues.put(name, entry.external().externalHostValue());
        }
      }

      for (var defId : EvalRootEmitter.referencedDefIds(spec.bodyCode())) {
        if (staticFunctionDefs.contains(defId)) {
          continue;
        }
        var name = prefix + ImportInstall.bindingIdDynamicKey(defId);
        allFieldNames.add(name);

      }
    }
    for (var name : allFieldNames) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              name,
              JvmConstants.DESC_OBJECT.value(),
              null,
              null)
          .visitEnd();
    }

    for (var spec : specs) {
      emitFunctionMethod(cw, internalName, spec, staticFunctionDefs);
    }

    cw.visitEnd();
    return new EmittedFunctions(binaryName, internalName, cw.toByteArray(), staticFieldValues);
  }

  private static void emitFunctionMethod(
      ClassWriter cw,
      String internalName,
      FunctionSpec spec,
      TSet<BindingId.Def> staticFunctionDefs) {
    var methodName = functionMethodName(spec.bindingIndex());
    var prefix = functionFieldPrefix(spec.bindingIndex());
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName,
            MDESC_FUNCTION_METHOD.value(),
            null,
            null);
    var target = new TargetDynamic(new Counter());
    target.registerStaticFunctionDefs(staticFunctionDefs);
    var ctx = new EmitCtx(internalName, 1, prefix);
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;

    var seq = JavaBytecodeUtils.seq();

    var argSlot = ctx.allocSlot(spec.argId());
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ALOAD, 0));
    seq.add(JavaBytecodeUtils.varInsn(loc, Opcodes.ASTORE, argSlot));

    var ownerInternal = JavaInternalName.of(internalName);
    for (int i = 0; i < spec.externals().size(); i++) {
      var entry = spec.externals().get(i);
      var fieldName = prefix + Prefixes.EXTERNAL_PREFIX + i;
      seq.add(
          JavaBytecodeUtils.accessStaticField(
              loc, ownerInternal, fieldName, JvmConstants.DESC_OBJECT));
      seq.add(
          new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }

    target.emit(seq, ctx, spec.bodyCode());
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ARETURN));
    JavaBytecodeUtils.renderMethod(seq, mv, TList.of());
  }
}
