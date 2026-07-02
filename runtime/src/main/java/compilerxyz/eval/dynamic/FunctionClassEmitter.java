package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ImportInstall;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import compilerxyz.util.TOrderedSet;
import java.util.concurrent.atomic.AtomicLong;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class FunctionClassEmitter {
  private static final AtomicLong COUNTER = new AtomicLong();

  private static final JavaInternalName INTNAME_CONTEXTUAL_FUNCTION =
      JavaInternalName.of("compilerxyz/eval/ContextualFunction");
  private static final JavaInternalName INTNAME_EXPR_CONTEXT =
      JavaInternalName.of("compilerxyz/eval/ExprContext");
  private static final JavaInternalName INTNAME_OBJECT_ARRAY_ELEM =
      JavaInternalName.of("[Ljava/lang/Object;");

  private static final JavaMethodDescriptor MDESC_OBJECT_INIT = JavaMethodDescriptor.VOID_NOARGS;
  private static final JavaMethodDescriptor MDESC_MAP_GET =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);
  private static final JavaMethodDescriptor MDESC_CONTEXTUAL_APPLY =
      JavaMethodDescriptor.fromParts(
          JvmConstants.DESC_OBJECT,
          JavaDataDescriptor.fromInternalName(INTNAME_EXPR_CONTEXT),
          JvmConstants.DESC_OBJECT);

  private FunctionClassEmitter() {}

  public record EmittedFunction(
      String binaryName,
      String internalName,
      byte[] bytecode,
      TOrderedMap<String, Object> staticFieldValues) {}

  public static EmittedFunction emit(
      TargetDynamic target,
      BindingId.Local argId,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, Object> builtinDynamics) {
    return emit(target, null, null, argId, bodyCode, externals, builtinDynamics);
  }

  public static EmittedFunction emit(
      TargetDynamic target,
      BindingId.@org.jspecify.annotations.Nullable Local selfId,
      @org.jspecify.annotations.Nullable String dataField,
      BindingId.@org.jspecify.annotations.Nullable Local argId,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, Object> builtinDynamics) {
    var ids = argId == null ? new TList<BindingId.Local>() : TList.of(argId);
    return emitGeneric(target, selfId, dataField, ids, false, bodyCode, externals, builtinDynamics);
  }

  public static EmittedFunction emitGeneric(
      TargetDynamic target,
      BindingId.@org.jspecify.annotations.Nullable Local selfId,
      @org.jspecify.annotations.Nullable String dataField,
      TList<BindingId.Local> argParamIds,
      boolean isArgMethod,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals,
      TOrderedMap<String, Object> builtinDynamics) {
    var index = COUNTER.getAndIncrement();
    var binaryName = "FuncImpl$" + index;
    var internalName = binaryName.replace('.', '/');

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        internalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        new String[] {INTNAME_CONTEXTUAL_FUNCTION.value()});

    var fieldNames = new TOrderedSet<String>();
    fieldNames.addAll(builtinDynamics.keySet());
    var externalFieldNames = new TOrderedMap<BindingId, String>();
    for (var entry : externals) {
      var fieldName = ImportInstall.bindingIdDynamicKey(entry.id());
      externalFieldNames.put(entry.id(), fieldName);
      fieldNames.add(fieldName);
    }
    for (var def : EvalRootEmitter.referencedDefIds(bodyCode)) {
      fieldNames.add(ImportInstall.bindingIdDynamicKey(def));
    }
    for (var name : fieldNames) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              name,
              JvmConstants.DESC_OBJECT.value(),
              null,
              null)
          .visitEnd();
    }

    {
      var ctor =
          cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", MDESC_OBJECT_INIT.value(), null, null);
      ctor.visitCode();
      var ctorSeq = new JavaBytecodeSequence();
      ctorSeq.varInsn(Opcodes.ALOAD, 0);
      ctorSeq.invokeSpecial(JvmConstants.INTNAME_OBJECT, "<init>", MDESC_OBJECT_INIT);
      ctorSeq.insn(Opcodes.RETURN);
      ctorSeq.render(ctor, TList.of());
      ctor.visitMaxs(0, 0);
      ctor.visitEnd();
    }

    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC,
            "contextualFunctionApplyCtx",
            MDESC_CONTEXTUAL_APPLY.value(),
            null,
            null);
    var ctx = new EmitCtx(internalName, 1, 3);
    var seq = new JavaBytecodeSequence();
    if (isArgMethod) {
      if (selfId == null) {
        throw new IllegalStateException(
            "FunctionClassEmitter.emit: selfId required when isArgMethod is true");
      }
      if (!argParamIds.isEmpty()) {
        throw new IllegalStateException(
            "FunctionClassEmitter.emit: argParamIds must be empty when isArgMethod is true");
      }

      var selfSlot = ctx.allocSlot(selfId);
      seq.varInsn(Opcodes.ALOAD, 2);
      seq.checkCast(INTNAME_OBJECT_ARRAY_ELEM);
      seq.loadConstant(0);
      seq.insn(Opcodes.AALOAD);
      seq.varInsn(Opcodes.ASTORE, selfSlot);
    } else if (selfId != null) {
      if (dataField == null) {
        throw new IllegalStateException(
            "FunctionClassEmitter.emit: dataField required when selfId is non-null");
      }
      var dataFieldCanonical = new ConstKey.Str(dataField).toCanonical();
      var selfSlot = ctx.allocSlot(selfId);
      if (argParamIds.isEmpty()) {

        seq.varInsn(Opcodes.ALOAD, 2);
        seq.checkCast(INTNAME_OBJECT_ARRAY_ELEM);
        seq.loadConstant(0);
        seq.insn(Opcodes.AALOAD);
        seq.loadConstant(dataFieldCanonical);
        seq.checkCast(JvmConstants.INTNAME_MAP);
        seq.invokeInterface(JvmConstants.INTNAME_MAP, "get", MDESC_MAP_GET);
        seq.varInsn(Opcodes.ASTORE, selfSlot);
      } else {

        seq.varInsn(Opcodes.ALOAD, 2);
        seq.checkCast(INTNAME_OBJECT_ARRAY_ELEM);
        seq.insn(Opcodes.DUP);
        seq.loadConstant(0);
        seq.insn(Opcodes.AALOAD);
        seq.loadConstant(dataFieldCanonical);
        seq.checkCast(JvmConstants.INTNAME_MAP);
        seq.invokeInterface(JvmConstants.INTNAME_MAP, "get", MDESC_MAP_GET);
        seq.varInsn(Opcodes.ASTORE, selfSlot);
        for (int i = 0; i < argParamIds.size(); i++) {
          var slot = ctx.allocSlot(argParamIds.get(i));
          if (i < argParamIds.size() - 1) {
            seq.insn(Opcodes.DUP);
          }
          seq.loadConstant(i + 1);
          seq.insn(Opcodes.AALOAD);
          seq.varInsn(Opcodes.ASTORE, slot);
        }
      }
    } else {
      if (argParamIds.size() != 1) {
        throw new IllegalStateException(
            "FunctionClassEmitter.emit: exactly one argParamId required when selfId is null");
      }
      var argSlot = ctx.allocSlot(argParamIds.get(0));
      seq.varInsn(Opcodes.ALOAD, 2);
      seq.varInsn(Opcodes.ASTORE, argSlot);
    }

    var ownerInternal = JavaInternalName.of(internalName);
    for (var entry : externals) {
      if (!(entry.id() instanceof BindingId.Local localId)) {
        throw new IllegalStateException(
            "External captured under non-Local BindingId: " + entry.id());
      }
      var fieldName = externalFieldNames.get(localId);
      seq.getStatic(ownerInternal, fieldName, JvmConstants.DESC_OBJECT);
      var slot = ctx.allocSlot(localId);
      seq.varInsn(Opcodes.ASTORE, slot);
    }

    target.emit(seq, ctx, bodyCode);
    seq.insn(Opcodes.ARETURN);
    seq.renderMethod(mv, TList.of());
    cw.visitEnd();
    var bytecode = cw.toByteArray();

    var staticFields = new TOrderedMap<String, Object>();
    staticFields.putAll(builtinDynamics);
    for (var entry : externals) {
      var fieldName = externalFieldNames.get(entry.id());
      staticFields.put(fieldName, entry.external().externalHostValue());
    }
    return new EmittedFunction(binaryName, internalName, bytecode, staticFields);
  }
}
