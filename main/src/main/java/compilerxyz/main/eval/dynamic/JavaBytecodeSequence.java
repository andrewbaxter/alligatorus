package compilerxyz.main.eval.dynamic;

import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.GETSTATIC;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.RETURN;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Iterator;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
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
    }

    private record Prefix(int consumer, int arg) {}

    void renderInternal(
            MethodVisitor out,
            TList<InitialSlot> initialSlots,
            JavaBytecodeRenderRefs refs,
            JavaBytecodeLocTable locTable) {

        var flat = new TList<Item>();
        final int[] realCount = {0};
        var destStack = new TList<JumpKey>();
        class Runs {
            Opt<JumpKey> dest = Opt.none();
            Opt<Label> start = Opt.none();
            int startCount = 0;

            void boundary(Opt<JumpKey> newDest) {
                if (newDest.optIsNone()
                        ? dest.optIsNone()
                        : dest.optIsSome() && newDest.optUnwrap() == dest.optUnwrap()) {
                    return;
                }
                var here = new Label();
                flat.add(new Item.RangeLabel(here));
                if (dest.optIsSome() && startCount != realCount[0]) {
                    flat.add(new Item.TryRange(start.optUnwrap(), here, dest.optUnwrap()));
                }
                dest = newDest;
                start = Opt.some(here);
                startCount = realCount[0];
            }
        }
        var runs = new Runs();
        record Frame(Iterator<JavaBytecode> iterator, Opt<JumpKey> guard, Opt<Integer> line) {}
        var frames = new ArrayDeque<Frame>();
        frames.addLast(new Frame(TList.<JavaBytecode>from(children).iterator(), Opt.none(), Opt.none()));
        while (!frames.isEmpty()) {
            var frame = frames.peekLast();
            if (!frame.iterator().hasNext()) {
                frames.removeLast();
                if (frame.guard().optIsSome()) {
                    destStack.removeLast();
                    runs.boundary(destStack.isEmpty() ? Opt.none() : Opt.some(destStack.get(destStack.size() - 1)));
                }
                if (frame.line().optIsSome()) {
                    var outer = frames.descendingIterator();
                    while (outer.hasNext()) {
                        var enclosing = outer.next().line();
                        if (enclosing.optIsSome()) {
                            pushLine(flat, enclosing.optUnwrap());
                            break;
                        }
                    }
                }
                continue;
            }
            var bc = frame.iterator().next();
            switch (bc) {
                case CodeDynamic cd -> {
                    Opt<Integer> line =
                            switch (cd.loc()) {
                                case CodeLoc.Some some -> Opt.some(locTable.line(some.qExprId()));
                                case CodeLoc.Infallible inf -> Opt.none();
                            };
                    if (line.optIsSome()) pushLine(flat, line.optUnwrap());
                    frames.addLast(new Frame(TList.<JavaBytecode>of(cd.code()).iterator(), Opt.none(), line));
                }
                case JavaBytecodeSequence s ->
                    frames.addLast(
                            new Frame(TList.<JavaBytecode>from(s.children).iterator(), Opt.none(), Opt.none()));
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
                case JavaBytecodeDefSet d -> refs.defSet(d.set());
                case JavaBytecodeGuarded g -> {
                    runs.boundary(Opt.some(g.dest));
                    destStack.add(g.dest);
                    frames.addLast(new Frame(TList.<JavaBytecode>of(g.inner).iterator(), Opt.some(g.dest), Opt.none()));
                }
            }
        }
        if (!destStack.isEmpty() || runs.dest.optIsSome()) {
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
            for (var a : argsOf(item)) {
                var info = infos.computeIfAbsent(a, key -> new RegInfo());
                info.uses += 1;
                if (i > info.lastUse) info.lastUse = i;
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
        var argPos = new int[flat.size()][];
        {
            var stack = new TList<JavaBytecodeReg>();
            for (int i = 0; i < flat.size(); i++) {
                var item = flat.get(i);
                var args = argsOf(item);
                for (var a : args) {
                    var info = regInfo(infos, a);
                    if (info == null || info.def < 0) {
                        throw new IllegalStateException("register used before its definition\n" + listing(flat, i));
                    }
                }
                int n = args.size();
                int s = 0;
                var pos = new int[n];
                {
                    int next = i;
                    for (int k = n - 1; k >= 0; k--) {
                        var info = regInfo(infos, args.get(k));
                        if (info.kind == RegKind.STACK) {
                            s++;
                            next = chainStart[info.def];
                        }
                        pos[k] = next;
                    }
                }
                boolean inPlace = stack.size() >= s;
                for (int k = n - 1, d = 0; inPlace && k >= 0; k--) {
                    var info = regInfo(infos, args.get(k));
                    switch (info.kind) {
                        case STACK -> {
                            d++;
                            if (stack.get(stack.size() - d).key != args.get(k).key) inPlace = false;
                        }
                        case LOCAL -> {
                            if (info.def >= pos[k]) inPlace = false;
                        }
                        case PARAM -> {}
                        case DEFER -> {}
                        case DEAD -> throw new AssertionError("dead register used");
                    }
                }
                if (inPlace) {
                    for (int k = 0; k < s; k++) stack.removeLast();
                } else {
                    for (var a : args) {
                        var info = regInfo(infos, a);
                        if (info.kind != RegKind.STACK) continue;
                        info.kind = RegKind.LOCAL;
                        for (int k = stack.size() - 1; k >= 0; k--) {
                            if (stack.get(k).key == a.key) {
                                stack.removeAt(k);
                                break;
                            }
                        }
                    }
                    for (int k = 0; k < n; k++) pos[k] = i;
                }
                argPos[i] = pos;
                chainStart[i] = n > 0 ? pos[0] : i;
                switch (item) {
                    case Item.Insn ins -> {
                        if (ins.n().out.optIsNone()) break;
                        var o = ins.n().out.optUnwrap();
                        var info = infos.computeIfAbsent(o, k -> new RegInfo());
                        if (info.def >= 0) {
                            throw new IllegalStateException("register defined twice\n" + listing(flat, i));
                        }
                        info.def = i;
                        if (info.uses == 0) {
                            info.kind = RegKind.DEAD;
                        } else if (info.uses == 1
                                && switch (ins.n().body) {
                                    case JavaBytecodeInsnBody.Op op ->
                                        ins.n().args.isEmpty();
                                    case JavaBytecodeInsnBody.Node nd ->
                                        ins.n().args.isEmpty() && nd.node() instanceof LdcInsnNode;
                                    case JavaBytecodeInsnBody.External e -> true;
                                    case JavaBytecodeInsnBody.Var v -> false;
                                    case JavaBytecodeInsnBody.VarIndexed v -> false;
                                    case JavaBytecodeInsnBody.New nw -> false;
                                    case JavaBytecodeInsnBody.ArrayNew an -> false;
                                }) {
                            info.kind = RegKind.DEFER;
                        } else if (info.uses == 1 && barriersBefore[info.lastUse] == barriersBefore[i + 1]) {
                            info.kind = RegKind.STACK;
                            stack.add(o);
                        } else {
                            info.kind = RegKind.LOCAL;
                        }
                    }
                    case Item.Land land -> {
                        if (!stack.isEmpty()) {
                            throw new AssertionError("operand stack not empty at a landing\n" + listing(flat, i));
                        }
                        for (var p : land.n().params) {
                            var info = infos.computeIfAbsent(p, k -> new RegInfo());
                            if (info.def >= 0) {
                                throw new IllegalStateException("landing parameter defined twice\n" + listing(flat, i));
                            }
                            info.def = i;
                            info.kind = RegKind.PARAM;
                        }
                    }
                    case Item.Jump j -> {
                        if (!stack.isEmpty()) {
                            throw new AssertionError("operand stack not empty at a jump\n" + listing(flat, i));
                        }
                    }
                    case Item.Branch b -> {
                        if (!stack.isEmpty()) {
                            throw new AssertionError("operand stack not empty at a branch\n" + listing(flat, i));
                        }
                    }
                    case Item.Return r -> {
                        if (!stack.isEmpty()) {
                            throw new AssertionError("operand stack not empty at a return\n" + listing(flat, i));
                        }
                    }
                    case Item.Throw t -> {
                        if (!stack.isEmpty()) {
                            throw new AssertionError("operand stack not empty at a throw\n" + listing(flat, i));
                        }
                    }
                    case Item.Line l -> {}
                    case Item.RangeLabel l -> {}
                    case Item.TryRange tr -> {}
                }
            }
            if (!stack.isEmpty()) {
                throw new AssertionError(
                        "operand stack not empty at end of method body\n" + listing(flat, flat.size() - 1));
            }
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
            for (int k = 0; k < argPos[i].length; k++) {
                prefixes.computeIfAbsentT(argPos[i][k], key -> new TList<>()).add(new Prefix(i, k));
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
            var scheduledOpt = prefixes.getOpt(i);
            if (scheduledOpt.optIsSome()) {
                var scheduled = scheduledOpt.optUnwrap();
                scheduled.sort((x, y) -> x.consumer() != y.consumer()
                        ? Integer.compare(y.consumer(), x.consumer())
                        : Integer.compare(x.arg(), y.arg()));
                for (var p : scheduled) {
                    var consumer = flat.get(p.consumer());
                    switch (consumer) {
                        case Item.Insn ins -> {
                            for (var n : segmentsOf(segments, p.consumer(), ins.n(), slots, refs)
                                    .get(p.arg())) {
                                node.instructions.add(n);
                            }
                        }
                        case Item.Jump j -> {}
                        case Item.Branch b -> {}
                        case Item.Return r -> {}
                        case Item.Throw t -> {}
                        case Item.Land l -> throw new AssertionError("landing has no operands");
                        case Item.Line l -> throw new AssertionError("line marker has no operands");
                        case Item.RangeLabel l -> throw new AssertionError("range label has no operands");
                        case Item.TryRange tr -> throw new AssertionError("try range has no operands");
                    }
                    var a = argsOf(consumer).get(p.arg());
                    var info = regInfo(infos, a);
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
                            for (var pn : segmentsOf(segments, info.def, def, slots, refs)
                                    .get(0)) node.instructions.add(pn);
                        }
                        case DEAD -> throw new AssertionError("dead register used");
                    }
                }
            }
            var item = flat.get(i);
            var args = argsOf(item);
            switch (item) {
                case Item.Insn ins -> {
                    var n = ins.n();
                    var outInfo = n.out.optIsSome() ? regInfo(infos, n.out.optUnwrap()) : null;
                    if (outInfo != null && outInfo.kind == RegKind.DEFER) {
                        break;
                    }
                    if (outInfo != null
                            && outInfo.kind == RegKind.DEAD
                            && switch (n.body) {
                                case JavaBytecodeInsnBody.Var v -> true;
                                case JavaBytecodeInsnBody.VarIndexed v -> true;
                                case JavaBytecodeInsnBody.External e -> true;
                                case JavaBytecodeInsnBody.Op o -> n.args.isEmpty();
                                case JavaBytecodeInsnBody.Node nd ->
                                    n.args.isEmpty() && nd.node() instanceof LdcInsnNode;
                                case JavaBytecodeInsnBody.New nw -> false;
                                case JavaBytecodeInsnBody.ArrayNew an -> false;
                            }) {
                        break;
                    }
                    for (var pn : segmentsOf(segments, i, n, slots, refs).get(args.size())) node.instructions.add(pn);
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
                    if (params == null) throw new IllegalStateException("jump to a land that was never emitted");
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
                case Item.Branch b -> {
                    node.visitJumpInsn(b.n().opcode, labels.computeIfAbsent(b.n().jumpKey, k -> new Label()));
                }
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
            for (var a : args) {
                var info = regInfo(infos, a);
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
    }

    private static void pushLine(TList<Item> flat, int line) {
        if (!flat.isEmpty()) {
            switch (flat.get(flat.size() - 1)) {
                case Item.Line l -> flat.removeAt(flat.size() - 1);
                default -> {}
            }
        }
        flat.add(new Item.Line(new Label(), line));
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
            TMap<Integer, TList<TList<AbstractInsnNode>>> cache,
            int index,
            JavaBytecodeInsn n,
            SlotAllocator slots,
            JavaBytecodeRenderRefs refs) {
        var existingOpt = cache.getOpt(index);
        if (existingOpt.optIsSome()) {
            var existing = existingOpt.optUnwrap();
            return existing;
        }
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
            case JavaBytecodeInsnBody.External e -> {
                var field = refs.external(e.entry());
                segs.get(argc).add(new FieldInsnNode(GETSTATIC, field.owner.value, field.name, field.descriptor.value));
            }
            case JavaBytecodeInsnBody.New nw -> {
                segs.get(0).add(new TypeInsnNode(NEW, nw.klass().value));
                segs.get(0).add(new InsnNode(DUP));
                segs.get(argc)
                        .add(new MethodInsnNode(INVOKESPECIAL, nw.klass().value, "<init>", nw.ctor().value, false));
            }
            case JavaBytecodeInsnBody.ArrayNew an -> {
                segs.get(0).add(intConst(argc));
                segs.get(0).add(an.element().arrayNewInsn());
                for (int i = 0; i < argc; i++) {
                    if (i > 0) segs.get(i).add(new InsnNode(an.element().arrayStoreOpcode()));
                    segs.get(i).add(new InsnNode(DUP));
                    segs.get(i).add(intConst(i));
                }
                if (argc > 0) segs.get(argc).add(new InsnNode(an.element().arrayStoreOpcode()));
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
            var existingOpt = named.getOpt(key);
            if (existingOpt.optIsSome()) {
                var existing = existingOpt.optUnwrap();
                return existing;
            }
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
            if (!pool.isEmpty()) return pool.removeLast();
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
                                                    "newarray " + an.element().value;
                                                case JavaBytecodeInsnBody.External e ->
                                                    "external "
                                                            + e.entry()
                                                                    .external()
                                                                    .externalHostClass()
                                                                    .getSimpleName();
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

    private static RegInfo regInfo(IdentityHashMap<JavaBytecodeReg, RegInfo> infos, JavaBytecodeReg reg) {
        var info = infos.get(reg);
        if (info == null) throw new IllegalStateException("register has no allocation info");
        return info;
    }
}
