package compilerxyz.eval.dynamic;

import compilerxyz.eval.JumpKey;

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
import static org.objectweb.asm.Opcodes.INSTANCEOF;
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
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import org.objectweb.asm.Label;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.MethodVisitor;

public class JavaBytecodeUtils {
  public static final JavaBytecode JBC_DUP = new JavaBytecodeInstructionInt(DUP);

  private static CodeLoc lc(QExprId loc) {
    return new CodeLoc.Some(loc);
  }

  // ------------------- primitive wrappers -------------------

  public static CodeDynamic wrapObj(CodeLoc loc, AbstractInsnNode node) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionObj(node));
  }

  public static CodeDynamic wrapObj(QExprId loc, AbstractInsnNode node) {
    return wrapObj(lc(loc), node);
  }

  // ------------------- bare instructions -------------------

  public static CodeDynamic inst(CodeLoc loc, int opcode) {
    return new CodeDynamic(loc, new JavaBytecodeInstructionInt(opcode));
  }

  public static CodeDynamic inst(QExprId loc, int opcode) {
    return inst(lc(loc), opcode);
  }

  public static CodeDynamic varInsn(CodeLoc loc, int opcode, int index) {
    return wrapObj(loc, new VarInsnNode(opcode, index));
  }

  public static CodeDynamic varInsn(QExprId loc, int opcode, int index) {
    return varInsn(lc(loc), opcode, index);
  }

  // ------------------- literals -------------------

  public static CodeDynamic literalString(CodeLoc loc, String value) {
    return wrapObj(loc, new LdcInsnNode(value));
  }

  public static CodeDynamic literalString(QExprId loc, String value) {
    return literalString(lc(loc), value);
  }

  public static CodeDynamic literalIntShortByte(CodeLoc loc, int value) {
    return wrapObj(loc, new LdcInsnNode(value));
  }

  public static CodeDynamic literalIntShortByte(QExprId loc, int value) {
    return literalIntShortByte(lc(loc), value);
  }

  public static CodeDynamic literalLong(QExprId loc, long value) {
    return wrapObj(lc(loc), new LdcInsnNode(value));
  }

  public static CodeDynamic literalDouble(QExprId loc, double value) {
    return wrapObj(lc(loc), new LdcInsnNode(value));
  }

  public static CodeDynamic literalFloat(QExprId loc, float value) {
    return wrapObj(lc(loc), new LdcInsnNode(value));
  }

  public static CodeDynamic literalNull(QExprId loc) {
    return inst(lc(loc), ACONST_NULL);
  }

  public static CodeDynamic literalBool(QExprId loc, boolean value) {
    return inst(lc(loc), value ? ICONST_1 : ICONST_0);
  }

  // ------------------- fields -------------------

  private static CodeDynamic fieldOp(
      CodeLoc loc, int opcode, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return wrapObj(loc, new FieldInsnNode(opcode, klass.value, field, fieldDesc.value));
  }

  public static CodeDynamic accessField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return fieldOp(loc, GETFIELD, klass, field, fieldDesc);
  }

  public static CodeDynamic accessField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return accessField(lc(loc), klass, field, fieldDesc);
  }

  public static CodeDynamic setField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return fieldOp(loc, PUTFIELD, klass, field, fieldDesc);
  }

  public static CodeDynamic accessStaticField(
      CodeLoc loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return fieldOp(loc, GETSTATIC, klass, field, fieldDesc);
  }

  public static CodeDynamic accessStaticField(
      QExprId loc, JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
    return accessStaticField(lc(loc), klass, field, fieldDesc);
  }

  // ------------------- method calls -------------------

  private static CodeDynamic methodCall(
      CodeLoc loc,
      int opcode,
      JavaInternalName klass,
      String method,
      JavaMethodDescriptor methodDesc,
      boolean isInterface) {
    return wrapObj(
        loc, new MethodInsnNode(opcode, klass.value, method, methodDesc.value, isInterface));
  }

  public static CodeDynamic callConstructor(
      CodeLoc loc, JavaInternalName klass, JavaMethodDescriptor methodDesc) {
    return methodCall(loc, INVOKESPECIAL, klass, "<init>", methodDesc, false);
  }

  public static CodeDynamic callConstructor(
      QExprId loc, JavaInternalName klass, JavaMethodDescriptor methodDesc) {
    return callConstructor(lc(loc), klass, methodDesc);
  }

  public static CodeDynamic invokeSpecial(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor desc) {
    return methodCall(loc, INVOKESPECIAL, klass, method, desc, false);
  }

  public static CodeDynamic invokeSpecial(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor desc) {
    return invokeSpecial(lc(loc), klass, method, desc);
  }

  public static CodeDynamic callInterfaceMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return methodCall(lc(loc), INVOKEINTERFACE, klass, method, methodDesc, true);
  }

  public static CodeDynamic callMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return methodCall(loc, INVOKEVIRTUAL, klass, method, methodDesc, false);
  }

  public static CodeDynamic callMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return callMethod(lc(loc), klass, method, methodDesc);
  }

  public static CodeDynamic callStaticMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return methodCall(loc, INVOKESTATIC, klass, method, methodDesc, false);
  }

  public static CodeDynamic callStaticInterfaceMethod(
      CodeLoc loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return methodCall(loc, INVOKESTATIC, klass, method, methodDesc, true);
  }

  public static CodeDynamic callStaticMethod(
      QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
    return callStaticMethod(lc(loc), klass, method, methodDesc);
  }

  public static CodeDynamic callStaticMethod(CodeLoc loc, JavaStaticMethodRef ref) {
    return callStaticMethod(loc, ref.owner, ref.name, ref.descriptor);
  }

  public static CodeDynamic callStaticMethod(QExprId loc, JavaStaticMethodRef ref) {
    return callStaticMethod(lc(loc), ref.owner, ref.name, ref.descriptor);
  }

  public static CodeDynamic callStaticMethodReflect(QExprId loc, Class<?> klass, String method) {
    for (var method1 : klass.getMethods()) {
      if (!method.equals(method1.getName())) continue;
      var ret = JavaDataDescriptor.fromClass(method1.getReturnType());
      var args = new TList<JavaDataDescriptor>();
      for (var p : method1.getParameters()) args.add(JavaDataDescriptor.fromClass(p.getType()));
      return callStaticMethod(
          lc(loc),
          qualifiedNameFromClass(klass).asInternalName(),
          method,
          JavaMethodDescriptor.fromParts(ret, args));
    }
    throw new AssertionError("No such method: " + klass.getName() + "." + method);
  }

  // ------------------- type ops -------------------

  private static CodeDynamic typeInsn(CodeLoc loc, int opcode, JavaInternalName klass) {
    return wrapObj(loc, new TypeInsnNode(opcode, klass.value));
  }

  public static CodeDynamic newInstance(CodeLoc loc, JavaInternalName klass) {
    return typeInsn(loc, NEW, klass);
  }

  public static CodeDynamic newInstance(QExprId loc, JavaInternalName klass) {
    return newInstance(lc(loc), klass);
  }

  public static CodeDynamic instanceOf(QExprId loc, JavaInternalName klass) {
    return typeInsn(lc(loc), INSTANCEOF, klass);
  }

  public static CodeDynamic cast(CodeLoc loc, JavaDataDescriptor toClass) {
    // Callers pass class descriptors (Lfoo/Bar;); CHECKCAST wants the internal name (foo/Bar).
    return wrapObj(
        loc,
        new TypeInsnNode(
            CHECKCAST, toClass.value.substring(1, toClass.value.length() - 1)));
  }

  public static CodeDynamic cast(QExprId loc, JavaDataDescriptor toClass) {
    return cast(lc(loc), toClass);
  }

  // ------------------- composite builders -------------------

  public static CodeDynamic instantiate(
      QExprId loc,
      JavaInternalName klass,
      JavaMethodDescriptor desc,
      CodeDynamic arguments) {
    var lc = lc(loc);
    var code = new JavaBytecodeSequence();
    code.add(newInstance(lc, klass));
    code.add(new CodeDynamic(lc, JBC_DUP));
    code.add(arguments);
    code.add(callConstructor(lc, klass, desc));
    return new CodeDynamic(lc, code);
  }

  public static CodeDynamic buildObjectArray(CodeLoc loc, TList<CodeDynamic> elements) {
    var seq = new JavaBytecodeSequence();
    seq.add(literalIntShortByte(loc, elements.size()));
    seq.add(
        wrapObj(
            loc,
            new TypeInsnNode(
                org.objectweb.asm.Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
    for (int i = 0; i < elements.size(); i++) {
      seq.add(inst(loc, DUP));
      seq.add(literalIntShortByte(loc, i));
      seq.add(elements.get(i));
      seq.add(inst(loc, org.objectweb.asm.Opcodes.AASTORE));
    }
    return new CodeDynamic(loc, seq);
  }

  public static CodeDynamic buildObjectArray(QExprId loc, TList<CodeDynamic> elements) {
    return buildObjectArray(lc(loc), elements);
  }

  // ------------------- fragment instantiation -------------------

  /**
   * Deep-copies a code fragment so it can be emitted more than once: every
   * label defined inside gets a fresh identity (references to labels outside
   * the fragment are preserved), and every Land key defined inside gets a
   * fresh JumpKey with its internal Jumps remapped. Binding keys are shared —
   * every copy addresses the same variables.
   */
  public static CodeDynamic freshen(CodeDynamic code) {
    var definedLabels = new TSet<Label>();
    var landKeys = new TSet<JumpKey>();
    freshenScan(code.code(), definedLabels, landKeys);
    var labelMap = new TMap<Label, Label>();
    for (var l : definedLabels) labelMap.put(l, new Label());
    var keyMap = new TMap<JumpKey, JumpKey>();
    for (var k : landKeys) keyMap.put(k, new JumpKey());
    @SuppressWarnings("serial")
    var labelNodeMap = new java.util.HashMap<LabelNode, LabelNode>() {
      @Override
      public LabelNode get(Object key) {
        var ln = (LabelNode) key;
        var existing = super.get(ln);
        if (existing != null) return existing;
        var mapped = labelMap.getOrNull(ln.getLabel());
        var fresh = mapped == null ? ln : new LabelNode(mapped);
        put(ln, fresh);
        return fresh;
      }
    };
    return freshenCode(code, labelMap, labelNodeMap, keyMap);
  }

  private static void freshenScan(
      JavaBytecode bc, TSet<Label> definedLabels, TSet<JumpKey> landKeys) {
    if (bc instanceof CodeDynamic cd) {
      freshenScan(cd.code(), definedLabels, landKeys);
    } else if (bc instanceof JavaBytecodeSequence seq) {
      for (var c : seq.children) freshenScan(c, definedLabels, landKeys);
    } else if (bc instanceof JavaBytecodeInstructionObj obj) {
      if (obj.node instanceof LabelNode ln) definedLabels.add(ln.getLabel());
    } else if (bc instanceof JavaBytecodeLand land) {
      landKeys.add(land.jumpKey);
    } else if (bc instanceof JavaBytecodeGuarded guarded) {
      freshenScan(guarded.inner, definedLabels, landKeys);
    } else if (bc instanceof JavaBytecodeInstructionInt
        || bc instanceof JavaBytecodeInstructionTryCatch
        || bc instanceof JavaBytecodeStoreLoad
        || bc instanceof JavaBytecodeJump) {
      // No labels or land keys of their own.
    } else {
      throw new AssertionError("freshen: unsupported bytecode node " + bc.getClass());
    }
  }

  private static CodeDynamic freshenCode(
      CodeDynamic cd,
      TMap<Label, Label> labelMap,
      java.util.Map<LabelNode, LabelNode> labelNodeMap,
      TMap<JumpKey, JumpKey> keyMap) {
    return new CodeDynamic(
        cd.loc(), freshenBytecode(cd.code(), labelMap, labelNodeMap, keyMap));
  }

  private static Label freshenLabel(Label l, TMap<Label, Label> labelMap) {
    var mapped = labelMap.getOrNull(l);
    return mapped == null ? l : mapped;
  }

  private static JavaBytecode freshenBytecode(
      JavaBytecode bc,
      TMap<Label, Label> labelMap,
      java.util.Map<LabelNode, LabelNode> labelNodeMap,
      TMap<JumpKey, JumpKey> keyMap) {
    if (bc instanceof CodeDynamic cd) {
      return freshenCode(cd, labelMap, labelNodeMap, keyMap);
    }
    if (bc instanceof JavaBytecodeSequence seq) {
      var out = new JavaBytecodeSequence();
      for (var c : seq.children) out.add(freshenCode(c, labelMap, labelNodeMap, keyMap));
      return out;
    }
    if (bc instanceof JavaBytecodeInstructionObj obj) {
      return new JavaBytecodeInstructionObj(obj.node.clone(labelNodeMap));
    }
    if (bc instanceof JavaBytecodeInstructionInt) {
      return bc;
    }
    if (bc instanceof JavaBytecodeInstructionTryCatch tc) {
      return new JavaBytecodeInstructionTryCatch(
          freshenLabel(tc.start, labelMap),
          freshenLabel(tc.end, labelMap),
          freshenLabel(tc.handler, labelMap),
          tc.excType);
    }
    if (bc instanceof JavaBytecodeStoreLoad) {
      return bc;
    }
    if (bc instanceof JavaBytecodeLand land) {
      var nk = keyMap.getOrNull(land.jumpKey);
      return nk == null ? bc : new JavaBytecodeLand(nk);
    }
    if (bc instanceof JavaBytecodeJump jump) {
      var nk = keyMap.getOrNull(jump.jumpKey);
      return nk == null ? bc : new JavaBytecodeJump(nk);
    }
    if (bc instanceof JavaBytecodeGuarded guarded) {
      var nk = keyMap.getOrNull(guarded.dest);
      return new JavaBytecodeGuarded(
          nk == null ? guarded.dest : nk,
          freshenCode(guarded.inner, labelMap, labelNodeMap, keyMap));
    }
    throw new AssertionError("freshen: unsupported bytecode node " + bc.getClass());
  }

  // ------------------- misc -------------------

  public static void renderMethod(
      JavaBytecodeSequence seq,
      MethodVisitor mv,
      TList<JavaBytecodeBindingKey> initialIndexes) {
    seq.render(mv, initialIndexes);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
  }

  public static void renderMethodTyped(
      JavaBytecodeSequence seq,
      MethodVisitor mv,
      TList<JavaBytecodeSequence.InitialSlot> initialSlots) {
    seq.renderTyped(mv, initialSlots);
    mv.visitMaxs(0, 0);
    mv.visitEnd();
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

  public static JavaBytecodeSequence seq() {
    return new JavaBytecodeSequence();
  }
}
