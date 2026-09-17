package compilerxyz.main.eval.dynamic;

import static org.objectweb.asm.Opcodes.AASTORE;
import static org.objectweb.asm.Opcodes.ANEWARRAY;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.DUP2;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.RETURN;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.Iterator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;

public final class JavaBytecodeSequence implements JavaBytecode {
    public TList<CodeDynamic> children = new TList<>();

    public record InitialSlot(BindingId key, boolean wide) {
        public InitialSlot(BindingId key) {
            this(key, false);
        }
    }

    public JavaBytecodeSequence add(CodeDynamic element) {
        if (element == null) return this;
        children.add(element);
        return this;
    }

    private sealed interface Item {
        record Insn(JavaBytecodeInsn n) implements Item {}

        record Land(JavaBytecodeLand n) implements Item {}

        record Jump(JavaBytecodeJump n) implements Item {}

        record Branch(JavaBytecodeBranch n) implements Item {}

        record Return(JavaBytecodeReturn n) implements Item {}

        record Throw(JavaBytecodeThrow n) implements Item {}

        record Line(Label label, int line) implements Item {}

        record RangeLabel(Label label) implements Item {}

        record TryRange(Label start, Label end, JumpKey handler) implements Item {}
    }

    private enum RegKind {
        STACK,
        LOCAL,
        DEAD,
        PARAM,
        DEFER
    }

    private static final class RegInfo {
        int uses;
        int lastUse = -1;
        int def = -1;
        RegKind kind = RegKind.DEAD;
        boolean dupOk = true;
        int remaining;
    }

    private record Prefix(int consumer, int arg) {}

    TMap<Integer, QExprId> renderInternal(MethodVisitor out, TList<InitialSlot> initialSlots) {
        var locMap = new TMap<Integer, QExprId>();

        var flat = new TList<Item>();
        final int[] locCounter = {0};
        final int[] realCount = {0};
        var destStack = new TList<JumpKey>();
        final JumpKey[] runDest = {null};
        final Label[] runStart = {null};
        final int[] runStartCount = {0};
        class Runs {
            void boundary(JumpKey newDest) {
                if (newDest == runDest[0]) return;
                var here = new Label();
                flat.add(new Item.RangeLabel(here));
                if (runDest[0] != null && runStartCount[0] != realCount[0]) {
                    flat.add(new Item.TryRange(runStart[0], here, runDest[0]));
                }
                runDest[0] = newDest;
                runStart[0] = here;
                runStartCount[0] = realCount[0];
            }
        }
        var runs = new Runs();
        record Frame(Iterator<JavaBytecode> iterator, Opt<JumpKey> guard) {}
        var frames = new ArrayDeque<Frame>();
        frames.addLast(new Frame(TList.<JavaBytecode>from(children).iterator(), Opt.none()));
        while (!frames.isEmpty()) {
            var frame = frames.peekLast();
            if (!frame.iterator().hasNext()) {
                frames.removeLast();
                if (frame.guard().optIsSome()) {
                    destStack.remove(destStack.size() - 1);
                    runs.boundary(destStack.isEmpty() ? null : destStack.get(destStack.size() - 1));
                }
                continue;
            }
            var bc = frame.iterator().next();
            switch (bc) {
                case CodeDynamic cd -> {
                    switch (cd.loc()) {
                        case CodeLoc.Some some -> {
                            locCounter[0] += 1;
                            int line = locCounter[0];
                            locMap.put(line, some.qExprId());
                            flat.add(new Item.Line(new Label(), line));
                        }
                        case CodeLoc.Infallible inf -> {}
                    }
                    frames.addLast(new Frame(TList.<JavaBytecode>of(cd.code()).iterator(), Opt.none()));
                }
                case JavaBytecodeSequence s ->
                    frames.addLast(
                            new Frame(TList.<JavaBytecode>from(s.children).iterator(), Opt.none()));
                case JavaBytecodeInsn n -> {
                    flat.add(new Item.Insn(n));
                    realCount[0] += 1;
                }
                case JavaBytecodeLand n -> flat.add(new Item.Land(n));
                case JavaBytecodeJump n -> {
                    flat.add(new Item.Jump(n));
                    realCount[0] += 1;
                }
                case JavaBytecodeBranch n -> {
                    flat.add(new Item.Branch(n));
                    realCount[0] += 1;
                }
                case JavaBytecodeReturn n -> {
                    flat.add(new Item.Return(n));
                    realCount[0] += 1;
                }
                case JavaBytecodeThrow n -> {
                    flat.add(new Item.Throw(n));
                    realCount[0] += 1;
                }
                case JavaBytecodeGuarded g -> {
                    runs.boundary(g.dest);
                    destStack.add(g.dest);
                    frames.addLast(new Frame(TList.<JavaBytecode>of(g.inner).iterator(), Opt.some(g.dest)));
                }
            }
        }
        if (!destStack.isEmpty() || runDest[0] != null) {
            throw new AssertionError("unbalanced exception-dest guard at end of method body");
        }

        var infos = new IdentityHashMap<JavaBytecodeReg, RegInfo>();
        var landIndex = new IdentityHashMap<JumpKey, Integer>();
        var landParams = new IdentityHashMap<JumpKey, TList<JavaBytecodeReg>>();
        var handlerKeys = new IdentityHashMap<JumpKey, Boolean>();
        var barriersBefore = new int[flat.size() + 1];
        for (int i = 0; i < flat.size(); i++) {
            var item = flat.get(i);
            barriersBefore[i + 1] = barriersBefore[i];
            {
                var args = argsOf(item);
                boolean plainInsn =
                        switch (item) {
                            case Item.Insn ins -> !hasSeg0(ins.n());
                            default -> false;
                        };
                for (int k = 0; k < args.size(); k++) {
                    var info = infos.computeIfAbsent(args.get(k), key -> new RegInfo());
                    info.uses += 1;
                    if (i > info.lastUse) info.lastUse = i;
                    if (k != 0 || !plainInsn) info.dupOk = false;
                }
            }
            switch (item) {
                case Item.Insn ins -> {}
                case Item.Land land -> {
                    if (landIndex.containsKey(land.n().jumpKey)) {
                        throw new IllegalStateException("landing emitted twice in one method\n" + listing(flat, i));
                    }
                    landIndex.put(land.n().jumpKey, i);
                    landParams.put(land.n().jumpKey, land.n().params);
                    barriersBefore[i + 1] += 1;
                }
                case Item.Jump j -> barriersBefore[i + 1] += 1;
                case Item.Branch b -> barriersBefore[i + 1] += 1;
                case Item.Return r -> barriersBefore[i + 1] += 1;
                case Item.Throw t -> barriersBefore[i + 1] += 1;
                case Item.TryRange tr -> handlerKeys.put(tr.handler(), Boolean.TRUE);
                case Item.Line l -> {}
                case Item.RangeLabel l -> {}
            }
        }

        var chainStart = new int[flat.size()];
        var argAt = new int[flat.size()][];
        var dupAt = new boolean[flat.size()];
        var chainDupStart = new boolean[flat.size()];
        while (true) {
            for (var info : infos.values()) {
                info.def = -1;
                info.kind = RegKind.DEAD;
                info.remaining = info.uses;
            }
            Arrays.fill(dupAt, false);
            Arrays.fill(chainDupStart, false);
            var stack = new TList<JavaBytecodeReg>();
            try {
                for (int i = 0; i < flat.size(); i++) {
                    var item = flat.get(i);
                    chainStart[i] = i;
                    var args = argsOf(item);
                    argAt[i] = new int[args.size()];
                    switch (item) {
                        case Item.Insn ins -> {
                            consume(flat, i, infos, stack, args, chainStart, argAt, dupAt, chainDupStart);
                            if (ins.n().out.optIsSome()) {
                                var o = ins.n().out.optUnwrap();
                                var info = infos.computeIfAbsent(o, k -> new RegInfo());
                                if (info.def >= 0) {
                                    throw new IllegalStateException("register defined twice\n" + listing(flat, i));
                                }
                                info.def = i;
                                boolean noBarrier =
                                        info.uses > 0 && barriersBefore[info.lastUse] == barriersBefore[i + 1];
                                if (info.uses == 0) {
                                    info.kind = RegKind.DEAD;
                                } else if (info.uses > 1 && info.dupOk && noBarrier) {
                                    info.kind = RegKind.STACK;
                                    stack.add(o);
                                } else if (info.uses > 1) {
                                    info.kind = RegKind.LOCAL;
                                } else if (noBarrier
                                        && switch (ins.n().body) {
                                            case JavaBytecodeInsnBody.Op op ->
                                                ins.n().args.isEmpty();
                                            case JavaBytecodeInsnBody.Node nd ->
                                                ins.n().args.isEmpty() && nd.node() instanceof LdcInsnNode;
                                            case JavaBytecodeInsnBody.Var v -> false;
                                            case JavaBytecodeInsnBody.VarIndexed v -> false;
                                            case JavaBytecodeInsnBody.New nw -> false;
                                            case JavaBytecodeInsnBody.ArrayNew an -> false;
                                        }) {
                                    info.kind = RegKind.DEFER;
                                } else {
                                    info.kind = RegKind.STACK;
                                    stack.add(o);
                                }
                            }
                        }
                        case Item.Land land -> {
                            spillAll(infos, stack);
                            for (var p : land.n().params) {
                                var info = infos.computeIfAbsent(p, k -> new RegInfo());
                                if (info.def >= 0) {
                                    throw new IllegalStateException(
                                            "landing parameter defined twice\n" + listing(flat, i));
                                }
                                info.def = i;
                                info.kind = RegKind.PARAM;
                            }
                        }
                        case Item.Jump j -> {
                            consume(flat, i, infos, stack, args, chainStart, argAt, dupAt, chainDupStart);
                            spillAll(infos, stack);
                        }
                        case Item.Branch b -> {
                            consume(flat, i, infos, stack, args, chainStart, argAt, dupAt, chainDupStart);
                            spillAll(infos, stack);
                        }
                        case Item.Return r -> {
                            consume(flat, i, infos, stack, args, chainStart, argAt, dupAt, chainDupStart);
                            spillAll(infos, stack);
                        }
                        case Item.Throw t -> {
                            consume(flat, i, infos, stack, args, chainStart, argAt, dupAt, chainDupStart);
                            spillAll(infos, stack);
                        }
                        case Item.Line l -> {}
                        case Item.RangeLabel l -> {}
                        case Item.TryRange tr -> {}
                    }
                }
                spillAll(infos, stack);
            } catch (DupRetry retry) {
                continue;
            }
            break;
        }

        {
            var backEdges = new TList<int[]>();
            for (int i = 0; i < flat.size(); i++) {
                JumpKey key =
                        switch (flat.get(i)) {
                            case Item.Jump j -> j.n().jumpKey;
                            case Item.Branch b -> b.n().jumpKey;
                            default -> null;
                        };
                if (key == null) continue;
                var target = landIndex.get(key);
                if (target == null) {
                    throw new IllegalStateException(
                            "jump to a landing that is not in this method\n" + listing(flat, i));
                }
                if (target < i) backEdges.add(new int[] {target, i});
            }
            boolean changed = true;
            while (changed) {
                changed = false;
                for (var edge : backEdges) {
                    int a = edge[0];
                    int b = edge[1];
                    for (var info : infos.values()) {
                        if (info.kind == RegKind.LOCAL && info.def < a && info.lastUse >= a && info.lastUse < b) {
                            info.lastUse = b;
                            changed = true;
                        }
                    }
                }
            }
        }

        var prefixes = new TMap<Integer, TList<Prefix>>();
        for (int i = 0; i < flat.size(); i++) {
            var args = argsOf(flat.get(i));
            if (dupAt[i])
                prefixes.computeIfAbsentT(chainStart[i], key -> new TList<>()).add(new Prefix(i, -1));
            for (int k = 0; k < args.size(); k++) {
                prefixes.computeIfAbsentT(argAt[i][k], key -> new TList<>()).add(new Prefix(i, k));
            }
        }

        var slots = new SlotAllocator();
        var fakeDesc = new StringBuilder("(");
        for (var s : initialSlots) {
            slots.fixed(s.key(), s.wide());
            fakeDesc.append(s.wide() ? "J" : "Ljava/lang/Object;");
        }
        fakeDesc.append(")V");
        for (var item : flat) {
            switch (item) {
                case Item.Insn ins -> {
                    switch (ins.n().body) {
                        case JavaBytecodeInsnBody.VarIndexed v -> {
                            boolean wide = v.opcode() == Opcodes.LLOAD
                                    || v.opcode() == Opcodes.DLOAD
                                    || v.opcode() == Opcodes.LSTORE
                                    || v.opcode() == Opcodes.DSTORE;
                            slots.reserveIndexed(v.index() + (wide ? 2 : 1));
                        }
                        default -> {}
                    }
                }
                default -> {}
            }
        }
        var node = new MethodNode(
                Opcodes.ASM9, Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, "render", fakeDesc.toString(), null, null);
        var labels = new IdentityHashMap<JumpKey, Label>();
        var handlerLabels = new IdentityHashMap<JumpKey, Label>();
        var paramSlots = new IdentityHashMap<JavaBytecodeReg, Integer>();
        for (var e : landParams.entrySet()) {
            for (var p : e.getValue()) paramSlots.put(p, slots.permanent(p.desc.isWide()));
        }
        var regSlots = new IdentityHashMap<JavaBytecodeReg, Integer>();
        var segments = new TMap<Integer, TList<TList<AbstractInsnNode>>>();
        var tryRanges = new TList<Item.TryRange>();
        boolean reachable = true;
        for (int i = 0; i < flat.size(); i++) {
            var scheduled = prefixes.getOrNull(i);
            if (scheduled != null) {
                scheduled.sort((x, y) -> x.consumer() != y.consumer()
                        ? Integer.compare(y.consumer(), x.consumer())
                        : Integer.compare(x.arg(), y.arg()));
                for (var p : scheduled) {
                    var consumer = flat.get(p.consumer());
                    if (p.arg() < 0) {
                        var retained = argsOf(consumer).get(0);
                        node.instructions.add(new InsnNode(retained.desc.isWide() ? DUP2 : DUP));
                        continue;
                    }
                    switch (consumer) {
                        case Item.Insn ins -> {
                            for (var n : segmentsOf(segments, p.consumer(), ins.n(), slots)
                                    .get(p.arg())) {
                                node.instructions.add(n);
                            }
                        }
                        default -> {}
                    }
                    var a = argsOf(consumer).get(p.arg());
                    var info = infos.get(a);
                    switch (info.kind) {
                        case STACK -> {}
                        case PARAM -> node.instructions.add(new VarInsnNode(a.desc.loadOpcode(), paramSlots.get(a)));
                        case LOCAL -> {
                            var slot = regSlots.get(a);
                            if (slot == null) throw new IllegalStateException("register read before it was stored");
                            node.instructions.add(new VarInsnNode(a.desc.loadOpcode(), slot));
                        }
                        case DEFER -> {
                            var def = ((Item.Insn) flat.get(info.def)).n();
                            for (var pn :
                                    segmentsOf(segments, info.def, def, slots).get(0)) node.instructions.add(pn);
                        }
                        case DEAD -> throw new AssertionError("dead register used");
                    }
                }
            }
            var item = flat.get(i);
            switch (item) {
                case Item.Insn ins -> {
                    var n = ins.n();
                    var outInfo = n.out.optIsSome() ? infos.get(n.out.optUnwrap()) : null;
                    if (outInfo != null && outInfo.kind == RegKind.DEFER) {
                        break;
                    }
                    if (outInfo != null
                            && outInfo.kind == RegKind.DEAD
                            && switch (n.body) {
                                case JavaBytecodeInsnBody.Var v -> true;
                                case JavaBytecodeInsnBody.VarIndexed v -> true;
                                case JavaBytecodeInsnBody.Op o -> n.args.isEmpty();
                                case JavaBytecodeInsnBody.Node nd ->
                                    n.args.isEmpty() && nd.node() instanceof LdcInsnNode;
                                case JavaBytecodeInsnBody.New nw -> false;
                                case JavaBytecodeInsnBody.ArrayNew an -> false;
                            }) {
                        break;
                    }
                    for (var pn : segmentsOf(segments, i, n, slots).get(n.args.size())) node.instructions.add(pn);
                    if (outInfo != null) {
                        var o = n.out.optUnwrap();
                        switch (outInfo.kind) {
                            case DEAD -> node.instructions.add(new InsnNode(o.desc.isWide() ? POP2 : POP));
                            case LOCAL -> {
                                int slot = slots.alloc(o.desc.isWide());
                                regSlots.put(o, slot);
                                node.instructions.add(new VarInsnNode(o.desc.storeOpcode(), slot));
                            }
                            case STACK -> {}
                            case DEFER -> throw new AssertionError("deferred instruction emitted in place");
                            case PARAM -> throw new AssertionError("instruction output marked as a parameter");
                        }
                    }
                }
                case Item.Land land -> {
                    var key = land.n().jumpKey;
                    if (handlerKeys.containsKey(key)) {
                        if (reachable) {
                            throw new IllegalStateException(
                                    "exception handler landing is reachable by fall-through\n" + listing(flat, i));
                        }
                        node.visitLabel(handlerLabels.computeIfAbsent(key, k -> new Label()));
                        if (land.n().params.isEmpty()) {
                            node.instructions.add(new InsnNode(POP));
                        } else if (land.n().params.size() == 1) {
                            node.instructions.add(new VarInsnNode(
                                    ASTORE, paramSlots.get(land.n().params.get(0))));
                        } else {
                            throw new IllegalStateException(
                                    "exception handler landing must take exactly one parameter\n" + listing(flat, i));
                        }
                    }
                    node.visitLabel(labels.computeIfAbsent(key, k -> new Label()));
                    reachable = true;
                }
                case Item.Jump j -> {
                    var key = j.n().jumpKey;
                    var params = landParams.get(key);
                    if (params.size() != j.n().args.size()) {
                        throw new IllegalStateException(
                                "jump argument count does not match the landing parameters\n" + listing(flat, i));
                    }
                    for (int k = params.size() - 1; k >= 0; k--) {
                        var p = params.get(k);
                        node.instructions.add(new VarInsnNode(p.desc.storeOpcode(), paramSlots.get(p)));
                    }
                    node.visitJumpInsn(GOTO, labels.computeIfAbsent(key, k -> new Label()));
                    reachable = false;
                }
                case Item.Branch b ->
                    node.visitJumpInsn(b.n().opcode, labels.computeIfAbsent(b.n().jumpKey, k -> new Label()));
                case Item.Return r -> {
                    if (r.n().value.optIsSome()) {
                        node.instructions.add(
                                new InsnNode(r.n().value.optUnwrap().desc.returnOpcode()));
                    } else {
                        node.instructions.add(new InsnNode(RETURN));
                    }
                    reachable = false;
                }
                case Item.Throw t -> {
                    node.instructions.add(new InsnNode(ATHROW));
                    reachable = false;
                }
                case Item.Line l -> {
                    node.visitLabel(l.label());
                    node.visitLineNumber(l.line(), l.label());
                }
                case Item.RangeLabel l -> node.visitLabel(l.label());
                case Item.TryRange tr -> tryRanges.add(tr);
            }
            for (var a : argsOf(item)) {
                var info = infos.get(a);
                if (info.kind == RegKind.LOCAL && info.lastUse == i) {
                    var slot = regSlots.remove(a);
                    if (slot != null) slots.free(slot, a.desc.isWide());
                }
            }
        }
        for (var tr : tryRanges) {
            if (!landIndex.containsKey(tr.handler())) {
                throw new IllegalStateException("guarded region whose handler landing is not in this method");
            }
            node.visitTryCatchBlock(
                    tr.start(), tr.end(), handlerLabels.computeIfAbsent(tr.handler(), k -> new Label()), null);
        }
        node.instructions.accept(out);
        for (var tcb : node.tryCatchBlocks) {
            out.visitTryCatchBlock(tcb.start.getLabel(), tcb.end.getLabel(), tcb.handler.getLabel(), tcb.type);
        }
        return locMap;
    }

    private static TList<JavaBytecodeReg> argsOf(Item item) {
        return switch (item) {
            case Item.Insn ins -> ins.n().args;
            case Item.Jump j -> j.n().args;
            case Item.Branch b -> b.n().args;
            case Item.Return r -> r.n().value.optIsSome() ? TList.of(r.n().value.optUnwrap()) : new TList<>();
            case Item.Throw t -> TList.of(t.n().exception);
            case Item.Land l -> new TList<>();
            case Item.Line l -> new TList<>();
            case Item.RangeLabel l -> new TList<>();
            case Item.TryRange tr -> new TList<>();
        };
    }

    private static TList<TList<AbstractInsnNode>> segmentsOf(
            TMap<Integer, TList<TList<AbstractInsnNode>>> cache, int index, JavaBytecodeInsn n, SlotAllocator slots) {
        var existing = cache.getOrNull(index);
        if (existing != null) return existing;
        int argc = n.args.size();
        var segs = new TList<TList<AbstractInsnNode>>(argc + 1);
        for (int i = 0; i <= argc; i++) segs.add(new TList<>());
        switch (n.body) {
            case JavaBytecodeInsnBody.Op o -> segs.get(argc).add(new InsnNode(o.opcode()));
            case JavaBytecodeInsnBody.Node nd -> segs.get(argc).add(nd.node().clone(new IdentityHashMap<>()));
            case JavaBytecodeInsnBody.Var v -> {
                boolean wide = v.opcode() == Opcodes.LLOAD
                        || v.opcode() == Opcodes.DLOAD
                        || v.opcode() == Opcodes.LSTORE
                        || v.opcode() == Opcodes.DSTORE;
                segs.get(argc).add(new VarInsnNode(v.opcode(), slots.named(v.slot(), wide)));
            }
            case JavaBytecodeInsnBody.VarIndexed v -> segs.get(argc).add(new VarInsnNode(v.opcode(), v.index()));
            case JavaBytecodeInsnBody.New nw -> {
                segs.get(0).add(new TypeInsnNode(NEW, nw.klass().value));
                segs.get(0).add(new InsnNode(DUP));
                segs.get(argc)
                        .add(new MethodInsnNode(INVOKESPECIAL, nw.klass().value, "<init>", nw.ctor().value, false));
            }
            case JavaBytecodeInsnBody.ArrayNew an -> {
                segs.get(0).add(intConst(argc));
                segs.get(0).add(new TypeInsnNode(ANEWARRAY, an.elementClass().value));
                for (int i = 0; i < argc; i++) {
                    if (i > 0) segs.get(i).add(new InsnNode(AASTORE));
                    segs.get(i).add(new InsnNode(DUP));
                    segs.get(i).add(intConst(i));
                }
                if (argc > 0) segs.get(argc).add(new InsnNode(AASTORE));
            }
        }
        cache.put(index, segs);
        return segs;
    }

    private static AbstractInsnNode intConst(int value) {
        if (value >= -1 && value <= 5) return new InsnNode(Opcodes.ICONST_0 + value);
        if (value >= Byte.MIN_VALUE && value <= Byte.MAX_VALUE) return new IntInsnNode(Opcodes.BIPUSH, value);
        if (value >= Short.MIN_VALUE && value <= Short.MAX_VALUE) return new IntInsnNode(Opcodes.SIPUSH, value);
        return new LdcInsnNode(value);
    }

    private static boolean hasSeg0(JavaBytecodeInsn n) {
        return switch (n.body) {
            case JavaBytecodeInsnBody.New nw -> true;
            case JavaBytecodeInsnBody.ArrayNew an -> true;
            case JavaBytecodeInsnBody.Op o -> false;
            case JavaBytecodeInsnBody.Node nd -> false;
            case JavaBytecodeInsnBody.Var v -> false;
            case JavaBytecodeInsnBody.VarIndexed v -> false;
        };
    }

    private static final class DupRetry extends RuntimeException {
        DupRetry() {
            super(null, null, false, false);
        }
    }

    private static void spillReg(RegInfo info) {
        if (info.remaining < info.uses) {
            info.dupOk = false;
            throw new DupRetry();
        }
        info.kind = RegKind.LOCAL;
    }

    private static void consume(
            TList<Item> flat,
            int index,
            IdentityHashMap<JavaBytecodeReg, RegInfo> infos,
            TList<JavaBytecodeReg> stack,
            TList<JavaBytecodeReg> args,
            int[] chainStart,
            int[][] argAt,
            boolean[] dupAt,
            boolean[] chainDupStart) {
        for (var a : args) {
            var info = infos.get(a);
            if (info == null || info.def < 0) {
                throw new IllegalStateException("register used before its definition\n" + listing(flat, index));
            }
        }
        int n = args.size();
        boolean seg0 =
                switch (flat.get(index)) {
                    case Item.Insn ins -> hasSeg0(ins.n());
                    default -> false;
                };
        var at = new int[n];
        int s = n;
        int depth = 0;
        int nextChain = index;
        while (s > 0) {
            var info = infos.get(args.get(s - 1));
            if (info.kind == RegKind.STACK) {
                if (stack.size() > depth && stack.get(stack.size() - 1 - depth).key == args.get(s - 1).key) {
                    depth++;
                    nextChain = chainStart[info.def];
                    at[s - 1] = nextChain;
                    s--;
                } else {
                    break;
                }
            } else {
                at[s - 1] = nextChain;
                s--;
            }
        }
        int firstStack = -1;
        for (int k = s; k < n; k++) {
            if (infos.get(args.get(k)).kind == RegKind.STACK) {
                firstStack = k;
                break;
            }
        }
        boolean retain = s == 0
                && firstStack == 0
                && !seg0
                && infos.get(args.get(0)).dupOk
                && infos.get(args.get(0)).remaining > 1;
        int cs = index;
        for (int k = s; k < n; k++) {
            if (k == 0 && retain) continue;
            if (infos.get(args.get(k)).kind == RegKind.STACK) cs = Math.min(cs, chainStart[infos.get(args.get(k)).def]);
        }
        if (retain) {
            for (int k = 1; k < n; k++) {
                if (at[k] == chainStart[infos.get(args.get(0)).def]) at[k] = cs;
            }
        }
        for (int k = 0; k < s; k++) at[k] = cs;
        boolean inPlace = true;
        if (firstStack >= 0 && !retain && chainDupStart[infos.get(args.get(firstStack)).def]) {
            for (int k = 0; k < firstStack; k++) inPlace = false;
            if (seg0) inPlace = false;
        }
        for (int k = 0; inPlace && k < n; k++) {
            var info = infos.get(args.get(k));
            if (k >= s && info.kind == RegKind.STACK) continue;
            int pos = at[k];
            inPlace = switch (info.kind) {
                case STACK -> false;
                case LOCAL -> info.def < pos;
                case PARAM -> true;
                case DEFER -> true;
                case DEAD -> throw new AssertionError("dead register used");
            };
        }
        if (!inPlace) {
            for (var a : args) {
                var info = infos.get(a);
                if (info.kind != RegKind.STACK) continue;
                spillReg(info);
                for (int k = stack.size() - 1; k >= 0; k--) {
                    if (stack.get(k).key == a.key) {
                        stack.remove(k);
                        break;
                    }
                }
            }
            cs = index;
            s = n;
            retain = false;
            for (int k = 0; k < n; k++) at[k] = index;
        } else {
            for (int k = n - 1; k >= s; k--) {
                var info = infos.get(args.get(k));
                info.remaining -= 1;
                if (info.kind != RegKind.STACK) continue;
                if (k == 0 && retain) break;
                stack.remove(stack.size() - 1);
            }
        }
        chainStart[index] = cs;
        argAt[index] = at;
        dupAt[index] = retain;
        chainDupStart[index] =
                retain || (s == 0 && firstStack == 0 && !seg0 && chainDupStart[infos.get(args.get(0)).def]);
    }

    private static void spillAll(IdentityHashMap<JavaBytecodeReg, RegInfo> infos, TList<JavaBytecodeReg> stack) {
        for (var r : stack) spillReg(infos.get(r));
        stack.clear();
    }

    private static final class SlotAllocator {
        private final TMap<BindingId, Integer> named = new TMap<>();
        private final TList<Integer> freeNarrow = new TList<>();
        private final TList<Integer> freeWide = new TList<>();
        private int next;

        void fixed(BindingId key, boolean wide) {
            named.put(key, next);
            next += wide ? 2 : 1;
        }

        int named(BindingId key, boolean wide) {
            var existing = named.getOrNull(key);
            if (existing != null) return existing;
            int index = next;
            named.put(key, index);
            next += wide ? 2 : 1;
            return index;
        }

        void reserveIndexed(int end) {
            if (end > next) next = end;
        }

        int permanent(boolean wide) {
            int index = next;
            next += wide ? 2 : 1;
            return index;
        }

        int alloc(boolean wide) {
            var pool = wide ? freeWide : freeNarrow;
            if (!pool.isEmpty()) return pool.remove(pool.size() - 1);
            return permanent(wide);
        }

        void free(int index, boolean wide) {
            (wide ? freeWide : freeNarrow).add(index);
        }
    }

    static String listing(TList<Item> flat, int upTo) {
        var out = new StringBuilder();
        for (int k = Math.max(0, upTo - 60); k <= upTo && k < flat.size(); k++) {
            var item = flat.get(k);
            out.append('#')
                    .append(k)
                    .append(' ')
                    .append(
                            switch (item) {
                                case Item.Insn ins -> {
                                    var n = ins.n();
                                    var sb = new StringBuilder();
                                    if (n.out.optIsSome())
                                        sb.append(regName(n.out.optUnwrap())).append(" = ");
                                    sb.append(
                                            switch (n.body) {
                                                case JavaBytecodeInsnBody.Op o -> "op " + o.opcode();
                                                case JavaBytecodeInsnBody.Node nd ->
                                                    nd.node().getClass().getSimpleName()
                                                            + (nd.node() instanceof MethodInsnNode m
                                                                    ? " " + m.owner + "." + m.name + m.desc
                                                                    : "")
                                                            + (nd.node() instanceof LdcInsnNode l ? " " + l.cst : "");
                                                case JavaBytecodeInsnBody.Var v ->
                                                    "var " + v.opcode() + " " + System.identityHashCode(v.slot());
                                                case JavaBytecodeInsnBody.VarIndexed v ->
                                                    "var " + v.opcode() + " #" + v.index();
                                                case JavaBytecodeInsnBody.New nw -> "new " + nw.klass();
                                                case JavaBytecodeInsnBody.ArrayNew an ->
                                                    "newarray " + an.elementClass();
                                            });
                                    sb.append('(');
                                    for (int i = 0; i < n.args.size(); i++) {
                                        if (i > 0) sb.append(", ");
                                        sb.append(regName(n.args.get(i)));
                                    }
                                    sb.append(')');
                                    yield sb.toString();
                                }
                                case Item.Land l ->
                                    "land " + System.identityHashCode(l.n().jumpKey) + " " + regNames(l.n().params);
                                case Item.Jump j ->
                                    "jump " + System.identityHashCode(j.n().jumpKey) + " " + regNames(j.n().args);
                                case Item.Branch b ->
                                    "branch " + b.n().opcode + " " + System.identityHashCode(b.n().jumpKey) + " "
                                            + regNames(b.n().args);
                                case Item.Return r ->
                                    "return "
                                            + (r.n().value.optIsSome()
                                                    ? regName(r.n().value.optUnwrap())
                                                    : "");
                                case Item.Throw t -> "throw " + regName(t.n().exception);
                                case Item.Line l -> "line " + l.line();
                                case Item.RangeLabel l -> "range-label";
                                case Item.TryRange tr -> "try-range -> " + System.identityHashCode(tr.handler());
                            })
                    .append('\n');
        }
        return out.toString();
    }

    private static String regNames(TList<JavaBytecodeReg> regs) {
        var sb = new StringBuilder("[");
        for (int i = 0; i < regs.size(); i++) {
            if (i > 0) sb.append(", ");
            sb.append(regName(regs.get(i)));
        }
        return sb.append(']').toString();
    }

    private static String regName(JavaBytecodeReg r) {
        return "r" + Integer.toHexString(System.identityHashCode(r)) + ":" + r.desc.value;
    }
}
