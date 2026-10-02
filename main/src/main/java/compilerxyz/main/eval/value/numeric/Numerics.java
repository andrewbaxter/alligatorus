package compilerxyz.main.eval.value.numeric;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.bool.TypeBoolConst;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.byte_.TypeByteConst;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.eval.value.double_.TypeDoubleConst;
import compilerxyz.main.eval.value.double_.TypeDoubleDynamic;
import compilerxyz.main.eval.value.float_.TypeFloatConst;
import compilerxyz.main.eval.value.float_.TypeFloatDynamic;
import compilerxyz.main.eval.value.int_.TypeIntConst;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.long_.TypeLongDynamic;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.short_.TypeShortConst;
import compilerxyz.main.eval.value.short_.TypeShortDynamic;
import compilerxyz.main.eval.value.ubyte.TypeUbyteConst;
import compilerxyz.main.eval.value.ubyte.TypeUbyteDynamic;
import compilerxyz.main.eval.value.uint.TypeUintConst;
import compilerxyz.main.eval.value.uint.TypeUintDynamic;
import compilerxyz.main.eval.value.ulong.TypeUlongConst;
import compilerxyz.main.eval.value.ulong.TypeUlongDynamic;
import compilerxyz.main.eval.value.ushort.TypeUshortConst;
import compilerxyz.main.eval.value.ushort.TypeUshortDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInt;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.GenAspectNumericDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.math.BigDecimal;
import java.math.BigInteger;
import org.objectweb.asm.Opcodes;

public final class Numerics {
    private Numerics() {}

    public enum Op {
        ADD,
        SUB,
        MUL,
        DIV,
        MOD,
        AND,
        OR,
        XOR,
        SHL,
        SHR,
        USHR,
        EQ,
        NE,
        LT,
        LE,
        GT,
        GE;

        boolean comparison() {
            return this == EQ || this == NE || this == LT || this == LE || this == GT || this == GE;
        }

        boolean ordered() {
            return this == LT || this == LE || this == GT || this == GE;
        }

        boolean shift() {
            return this == SHL || this == SHR || this == USHR;
        }

        String symbol() {
            return name().toLowerCase();
        }
    }

    public enum Unary {
        NEG,
        BITNOT
    }

    public static Type constType(NumericKind k) {
        return switch (k) {
            case BYTE -> new TypeByteConst();
            case SHORT -> new TypeShortConst();
            case INT -> new TypeIntConst();
            case LONG -> new TypeLongConst();
            case FLOAT -> new TypeFloatConst();
            case DOUBLE -> new TypeDoubleConst();
            case UBYTE -> new TypeUbyteConst();
            case USHORT -> new TypeUshortConst();
            case UINT -> new TypeUintConst();
            case ULONG -> new TypeUlongConst();
        };
    }

    public static Type dynamicType(NumericKind k) {
        return switch (k) {
            case BYTE -> new TypeByteDynamic();
            case SHORT -> new TypeShortDynamic();
            case INT -> new TypeIntDynamic();
            case LONG -> new TypeLongDynamic();
            case FLOAT -> new TypeFloatDynamic();
            case DOUBLE -> new TypeDoubleDynamic();
            case UBYTE -> new TypeUbyteDynamic();
            case USHORT -> new TypeUshortDynamic();
            case UINT -> new TypeUintDynamic();
            case ULONG -> new TypeUlongDynamic();
        };
    }

    public static Value constValue(NumericKind k, Number n) {
        Object boxed =
                switch (k) {
                    case BYTE, UBYTE -> (byte) n.longValue();
                    case SHORT, USHORT -> (short) n.longValue();
                    case INT, UINT -> (int) n.longValue();
                    case LONG, ULONG -> n.longValue();
                    case FLOAT -> n.floatValue();
                    case DOUBLE -> n.doubleValue();
                };
        return new ValueObjectConst(constType(k), boxed);
    }

    private record Operand(NumericKind kind, boolean isConst, Value value) {}

    private static Opt<Operand> classify(ExprContext xx, Value v) {
        var t = v.valueType();
        var constOpt = GenAspectNumericConst.getMaybe(xx, t);
        if (constOpt.optIsSome()) {
            return Opt.some(new Operand(constOpt.optUnwrap().aspectNumericConstKind(), true, v));
        }
        var dynOpt = GenAspectNumericDynamic.getMaybe(xx, t);
        if (dynOpt.optIsSome()) {
            return Opt.some(new Operand(dynOpt.optUnwrap().aspectNumericDynamicKind(), false, v));
        }
        if (GenAspectInt.getMaybe(xx, t).optIsSome()) {
            return Opt.some(new Operand(NumericKind.LONG, true, v));
        }
        var cdOpt = GenAspectConsumeDynamic.getMaybe(xx, t);
        if (cdOpt.optIsSome()) {
            var desc = cdOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx).value();
            if (!desc.isEmpty()) {
                NumericKind k =
                        switch (desc.charAt(0)) {
                            case 'B' -> NumericKind.BYTE;
                            case 'S' -> NumericKind.SHORT;
                            case 'I', 'C' -> NumericKind.INT;
                            case 'J' -> NumericKind.LONG;
                            case 'F' -> NumericKind.FLOAT;
                            case 'D' -> NumericKind.DOUBLE;
                            default -> null;
                        };
                if (k != null) return Opt.some(new Operand(k, false, v));
            }
        }
        return Opt.none();
    }

    private static Number constNumber(ExprContext xx, Value v) {
        var a = GenAspectNumericConst.getMaybe(xx, v.valueType());
        if (a.optIsSome()) {
            return a.optUnwrap().aspectNumericConstGet(v);
        }
        var i = GenAspectInt.getMaybe(xx, v.valueType());
        if (i.optIsSome()) {
            return i.optUnwrap().aspectIntGet(v);
        }
        throw new IllegalStateException("Numerics::constNumber: value is not a numeric constant");
    }

    private static EResult<Operand> constAdopt(ExprContext xx, QExprId loc, Op op, Operand c, NumericKind kind) {
        var n = constNumber(xx, c.value());
        var exact =
                switch (c.kind()) {
                    case BYTE, SHORT, INT, LONG -> Opt.some(BigInteger.valueOf(n.longValue()));
                    case UBYTE, USHORT, UINT, ULONG ->
                        Opt.some(new BigInteger(Long.toUnsignedString(ulongVal(c.kind(), n))));
                    case FLOAT, DOUBLE -> {
                        var d = n.doubleValue();
                        yield Double.isFinite(d) && d == Math.rint(d)
                                ? Opt.some(new BigDecimal(d).toBigIntegerExact())
                                : Opt.<BigInteger>none();
                    }
                };
        var w = kind.intWidth();
        var min = kind.unsigned()
                ? BigInteger.ZERO
                : BigInteger.ONE.shiftLeft(w - 1).negate();
        var max = (kind.unsigned() ? BigInteger.ONE.shiftLeft(w) : BigInteger.ONE.shiftLeft(w - 1))
                .subtract(BigInteger.ONE);
        if (exact.optIsNone()
                || exact.optUnwrap().compareTo(min) < 0
                || exact.optUnwrap().compareTo(max) > 0) {
            return EResult.err(
                    loc,
                    "Numerics::" + op.symbol() + ": constant " + n + " is not representable as "
                            + kind.name().toLowerCase());
        }
        return EResult.ok(
                new Operand(kind, true, constValue(kind, exact.optUnwrap().longValue())));
    }

    private static Opt<Value> valueOf(CodeValue cv) {
        if (cv.value().optIsSome()) return cv.value();
        return Opt.none();
    }

    public static EResult<CodeValue> binary(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference, Op op) {
        var bvOpt = valueOf(base);
        var rvOpt = valueOf(reference);
        if (bvOpt.optIsNone() || rvOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::" + op.symbol() + ": operand has no compile-time value");
        }
        var bClassOpt = classify(xx, ((Opt.Some<Value>) bvOpt).value());
        var rClassOpt = classify(xx, ((Opt.Some<Value>) rvOpt).value());
        if (bClassOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::" + op.symbol() + ": left operand is not numeric");
        }
        if (rClassOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::" + op.symbol() + ": right operand is not numeric");
        }
        var b = ((Opt.Some<Operand>) bClassOpt).value();
        var r = ((Opt.Some<Operand>) rClassOpt).value();

        if (op.shift() && r.isConst()) {
            var adopted = constAdopt(xx, loc, op, r, NumericKind.INT);
            if (adopted.hasErrors()) return adopted.reerr();
            r = adopted.requireValue("Numerics::constAdopt shift amount");
        } else if (op == Op.AND || op == Op.OR || op == Op.XOR) {
            if (b.isConst() && !r.isConst() && r.kind().integral()) {
                var adopted = constAdopt(xx, loc, op, b, r.kind());
                if (adopted.hasErrors()) return adopted.reerr();
                b = adopted.requireValue("Numerics::constAdopt left");
            } else if (r.isConst() && !b.isConst() && b.kind().integral()) {
                var adopted = constAdopt(xx, loc, op, r, b.kind());
                if (adopted.hasErrors()) return adopted.reerr();
                r = adopted.requireValue("Numerics::constAdopt right");
            }
        }

        NumericKind compute;
        if (op.shift()) {
            if (!b.kind().integral()) {
                return EResult.err(loc, "Numerics::" + op.symbol() + ": shift requires an integral value");
            }
            if (!r.kind().integral()) {
                return EResult.err(loc, "Numerics::" + op.symbol() + ": shift amount must be integral");
            }
            compute = NumericKind.unaryPromote(b.kind());
        } else {
            if (b.kind().unsigned() != r.kind().unsigned()) {
                return EResult.err(
                        loc, "Numerics::" + op.symbol() + ": cannot mix signed and unsigned operands; cast explicitly");
            }
            var bk = b.kind();
            var rk = r.kind();
            compute = bk.unsigned() || rk.unsigned()
                    ? (bk == NumericKind.ULONG || rk == NumericKind.ULONG ? NumericKind.ULONG : NumericKind.UINT)
                    : bk == NumericKind.DOUBLE || rk == NumericKind.DOUBLE
                            ? NumericKind.DOUBLE
                            : bk == NumericKind.FLOAT || rk == NumericKind.FLOAT
                                    ? NumericKind.FLOAT
                                    : bk == NumericKind.LONG || rk == NumericKind.LONG
                                            ? NumericKind.LONG
                                            : NumericKind.INT;
            if ((op == Op.AND || op == Op.OR || op == Op.XOR) && !compute.integral()) {
                return EResult.err(loc, "Numerics::" + op.symbol() + ": bitwise operator requires integral operands");
            }
        }

        if (b.isConst() && r.isConst()) {
            var a = constNumber(xx, b.value());
            var c = constNumber(xx, r.value());
            Value result;
            if (op.comparison()) {
                boolean boolResult =
                        switch (compute) {
                            case INT, BYTE, SHORT ->
                                switch (op) {
                                    case EQ -> a.intValue() == c.intValue();
                                    case NE -> a.intValue() != c.intValue();
                                    case LT -> a.intValue() < c.intValue();
                                    case LE -> a.intValue() <= c.intValue();
                                    case GT -> a.intValue() > c.intValue();
                                    case GE -> a.intValue() >= c.intValue();
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            case LONG ->
                                switch (op) {
                                    case EQ -> a.longValue() == c.longValue();
                                    case NE -> a.longValue() != c.longValue();
                                    case LT -> a.longValue() < c.longValue();
                                    case LE -> a.longValue() <= c.longValue();
                                    case GT -> a.longValue() > c.longValue();
                                    case GE -> a.longValue() >= c.longValue();
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            case FLOAT ->
                                switch (op) {
                                    case EQ -> a.floatValue() == c.floatValue();
                                    case NE -> a.floatValue() != c.floatValue();
                                    case LT -> a.floatValue() < c.floatValue();
                                    case LE -> a.floatValue() <= c.floatValue();
                                    case GT -> a.floatValue() > c.floatValue();
                                    case GE -> a.floatValue() >= c.floatValue();
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            case DOUBLE ->
                                switch (op) {
                                    case EQ -> a.doubleValue() == c.doubleValue();
                                    case NE -> a.doubleValue() != c.doubleValue();
                                    case LT -> a.doubleValue() < c.doubleValue();
                                    case LE -> a.doubleValue() <= c.doubleValue();
                                    case GT -> a.doubleValue() > c.doubleValue();
                                    case GE -> a.doubleValue() >= c.doubleValue();
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            case UBYTE, USHORT, UINT -> {
                                int av = uintVal(b.kind(), a), cv2 = uintVal(r.kind(), c);
                                yield switch (op) {
                                    case EQ -> av == cv2;
                                    case NE -> av != cv2;
                                    case LT -> Integer.compareUnsigned(av, cv2) < 0;
                                    case LE -> Integer.compareUnsigned(av, cv2) <= 0;
                                    case GT -> Integer.compareUnsigned(av, cv2) > 0;
                                    case GE -> Integer.compareUnsigned(av, cv2) >= 0;
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            }
                            case ULONG -> {
                                long av = ulongVal(b.kind(), a), cv2 = ulongVal(r.kind(), c);
                                yield switch (op) {
                                    case EQ -> av == cv2;
                                    case NE -> av != cv2;
                                    case LT -> Long.compareUnsigned(av, cv2) < 0;
                                    case LE -> Long.compareUnsigned(av, cv2) <= 0;
                                    case GT -> Long.compareUnsigned(av, cv2) > 0;
                                    case GE -> Long.compareUnsigned(av, cv2) >= 0;
                                    default -> throw new IllegalStateException("not a comparison op: " + op);
                                };
                            }
                        };
                result = new ValueObjectConst(new TypeBoolConst(), boolResult);
            } else {
                EResult<Value> foldedRes =
                        switch (compute) {
                            case INT, BYTE, SHORT -> {
                                var v =
                                        switch (op) {
                                            case ADD -> Opt.some(a.intValue() + c.intValue());
                                            case SUB -> Opt.some(a.intValue() - c.intValue());
                                            case MUL -> Opt.some(a.intValue() * c.intValue());
                                            case DIV ->
                                                c.intValue() == 0
                                                        ? Opt.<Integer>none()
                                                        : Opt.some(a.intValue() / c.intValue());
                                            case MOD ->
                                                c.intValue() == 0
                                                        ? Opt.<Integer>none()
                                                        : Opt.some(a.intValue() % c.intValue());
                                            case AND -> Opt.some(a.intValue() & c.intValue());
                                            case OR -> Opt.some(a.intValue() | c.intValue());
                                            case XOR -> Opt.some(a.intValue() ^ c.intValue());
                                            case SHL -> Opt.some(a.intValue() << c.intValue());
                                            case SHR -> Opt.some(a.intValue() >> c.intValue());
                                            case USHR -> Opt.some(a.intValue() >>> c.intValue());
                                            default -> throw new IllegalStateException("not an int fold op: " + op);
                                        };
                                yield v.optIsNone()
                                        ? divZero(loc, op)
                                        : EResult.ok(constValue(NumericKind.INT, ((Opt.Some<Integer>) v).value()));
                            }
                            case LONG -> {
                                var v =
                                        switch (op) {
                                            case ADD -> Opt.some(a.longValue() + c.longValue());
                                            case SUB -> Opt.some(a.longValue() - c.longValue());
                                            case MUL -> Opt.some(a.longValue() * c.longValue());
                                            case DIV ->
                                                c.longValue() == 0
                                                        ? Opt.<Long>none()
                                                        : Opt.some(a.longValue() / c.longValue());
                                            case MOD ->
                                                c.longValue() == 0
                                                        ? Opt.<Long>none()
                                                        : Opt.some(a.longValue() % c.longValue());
                                            case AND -> Opt.some(a.longValue() & c.longValue());
                                            case OR -> Opt.some(a.longValue() | c.longValue());
                                            case XOR -> Opt.some(a.longValue() ^ c.longValue());
                                            case SHL -> Opt.some(a.longValue() << c.longValue());
                                            case SHR -> Opt.some(a.longValue() >> c.longValue());
                                            case USHR -> Opt.some(a.longValue() >>> c.longValue());
                                            default -> throw new IllegalStateException("not a long fold op: " + op);
                                        };
                                yield v.optIsNone()
                                        ? divZero(loc, op)
                                        : EResult.ok(constValue(NumericKind.LONG, ((Opt.Some<Long>) v).value()));
                            }
                            case FLOAT ->
                                EResult.ok(constValue(
                                        NumericKind.FLOAT,
                                        switch (op) {
                                            case ADD -> a.floatValue() + c.floatValue();
                                            case SUB -> a.floatValue() - c.floatValue();
                                            case MUL -> a.floatValue() * c.floatValue();
                                            case DIV -> a.floatValue() / c.floatValue();
                                            case MOD -> a.floatValue() % c.floatValue();
                                            default -> throw new IllegalStateException("not a float fold op: " + op);
                                        }));
                            case DOUBLE ->
                                EResult.ok(constValue(
                                        NumericKind.DOUBLE,
                                        switch (op) {
                                            case ADD -> a.doubleValue() + c.doubleValue();
                                            case SUB -> a.doubleValue() - c.doubleValue();
                                            case MUL -> a.doubleValue() * c.doubleValue();
                                            case DIV -> a.doubleValue() / c.doubleValue();
                                            case MOD -> a.doubleValue() % c.doubleValue();
                                            default -> throw new IllegalStateException("not a double fold op: " + op);
                                        }));
                            case UBYTE, USHORT, UINT -> {
                                int av = uintVal(b.kind(), a), cv2 = uintVal(r.kind(), c);
                                var v =
                                        switch (op) {
                                            case ADD -> Opt.some(av + cv2);
                                            case SUB -> Opt.some(av - cv2);
                                            case MUL -> Opt.some(av * cv2);
                                            case DIV ->
                                                cv2 == 0
                                                        ? Opt.<Integer>none()
                                                        : Opt.some(Integer.divideUnsigned(av, cv2));
                                            case MOD ->
                                                cv2 == 0
                                                        ? Opt.<Integer>none()
                                                        : Opt.some(Integer.remainderUnsigned(av, cv2));
                                            case AND -> Opt.some(av & cv2);
                                            case OR -> Opt.some(av | cv2);
                                            case XOR -> Opt.some(av ^ cv2);
                                            case SHL -> Opt.some(av << cv2);
                                            case SHR, USHR -> Opt.some(av >>> cv2);
                                            default ->
                                                throw new IllegalStateException("not an unsigned-int fold op: " + op);
                                        };
                                yield v.optIsNone()
                                        ? divZero(loc, op)
                                        : EResult.ok(constValue(NumericKind.UINT, ((Opt.Some<Integer>) v).value()));
                            }
                            case ULONG -> {
                                long av = ulongVal(b.kind(), a), cv2 = ulongVal(r.kind(), c);
                                var v =
                                        switch (op) {
                                            case ADD -> Opt.some(av + cv2);
                                            case SUB -> Opt.some(av - cv2);
                                            case MUL -> Opt.some(av * cv2);
                                            case DIV ->
                                                cv2 == 0 ? Opt.<Long>none() : Opt.some(Long.divideUnsigned(av, cv2));
                                            case MOD ->
                                                cv2 == 0 ? Opt.<Long>none() : Opt.some(Long.remainderUnsigned(av, cv2));
                                            case AND -> Opt.some(av & cv2);
                                            case OR -> Opt.some(av | cv2);
                                            case XOR -> Opt.some(av ^ cv2);
                                            case SHL -> Opt.some(av << cv2);
                                            case SHR, USHR -> Opt.some(av >>> cv2);
                                            default ->
                                                throw new IllegalStateException("not an unsigned-long fold op: " + op);
                                        };
                                yield v.optIsNone()
                                        ? divZero(loc, op)
                                        : EResult.ok(constValue(NumericKind.ULONG, ((Opt.Some<Long>) v).value()));
                            }
                        };
                if (foldedRes.hasErrors()) return foldedRes.reerr();
                result = foldedRes.requireValue("Numerics::foldArith");
            }
            var concatRes = Eval.codeConcat(xx, loc, base.code(), reference.code());
            if (concatRes.hasErrors()) return concatRes.reerr();
            return EResult.ok(new CodeValue(concatRes.requireValue("concat"), EvalOutcome.ofValue(result)));
        }

        var jb = new JavaBytecodeBuilder(loc);
        var baseRegRes = pushConverted(xx, loc, jb, base, b, compute);
        if (baseRegRes.hasErrors()) return baseRegRes.reerr();
        var baseReg = baseRegRes.requireValue("Numerics::pushConverted base");
        var refTarget = op.shift() ? NumericKind.INT : compute;
        var refRegRes = pushConverted(xx, loc, jb, reference, r, refTarget);
        if (refRegRes.hasErrors()) return refRegRes.reerr();
        var refReg = refRegRes.requireValue("Numerics::pushConverted reference");

        Type resultType;
        JavaBytecodeReg out;
        if (op.comparison()) {
            int branch;
            TList<JavaBytecodeReg> branchArgs;
            switch (compute) {
                case INT, BYTE, SHORT -> {
                    branch = intCmpBranch(op);
                    branchArgs = TList.of(baseReg, refReg);
                }
                case UINT -> {
                    if (op.ordered()) {
                        var c = staticCall(
                                jb, Integer.class, "compareUnsigned", "I", TList.of("I", "I"), baseReg, refReg);
                        branch = zeroCmpBranch(op);
                        branchArgs = TList.of(c);
                    } else {
                        branch = intCmpBranch(op);
                        branchArgs = TList.of(baseReg, refReg);
                    }
                }
                case LONG -> {
                    var c = jb.op(Opcodes.LCMP, JavaDataDescriptor.DESC_INT, baseReg, refReg);
                    branch = zeroCmpBranch(op);
                    branchArgs = TList.of(c);
                }
                case ULONG -> {
                    JavaBytecodeReg c;
                    if (op.ordered()) {
                        c = staticCall(jb, Long.class, "compareUnsigned", "I", TList.of("J", "J"), baseReg, refReg);
                    } else {
                        c = jb.op(Opcodes.LCMP, JavaDataDescriptor.DESC_INT, baseReg, refReg);
                    }
                    branch = zeroCmpBranch(op);
                    branchArgs = TList.of(c);
                }
                case FLOAT -> {
                    var c = jb.op(floatCmpOpcode(op, false), JavaDataDescriptor.DESC_INT, baseReg, refReg);
                    branch = zeroCmpBranch(op);
                    branchArgs = TList.of(c);
                }
                case DOUBLE -> {
                    var c = jb.op(floatCmpOpcode(op, true), JavaDataDescriptor.DESC_INT, baseReg, refReg);
                    branch = zeroCmpBranch(op);
                    branchArgs = TList.of(c);
                }
                case UBYTE, USHORT -> throw new IllegalStateException("compare kind not promoted: " + compute);
                default -> throw new IllegalStateException("unreachable");
            }
            var trueKey = new JumpKey();
            var endKey = new JumpKey();
            jb.branch(branch, trueKey, branchArgs.toArray(JavaBytecodeReg[]::new));
            jb.jump(endKey, jb.constInt(0));
            jb.land(trueKey);
            jb.jump(endKey, jb.constInt(1));
            resultType = new TypeBoolDynamic();
            out = new JavaBytecodeReg(JvmSort.descriptor(xx, resultType));
            jb.land(endKey, out);
        } else if (compute.unsigned() && (op == Op.DIV || op == Op.MOD)) {
            out = compute == NumericKind.UINT
                    ? staticCall(
                            jb,
                            Integer.class,
                            op == Op.DIV ? "divideUnsigned" : "remainderUnsigned",
                            "I",
                            TList.of("I", "I"),
                            baseReg,
                            refReg)
                    : staticCall(
                            jb,
                            Long.class,
                            op == Op.DIV ? "divideUnsigned" : "remainderUnsigned",
                            "J",
                            TList.of("J", "J"),
                            baseReg,
                            refReg);
            resultType = dynamicType(compute);
        } else {
            int opcode =
                    switch (compute) {
                        case INT, BYTE, SHORT ->
                            switch (op) {
                                case ADD -> Opcodes.IADD;
                                case SUB -> Opcodes.ISUB;
                                case MUL -> Opcodes.IMUL;
                                case DIV -> Opcodes.IDIV;
                                case MOD -> Opcodes.IREM;
                                case AND -> Opcodes.IAND;
                                case OR -> Opcodes.IOR;
                                case XOR -> Opcodes.IXOR;
                                case SHL -> Opcodes.ISHL;
                                case SHR -> Opcodes.ISHR;
                                case USHR -> Opcodes.IUSHR;
                                default -> throw new IllegalStateException("not an int arithmetic op: " + op);
                            };
                        case LONG ->
                            switch (op) {
                                case ADD -> Opcodes.LADD;
                                case SUB -> Opcodes.LSUB;
                                case MUL -> Opcodes.LMUL;
                                case DIV -> Opcodes.LDIV;
                                case MOD -> Opcodes.LREM;
                                case AND -> Opcodes.LAND;
                                case OR -> Opcodes.LOR;
                                case XOR -> Opcodes.LXOR;
                                case SHL -> Opcodes.LSHL;
                                case SHR -> Opcodes.LSHR;
                                case USHR -> Opcodes.LUSHR;
                                default -> throw new IllegalStateException("not a long arithmetic op: " + op);
                            };
                        case UINT ->
                            switch (op) {
                                case ADD -> Opcodes.IADD;
                                case SUB -> Opcodes.ISUB;
                                case MUL -> Opcodes.IMUL;
                                case AND -> Opcodes.IAND;
                                case OR -> Opcodes.IOR;
                                case XOR -> Opcodes.IXOR;
                                case SHL -> Opcodes.ISHL;
                                case SHR, USHR -> Opcodes.IUSHR;
                                default -> throw new IllegalStateException("not an unsigned-int arithmetic op: " + op);
                            };
                        case ULONG ->
                            switch (op) {
                                case ADD -> Opcodes.LADD;
                                case SUB -> Opcodes.LSUB;
                                case MUL -> Opcodes.LMUL;
                                case AND -> Opcodes.LAND;
                                case OR -> Opcodes.LOR;
                                case XOR -> Opcodes.LXOR;
                                case SHL -> Opcodes.LSHL;
                                case SHR, USHR -> Opcodes.LUSHR;
                                default -> throw new IllegalStateException("not an unsigned-long arithmetic op: " + op);
                            };
                        case FLOAT ->
                            switch (op) {
                                case ADD -> Opcodes.FADD;
                                case SUB -> Opcodes.FSUB;
                                case MUL -> Opcodes.FMUL;
                                case DIV -> Opcodes.FDIV;
                                case MOD -> Opcodes.FREM;
                                default -> throw new IllegalStateException("not a float arithmetic op: " + op);
                            };
                        case DOUBLE ->
                            switch (op) {
                                case ADD -> Opcodes.DADD;
                                case SUB -> Opcodes.DSUB;
                                case MUL -> Opcodes.DMUL;
                                case DIV -> Opcodes.DDIV;
                                case MOD -> Opcodes.DREM;
                                default -> throw new IllegalStateException("not a double arithmetic op: " + op);
                            };
                        case UBYTE, USHORT -> throw new IllegalStateException("compute kind not promoted: " + compute);
                    };
            out = jb.op(opcode, primDesc(compute), baseReg, refReg);
            resultType = dynamicType(compute);
        }
        return EResult.ok(DynUtil.regToValue(jb.code(), out, resultType));
    }

    public static EResult<CodeValue> unary(ExprContext xx, QExprId loc, CodeValue base, Unary op) {
        var bvOpt = valueOf(base);
        if (bvOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::unary: operand has no compile-time value");
        }
        var bClassOpt = classify(xx, ((Opt.Some<Value>) bvOpt).value());
        if (bClassOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::unary: operand is not numeric");
        }
        var b = ((Opt.Some<Operand>) bClassOpt).value();
        if (op == Unary.BITNOT && !b.kind().integral()) {
            return EResult.err(loc, "Numerics::bitnot: requires an integral value");
        }
        var compute = NumericKind.unaryPromote(b.kind());

        if (b.isConst()) {
            var n = constNumber(xx, b.value());
            Value result =
                    switch (compute) {
                        case INT, BYTE, SHORT ->
                            new ValueObjectConst(
                                    constType(NumericKind.INT), op == Unary.NEG ? -n.intValue() : ~n.intValue());
                        case LONG ->
                            new ValueObjectConst(
                                    constType(NumericKind.LONG), op == Unary.NEG ? -n.longValue() : ~n.longValue());
                        case FLOAT -> new ValueObjectConst(constType(NumericKind.FLOAT), -n.floatValue());
                        case DOUBLE -> new ValueObjectConst(constType(NumericKind.DOUBLE), -n.doubleValue());
                        case UBYTE, USHORT, UINT -> {
                            int uv = uintVal(b.kind(), n);
                            yield new ValueObjectConst(constType(NumericKind.UINT), op == Unary.NEG ? -uv : ~uv);
                        }
                        case ULONG -> {
                            long uv = ulongVal(b.kind(), n);
                            yield new ValueObjectConst(constType(NumericKind.ULONG), op == Unary.NEG ? -uv : ~uv);
                        }
                    };
            return EResult.ok(new CodeValue(base.code(), EvalOutcome.ofValue(result)));
        }

        var jb = new JavaBytecodeBuilder(loc);
        var baseRegRes = pushConverted(xx, loc, jb, base, b, compute);
        if (baseRegRes.hasErrors()) return baseRegRes.reerr();
        var baseReg = baseRegRes.requireValue("Numerics::unary pushConverted");
        JavaBytecodeReg out;
        if (op == Unary.NEG) {
            out = jb.op(
                    switch (compute) {
                        case INT, BYTE, SHORT, UINT, UBYTE, USHORT -> Opcodes.INEG;
                        case LONG, ULONG -> Opcodes.LNEG;
                        case FLOAT -> Opcodes.FNEG;
                        case DOUBLE -> Opcodes.DNEG;
                    },
                    primDesc(compute),
                    baseReg);
        } else if (compute.wide()) {
            var minusOne = jb.constLong(-1L);
            out = jb.op(Opcodes.LXOR, JavaDataDescriptor.DESC_LONG, baseReg, minusOne);
        } else {
            var minusOne = jb.op(Opcodes.ICONST_M1, JavaDataDescriptor.DESC_INT);
            out = jb.op(Opcodes.IXOR, JavaDataDescriptor.DESC_INT, baseReg, minusOne);
        }
        return EResult.ok(DynUtil.regToValue(jb.code(), out, dynamicType(compute)));
    }

    public static boolean isNumeric(ExprContext xx, Type type) {
        return GenAspectNumericConst.getMaybe(xx, type).optIsSome()
                || GenAspectNumericDynamic.getMaybe(xx, type).optIsSome();
    }

    public static Opt<NumericKind> kindOf(ExprContext xx, Type type) {
        var cOpt = GenAspectNumericConst.getMaybe(xx, type);
        if (cOpt.optIsSome()) {
            return Opt.some(cOpt.optUnwrap().aspectNumericConstKind());
        }
        var dOpt = GenAspectNumericDynamic.getMaybe(xx, type);
        if (dOpt.optIsSome()) {
            return Opt.some(dOpt.optUnwrap().aspectNumericDynamicKind());
        }
        return Opt.none();
    }

    private static Class<?> wrapperClass(NumericKind kind) {
        return switch (kind) {
            case BYTE, UBYTE -> Byte.class;
            case SHORT, USHORT -> Short.class;
            case INT, UINT -> Integer.class;
            case LONG, ULONG -> Long.class;
            case FLOAT -> Float.class;
            case DOUBLE -> Double.class;
        };
    }

    private static JavaDataDescriptor primDesc(NumericKind kind) {
        return switch (kind) {
            case BYTE, UBYTE -> JavaDataDescriptor.DESC_BYTE;
            case SHORT, USHORT -> JavaDataDescriptor.DESC_SHORT;
            case INT, UINT -> JavaDataDescriptor.DESC_INT;
            case LONG, ULONG -> JavaDataDescriptor.DESC_LONG;
            case FLOAT -> JavaDataDescriptor.DESC_FLOAT;
            case DOUBLE -> JavaDataDescriptor.DESC_DOUBLE;
        };
    }

    public static JavaBytecodeReg box(JavaBytecodeBuilder b, NumericKind kind, JavaBytecodeReg value) {
        var wrapper = JavaInternalName.fromClass(wrapperClass(kind));
        return b.invokeStatic(
                wrapper,
                "valueOf",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromInternalName(wrapper), TList.of(primDesc(kind))),
                value);
    }

    public static JavaBytecodeReg unbox(JavaBytecodeBuilder b, NumericKind kind, JavaBytecodeReg boxed) {
        var wrapper = JavaInternalName.fromClass(wrapperClass(kind));
        String name =
                switch (kind) {
                    case BYTE, UBYTE -> "byteValue";
                    case SHORT, USHORT -> "shortValue";
                    case INT, UINT -> "intValue";
                    case LONG, ULONG -> "longValue";
                    case FLOAT -> "floatValue";
                    case DOUBLE -> "doubleValue";
                };
        return b.invokeVirtual(wrapper, name, JavaMethodDescriptor.fromParts(primDesc(kind), TList.of()), boxed);
    }

    public static EResult<CodeValue> castTo(ExprContext xx, QExprId loc, CodeValue cv, NumericKind to, Type dest) {
        var vOpt = valueOf(cv);
        if (vOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::cast: operand has no compile-time value");
        }
        var classOpt = classify(xx, ((Opt.Some<Value>) vOpt).value());
        if (classOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::cast: operand is not numeric");
        }
        var src = ((Opt.Some<Operand>) classOpt).value();
        var jb = new JavaBytecodeBuilder(loc);
        JavaBytecodeReg out;
        if (src.isConst()) {
            var n = constNumber(xx, src.value());
            var carriedRes = jb.addCode(loc, cv.code());
            if (carriedRes.optIsSome()) return EResult.errs(carriedRes.optUnwrap());
            out = switch (to) {
                case BYTE, UBYTE -> jb.constInt((byte) n.longValue());
                case SHORT, USHORT -> jb.constInt((short) n.longValue());
                case INT, UINT -> jb.constInt((int) n.longValue());
                case LONG, ULONG -> jb.constLong(n.longValue());
                case FLOAT -> jb.constFloat(n.floatValue());
                case DOUBLE -> jb.constDouble(n.doubleValue());
            };
        } else {
            var consumed = DynUtil.materialize(xx, loc, cv);
            if (consumed.hasErrors()) return consumed.reerr();
            out = convert(jb, src.kind(), to, jb.reg(consumed.requireValue("Numerics::cast consume")));
        }
        return EResult.ok(DynUtil.regToValue(jb.code(), out, dest));
    }

    private static EResult<JavaBytecodeReg> pushConverted(
            ExprContext xx, QExprId loc, JavaBytecodeBuilder jb, CodeValue cv, Operand op, NumericKind target) {
        if (op.isConst()) {
            var n = constNumber(xx, op.value());
            var carriedRes = jb.addCode(loc, cv.code());
            if (carriedRes.optIsSome()) return EResult.errs(carriedRes.optUnwrap());
            return EResult.ok(convert(jb, op.kind(), target, pushNatural(jb, op.kind(), n)));
        }
        var consumed = DynUtil.materialize(xx, loc, cv);
        if (consumed.hasErrors()) return consumed.reerr();
        return EResult.ok(
                convert(jb, op.kind(), target, jb.reg(consumed.requireValue("Numerics::pushConverted consume"))));
    }

    public static EResult<CodeValue> constConsume(ExprContext xx, QExprId loc, CodeValue cv, NumericKind kind) {
        var vOpt = valueOf(cv);
        if (vOpt.optIsNone()) {
            return EResult.err(loc, "Numerics::constConsume: operand has no compile-time value");
        }
        var n = constNumber(xx, ((Opt.Some<Value>) vOpt).value());
        var jb = new JavaBytecodeBuilder(loc);
        var carriedRes = jb.addCode(loc, cv.code());
        if (carriedRes.optIsSome()) return EResult.errs(carriedRes.optUnwrap());
        var boxed = box(jb, kind, pushNatural(jb, kind, n));
        return EResult.ok(DynUtil.regToValue(jb.code(), boxed, DynUtil.substituteBoxed(xx, dynamicType(kind))));
    }

    private static JavaBytecodeReg pushNatural(JavaBytecodeBuilder jb, NumericKind kind, Number n) {
        return switch (kind) {
            case BYTE, SHORT, INT, UBYTE, USHORT, UINT -> jb.constInt((int) n.longValue());
            case LONG, ULONG -> jb.constLong(n.longValue());
            case FLOAT -> jb.constFloat(n.floatValue());
            case DOUBLE -> jb.constDouble(n.doubleValue());
        };
    }

    private static JavaBytecodeReg convert(
            JavaBytecodeBuilder jb, NumericKind from, NumericKind to, JavaBytecodeReg v) {
        if (from == to) return v;
        boolean fromInt = from.integral();
        boolean toInt = to.integral();
        if (fromInt && toInt) {
            int fw = from.intWidth();
            int tw = to.intWidth();
            if (fw == tw) return v;
            if (fw < tw) {
                if (from.unsigned()) {
                    return switch (from) {
                        case UBYTE ->
                            tw <= 32
                                    ? staticCall(jb, Byte.class, "toUnsignedInt", "I", TList.of("B"), v)
                                    : staticCall(jb, Byte.class, "toUnsignedLong", "J", TList.of("B"), v);
                        case USHORT ->
                            tw <= 32
                                    ? staticCall(jb, Short.class, "toUnsignedInt", "I", TList.of("S"), v)
                                    : staticCall(jb, Short.class, "toUnsignedLong", "J", TList.of("S"), v);
                        case UINT -> staticCall(jb, Integer.class, "toUnsignedLong", "J", TList.of("I"), v);
                        default -> throw new IllegalStateException("zeroExtend from non-unsigned kind " + from);
                    };
                }
                if (tw == 64) return jb.op(Opcodes.I2L, JavaDataDescriptor.DESC_LONG, v);
                return v;
            }
            var out = v;
            if (fw == 64) out = jb.op(Opcodes.L2I, JavaDataDescriptor.DESC_INT, out);
            if (tw == 8) out = jb.op(Opcodes.I2B, JavaDataDescriptor.DESC_BYTE, out);
            else if (tw == 16) out = jb.op(Opcodes.I2S, JavaDataDescriptor.DESC_SHORT, out);
            return out;
        }
        if (fromInt) {
            boolean toDouble = to == NumericKind.DOUBLE;
            var floatDesc = toDouble ? JavaDataDescriptor.DESC_DOUBLE : JavaDataDescriptor.DESC_FLOAT;
            if (from.unsigned()) {
                return switch (from) {
                    case UBYTE -> {
                        var i = staticCall(jb, Byte.class, "toUnsignedInt", "I", TList.of("B"), v);
                        yield jb.op(toDouble ? Opcodes.I2D : Opcodes.I2F, floatDesc, i);
                    }
                    case USHORT -> {
                        var i = staticCall(jb, Short.class, "toUnsignedInt", "I", TList.of("S"), v);
                        yield jb.op(toDouble ? Opcodes.I2D : Opcodes.I2F, floatDesc, i);
                    }
                    case UINT -> {
                        var l = staticCall(jb, Integer.class, "toUnsignedLong", "J", TList.of("I"), v);
                        yield jb.op(toDouble ? Opcodes.L2D : Opcodes.L2F, floatDesc, l);
                    }
                    case ULONG ->
                        jb.invokeStatic(
                                toDouble
                                        ? GenCodegenDescriptors.UNSIGNED_TO_DOUBLE
                                        : GenCodegenDescriptors.UNSIGNED_TO_FLOAT,
                                v);
                    default -> throw new IllegalStateException("intToFloat from " + from);
                };
            }
            if (from.intWidth() == 64) {
                return jb.op(toDouble ? Opcodes.L2D : Opcodes.L2F, floatDesc, v);
            }
            return jb.op(toDouble ? Opcodes.I2D : Opcodes.I2F, floatDesc, v);
        }
        if (toInt) {
            boolean toLong = to.intWidth() == 64;
            var intDesc = toLong ? JavaDataDescriptor.DESC_LONG : JavaDataDescriptor.DESC_INT;
            JavaBytecodeReg out;
            if (from == NumericKind.FLOAT) {
                out = jb.op(toLong ? Opcodes.F2L : Opcodes.F2I, intDesc, v);
            } else {
                out = jb.op(toLong ? Opcodes.D2L : Opcodes.D2I, intDesc, v);
            }
            if (to.intWidth() == 8) out = jb.op(Opcodes.I2B, JavaDataDescriptor.DESC_BYTE, out);
            else if (to.intWidth() == 16) out = jb.op(Opcodes.I2S, JavaDataDescriptor.DESC_SHORT, out);
            return out;
        }
        return from == NumericKind.FLOAT
                ? jb.op(Opcodes.F2D, JavaDataDescriptor.DESC_DOUBLE, v)
                : jb.op(Opcodes.D2F, JavaDataDescriptor.DESC_FLOAT, v);
    }

    private static int intCmpBranch(Op op) {
        return switch (op) {
            case EQ -> Opcodes.IF_ICMPEQ;
            case NE -> Opcodes.IF_ICMPNE;
            case LT -> Opcodes.IF_ICMPLT;
            case LE -> Opcodes.IF_ICMPLE;
            case GT -> Opcodes.IF_ICMPGT;
            case GE -> Opcodes.IF_ICMPGE;
            default -> throw new IllegalStateException("not a comparison op: " + op);
        };
    }

    private static int zeroCmpBranch(Op op) {
        return switch (op) {
            case EQ -> Opcodes.IFEQ;
            case NE -> Opcodes.IFNE;
            case LT -> Opcodes.IFLT;
            case LE -> Opcodes.IFLE;
            case GT -> Opcodes.IFGT;
            case GE -> Opcodes.IFGE;
            default -> throw new IllegalStateException("not a comparison op: " + op);
        };
    }

    private static int floatCmpOpcode(Op op, boolean isDouble) {
        boolean g = op == Op.LT || op == Op.LE;
        if (isDouble) return g ? Opcodes.DCMPG : Opcodes.DCMPL;
        return g ? Opcodes.FCMPG : Opcodes.FCMPL;
    }

    private static JavaBytecodeReg staticCall(
            JavaBytecodeBuilder jb,
            Class<?> owner,
            String method,
            String retDesc,
            TList<String> argDescs,
            JavaBytecodeReg... args) {
        var params = new TList<JavaDataDescriptor>();
        for (var a : argDescs) params.add(JavaDataDescriptor.create(a));
        return jb.invokeStatic(
                JavaInternalName.fromClass(owner),
                method,
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.create(retDesc), params),
                args);
    }

    private static int uintVal(NumericKind kind, Number n) {
        return switch (kind) {
            case UBYTE -> Byte.toUnsignedInt(n.byteValue());
            case USHORT -> Short.toUnsignedInt(n.shortValue());
            case UINT -> n.intValue();
            default -> throw new IllegalStateException("uintVal on " + kind);
        };
    }

    private static long ulongVal(NumericKind kind, Number n) {
        return switch (kind) {
            case UBYTE -> Byte.toUnsignedLong(n.byteValue());
            case USHORT -> Short.toUnsignedLong(n.shortValue());
            case UINT -> Integer.toUnsignedLong(n.intValue());
            case ULONG -> n.longValue();
            default -> throw new IllegalStateException("ulongVal on " + kind);
        };
    }

    private static EResult<Value> divZero(QExprId loc, Op op) {
        return EResult.err(loc, "Numerics::" + op.symbol() + ": division by zero");
    }
}
