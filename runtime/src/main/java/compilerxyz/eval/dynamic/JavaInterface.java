package compilerxyz.eval.dynamic;

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_INTERFACE;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;

import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import org.objectweb.asm.ClassWriter;

public class JavaInterface {
  public final ClassWriter cw;
  public final TSet<String> seenFunctions = new TSet<>();

  public JavaInterface(JavaInternalName jvmName) {
    this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cw.visit(
        52,
        ACC_PUBLIC + ACC_ABSTRACT + ACC_INTERFACE,
        jvmName.value,
        null,
        "java/lang/Object",
        null);
  }

  public JavaInterface setMetaSource(String address) {
    cw.visitSource(address, null);
    return this;
  }

  public JavaInterface declareMethod(
      String methodId, JavaMethodDescriptor desc, TList<String> parameters) {
    var mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, methodId, desc.value, null, null);
    for (var parameter : parameters) mv.visitParameter(parameter, 0);
    mv.visitEnd();
    return this;
  }

  public byte[] render() {
    cw.visitEnd();
    return cw.toByteArray();
  }

  public boolean isDefined(String name) {
    return seenFunctions.contains(name);
  }
}
