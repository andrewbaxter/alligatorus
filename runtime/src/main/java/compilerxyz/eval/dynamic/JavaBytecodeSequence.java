package compilerxyz.eval.dynamic;

import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.BASTORE;
import static org.objectweb.asm.Opcodes.DSTORE;
import static org.objectweb.asm.Opcodes.FSTORE;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.IFEQ;
import static org.objectweb.asm.Opcodes.IFGE;
import static org.objectweb.asm.Opcodes.IFGT;
import static org.objectweb.asm.Opcodes.IFLE;
import static org.objectweb.asm.Opcodes.IFLT;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.IFNONNULL;
import static org.objectweb.asm.Opcodes.IFNULL;
import static org.objectweb.asm.Opcodes.IF_ACMPEQ;
import static org.objectweb.asm.Opcodes.IF_ACMPNE;
import static org.objectweb.asm.Opcodes.IF_ICMPEQ;
import static org.objectweb.asm.Opcodes.IF_ICMPGE;
import static org.objectweb.asm.Opcodes.IF_ICMPGT;
import static org.objectweb.asm.Opcodes.IF_ICMPLE;
import static org.objectweb.asm.Opcodes.IF_ICMPLT;
import static org.objectweb.asm.Opcodes.IF_ICMPNE;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.JSR;
import static org.objectweb.asm.Opcodes.LSTORE;

import compilerxyz.eval.QExprId;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Function;

import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.pcollections.HashTreePMap;
import org.pcollections.PMap;
import org.pcollections.PVector;
import org.pcollections.TreePVector;

public class JavaBytecodeSequence implements JavaBytecode {
  public TList<CodeDynamic> children = new TList<>();

  static void dispatchAsmFraming(AbstractInsnNode n, AsmFramingVisitor v) {
    n.accept(
        new MethodVisitor(ASM8) {
          @Override
          public void visitInsn(int opcode) {
            throw new AssertionError();
          }

          @Override
          public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
            throw new AssertionError();
          }

          @Override
          public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
            v.handleTable(dflt, labels);
          }

          @Override
          public void visitJumpInsn(int opcode, Label label) {
            switch (opcode) {
              case IFEQ:
              case IFNE:
              case IFLT:
              case IFGE:
              case IFGT:
              case IFLE:
              case IF_ICMPEQ:
              case IF_ICMPNE:
              case IF_ICMPLT:
              case IF_ICMPGE:
              case IF_ICMPGT:
              case IF_ICMPLE:
              case IF_ACMPEQ:
              case IF_ACMPNE:
              case IFNULL:
              case IFNONNULL:
                v.handleBranch(label);
                break;
              case GOTO:
                v.handleJump(label);
                break;
              case JSR:
                throw new AssertionError();
              default:
                throw new AssertionError();
            }
          }

          @Override
          public void visitLabel(Label label) {
            v.handleLabel(label);
          }
        });
  }

    private void walk(Function<CodeDynamic, Iterator<CodeDynamic>> handler) {
        var stack = new ArrayDeque<Iterator<CodeDynamic>>();
        {
            var iter = this.children.iterator();
            if (iter.hasNext()) stack.addLast(iter);
        }
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

  private static CodeDynamic wrap(compilerxyz.eval.QExprId loc, JavaBytecode bc) {
    return new CodeDynamic(loc, bc);
  }

    public TMap<Integer, QExprId> render(
            MethodVisitor out, TList<JavaBytecodeBindingKey> initialIndexes) {
        var locMap = new TMap<Integer, QExprId>();
        final int[] locCounter = {0};
        var catchLastUses = new TMap<JavaBytecodeCatchKey, Integer>();
        {
            final int[] catchIndex = {0};
            walk(
                    cd ->
                            cd.code()
                                    .dispatch(
                                            new DefaultDispatcher<Iterator<CodeDynamic>>() {
                                                @Override
                                                public Iterator<CodeDynamic> handleSequence(JavaBytecodeSequence n) {
                                                    return n.children.iterator();
                                                }

                                                @Override
                                                public Iterator<CodeDynamic> handleCatch(JavaBytecodeCatch n) {
                                                    catchIndex[0] += 1;
                                                    catchLastUses.put(n.key, catchIndex[0]);
                                                    return TList.of(n.inner).iterator();
                                                }
                                            }));
        }
        var flattened = new TList<JavaBytecode>();
        {
            var jumpLabels = new TMap<JumpKey, LabelNode>();
            var catchStartLabels = new TMap<JavaBytecodeCatchKey, Label>();
            var catchFinallyLabels = new TMap<JavaBytecodeCatchKey, Label>();
            final int[] catchIndex = {0};
            walk(
                    cd -> {
                        locCounter[0] += 1;
                        int line = locCounter[0];
                        locMap.put(line, cd.loc());
                        var label = new LabelNode(new Label());
                        flattened.add(new JavaBytecodeInstructionObj(label));
                        flattened.add(
                                new JavaBytecodeInstructionObj(
                                        new org.objectweb.asm.tree.LineNumberNode(line, label)));
                        return cd.code()
                                .dispatch(
                                        new Dispatcher<Iterator<CodeDynamic>>() {
                                            @Override
                                            public Iterator<CodeDynamic> handleInstruction(JavaBytecodeInstruction n) {
                                                flattened.add(n);
                                                return null;
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleSequence(JavaBytecodeSequence n) {
                                                return n.children.iterator();
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleStoreLoad(JavaBytecodeStoreLoad n) {
                                                flattened.add(n);
                                                return null;
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleCatch(JavaBytecodeCatch n) {
                                                catchIndex[0] += 1;

                                                final Label endLabel = new Label();
                                                flattened.add(new JavaBytecodeInstructionObj(new LabelNode(endLabel)));
                                                flattened.add(
                                                        new JavaBytecodeInstructionTryCatch(
                                                                catchStartLabels.getOrNull(n.key),
                                                                endLabel,
                                                                catchFinallyLabels.getOrNull(n.key),
                                                                "java/lang/RuntimeException"));
                                                var enclosingLoc = cd.loc();
                                                if (catchIndex[0] < catchLastUses.getOrNull(n.key)) {
                                                    return TList.of(
                                                                    n.inner,
                                                                    wrap(
                                                                            enclosingLoc,
                                                                            new JavaBytecodeOther<>(
                                                                                    () -> {
                                                                                        final Label newStartLabel = new Label();
                                                                                        flattened.add(
                                                                                                new JavaBytecodeInstructionObj(
                                                                                                        new LabelNode(newStartLabel)));
                                                                                        catchStartLabels.put(n.key, newStartLabel);
                                                                                        return null;
                                                                                    })))
                                                            .iterator();
                                                } else {
                                                    final LabelNode postLabelNode = new LabelNode(new Label());
                                                    final JavaBytecodeBindingKey exceptionKey =
                                                            new JavaBytecodeBindingKey();
                                                    return TList.of(
                                                                    n.inner,
                                                                    wrap(
                                                                            enclosingLoc,
                                                                            new JavaBytecodeOther<>(
                                                                                    () -> {
                                                                                        flattened.add(
                                                                                                new JavaBytecodeInstructionObj(
                                                                                                        new JumpInsnNode(GOTO, postLabelNode)));
                                                                                        flattened.add(
                                                                                                new JavaBytecodeInstructionObj(
                                                                                                        new LabelNode(catchFinallyLabels.getOrNull(n.key))));
                                                                                        flattened.add(new JavaBytecodeStoreLoad(ASTORE, exceptionKey));
                                                                                        return null;
                                                                                    })),
                                                                    n.inner,
                                                                    wrap(
                                                                            enclosingLoc,
                                                                            new JavaBytecodeOther<>(
                                                                                    () -> {
                                                                                        flattened.add(new JavaBytecodeStoreLoad(ALOAD, exceptionKey));
                                                                                        flattened.add(new JavaBytecodeInstructionInt(ATHROW));
                                                                                        flattened.add(new JavaBytecodeInstructionObj(postLabelNode));
                                                                                        return null;
                                                                                    })))
                                                            .iterator();
                                                }
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleCatchStart(JavaBytecodeCatchStart n) {
                                                if (!catchLastUses.containsKey(n.key)) return null;
                                                final Label startLabel = new Label();
                                                final Label handleLabel = new Label();
                                                flattened.add(new JavaBytecodeInstructionObj(new LabelNode(startLabel)));
                                                catchStartLabels.put(n.key, startLabel);
                                                catchFinallyLabels.put(n.key, handleLabel);
                                                return null;
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleLand(JavaBytecodeLand n) {
                                                flattened.add(
                                                        new JavaBytecodeInstructionObj(
                                                                jumpLabels.computeIfAbsentT(n.jumpKey, k -> new LabelNode())));
                                                return null;
                                            }

                                            @Override
                                            public Iterator<CodeDynamic> handleJump(JavaBytecodeJump n) {
                                                flattened.add(
                                                        new JavaBytecodeInstructionObj(
                                                                new JumpInsnNode(
                                                                        GOTO,
                                                                        jumpLabels.computeIfAbsentT(n.jumpKey, k -> new LabelNode()))));
                                                return null;
                                            }
                                        });
                    });
        }
        var bindingLastUses = new TMap<JavaBytecodeBindingKey, TList<Integer>>();
        {
            var totalUses = new TMap<JavaBytecodeBindingKey, Integer>();
            @SuppressWarnings("unchecked") final PMap<JavaBytecodeBindingKey, Integer>[] currentBindingLastUses =
                    new PMap[]{HashTreePMap.<JavaBytecodeBindingKey, Integer>empty()};
            var branchBindingLastUses = new TMap<Label, PMap<JavaBytecodeBindingKey, Integer>>();
            int index = 0;
            for (int i = flattened.size() - 1; i >= 0; i--) {
                JavaBytecode e = flattened.get(i);
                int finalIndex = index;
                e.dispatch(
                        new DefaultDispatcher<Object>() {
                            @Override
                            public Object handleInstruction(JavaBytecodeInstruction n) {
                                n.dispatchMore(
                                        new JavaBytecodeInstruction.MoreDispatcher() {
                                            @Override
                                            public void handleObj(JavaBytecodeInstructionObj n) {
                                                dispatchAsmFraming(
                                                        n.node,
                                                        new AsmFramingVisitor() {
                                                            @Override
                                                            public void handleTable(Label dflt, Label[] labels) {
                                                                final HashMap<JavaBytecodeBindingKey, Integer> merged =
                                                                        new HashMap<>(
                                                                                emptyIfNull(branchBindingLastUses.getOrNull(dflt)));
                                                                for (Label label : labels) {
                                                                    merged.putAll(emptyIfNull(branchBindingLastUses.getOrNull(label)));
                                                                }
                                                                currentBindingLastUses[0] = HashTreePMap.from(merged);
                                                            }

                                                            @Override
                                                            public void handleBranch(Label label) {
                                                                final HashMap<JavaBytecodeBindingKey, Integer> merged =
                                                                        new HashMap<>();
                                                                merged.putAll(currentBindingLastUses[0]);
                                                                merged.putAll(
                                                                        emptyIfNull(branchBindingLastUses.getOrNull(label)));
                                                                currentBindingLastUses[0] = HashTreePMap.from(merged);
                                                            }

                                                            @Override
                                                            public void handleJump(Label label) {
                                                                currentBindingLastUses[0] =
                                                                        emptyIfNull(branchBindingLastUses.getOrNull(label));
                                                            }

                                                            @Override
                                                            public void handleLabel(Label label) {
                                                                branchBindingLastUses.put(label, currentBindingLastUses[0]);
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void handleInt(JavaBytecodeInstructionInt n) {
                                                if (n.code == ATHROW) {
                                                    currentBindingLastUses[0] = HashTreePMap.empty();
                                                }
                                            }

                                            @Override
                                            public void handleTryCatch(JavaBytecodeInstructionTryCatch n) {
                                            }
                                        });
                                return null;
                            }

                            @Override
                            public Object handleStoreLoad(JavaBytecodeStoreLoad n) {
                                if (currentBindingLastUses[0].get(n.key) == null) {
                                    bindingLastUses.computeIfAbsentT(n.key, k -> new TList<>()).add(finalIndex);
                                    currentBindingLastUses[0] = currentBindingLastUses[0].plus(n.key, finalIndex);
                                }
                                totalUses.put(n.key, totalUses.getOrDefault(n.key, 0) + 1);
                                return null;
                            }
                        });
                index += 1;
            }
            for (Map.Entry<JavaBytecodeBindingKey, TList<Integer>> use : bindingLastUses.entrySet()) {
                final TList<Integer> lastUses = use.getValue();
                for (int i = 0; i < lastUses.size(); i++) {
                    lastUses.set(i, totalUses.get(use.getKey()) - 1 - lastUses.get(i));
                }
            }
        }
        {
            @SuppressWarnings("unchecked") final PMap<JavaBytecodeBindingKey, Integer>[] currentIndexes =
                    new PMap[]{HashTreePMap.empty()};
            @SuppressWarnings("unchecked") final PVector<JavaBytecodeBindingKey>[] currentRevIndexes =
                    new PVector[]{TreePVector.empty()};
            var branchIndexes =
                    new TMap<Label, Pair<PMap<JavaBytecodeBindingKey, Integer>, PVector<JavaBytecodeBindingKey>>>();
            for (int i = 0; i < initialIndexes.size(); i++) {
                final JavaBytecodeBindingKey key = initialIndexes.get(i);
                currentIndexes[0] = currentIndexes[0].plus(key, i);
                currentRevIndexes[0] = currentRevIndexes[0].plus(key);
            }
            int codeIndex = 0;
            for (JavaBytecode n : flattened) {
                int finalCodeIndex = codeIndex;
                n.dispatch(
                        new Dispatcher<Object>() {
                            @Override
                            public Object handleInstruction(JavaBytecodeInstruction n) {
                                n.dispatchMore(
                                        new JavaBytecodeInstruction.MoreDispatcher() {
                                            @Override
                                            public void handleObj(JavaBytecodeInstructionObj n) {
                                                n.node.accept(out);

                                                dispatchAsmFraming(
                                                        n.node,
                                                        new AsmFramingVisitor() {
                                                            void setFrameInitialVars(Label l) {
                                                                var pair = new Pair<>(currentIndexes[0], currentRevIndexes[0]);
                                                                var old = branchIndexes.put(l, pair);
                                                                if (old != null) {
                                                                    if (old.second().size() != currentRevIndexes[0].size()) {
                                                                        throw new AssertionError();
                                                                    }
                                                                    for (int i = 0; i < old.second().size(); ++i) {
                                                                        if (currentRevIndexes[0].get(i) != old.second().get(i)) {
                                                                            throw new AssertionError();
                                                                        }
                                                                    }
                                                                }
                                                            }

                                                            @Override
                                                            public void handleTable(Label dflt, Label[] labels) {
                                                                setFrameInitialVars(dflt);
                                                                for (Label label : labels) setFrameInitialVars(label);
                                                                currentIndexes[0] = null;
                                                                currentRevIndexes[0] = null;
                                                            }

                                                            @Override
                                                            public void handleBranch(Label label) {
                                                                setFrameInitialVars(label);
                                                            }

                                                            @Override
                                                            public void handleJump(Label label) {
                                                                setFrameInitialVars(label);
                                                                currentIndexes[0] = null;
                                                                currentRevIndexes[0] = null;
                                                            }

                                                            @Override
                                                            public void handleLabel(Label label) {
                                                                var p = branchIndexes.remove(label);
                                                                if (p != null) {
                                                                    currentIndexes[0] = p.first();
                                                                    currentRevIndexes[0] = p.second();
                                                                }
                                                            }
                                                        });
                                            }

                                            @Override
                                            public void handleInt(JavaBytecodeInstructionInt n) {
                                                out.visitInsn(n.code);
                                            }

                                            @Override
                                            public void handleTryCatch(JavaBytecodeInstructionTryCatch n) {
                                                out.visitTryCatchBlock(n.start, n.end, n.handler, n.excType);
                                            }
                                        });
                                return null;
                            }

                            @Override
                            public Object handleSequence(JavaBytecodeSequence n) {
                                throw new AssertionError();
                            }

                            @Override
                            public Object handleStoreLoad(JavaBytecodeStoreLoad storeLoad) {
                                int index = currentIndexes[0].getOrDefault(storeLoad.key, -1);
                                if (index == -1) {
                                    if (storeLoad.code == ISTORE
                                            || storeLoad.code == LSTORE
                                            || storeLoad.code == ASTORE
                                            || storeLoad.code == BASTORE
                                            || storeLoad.code == FSTORE
                                            || storeLoad.code == DSTORE) {
                                        for (int j = 0; j < currentRevIndexes[0].size(); j++) {
                                            if (currentRevIndexes[0].get(j) != null) continue;
                                            currentIndexes[0] = currentIndexes[0].plus(storeLoad.key, j);
                                            currentRevIndexes[0] = currentRevIndexes[0].with(j, storeLoad.key);
                                            index = j;
                                        }
                                        if (index == -1) {
                                            index = currentIndexes[0].size();
                                            currentIndexes[0] = currentIndexes[0].plus(storeLoad.key, index);
                                            currentRevIndexes[0] = currentRevIndexes[0].plus(storeLoad.key);
                                        }
                                    } else {
                                        throw new AssertionError();
                                    }
                                }
                                out.visitVarInsn(storeLoad.code, index);
                                if (bindingLastUses.get(storeLoad.key).lastIndexOf(finalCodeIndex) != -1) {
                                    currentIndexes[0] = currentIndexes[0].minus(storeLoad.key);
                                    currentRevIndexes[0] = currentRevIndexes[0].with(index, null);
                                }
                                return null;
                            }

                            @Override
                            public Object handleCatch(JavaBytecodeCatch n) {
                                throw new AssertionError();
                            }

                            @Override
                            public Object handleCatchStart(JavaBytecodeCatchStart n) {
                                throw new AssertionError();
                            }

                            @Override
                            public Object handleLand(JavaBytecodeLand n) {
                                throw new AssertionError();
                            }

                            @Override
                            public Object handleJump(JavaBytecodeJump n) {
                                throw new AssertionError();
                            }
                        });
                codeIndex += 1;
            }
        }

        return locMap;
    }

  private static PMap<JavaBytecodeBindingKey, Integer> emptyIfNull(
      PMap<JavaBytecodeBindingKey, Integer> m) {
    return m == null ? HashTreePMap.empty() : m;
  }

    public JavaBytecodeSequence add(CodeDynamic element) {
        if (element == null) return this;
        children.add(element);
        return this;
    }

  public int size() {
    return children.size();
  }

  @Override
  public <T> T dispatch(Dispatcher<T> dispatcher) {
    return dispatcher.handleSequence(this);
  }

  interface AsmFramingVisitor {
    void handleTable(Label dflt, Label[] labels);

    void handleBranch(Label label);

    void handleJump(Label label);

    void handleLabel(Label label);
  }
}
