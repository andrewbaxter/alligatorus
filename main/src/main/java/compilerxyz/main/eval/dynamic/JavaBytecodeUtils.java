package compilerxyz.main.eval.dynamic;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TSet;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public class JavaBytecodeUtils {
    public static final JavaBytecode JBC_DUP = new JavaBytecodeInstructionInt(DUP);
    private static final boolean VERIFY_BYTECODE = Boolean.getBoolean("compilerxyz.verifyBytecode");
    private static final boolean DUMP_BYTECODE = Boolean.getBoolean("compilerxyz.dumpBytecode");

    private static CodeLoc lc(QExprId loc) {
        return new CodeLoc.Some(loc);
    }

    public static CodeDynamic wrapObj(CodeLoc loc, AbstractInsnNode node) {
        return new CodeDynamic(loc, new JavaBytecodeInstructionObj(node));
    }

    public static CodeDynamic wrapObj(QExprId loc, AbstractInsnNode node) {
        return wrapObj(lc(loc), node);
    }

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

    private static CodeDynamic methodCall(
            CodeLoc loc,
            int opcode,
            JavaInternalName klass,
            String method,
            JavaMethodDescriptor methodDesc,
            boolean isInterface) {
        return wrapObj(loc, new MethodInsnNode(opcode, klass.value, method, methodDesc.value, isInterface));
    }

    public static CodeDynamic callConstructor(CodeLoc loc, JavaInternalName klass, JavaMethodDescriptor methodDesc) {
        return methodCall(loc, INVOKESPECIAL, klass, "<init>", methodDesc, false);
    }

    public static CodeDynamic callConstructor(QExprId loc, JavaInternalName klass, JavaMethodDescriptor methodDesc) {
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
            QExprId loc, JavaInternalName klass, String method, JavaMethodDescriptor methodDesc) {
        return methodCall(lc(loc), INVOKESTATIC, klass, method, methodDesc, false);
    }

    public static CodeDynamic callStaticMethod(CodeLoc loc, JavaStaticMethodRef ref) {
        return methodCall(loc, INVOKESTATIC, ref.owner, ref.name, ref.descriptor, ref.ownerIsInterface);
    }

    public static CodeDynamic callStaticMethod(QExprId loc, JavaStaticMethodRef ref) {
        return callStaticMethod(lc(loc), ref);
    }

    public static CodeDynamic callMethod(QExprId loc, JavaVirtualMethodRef ref) {
        return methodCall(
                lc(loc),
                ref.ownerIsInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                ref.owner,
                ref.name,
                ref.descriptor,
                ref.ownerIsInterface);
    }

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
        return wrapObj(loc, new TypeInsnNode(CHECKCAST, toClass.value.substring(1, toClass.value.length() - 1)));
    }

    public static CodeDynamic cast(QExprId loc, JavaDataDescriptor toClass) {
        return cast(lc(loc), toClass);
    }

    public static CodeDynamic instantiate(
            QExprId loc, JavaInternalName klass, JavaMethodDescriptor desc, CodeDynamic arguments) {
        var lc = lc(loc);
        var code = new JavaBytecodeSequence();
        code.add(newInstance(lc, klass));
        code.add(new CodeDynamic(lc, JBC_DUP));
        code.add(arguments);
        code.add(callConstructor(lc, klass, desc));
        return new CodeDynamic(lc, code);
    }

    public static CodeDynamic buildObjectArray(QExprId loc, TList<CodeDynamic> elements) {
        var boaLoc = lc(loc);
        var seq = new JavaBytecodeSequence();
        seq.add(literalIntShortByte(boaLoc, elements.size()));
        seq.add(wrapObj(boaLoc, new TypeInsnNode(Opcodes.ANEWARRAY, JvmConstants.INTNAME_OBJECT.value())));
        for (int i = 0; i < elements.size(); i++) {
            seq.add(inst(boaLoc, DUP));
            seq.add(literalIntShortByte(boaLoc, i));
            seq.add(elements.get(i));
            seq.add(inst(boaLoc, Opcodes.AASTORE));
        }
        return new CodeDynamic(boaLoc, seq);
    }

    public static CodeDynamic freshen(CodeDynamic code) {
        var definedLabels = new TSet<Label>();
        var landKeys = new TSet<JumpKey>();
        freshenScan(code.code(), definedLabels, landKeys);
        var labelMap = new IdentityHashMap<Label, Label>();
        for (var l : definedLabels) labelMap.put(l, new Label());
        var keyMap = new IdentityHashMap<JumpKey, JumpKey>();
        for (var k : landKeys) keyMap.put(k, new JumpKey());
        return freshenCode(code, labelMap, new FreshLabelNodes(labelMap), keyMap);
    }

    private static final class FreshLabelNodes implements Map<LabelNode, LabelNode> {
        private final IdentityHashMap<LabelNode, LabelNode> backing = new IdentityHashMap<>();
        private final IdentityHashMap<Label, Label> labelMap;

        FreshLabelNodes(IdentityHashMap<Label, Label> labelMap) {
            this.labelMap = labelMap;
        }

        @Override
        public LabelNode get(Object key) {
            var ln = (LabelNode) key;
            var existing = backing.get(ln);
            if (existing != null) return existing;
            var mapped = labelMap.get(ln.getLabel());
            var fresh = mapped == null ? ln : new LabelNode(mapped);
            backing.put(ln, fresh);
            return fresh;
        }

        @Override
        public int size() {
            return backing.size();
        }

        @Override
        public boolean isEmpty() {
            return backing.isEmpty();
        }

        @Override
        public boolean containsKey(Object key) {
            return backing.containsKey(key);
        }

        @Override
        public boolean containsValue(Object value) {
            return backing.containsValue(value);
        }

        @Override
        public LabelNode put(LabelNode key, LabelNode value) {
            return backing.put(key, value);
        }

        @Override
        public LabelNode remove(Object key) {
            return backing.remove(key);
        }

        @Override
        public void putAll(Map<? extends LabelNode, ? extends LabelNode> source) {
            backing.putAll(source);
        }

        @Override
        public void clear() {
            backing.clear();
        }

        @Override
        public Set<LabelNode> keySet() {
            return backing.keySet();
        }

        @Override
        public Collection<LabelNode> values() {
            return backing.values();
        }

        @Override
        public Set<Entry<LabelNode, LabelNode>> entrySet() {
            return backing.entrySet();
        }

        @Override
        public boolean equals(Object other) {
            return backing.equals(other);
        }

        @Override
        public int hashCode() {
            return backing.hashCode();
        }
    }

    private static void freshenScan(JavaBytecode bc, TSet<Label> definedLabels, TSet<JumpKey> landKeys) {
        if (bc instanceof CodeDynamic cd) {
            freshenScan(cd.code(), definedLabels, landKeys);
        } else if (bc instanceof JavaBytecodeSequence seq) {
            for (var c : seq.children) freshenScan(c, definedLabels, landKeys);
        } else if (bc instanceof JavaBytecodeInstructionObj obj) {
            if (obj.node instanceof LabelNode ln) definedLabels.add(ln.getLabel());
        } else if (bc instanceof JavaBytecodeLand land) {
            landKeys.add(land.jumpKey);
        } else if (bc instanceof JavaBytecodeStackMark mark) {
            landKeys.add(mark.level);
        } else if (bc instanceof JavaBytecodeGuarded guarded) {
            freshenScan(guarded.inner, definedLabels, landKeys);
        } else if (bc instanceof JavaBytecodeInstructionInt
                || bc instanceof JavaBytecodeInstructionTryCatch
                || bc instanceof JavaBytecodeStoreLoad
                || bc instanceof JavaBytecodeJump
                || bc instanceof JavaBytecodeStackUnwind) {
        } else {
            throw new AssertionError("freshen: unsupported bytecode node " + bc.getClass());
        }
    }

    private static CodeDynamic freshenCode(
            CodeDynamic cd,
            IdentityHashMap<Label, Label> labelMap,
            FreshLabelNodes labelNodeMap,
            IdentityHashMap<JumpKey, JumpKey> keyMap) {
        var bc = cd.code();
        JavaBytecode result;
        if (bc instanceof CodeDynamic fbCd) {
            result = freshenCode(fbCd, labelMap, labelNodeMap, keyMap);
        } else if (bc instanceof JavaBytecodeSequence seq) {
            var out = new JavaBytecodeSequence();
            for (var c : seq.children) out.add(freshenCode(c, labelMap, labelNodeMap, keyMap));
            result = out;
        } else if (bc instanceof JavaBytecodeInstructionObj obj) {
            result = new JavaBytecodeInstructionObj(obj.node.clone(labelNodeMap));
        } else if (bc instanceof JavaBytecodeInstructionInt) {
            result = bc;
        } else if (bc instanceof JavaBytecodeInstructionTryCatch tc) {
            result = new JavaBytecodeInstructionTryCatch(
                    freshenLabel(tc.start, labelMap),
                    freshenLabel(tc.end, labelMap),
                    freshenLabel(tc.handler, labelMap),
                    tc.excType);
        } else if (bc instanceof JavaBytecodeStoreLoad) {
            result = bc;
        } else if (bc instanceof JavaBytecodeLand land) {
            var nk = keyMap.get(land.jumpKey);
            result = nk == null ? bc : new JavaBytecodeLand(nk);
        } else if (bc instanceof JavaBytecodeJump jump) {
            var fbNk = keyMap.get(jump.jumpKey);
            result = fbNk == null ? bc : new JavaBytecodeJump(fbNk);
        } else if (bc instanceof JavaBytecodeStackMark mark) {
            var nk = keyMap.get(mark.level);
            result = nk == null ? bc : new JavaBytecodeStackMark(nk);
        } else if (bc instanceof JavaBytecodeStackUnwind unwind) {
            var nk = keyMap.get(unwind.level);
            result = nk == null ? bc : new JavaBytecodeStackUnwind(nk, unwind.carry);
        } else if (bc instanceof JavaBytecodeGuarded guarded) {
            var nk = keyMap.get(guarded.dest);
            result = new JavaBytecodeGuarded(
                    nk == null ? guarded.dest : nk, freshenCode(guarded.inner, labelMap, labelNodeMap, keyMap));
        } else {
            throw new AssertionError("freshen: unsupported bytecode node " + bc.getClass());
        }
        return new CodeDynamic(cd.loc(), result);
    }

    private static Label freshenLabel(Label l, IdentityHashMap<Label, Label> labelMap) {
        var mapped = labelMap.get(l);
        return mapped == null ? l : mapped;
    }

    public static void renderMethod(JavaBytecodeSequence seq, MethodVisitor mv, TList<BindingId> initialIndexes) {
        var slots = new TList<JavaBytecodeSequence.InitialSlot>(initialIndexes.size());
        for (var k : initialIndexes) slots.add(new JavaBytecodeSequence.InitialSlot(k));
        seq.renderInternal(mv, slots);
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void renderMethodTyped(
            JavaBytecodeSequence seq,
            MethodVisitor mv,
            TList<JavaBytecodeSequence.InitialSlot> initialSlots,
            String context,
            JavaMethodDescriptor descriptor) {
        try {
            seq.renderInternal(mv, initialSlots);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "bytecode rendering failed for " + context + ":\n" + dumpMethod(seq, initialSlots, descriptor), e);
        }
        if (DUMP_BYTECODE) {
            System.err.println("bytecode for " + context + ":\n" + dumpMethod(seq, initialSlots, descriptor));
        }
        if (VERIFY_BYTECODE) {
            var dump = dumpMethod(seq, initialSlots, descriptor);
            if (!dump.startsWith("analyzer: ok")) {
                throw new IllegalStateException("bytecode verification failed for " + context + ":\n" + dump);
            }
        }
    }

    public static String dumpMethod(
            JavaBytecodeSequence seq,
            TList<JavaBytecodeSequence.InitialSlot> initialSlots,
            JavaMethodDescriptor descriptor) {
        var node = new MethodNode(Opcodes.ASM9, Opcodes.ACC_PUBLIC, "dump", descriptor.value(), null, null);
        seq.renderInternal(node, initialSlots);
        node.maxLocals = 512;
        node.maxStack = 512;
        var sb = new StringBuilder();
        var analyzer = new Analyzer<>(new BasicVerifier());
        try {
            analyzer.analyze("Dump", node);
            sb.append("analyzer: ok\n");
        } catch (AnalyzerException e) {
            sb.append("analyzer: ")
                    .append(e.getMessage())
                    .append(" at insn #")
                    .append(e.node == null ? -1 : node.instructions.indexOf(e.node))
                    .append("\n");
        }
        var textifier = new Textifier();
        var tracer = new TraceMethodVisitor(textifier);
        int index = 0;
        for (var insn : node.instructions) {
            insn.accept(tracer);
            for (var line : textifier.getText()) {
                sb.append('#').append(index).append(' ').append(line);
            }
            textifier.getText().clear();
            index++;
        }
        return sb.toString();
    }

    public static JavaBytecodeSequence seq() {
        return new JavaBytecodeSequence();
    }
}
