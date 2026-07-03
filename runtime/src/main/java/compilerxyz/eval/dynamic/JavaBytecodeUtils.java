package compilerxyz.eval.dynamic;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;
import static org.objectweb.asm.Opcodes.PUTSTATIC;

import compilerxyz.eval.QExprId;
import compilerxyz.util.TList;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class JavaBytecodeUtils {
  public static final JavaBytecode JBC_DUP = new JavaBytecodeInstructionObj(new InsnNode(DUP));

  public static boolean empty(JavaBytecode e) {
    if (e == null) return true;
    if (e instanceof JavaBytecodeSequence s && s.size() == 0) return true;
    return false;
  }

  public static void print(MethodNode m) {
    System.out.format("--\n");
    var printer = new Textifier();
    m.accept(new TraceMethodVisitor(printer));
    var pw = new PrintWriter(System.out);
    printer.print(pw);
    pw.flush();
  }

    private static CodeDynamic w(QExprId loc, JavaBytecode bc) {
        return new CodeDynamic(loc, bc);
    }

  public static CodeDynamic instantiate(
      QExprId loc, JavaInternalName klass, JavaMethodDescriptor desc, CodeDynamic arguments) {
    var code = new JavaBytecodeSequence();
    code.add(w(loc, new JavaBytecodeInstructionObj(new TypeInsnNode(NEW, klass.value))));
    code.add(w(loc, JBC_DUP));
    code.add(arguments);
    code.add(
        w(
            loc,
            new JavaBytecodeInstructionObj(
                new MethodInsnNode(INVOKESPECIAL, klass.value, "<init>", desc.value, false))));
    return new CodeDynamic(loc, code);
  }

  public static CodeDynamic accessField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(new FieldInsnNode(GETFIELD, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic setField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(new FieldInsnNode(PUTFIELD, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic setStaticField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(new FieldInsnNode(PUTSTATIC, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic accessStaticField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(new FieldInsnNode(GETSTATIC, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic callConstructor(
      QExprId loc, JavaInternalName klass, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESPECIAL, klass.value, "<init>", methodDesc.value, false)));
  }

  public static CodeDynamic callConstructor(
      JavaInternalName klass, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        CodeLoc.Infallible.INSTANCE,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESPECIAL, klass.value, "<init>", methodDesc.value, false)));
  }

  public static CodeDynamic callInterfaceMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKEINTERFACE, klass.value, method, methodDesc.value, true)));
  }

  public static CodeDynamic callMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKEVIRTUAL, klass.value, method, methodDesc.value, false)));
  }

  public static CodeDynamic callStaticMethodReflect(QExprId loc, Class<?> klass, String method) {
    for (var method1 : klass.getMethods()) {
      if (!method.equals(method1.getName())) continue;
      var ret = JavaDataDescriptor.fromClass(method1.getReturnType());
      var args = new TList<JavaDataDescriptor>();
      for (var p : method1.getParameters()) args.add(JavaDataDescriptor.fromClass(p.getType()));
      return new CodeDynamic(
          loc,
          new JavaBytecodeInstructionObj(
              new MethodInsnNode(
                  INVOKESTATIC,
                  internalNameFromClass(klass).value,
                  method,
                  JavaMethodDescriptor.fromParts(ret, args).value,
                  false)));
    }
    throw new AssertionError("No such method: " + klass.getName() + "." + method);
  }

  public static CodeDynamic callStaticMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESTATIC, klass.value, method, methodDesc.value, false)));
  }

  public static CodeDynamic literalString(QExprId loc, String value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic inst(QExprId loc, int opcode) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionInt(opcode));
  }

  public static CodeDynamic inst(CodeLoc loc, int opcode) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionInt(opcode));
  }

  public static CodeDynamic inst(int opcode) {
    return inst(CodeLoc.Infallible.INSTANCE, opcode);
  }

  public static CodeDynamic varInsn(int opcode, int index) {
    return varInsn(CodeLoc.Infallible.INSTANCE, opcode, index);
  }

  public static CodeDynamic literalString(String value) {
    return literalString(CodeLoc.Infallible.INSTANCE, value);
  }

  public static CodeDynamic literalIntShortByte(int value) {
    return literalIntShortByte(CodeLoc.Infallible.INSTANCE, value);
  }

  public static CodeDynamic literalNull() {
    return literalNull(CodeLoc.Infallible.INSTANCE);
  }

  public static CodeDynamic accessStaticField(
      JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return accessStaticField(CodeLoc.Infallible.INSTANCE, klass, field, fieldDesc);
  }

  public static CodeDynamic setStaticField(
      JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return setStaticField(CodeLoc.Infallible.INSTANCE, klass, field, fieldDesc);
  }

  public static CodeDynamic setField(
      JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return setField(CodeLoc.Infallible.INSTANCE, klass, field, fieldDesc);
  }

  public static CodeDynamic accessField(
      JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return accessField(CodeLoc.Infallible.INSTANCE, klass, field, fieldDesc);
  }

  public static CodeDynamic invokeSpecial(
      JavaInternalName klass, String method, JavaMethodDescriptor desc) {
    return invokeSpecial(CodeLoc.Infallible.INSTANCE, klass, method, desc);
  }

  public static CodeDynamic callStaticMethod(
      JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return callStaticMethod(CodeLoc.Infallible.INSTANCE, klass, method, methodDesc);
  }

  public static CodeDynamic callInterfaceMethod(
      JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return callInterfaceMethod(CodeLoc.Infallible.INSTANCE, klass, method, methodDesc);
  }

  public static CodeDynamic callMethod(
      JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return callMethod(CodeLoc.Infallible.INSTANCE, klass, method, methodDesc);
  }

  public static CodeDynamic cast(JavaDataDescriptor toClass) {
    return cast(CodeLoc.Infallible.INSTANCE, toClass);
  }

  public static CodeDynamic instanceOf(CodeLoc loc, JavaInternalName klass) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new org.objectweb.asm.tree.TypeInsnNode(
                org.objectweb.asm.Opcodes.INSTANCEOF, klass.value())));
  }

  public static CodeDynamic newInstance(CodeLoc loc, JavaInternalName klass) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new org.objectweb.asm.tree.TypeInsnNode(
                org.objectweb.asm.Opcodes.NEW, klass.value())));
  }

  public static CodeDynamic wrapObj(CodeLoc loc, org.objectweb.asm.tree.AbstractInsnNode node) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(node));
  }

  public static CodeDynamic bindingUse(
      CodeLoc loc, compilerxyz.eval.EvalContext vx, compilerxyz.eval.BindingId id) {
    return switch (id) {
      case compilerxyz.eval.BindingId.Local local ->
          new CodeDynamic(
              loc,
              new JavaBytecodeStoreLoad(org.objectweb.asm.Opcodes.ALOAD, local.key()));
      case compilerxyz.eval.BindingId.Def def ->
          accessStaticField(
              loc,
              JavaInternalName.of(vx.evalRootInternalName),
              compilerxyz.eval.ImportInstall.bindingIdDynamicKey(id),
              JvmConstants.DESC_OBJECT);
      case compilerxyz.eval.BindingId.Builtin builtin ->
          accessStaticField(
              loc,
              JavaInternalName.of(vx.evalRootInternalName),
              compilerxyz.eval.ImportInstall.bindingIdDynamicKey(id),
              JvmConstants.DESC_OBJECT);
    };
  }

  public static CodeDynamic bindingSet(
      CodeLoc loc,
      compilerxyz.eval.EvalContext vx,
      compilerxyz.eval.BindingId id,
      CodeDynamic value) {
    var seq = new JavaBytecodeSequence();
    seq.add(value);
    switch (id) {
      case compilerxyz.eval.BindingId.Local local ->
          seq.add(
              new CodeDynamic(
                  loc,
                  new JavaBytecodeStoreLoad(org.objectweb.asm.Opcodes.ASTORE, local.key())));
      case compilerxyz.eval.BindingId.Def def ->
          seq.add(
              setStaticField(
                  loc,
                  JavaInternalName.of(vx.evalRootInternalName),
                  compilerxyz.eval.ImportInstall.bindingIdDynamicKey(id),
                  JvmConstants.DESC_OBJECT));
      case compilerxyz.eval.BindingId.Builtin builtin ->
          seq.add(
              setStaticField(
                  loc,
                  JavaInternalName.of(vx.evalRootInternalName),
                  compilerxyz.eval.ImportInstall.bindingIdDynamicKey(id),
                  JvmConstants.DESC_OBJECT));
    }
    return new CodeDynamic(loc, seq);
  }

  public static CodeDynamic varInsn(CodeLoc loc, int opcode, int index) {
    return new CodeDynamic(
        loc, new JavaBytecodeInstructionObj(new org.objectweb.asm.tree.VarInsnNode(opcode, index)));
  }

  public static CodeDynamic literalString(CodeLoc loc, String value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic literalIntShortByte(CodeLoc loc, int value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic literalNull(CodeLoc loc) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionInt(ACONST_NULL));
  }

  public static CodeDynamic accessStaticField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new FieldInsnNode(GETSTATIC, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic setStaticField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new FieldInsnNode(PUTSTATIC, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic setField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new FieldInsnNode(PUTFIELD, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic invokeSpecial(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor desc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESPECIAL, klass.value, method, desc.value, false)));
  }

  public static CodeDynamic callStaticMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESTATIC, klass.value, method, methodDesc.value, false)));
  }

  public static CodeDynamic callInterfaceMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKEINTERFACE, klass.value, method, methodDesc.value, true)));
  }

  public static CodeDynamic callMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKEVIRTUAL, klass.value, method, methodDesc.value, false)));
  }

  public static CodeDynamic accessField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new FieldInsnNode(GETFIELD, klass.value, field, fieldDesc.value)));
  }

  public static CodeDynamic cast(CodeLoc loc, JavaDataDescriptor toClass) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new org.objectweb.asm.tree.TypeInsnNode(CHECKCAST, toClass.value.substring(1, toClass.value.length() - 1))));
  }


  public static CodeDynamic varInsn(QExprId loc, int opcode, int index) {
    return new CodeDynamic(
        loc, new JavaBytecodeInstructionObj(new org.objectweb.asm.tree.VarInsnNode(opcode, index)));
  }

  public static CodeDynamic buildObjectArray(QExprId loc, TList<CodeDynamic> elements) {
    var seq = new JavaBytecodeSequence();
    seq.add(literalIntShortByte(loc, elements.size()));
    seq.add(
        new CodeDynamic(
            loc,
            new JavaBytecodeInstructionObj(
                new org.objectweb.asm.tree.TypeInsnNode(
                    org.objectweb.asm.Opcodes.ANEWARRAY,
                    JvmConstants.INTNAME_OBJECT.value()))));
    for (int i = 0; i < elements.size(); i++) {
      seq.add(inst(loc, org.objectweb.asm.Opcodes.DUP));
      seq.add(literalIntShortByte(loc, i));
      seq.add(elements.get(i));
      seq.add(inst(loc, org.objectweb.asm.Opcodes.AASTORE));
    }
    return new CodeDynamic(loc, seq);
  }

  public static CodeDynamic instantiateVarargs(
      QExprId loc, JavaInternalName klass, TList<CodeDynamic> args) {
    var ctorDesc =
        JavaMethodDescriptor.fromConstructorParts(
            TList.of(JvmConstants.DESC_OBJECT.array()));
    return instantiate(loc, klass, ctorDesc, buildObjectArray(loc, args));
  }

  public static CodeDynamic invokeSpecial(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor desc) {
    return new CodeDynamic(
        loc,
        new JavaBytecodeInstructionObj(
            new MethodInsnNode(INVOKESPECIAL, klass.value, method, desc.value, false)));
  }

  public static void renderMethod(
      JavaBytecodeSequence seq,
      org.objectweb.asm.MethodVisitor mv,
      compilerxyz.util.TList<JavaBytecodeBindingKey> initialIndexes) {
    seq.render(mv, initialIndexes);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public static CodeDynamic literalLong(QExprId loc, long value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic literalDouble(QExprId loc, double value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic literalNull(QExprId loc) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionInt(ACONST_NULL));
  }

  public static CodeDynamic literalBool(QExprId loc, boolean value) {
    return literalIntShortByte(loc, value ? 1 : 0);
  }

  public static CodeDynamic literalIntShortByte(QExprId loc, int value) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new LdcInsnNode(value)));
  }

  public static CodeDynamic box(QExprId loc, JavaDataDescriptor primDescriptor, JavaInternalName box) {
    return callStaticMethod(
        loc,
        box,
        "valueOf",
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(box), TList.of(primDescriptor)));
  }

  public static CodeDynamic cast(QExprId loc, JavaDataDescriptor toClass) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(new TypeInsnNode(CHECKCAST, toClass.value)));
  }

  public static CodeDynamic bool_(QExprId loc, boolean value) {
    return inst(loc, value ? ICONST_1 : ICONST_0);
  }

  public static JavaQualifiedName qualifiedNameFromClass(Class<?> klass) {
    var value = new TList<String>();
    Class<?> prevAt = klass;
    Class<?> at = klass.getNestHost();
    while (true) {
      value.add(at.getSimpleName());
      if (at == prevAt) {
        value.removeLast();
        value.add(at.getCanonicalName());
        break;
      }
      prevAt = at;
      at = at.getNestHost();
    }
    java.util.Collections.reverse(value);
    return new JavaQualifiedName(value);
  }

  public static JavaQualifiedName qualifiedName(String name) {
    return JavaQualifiedName.create(name);
  }

  public static JavaInternalName internalName(String name) {
    return JavaInternalName.of(name);
  }

  public static CodeDynamic loadIntShortByteBool(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(ILOAD, key));
  }

  public static CodeDynamic storeIntShortByteBool(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(ISTORE, key));
  }

  public static CodeDynamic loadLong(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(LLOAD, key));
  }

  public static CodeDynamic storeLong(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(LSTORE, key));
  }

  public static CodeDynamic loadFloat(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(FLOAD, key));
  }

  public static CodeDynamic storeFloat(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(FSTORE, key));
  }

  public static CodeDynamic loadDouble(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(DLOAD, key));
  }

  public static CodeDynamic storeDouble(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(DSTORE, key));
  }

  public static CodeDynamic loadObj(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(ALOAD, key));
  }

  public static CodeDynamic storeObj(QExprId loc, JavaBytecodeBindingKey key) {
    return new CodeDynamic(loc, new JavaBytecodeStoreLoad(ASTORE, key));
  }

  public static JavaInternalName internalNameFromClass(Class<?> klass) {
    return qualifiedNameFromClass(klass).asInternalName();
  }

  public static JavaBytecodeSequence seq() {
    return new JavaBytecodeSequence();
  }
}
