package compilerxyz.eval.dynamic;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.DLOAD;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.FLOAD;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LLOAD;
import static org.objectweb.asm.Opcodes.LSTORE;

import compilerxyz.eval.JumpKey;
import compilerxyz.eval.QExprId;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TSet;
import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.function.Function;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;

public class JavaBytecodeSequence implements JavaBytecode {
    public TList<CodeDynamic> children = new TList<>();

    private static void walkFrom(Iterator<CodeDynamic> root, Function<CodeDynamic, Iterator<CodeDynamic>> handler) {
        var stack = new ArrayDeque<Iterator<CodeDynamic>>();
        if (root.hasNext()) stack.addLast(root);
        while (!stack.isEmpty()) {
            CodeDynamic next;
            {
                var iter = stack.peekLast();
                next = iter.next();
                if (!iter.hasNext()) stack.removeLast();
            }
            var res = handler.apply(next);
            if (res != null && res.hasNext()) stack.addLast(res);
        }
    }

    public record InitialSlot(JavaBytecodeBindingKey key, boolean wide) {
        public InitialSlot(JavaBytecodeBindingKey key) {
            this(key, false);
        }
    }

    TMap<Integer, QExprId> renderInternal(
            MethodVisitor out, TList<JavaBytecodeBindingKey> initialIndexes, TList<InitialSlot> initialSlotWidths) {
        var locMap = new TMap<Integer, QExprId>();
        final int[] locCounter = {0};
        var flattened = new TList<JavaBytecode>();
        {
            var jumpLabels = new TMap<JumpKey, LabelNode>();
            final int[] realInsnCount = {0};
            var destStack = new TList<JumpKey>();
            final JumpKey[] runDest = {null};
            final LabelNode[] runStart = {null};
            final int[] runStartCount = {0};
            class DestRuns {
                void boundary(JumpKey newDest) {
                    if (newDest == runDest[0]) return;
                    var here = new LabelNode(new Label());
                    flattened.add(new JavaBytecodeInstructionObj(here));
                    if (runDest[0] != null && runStartCount[0] != realInsnCount[0]) {
                        flattened.add(new JavaBytecodeInstructionTryCatch(
                                runStart[0].getLabel(),
                                here.getLabel(),
                                jumpLabels
                                        .computeIfAbsentT(runDest[0], k -> new LabelNode())
                                        .getLabel(),
                                null));
                    }
                    runDest[0] = newDest;
                    runStart[0] = here;
                    runStartCount[0] = realInsnCount[0];
                }
            }
            var destRuns = new DestRuns();
            @SuppressWarnings("unchecked")
            Function<CodeDynamic, Iterator<CodeDynamic>>[] flattenStepRef = new Function[1];
            Function<CodeDynamic, Iterator<CodeDynamic>> flattenStep = cd -> {
                switch (cd.loc()) {
                    case CodeLoc.Some some -> {
                        locCounter[0] += 1;
                        int line = locCounter[0];
                        locMap.put(line, some.qExprId());
                        var label = new LabelNode(new Label());
                        flattened.add(new JavaBytecodeInstructionObj(label));
                        flattened.add(new JavaBytecodeInstructionObj(new LineNumberNode(line, label)));
                    }
                    case CodeLoc.Infallible i -> {}
                }
                return cd.code().javaBytecodeDispatch(new Dispatcher<Iterator<CodeDynamic>>() {
                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleInstruction(JavaBytecodeInstruction n) {
                        if (!(n instanceof JavaBytecodeInstructionTryCatch)
                                && !(n instanceof JavaBytecodeInstructionObj obj
                                        && (obj.node instanceof LabelNode || obj.node instanceof LineNumberNode))) {
                            realInsnCount[0] += 1;
                        }
                        flattened.add(n);
                        return null;
                    }

                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleSequence(JavaBytecodeSequence n) {
                        return n.children.iterator();
                    }

                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleStoreLoad(JavaBytecodeStoreLoad n) {
                        realInsnCount[0] += 1;
                        flattened.add(n);
                        return null;
                    }

                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleLand(JavaBytecodeLand n) {
                        flattened.add(new JavaBytecodeInstructionObj(
                                jumpLabels.computeIfAbsentT(n.jumpKey, k -> new LabelNode())));
                        return null;
                    }

                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleJump(JavaBytecodeJump n) {
                        realInsnCount[0] += 1;
                        flattened.add(new JavaBytecodeInstructionObj(
                                new JumpInsnNode(GOTO, jumpLabels.computeIfAbsentT(n.jumpKey, k -> new LabelNode()))));
                        return null;
                    }

                    @Override
                    public Iterator<CodeDynamic> dispatcherHandleGuarded(JavaBytecodeGuarded n) {
                        destRuns.boundary(n.dest);
                        destStack.add(n.dest);
                        walkFrom(TList.of(n.inner).iterator(), flattenStepRef[0]);
                        destStack.remove(destStack.size() - 1);
                        destRuns.boundary(destStack.isEmpty() ? null : destStack.get(destStack.size() - 1));
                        return null;
                    }
                });
            };
            flattenStepRef[0] = flattenStep;
            walkFrom(this.children.iterator(), flattenStep);
            if (!destStack.isEmpty() || runDest[0] != null) {
                throw new AssertionError("unbalanced exception-dest guard at end of method body");
            }
        }
        TList<JavaBytecode> emitList;
        {
            var loadedKeys = new TSet<JavaBytecodeBindingKey>();
            for (var n : flattened) {
                if (n instanceof JavaBytecodeStoreLoad sl
                        && (sl.code == ALOAD
                                || sl.code == ILOAD
                                || sl.code == FLOAD
                                || sl.code == LLOAD
                                || sl.code == DLOAD)) {
                    loadedKeys.add(sl.key);
                }
            }
            emitList = new TList<>();
            for (int i = 0; i < flattened.size(); i++) {
                var n = flattened.get(i);
                if (n instanceof JavaBytecodeInstructionInt ii
                        && (ii.code == DUP || ii.code == DUP2)
                        && i + 1 < flattened.size()
                        && flattened.get(i + 1) instanceof JavaBytecodeStoreLoad sl
                        && !loadedKeys.contains(sl.key)
                        && (ii.code == DUP
                                ? (sl.code == ASTORE || sl.code == ISTORE || sl.code == FSTORE)
                                : (sl.code == LSTORE || sl.code == DSTORE))) {
                    i += 1;
                    continue;
                }
                emitList.add(n);
            }
        }
        {
            var slotIndexes = new TMap<JavaBytecodeBindingKey, Integer>();
            final int[] nextSlot = {0};
            for (var s : initialSlotWidths) {
                slotIndexes.put(s.key(), nextSlot[0]);
                nextSlot[0] += s.wide() ? 2 : 1;
            }
            var pendingTryCatchBlocks = new TList<JavaBytecodeInstructionTryCatch>();
            var emittedLabels = new TSet<Label>();
            for (JavaBytecode n : emitList) {
                n.javaBytecodeDispatch(new Dispatcher<Object>() {
                    @Override
                    public Object dispatcherHandleInstruction(JavaBytecodeInstruction n) {
                        n.javaBytecodeInstructionDispatchMore(new JavaBytecodeInstruction.MoreDispatcher() {
                            @Override
                            public void moreDispatcherHandleObj(JavaBytecodeInstructionObj n) {
                                if (n.node instanceof LabelNode ln && !emittedLabels.add(ln.getLabel())) {
                                    throw new AssertionError(
                                            "duplicate label emission — code fragment" + " reused without freshen");
                                }
                                n.node.accept(out);
                            }

                            @Override
                            public void moreDispatcherHandleInt(JavaBytecodeInstructionInt n) {
                                out.visitInsn(n.code);
                            }

                            @Override
                            public void moreDispatcherHandleTryCatch(JavaBytecodeInstructionTryCatch n) {
                                pendingTryCatchBlocks.add(n);
                            }
                        });
                        return null;
                    }

                    @Override
                    public Object dispatcherHandleSequence(JavaBytecodeSequence n) {
                        throw new AssertionError();
                    }

                    @Override
                    public Object dispatcherHandleStoreLoad(JavaBytecodeStoreLoad storeLoad) {
                        var index = slotIndexes.getOrNull(storeLoad.key);
                        if (index == null) {
                            boolean wide = storeLoad.code == DSTORE
                                    || storeLoad.code == LSTORE
                                    || storeLoad.code == DLOAD
                                    || storeLoad.code == LLOAD;
                            index = nextSlot[0];
                            slotIndexes.put(storeLoad.key, index);
                            nextSlot[0] += wide ? 2 : 1;
                        }
                        out.visitVarInsn(storeLoad.code, index);
                        return null;
                    }

                    @Override
                    public Object dispatcherHandleLand(JavaBytecodeLand n) {
                        throw new AssertionError();
                    }

                    @Override
                    public Object dispatcherHandleJump(JavaBytecodeJump n) {
                        throw new AssertionError();
                    }

                    @Override
                    public Object dispatcherHandleGuarded(JavaBytecodeGuarded n) {
                        throw new AssertionError();
                    }
                });
            }
            for (var n : pendingTryCatchBlocks) {
                out.visitTryCatchBlock(n.start, n.end, n.handler, n.excType);
            }
        }

        return locMap;
    }

    public JavaBytecodeSequence add(CodeDynamic element) {
        if (element == null) return this;
        children.add(element);
        return this;
    }

    @Override
    public <T> T javaBytecodeDispatch(Dispatcher<T> dispatcher) {
        return dispatcher.dispatcherHandleSequence(this);
    }
}
