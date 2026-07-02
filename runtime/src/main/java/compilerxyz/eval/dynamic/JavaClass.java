package compilerxyz.eval.dynamic;

import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_STATIC;
import static org.objectweb.asm.Opcodes.ACC_SUPER;

import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import org.objectweb.asm.ClassWriter;

public class JavaClass {
  public final ClassWriter cw;
  public final TSet<String> seenFunctions = new TSet<>();

  public JavaClass(JavaInternalName jvmName, TList<String> interfaces) {
    this.cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES);
    cw.visit(
        52,
        ACC_PUBLIC + ACC_SUPER,
        jvmName.value,
        null,
        "java/lang/Object",
        interfaces.toArray(new String[0]));
  }

  public JavaClass setMetaSource(String address) {
    cw.visitSource(address, null);
    return this;
  }

  public JavaClass defineConstructor(
      JavaMethodDescriptor desc,
      JavaBytecodeSequence code,
      TList<JavaBytecodeBindingKey> initialIndexes,
      TList<String> parameters) {
    var mv = cw.visitMethod(ACC_PUBLIC, "<init>", desc.value, null, null);
    for (var parameter : parameters) mv.visitParameter(parameter, 0);
    mv.visitCode();
    code.render(mv, initialIndexes);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();
    return this;
  }

  public JavaClass defineFunction(
      String methodId,
      JavaMethodDescriptor desc,
      JavaBytecodeSequence code,
      TList<JavaBytecodeBindingKey> initialIndexes,
      TList<String> parameters) {
    var mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, methodId, desc.value, null, null);
    for (var parameter : parameters) mv.visitParameter(parameter, 0);
    mv.visitCode();
    code.render(mv, initialIndexes);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();
    return this;
  }

  public JavaClass defineMethod(
      String methodId,
      JavaMethodDescriptor desc,
      JavaBytecodeSequence code,
      TList<JavaBytecodeBindingKey> initialIndexes,
      TList<String> parameters) {
    var mv = cw.visitMethod(ACC_PUBLIC, methodId, desc.value, null, null);
    for (var parameter : parameters) mv.visitParameter(parameter, 0);
    mv.visitCode();
    code.render(mv, initialIndexes);
    mv.visitMaxs(-1, -1);
    mv.visitEnd();
    return this;
  }

  public byte[] render() {
    cw.visitEnd();
    return cw.toByteArray();
  }

  public JavaClass defineStaticField(String id, JavaDataDescriptor t) {
    cw.visitField(ACC_PUBLIC + ACC_STATIC, id, t.value, t.value, null);
    return this;
  }

  public JavaClass defineField(String id, JavaDataDescriptor t) {
    cw.visitField(ACC_PUBLIC, id, t.value, t.value, null);
    return this;
  }

  public boolean isDefined(String name) {
    return seenFunctions.contains(name);
  }
}
