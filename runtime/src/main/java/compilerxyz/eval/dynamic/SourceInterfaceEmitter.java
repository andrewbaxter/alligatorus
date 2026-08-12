package compilerxyz.eval.dynamic;

import compilerxyz.eval.DefSetId;
import compilerxyz.eval.value.source_interface.InterfaceData;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

public final class SourceInterfaceEmitter {
  private static final JavaMethodDescriptor MDESC_INTERFACE_METHOD =
      JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT);

  private SourceInterfaceEmitter() {}

  public record EmittedInterface(String binaryName, String internalName, byte[] bytecode) {}

  public static String binaryNameFor(DefSetId defSet, long interfaceIndex) {
    var compileId = defSet.compileId();
    return "compilerxyz.eval.gen.Interface$"
        + compileId.importState().value()
        + "_"
        + compileId.source().value()
        + "_"
        + defSet.index()
        + "_"
        + interfaceIndex;
  }

  public static EmittedInterface emit(DefSetId defSet, long interfaceIndex, InterfaceData data) {
    var binaryName = binaryNameFor(defSet, interfaceIndex);
    var internalName = binaryName.replace('.', '/');

    var cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
    cw.visit(
        Opcodes.V11,
        Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT,
        internalName,
        null,
        JvmConstants.INTNAME_OBJECT.value(),
        null);

    for (var method : data.methods) {
      if (method.argType == null) {
        throw new IllegalStateException(
            "SourceInterfaceEmitter.emit: method '"
                + method.name
                + "' has no arg type (finish ordering violation)");
      }
      cw.visitMethod(
              Opcodes.ACC_PUBLIC | Opcodes.ACC_ABSTRACT,
              method.name,
              MDESC_INTERFACE_METHOD.value(),
              null,
              null)
          .visitEnd();
    }

    cw.visitEnd();
    return new EmittedInterface(binaryName, internalName, cw.toByteArray());
  }
}
