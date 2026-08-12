package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefSetId;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;

public final class SourceUnionEmitter {
  private SourceUnionEmitter() {}

  public record EmittedSourceUnion(
      String baseBinaryName,
      byte[] baseBytecode,
      TList<String> variantBinaryNames,
      TList<byte[]> variantBytecode) {}

  public static String baseBinaryNameFor(DefSetId defSet, long unionIndex) {
    var compileId = defSet.compileId();
    return "SourceUnion$"
        + compileId.importState().value()
        + "_"
        + compileId.source().value()
        + "_"
        + defSet.index()
        + "_"
        + unionIndex;
  }

  public static String variantBinaryNameFor(DefSetId defSet, long unionIndex, int variantIndex) {
    return baseBinaryNameFor(defSet, unionIndex) + "$V" + variantIndex;
  }

  public static final String PAYLOAD_FIELD = "payload";

  public static EmittedSourceUnion emit(
      ExprContext xx, DefSetId defSet, long unionIndex, TypeSourceUnionDynamic data) {
    var baseBinaryName = baseBinaryNameFor(defSet, unionIndex);
    var baseInternalName = baseBinaryName.replace('.', '/');

    var variantBinaryNames = new TList<String>(data.variants.size());
    var variantInternalNames = new TList<String>(data.variants.size());
    for (int i = 0; i < data.variants.size(); i++) {
      var vbn = variantBinaryNameFor(defSet, unionIndex, i);
      variantBinaryNames.add(vbn);
      variantInternalNames.add(vbn.replace('.', '/'));
    }

    var baseBytecode = emitBase(baseInternalName, variantInternalNames);

    var variantBytecode = new TList<byte[]>(data.variants.size());
    for (int i = 0; i < data.variants.size(); i++) {
      var variant = data.variants.get(i);
      var payloadDesc = payloadDescriptor(xx, variant.type);
      variantBytecode.add(
          emitVariant(variantInternalNames.get(i), baseInternalName, payloadDesc));
    }

    return new EmittedSourceUnion(
        baseBinaryName, baseBytecode, variantBinaryNames, variantBytecode);
  }

  public static JavaDataDescriptor payloadDescriptor(ExprContext xx, Type type) {
    return switch (GenAspectConsumeDynamic.getMaybe(xx, type)) {
      case Opt.Some<AspectConsumeDynamic> some -> some.value().aspectConsumeDynamicJvmClass(xx);
      case Opt.None<AspectConsumeDynamic> n -> JvmConstants.DESC_OBJECT;
    };
  }

  private static byte[] emitBase(String baseInternalName, TList<String> variantInternalNames) {
    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V17,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT | Opcodes.ACC_SUPER,
        baseInternalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);
    for (var vin : variantInternalNames) {
      cw.visitPermittedSubclass(vin);
    }
    var ctor = cw.visitMethod(Opcodes.ACC_PROTECTED, "<init>", "()V", null, null);
    ctor.visitCode();
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    ctor.visitMethodInsn(
        Opcodes.INVOKESPECIAL,
        JvmConstants.INTNAME_OBJECT.value(),
        "<init>",
        "()V",
        false);
    ctor.visitInsn(Opcodes.RETURN);
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  private static byte[] emitVariant(
      String variantInternalName, String baseInternalName, JavaDataDescriptor payloadDesc) {
    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V17,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
        variantInternalName,
        null,
        baseInternalName,
        null);
    cw.visitField(Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL, PAYLOAD_FIELD, payloadDesc.value(), null, null)
        .visitEnd();

    var ctorDesc = "(" + payloadDesc.value() + ")V";
    var ctor = cw.visitMethod(Opcodes.ACC_PUBLIC, "<init>", ctorDesc, null, null);
    ctor.visitCode();
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    ctor.visitMethodInsn(Opcodes.INVOKESPECIAL, baseInternalName, "<init>", "()V", false);
    ctor.visitVarInsn(Opcodes.ALOAD, 0);
    int loadOp = loadOpcodeFor(payloadDesc.value());
    ctor.visitVarInsn(loadOp, 1);
    ctor.visitFieldInsn(
        Opcodes.PUTFIELD, variantInternalName, PAYLOAD_FIELD, payloadDesc.value());
    ctor.visitInsn(Opcodes.RETURN);
    ctor.visitMaxs(0, 0);
    ctor.visitEnd();
    cw.visitEnd();
    return cw.toByteArray();
  }

  private static int loadOpcodeFor(String desc) {
    return switch (desc.charAt(0)) {
      case 'Z', 'B', 'C', 'S', 'I' -> Opcodes.ILOAD;
      case 'J' -> Opcodes.LLOAD;
      case 'F' -> Opcodes.FLOAD;
      case 'D' -> Opcodes.DLOAD;
      default -> Opcodes.ALOAD;
    };
  }
}
