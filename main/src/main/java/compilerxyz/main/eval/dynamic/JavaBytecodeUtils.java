package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicVerifier;
import org.objectweb.asm.util.Textifier;
import org.objectweb.asm.util.TraceMethodVisitor;

public final class JavaBytecodeUtils {
    private JavaBytecodeUtils() {}

    private static final boolean VERIFY_BYTECODE = Boolean.getBoolean("compilerxyz.verifyBytecode");
    private static final boolean DUMP_BYTECODE = Boolean.getBoolean("compilerxyz.dumpBytecode");

    public static boolean isLoadOpcode(int opcode) {
        return switch (opcode) {
            case Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD -> true;
            case Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE -> false;
            default -> throw new IllegalArgumentException("not a local variable opcode: " + opcode);
        };
    }

    public static Opt<JavaBytecodeReg> lastOut(JavaBytecode bc) {
        return switch (bc) {
            case CodeDynamic cd -> lastOut(cd.code());
            case JavaBytecodeSequence seq -> {
                for (int i = seq.children.size() - 1; i >= 0; i--) {
                    var found = lastOut(seq.children.get(i));
                    if (found.optIsSome()) yield found;
                }
                yield Opt.none();
            }
            case JavaBytecodeInsn n -> n.out;
            case JavaBytecodeGuarded g -> lastOut(g.inner);
            default -> Opt.none();
        };
    }

    public static void renderMethod(
            JavaBytecodeSequence seq,
            MethodVisitor mv,
            TList<BindingId> initialIndexes,
            JavaInternalName owner,
            JavaBytecodeLocTable locTable) {
        var slots = new TList<JavaBytecodeSequence.InitialSlot>(initialIndexes.size());
        for (var k : initialIndexes) slots.add(new JavaBytecodeSequence.InitialSlot(k));
        var refs = new JavaBytecodeRenderRefs(owner, "");
        seq.renderInternal(mv, slots, refs, locTable);
        if (!refs.isEmpty()) {
            throw new IllegalStateException(
                    "method body references externals or definition sets but its class declares none: "
                            + owner.value());
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public static void renderMethodTyped(
            JavaBytecodeSequence seq,
            MethodVisitor mv,
            TList<JavaBytecodeSequence.InitialSlot> initialSlots,
            String context,
            int access,
            JavaMethodDescriptor descriptor,
            JavaBytecodeRenderRefs refs,
            JavaBytecodeLocTable locTable) {
        try {
            seq.renderInternal(mv, initialSlots, refs, locTable);
            mv.visitMaxs(0, 0);
            mv.visitEnd();
        } catch (RuntimeException e) {
            throw new IllegalStateException(
                    "bytecode rendering failed for " + context + ":\n"
                            + dumpMethod(seq, initialSlots, access, descriptor, refs),
                    e);
        }
        if (DUMP_BYTECODE) {
            System.err.println(
                    "bytecode for " + context + ":\n" + dumpMethod(seq, initialSlots, access, descriptor, refs));
        }
        if (VERIFY_BYTECODE) {
            var dump = dumpMethod(seq, initialSlots, access, descriptor, refs);
            if (!dump.startsWith("analyzer: ok")) {
                throw new IllegalStateException("bytecode verification failed for " + context + ":\n" + dump);
            }
        }
    }

    public static void renderMethodNode(
            ClassWriter cw, String name, JavaMethodDescriptor descriptor, MethodNode node, String context) {
        var access = Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC;
        var mv = cw.visitMethod(access, name, descriptor.value(), null, null);
        node.instructions.accept(mv);
        for (var tcb : node.tryCatchBlocks) {
            mv.visitTryCatchBlock(tcb.start.getLabel(), tcb.end.getLabel(), tcb.handler.getLabel(), tcb.type);
        }
        mv.visitMaxs(0, 0);
        mv.visitEnd();
        if (DUMP_BYTECODE) {
            System.err.println("bytecode for " + context + ":\n" + dumpNode(node, access, descriptor));
        }
        if (VERIFY_BYTECODE) {
            var dump = dumpNode(node, access, descriptor);
            if (!dump.startsWith("analyzer: ok")) {
                throw new IllegalStateException("bytecode verification failed for " + context + ":\n" + dump);
            }
        }
    }

    public static String dumpMethod(
            JavaBytecodeSequence seq,
            TList<JavaBytecodeSequence.InitialSlot> initialSlots,
            int access,
            JavaMethodDescriptor descriptor,
            JavaBytecodeRenderRefs refs) {
        var node = new MethodNode(Opcodes.ASM9, access, "dump", descriptor.value(), null, null);
        try {
            seq.renderInternal(node, initialSlots, refs, new JavaBytecodeLocTable());
        } catch (RuntimeException e) {
            return "render: " + e.getMessage() + "\n" + dumpNode(node, access, descriptor);
        }
        return dumpNode(node, access, descriptor);
    }

    private static String dumpNode(MethodNode source, int access, JavaMethodDescriptor descriptor) {
        var node = new MethodNode(Opcodes.ASM9, access, "dump", descriptor.value(), null, null);
        source.instructions.accept(node);
        for (var tcb : source.tryCatchBlocks) {
            node.visitTryCatchBlock(tcb.start.getLabel(), tcb.end.getLabel(), tcb.handler.getLabel(), tcb.type);
        }
        var sb = new StringBuilder();
        node.maxLocals = 512;
        node.maxStack = 512;
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
}
