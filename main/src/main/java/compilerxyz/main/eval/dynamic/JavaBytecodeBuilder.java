package compilerxyz.main.eval.dynamic;

import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.GETFIELD;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.ICONST_1;
import static org.objectweb.asm.Opcodes.INSTANCEOF;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.PUTFIELD;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class JavaBytecodeBuilder {
    private final CodeLoc loc;
    private final JavaBytecodeSequence seq = new JavaBytecodeSequence();

    public JavaBytecodeBuilder(QExprId loc) {
        this(new CodeLoc.Some(loc));
    }

    public JavaBytecodeBuilder(CodeLoc loc) {
        this.loc = loc;
    }

    public JavaBytecodeBuilder add(CodeDynamic code) {
        seq.add(code);
        return this;
    }

    public JavaBytecodeBuilder add(Opt<CodeDynamic> code) {
        if (code.optIsSome()) seq.add(code.optUnwrap());
        return this;
    }

    public EResult<Void> addCode(QExprId at, Code code) {
        var cast = CodeDynamic.castOpt(at, code);
        if (cast.hasErrors()) return cast.reerr();
        add(cast.requireValue("JavaBytecodeBuilder.addCode"));
        return EResult.ok(null);
    }

    public JavaBytecodeReg reg(CodeDynamicReg value) {
        seq.add(value.code());
        return value.reg();
    }

    private void addNode(JavaBytecode node) {
        seq.add(new CodeDynamic(CodeLoc.Infallible.INSTANCE, node));
    }

    public JavaBytecodeReg insn(JavaBytecodeInsnBody body, JavaDataDescriptor out, JavaBytecodeReg... args) {
        var reg = new JavaBytecodeReg(out);
        addNode(new JavaBytecodeInsn(TList.of(args), Opt.some(reg), body));
        return reg;
    }

    public JavaBytecodeReg insn(JavaBytecodeInsnBody body, JavaDataDescriptor out, TList<JavaBytecodeReg> args) {
        var reg = new JavaBytecodeReg(out);
        addNode(new JavaBytecodeInsn(TList.from(args), Opt.some(reg), body));
        return reg;
    }

    public void insnVoid(JavaBytecodeInsnBody body, JavaBytecodeReg... args) {
        addNode(new JavaBytecodeInsn(TList.of(args), Opt.none(), body));
    }

    public JavaBytecodeReg op(int opcode, JavaDataDescriptor out, JavaBytecodeReg... args) {
        return insn(new JavaBytecodeInsnBody.Op(opcode), out, args);
    }

    public JavaBytecodeReg node(AbstractInsnNode node, JavaDataDescriptor out, JavaBytecodeReg... args) {
        return insn(new JavaBytecodeInsnBody.Node(node), out, args);
    }

    public void nodeVoid(AbstractInsnNode node, JavaBytecodeReg... args) {
        insnVoid(new JavaBytecodeInsnBody.Node(node), args);
    }

    public JavaBytecodeReg constDefault(JavaDataDescriptor desc) {
        if (desc.isReference()) return constNull();
        return switch (desc.value) {
            case "J" -> constLong(0L);
            case "D" -> constDouble(0.0);
            case "F" -> constFloat(0.0f);
            default -> constInt(0);
        };
    }

    public JavaBytecodeReg constNull() {
        return op(ACONST_NULL, JvmConstants.DESC_OBJECT);
    }

    public JavaBytecodeReg constBool(boolean value) {
        return op(value ? ICONST_1 : ICONST_0, JavaDataDescriptor.DESC_BOOL);
    }

    public JavaBytecodeReg constInt(int value) {
        return node(new LdcInsnNode(value), JavaDataDescriptor.DESC_INT);
    }

    public JavaBytecodeReg constLong(long value) {
        return node(new LdcInsnNode(value), JavaDataDescriptor.DESC_LONG);
    }

    public JavaBytecodeReg constFloat(float value) {
        return node(new LdcInsnNode(value), JavaDataDescriptor.DESC_FLOAT);
    }

    public JavaBytecodeReg constDouble(double value) {
        return node(new LdcInsnNode(value), JavaDataDescriptor.DESC_DOUBLE);
    }

    public JavaBytecodeReg constString(String value) {
        return node(new LdcInsnNode(value), JvmConstants.DESC_STRING);
    }

    public JavaBytecodeReg load(BindingId slot, JavaDataDescriptor desc) {
        return insn(new JavaBytecodeInsnBody.Var(desc.loadOpcode(), slot), desc);
    }

    public void store(BindingId slot, JavaBytecodeReg value) {
        insnVoid(new JavaBytecodeInsnBody.Var(value.desc.storeOpcode(), slot), value);
    }

    public JavaBytecodeReg loadIndexed(int index, JavaDataDescriptor desc) {
        return insn(new JavaBytecodeInsnBody.VarIndexed(desc.loadOpcode(), index), desc);
    }

    public JavaBytecodeReg getField(
            JavaInternalName klass, String field, JavaDataDescriptor fieldDesc, JavaBytecodeReg base) {
        return node(new FieldInsnNode(GETFIELD, klass.value, field, fieldDesc.value), fieldDesc, base);
    }

    public void putField(
            JavaInternalName klass,
            String field,
            JavaDataDescriptor fieldDesc,
            JavaBytecodeReg base,
            JavaBytecodeReg value) {
        nodeVoid(new FieldInsnNode(PUTFIELD, klass.value, field, fieldDesc.value), base, value);
    }

    public JavaBytecodeReg getStatic(JavaInternalName klass, String field, JavaDataDescriptor fieldDesc) {
        return node(new FieldInsnNode(GETSTATIC, klass.value, field, fieldDesc.value), fieldDesc);
    }

    public JavaBytecodeReg newInstance(JavaInternalName klass, JavaMethodDescriptor ctor, JavaBytecodeReg... args) {
        return insn(new JavaBytecodeInsnBody.New(klass, ctor), JavaDataDescriptor.fromInternalName(klass), args);
    }

    public JavaBytecodeReg newInstance(JavaInternalName klass, JavaMethodDescriptor ctor, TList<JavaBytecodeReg> args) {
        return insn(new JavaBytecodeInsnBody.New(klass, ctor), JavaDataDescriptor.fromInternalName(klass), args);
    }

    public JavaBytecodeReg newObjectArray(TList<JavaBytecodeReg> elements) {
        return insn(
                new JavaBytecodeInsnBody.ArrayNew(JvmConstants.INTNAME_OBJECT),
                new JavaDataDescriptor("[" + JvmConstants.DESC_OBJECT.value),
                elements);
    }

    public Opt<JavaBytecodeReg> invoke(
            int opcode,
            JavaInternalName klass,
            String method,
            JavaMethodDescriptor desc,
            boolean ownerIsInterface,
            TList<JavaBytecodeReg> args) {
        var node = new MethodInsnNode(opcode, klass.value, method, desc.value, ownerIsInterface);
        var ret = desc.returnDescriptor();
        if (ret.value.equals(JavaDataDescriptor.DESC_VOID.value)) {
            addNode(new JavaBytecodeInsn(TList.from(args), Opt.none(), new JavaBytecodeInsnBody.Node(node)));
            return Opt.none();
        }
        return Opt.some(insn(new JavaBytecodeInsnBody.Node(node), ret, args));
    }

    private JavaBytecodeReg invokeValue(
            int opcode,
            JavaInternalName klass,
            String method,
            JavaMethodDescriptor desc,
            boolean ownerIsInterface,
            JavaBytecodeReg... args) {
        var out = invoke(opcode, klass, method, desc, ownerIsInterface, TList.of(args));
        if (out.optIsNone()) {
            throw new IllegalStateException("JavaBytecodeBuilder: method " + klass + "." + method + desc
                    + " returns void but a value was expected");
        }
        return out.optUnwrap();
    }

    public JavaBytecodeReg invokeStatic(
            JavaInternalName klass, String method, JavaMethodDescriptor desc, JavaBytecodeReg... args) {
        return invokeValue(INVOKESTATIC, klass, method, desc, false, args);
    }

    public JavaBytecodeReg invokeStatic(JavaStaticMethodRef ref, JavaBytecodeReg... args) {
        return invokeValue(INVOKESTATIC, ref.owner, ref.name, ref.descriptor, ref.ownerIsInterface, args);
    }

    public JavaBytecodeReg invokeVirtual(
            JavaInternalName klass, String method, JavaMethodDescriptor desc, JavaBytecodeReg... args) {
        return invokeValue(INVOKEVIRTUAL, klass, method, desc, false, args);
    }

    public JavaBytecodeReg invokeVirtual(JavaVirtualMethodRef ref, JavaBytecodeReg... args) {
        return invokeValue(
                ref.ownerIsInterface ? INVOKEINTERFACE : INVOKEVIRTUAL,
                ref.owner,
                ref.name,
                ref.descriptor,
                ref.ownerIsInterface,
                args);
    }

    public void invokeVirtualVoid(
            JavaInternalName klass, String method, JavaMethodDescriptor desc, JavaBytecodeReg... args) {
        invoke(INVOKEVIRTUAL, klass, method, desc, false, TList.of(args));
    }

    public JavaBytecodeReg invokeInterface(
            JavaInternalName klass, String method, JavaMethodDescriptor desc, JavaBytecodeReg... args) {
        return invokeValue(INVOKEINTERFACE, klass, method, desc, true, args);
    }

    public void invokeSpecialVoid(
            JavaInternalName klass, String method, JavaMethodDescriptor desc, JavaBytecodeReg... args) {
        invoke(INVOKESPECIAL, klass, method, desc, false, TList.of(args));
    }

    public JavaBytecodeReg checkcast(JavaDataDescriptor to, JavaBytecodeReg value) {
        var internal = to.value.startsWith("L") ? to.value.substring(1, to.value.length() - 1) : to.value;
        return node(new TypeInsnNode(CHECKCAST, internal), to, value);
    }

    public JavaBytecodeReg instanceOf(JavaInternalName klass, JavaBytecodeReg value) {
        return node(new TypeInsnNode(INSTANCEOF, klass.value), JavaDataDescriptor.DESC_BOOL, value);
    }

    public void land(JumpKey key, JavaBytecodeReg... params) {
        addNode(new JavaBytecodeLand(key, TList.of(params)));
    }

    public void jump(JumpKey key, JavaBytecodeReg... args) {
        addNode(new JavaBytecodeJump(key, TList.of(args)));
    }

    public void branch(int opcode, JumpKey key, JavaBytecodeReg... args) {
        addNode(new JavaBytecodeBranch(opcode, TList.of(args), key));
    }

    public void ret(Opt<JavaBytecodeReg> value) {
        addNode(new JavaBytecodeReturn(value));
    }

    public void ret(JavaBytecodeReg value) {
        addNode(new JavaBytecodeReturn(Opt.some(value)));
    }

    public void retVoid() {
        addNode(new JavaBytecodeReturn(Opt.none()));
    }

    public void athrow(JavaBytecodeReg exception) {
        addNode(new JavaBytecodeThrow(exception));
    }

    public boolean isEmpty() {
        return seq.children.isEmpty();
    }

    public CodeDynamic code() {
        return new CodeDynamic(loc, seq);
    }

    public CodeDynamicReg code(JavaBytecodeReg reg) {
        return new CodeDynamicReg(code(), reg);
    }
}
