package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Counter;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.EvalContext;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class DefSetFunctionsEmitter {
  private DefSetFunctionsEmitter() {}

  public record ArgSlot(BindingId.Local id, JavaDataDescriptor jvmDesc) {}

  public record FunctionSpec(
      long funcIdx,
      TList<ArgSlot> argSlots,
      JavaDataDescriptor retDesc,
      CodeDynamic bodyCode,
      TList<EvalContext.ExternalEntry> externals) {
    public JavaMethodDescriptor descriptor() {
      var argDescs = new TList<JavaDataDescriptor>(argSlots.size());
      for (var s : argSlots) argDescs.add(s.jvmDesc());
      return JavaMethodDescriptor.fromParts(retDesc, argDescs);
    }
  }

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

  public static String functionMethodName(long funcIdx) {
    return "_" + funcIdx;
  }

  public static String functionFieldPrefix(long funcIdx) {
    return "_" + funcIdx + "_";
  }

  public static EmittedFunctions emit(
      DefSetId defSet,
      TList<FunctionSpec> specs,
      TOrderedMap<String, compilerxyz.eval.External> builtinDynamics) {
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

    var fieldDescriptors = new TOrderedMap<String, JavaDataDescriptor>();
    var staticFieldValues = new TOrderedMap<String, Object>();

    for (var spec : specs) {
      var prefix = functionFieldPrefix(spec.funcIdx());

      for (var builtinId : EvalRootEmitter.referencedBuiltinIds(spec.bodyCode())) {
        var name = Prefixes.BUILTIN_PREFIX + builtinId;
        var v = builtinDynamics.get(name);
        if (v == null) {
          throw new AssertionError(
              "DefSetFunctionsEmitter::emit: no builtin registered for " + name);
        }
        var desc = JavaDataDescriptor.fromClass(v.externalHostClass());
        var prev = fieldDescriptors.put(name, desc);
        if (prev != null && !prev.value().equals(desc.value())) {
          throw new AssertionError(
              "DefSetFunctionsEmitter::emit: descriptor conflict for " + name);
        }
        staticFieldValues.put(name, v.externalHostValue());
      }

      for (int i = 0; i < spec.externals().size(); i++) {
        var entry = spec.externals().get(i);
        var name = prefix + Prefixes.EXTERNAL_PREFIX + i;
        var external = entry.external();
        var desc = JavaDataDescriptor.fromClass(external.externalHostClass());
        var prev = fieldDescriptors.put(name, desc);
        if (prev != null && !prev.value().equals(desc.value())) {
          throw new AssertionError(
              "DefSetFunctionsEmitter::emit: descriptor conflict for " + name);
        }
        staticFieldValues.put(name, external.externalHostValue());
      }
    }
    for (var entry : fieldDescriptors.entrySet()) {
      cw.visitField(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
              entry.getKey(),
              entry.getValue().value(),
              null,
              null)
          .visitEnd();
    }
    cw.visitField(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            EvalRootEmitter.EXPR_CONTEXT_FIELD,
            EvalRootEmitter.DESC_EXPR_CONTEXT.value(),
            null,
            null)
        .visitEnd();

    for (var spec : specs) {
      emitFunctionMethod(cw, internalName, spec);
    }

    cw.visitEnd();
    return new EmittedFunctions(binaryName, internalName, cw.toByteArray(), staticFieldValues);
  }

  private static void emitFunctionMethod(
      ClassWriter cw,
      String internalName,
      FunctionSpec spec) {
    var methodName = functionMethodName(spec.funcIdx());
    var prefix = functionFieldPrefix(spec.funcIdx());
    var mv =
        cw.visitMethod(
            Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC,
            methodName,
            spec.descriptor().value(),
            null,
            null);
    var target = new TargetDynamic(new Counter());
    int reservedLocals = 0;
    for (var s : spec.argSlots()) reservedLocals += slotWidth(s.jvmDesc());
    var ctx = new EmitCtx(internalName, Math.max(1, reservedLocals), prefix);
    var loc = compilerxyz.eval.dynamic.CodeLoc.Infallible.INSTANCE;

    var seq = JavaBytecodeUtils.seq();

    var ownerInternal = JavaInternalName.of(internalName);
    for (int i = 0; i < spec.externals().size(); i++) {
      var entry = spec.externals().get(i);
      var fieldName = prefix + Prefixes.EXTERNAL_PREFIX + i;
      var fieldDesc =
          JavaDataDescriptor.fromClass(entry.external().externalHostClass());
      seq.add(
          JavaBytecodeUtils.accessStaticField(loc, ownerInternal, fieldName, fieldDesc));
      seq.add(
          new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entry.key())));
    }

    target.emit(seq, ctx, spec.bodyCode());
    seq.add(JavaBytecodeUtils.inst(loc, returnOpcode(spec.retDesc())));

    var initialSlots = new TList<JavaBytecodeSequence.InitialSlot>();
    for (var s : spec.argSlots()) {
      initialSlots.add(
          new JavaBytecodeSequence.InitialSlot(s.id().key(), slotWidth(s.jvmDesc()) == 2));
    }
    JavaBytecodeUtils.renderMethodTyped(seq, mv, initialSlots);
  }

  private static int slotWidth(JavaDataDescriptor d) {
    var v = d.value();
    return (v.equals("D") || v.equals("J")) ? 2 : 1;
  }

  private static int returnOpcode(JavaDataDescriptor d) {
    return switch (d.value()) {
      case "V" -> Opcodes.RETURN;
      case "I", "Z", "B", "S", "C" -> Opcodes.IRETURN;
      case "J" -> Opcodes.LRETURN;
      case "F" -> Opcodes.FRETURN;
      case "D" -> Opcodes.DRETURN;
      default -> Opcodes.ARETURN;
    };
  }
}
