package compilerxyz.eval.value.numeric;

import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.bool_.TypeBoolConst;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectNumericConst;
import compilerxyz.gen.GenAspectNumericDynamic;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

/**
 * Central operator/cast machinery shared by every numeric type family. A type's operator aspects
 * are thin delegations to {@link #binary} / {@link #unary}; its cast entry points delegate to {@link
 * #castTo}. All widening, Java binary/unary numeric promotion, const folding and JVM bytecode
 * emission lives here so it is written exactly once.
 */
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

    boolean bitwise() {
      return this == AND || this == OR || this == XOR;
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

  // ------------------------------------------------------------------
  // kind -> Type factories
  // ------------------------------------------------------------------

  public static Type constType(NumericKind k) {
    return switch (k) {
      case BYTE -> new compilerxyz.eval.value.byte_.TypeByteConst();
      case SHORT -> new compilerxyz.eval.value.short_.TypeShortConst();
      case INT -> new compilerxyz.eval.value.int_.TypeIntConst();
      case LONG -> new compilerxyz.eval.value.long_.TypeLongConst();
      case FLOAT -> new compilerxyz.eval.value.float_.TypeFloatConst();
      case DOUBLE -> new compilerxyz.eval.value.double_.TypeDoubleConst();
    };
  }

  public static Type dynamicType(NumericKind k) {
    return switch (k) {
      case BYTE -> new compilerxyz.eval.value.byte_.TypeByteDynamic();
      case SHORT -> new compilerxyz.eval.value.short_.TypeShortDynamic();
      case INT -> new compilerxyz.eval.value.int_.TypeIntDynamic();
      case LONG -> new compilerxyz.eval.value.long_.TypeLongDynamic();
      case FLOAT -> new compilerxyz.eval.value.float_.TypeFloatDynamic();
      case DOUBLE -> new compilerxyz.eval.value.double_.TypeDoubleDynamic();
    };
  }

  /** Box a Java numeric into a compile-time constant Value of the given kind. */
  public static Value constValue(NumericKind k, Number n) {
    Object boxed =
        switch (k) {
          case BYTE -> (byte) n.intValue();
          case SHORT -> (short) n.intValue();
          case INT -> n.intValue();
          case LONG -> n.longValue();
          case FLOAT -> n.floatValue();
          case DOUBLE -> n.doubleValue();
        };
    return new ValueObjectConst(constType(k), boxed);
  }

  // ------------------------------------------------------------------
  // operand classification
  // ------------------------------------------------------------------

  private record Operand(NumericKind kind, boolean isConst, Value value) {}

  private static Opt<Operand> classify(ExprContext xx, Value v) {
    var t = v.valueType();
    if (GenAspectNumericConst.getMaybe(xx, t) instanceof Opt.Some<compilerxyz.gen.AspectNumericConst> c) {
      return Opt.some(new Operand(c.value().aspectNumericConstKind(), true, v));
    }
    if (GenAspectNumericDynamic.getMaybe(xx, t)
        instanceof Opt.Some<compilerxyz.gen.AspectNumericDynamic> d) {
      return Opt.some(new Operand(d.value().aspectNumericDynamicKind(), false, v));
    }
    // Fallback: any type that can yield an integral value (AspectInt) participates as a long
    // constant. Lets non-primitive integral-like types (e.g. source-defined wrappers) take part in
    // numeric operators via their long representation.
    if (compilerxyz.gen.GenAspectInt.getMaybe(xx, t)
        instanceof Opt.Some<compilerxyz.gen.AspectInt>) {
      return Opt.some(new Operand(NumericKind.LONG, true, v));
    }
    // Fallback: a dynamic value whose consumed JVM representation is a numeric primitive (e.g. a
    // source-defined wrapper that delegates its consume to a numeric type) participates by that
    // primitive kind.
    if (GenAspectConsumeDynamic.getMaybe(xx, t)
        instanceof Opt.Some<compilerxyz.gen.AspectConsumeDynamic> cd) {
      var desc = cd.value().aspectConsumeDynamicJvmClass(xx).value();
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
    if (a instanceof Opt.Some<compilerxyz.gen.AspectNumericConst> some) {
      return some.value().aspectNumericConstGet(v);
    }
    var i = compilerxyz.gen.GenAspectInt.getMaybe(xx, v.valueType());
    if (i instanceof Opt.Some<compilerxyz.gen.AspectInt> some) {
      return some.value().aspectIntGet(v);
    }
    throw new IllegalStateException("Numerics::constNumber: value is not a numeric constant");
  }

  private static Opt<Value> valueOf(CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) return Opt.some(some.value());
    return Opt.none();
  }

  // ------------------------------------------------------------------
  // binary operators
  // ------------------------------------------------------------------

  public static EResult<CodeValue> binary(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference, Op op) {
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

    NumericKind compute;
    if (op.shift()) {
      if (!b.kind().integral()) {
        return EResult.err(loc, "Numerics::" + op.symbol() + ": shift requires an integral value");
      }
      if (!r.kind().integral()) {
        return EResult.err(
            loc, "Numerics::" + op.symbol() + ": shift amount must be integral");
      }
      compute = NumericKind.unaryPromote(b.kind());
    } else {
      compute = NumericKind.promote(b.kind(), r.kind());
      if (op.bitwise() && !compute.integral()) {
        return EResult.err(
            loc, "Numerics::" + op.symbol() + ": bitwise operator requires integral operands");
      }
    }

    if (b.isConst() && r.isConst()) {
      return foldBinary(xx, loc, base, reference, b, r, compute, op);
    }
    return dynamicBinary(xx, loc, base, reference, b, r, compute, op);
  }

  private static EResult<CodeValue> foldBinary(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      CodeValue reference,
      Operand b,
      Operand r,
      NumericKind compute,
      Op op) {
    var a = constNumber(xx, b.value());
    var c = constNumber(xx, r.value());
    Value result;
    if (op.comparison()) {
      boolean bool =
          switch (compute) {
            case INT, BYTE, SHORT -> compareInt(op, a.intValue(), c.intValue());
            case LONG -> compareLong(op, a.longValue(), c.longValue());
            case FLOAT -> compareFloat(op, a.floatValue(), c.floatValue());
            case DOUBLE -> compareDouble(op, a.doubleValue(), c.doubleValue());
          };
      result = new ValueObjectConst(new TypeBoolConst(), bool);
    } else {
      switch (compute) {
        case INT, BYTE, SHORT -> {
          var v = foldIntOp(op, a.intValue(), c.intValue());
          if (v.optIsNone()) return divZero(loc, op);
          result = new ValueObjectConst(constType(NumericKind.INT), ((Opt.Some<Integer>) v).value());
        }
        case LONG -> {
          var v = foldLongOp(op, a.longValue(), c.longValue());
          if (v.optIsNone()) return divZero(loc, op);
          result = new ValueObjectConst(constType(NumericKind.LONG), ((Opt.Some<Long>) v).value());
        }
        case FLOAT -> result =
            new ValueObjectConst(
                constType(NumericKind.FLOAT), foldFloatOp(op, a.floatValue(), c.floatValue()));
        case DOUBLE -> result =
            new ValueObjectConst(
                constType(NumericKind.DOUBLE), foldDoubleOp(op, a.doubleValue(), c.doubleValue()));
        default -> throw new IllegalStateException("unreachable");
      }
    }
    return EResult.ok(
        new CodeValue(
            Code.concat(loc, base.code(), reference.code()), EvalOutcome.ofValue(result)));
  }

  private static EResult<CodeValue> divZero(QExprId loc, Op op) {
    return EResult.err(loc, "Numerics::" + op.symbol() + ": division by zero");
  }

  private static EResult<CodeValue> dynamicBinary(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      CodeValue reference,
      Operand b,
      Operand r,
      NumericKind compute,
      Op op) {
    var seq = new JavaBytecodeSequence();
    var baseSeq = pushConverted(xx, loc, base, b, compute);
    if (baseSeq.hasErrors()) return baseSeq.reerr();
    seq.add(new CodeDynamic(loc, baseSeq.requireValue("Numerics::pushConverted base")));
    var refTarget = op.shift() ? NumericKind.INT : compute;
    var refSeq = pushConverted(xx, loc, reference, r, refTarget);
    if (refSeq.hasErrors()) return refSeq.reerr();
    seq.add(new CodeDynamic(loc, refSeq.requireValue("Numerics::pushConverted reference")));

    Value resultValue;
    if (op.comparison()) {
      seq.add(compareBytecode(loc, compute, op));
      resultValue = ValueDynamic.stack(new TypeBoolDynamic(), 1);
    } else {
      seq.add(JavaBytecodeUtils.inst(loc, arithOpcode(compute, op)));
      resultValue = ValueDynamic.stack(dynamicType(compute), compute.slots());
    }
    return EResult.ok(
        new CodeValue(Code.of(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(resultValue)));
  }

  // ------------------------------------------------------------------
  // unary operators
  // ------------------------------------------------------------------

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
            case INT, BYTE, SHORT -> new ValueObjectConst(
                constType(NumericKind.INT), op == Unary.NEG ? -n.intValue() : ~n.intValue());
            case LONG -> new ValueObjectConst(
                constType(NumericKind.LONG), op == Unary.NEG ? -n.longValue() : ~n.longValue());
            case FLOAT -> new ValueObjectConst(constType(NumericKind.FLOAT), -n.floatValue());
            case DOUBLE -> new ValueObjectConst(constType(NumericKind.DOUBLE), -n.doubleValue());
          };
      return EResult.ok(new CodeValue(base.code(), EvalOutcome.ofValue(result)));
    }

    var seq = new JavaBytecodeSequence();
    var baseSeq = pushConverted(xx, loc, base, b, compute);
    if (baseSeq.hasErrors()) return baseSeq.reerr();
    seq.add(new CodeDynamic(loc, baseSeq.requireValue("Numerics::unary pushConverted")));
    if (op == Unary.NEG) {
      seq.add(JavaBytecodeUtils.inst(loc, negOpcode(compute)));
    } else {
      switch (compute) {
        case LONG -> {
          seq.add(JavaBytecodeUtils.literalLong(loc, -1L));
          seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LXOR));
        }
        default -> {
          seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ICONST_M1));
          seq.add(JavaBytecodeUtils.inst(loc, Opcodes.IXOR));
        }
      }
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(dynamicType(compute), compute.slots()))));
  }

  // ------------------------------------------------------------------
  // casting
  // ------------------------------------------------------------------

  public static boolean isNumeric(ExprContext xx, Type type) {
    return GenAspectNumericConst.getMaybe(xx, type).optIsSome()
        || GenAspectNumericDynamic.getMaybe(xx, type).optIsSome();
  }

  public static Opt<NumericKind> kindOf(ExprContext xx, Type type) {
    if (GenAspectNumericConst.getMaybe(xx, type)
        instanceof Opt.Some<compilerxyz.gen.AspectNumericConst> c) {
      return Opt.some(c.value().aspectNumericConstKind());
    }
    if (GenAspectNumericDynamic.getMaybe(xx, type)
        instanceof Opt.Some<compilerxyz.gen.AspectNumericDynamic> d) {
      return Opt.some(d.value().aspectNumericDynamicKind());
    }
    return Opt.none();
  }

  private static Class<?> wrapperClass(NumericKind kind) {
    return switch (kind) {
      case BYTE -> Byte.class;
      case SHORT -> Short.class;
      case INT -> Integer.class;
      case LONG -> Long.class;
      case FLOAT -> Float.class;
      case DOUBLE -> Double.class;
    };
  }

  private static compilerxyz.eval.dynamic.JavaDataDescriptor primDesc(NumericKind kind) {
    return switch (kind) {
      case BYTE -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_BYTE;
      case SHORT -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_SHORT;
      case INT -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_INT;
      case LONG -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_LONG;
      case FLOAT -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_FLOAT;
      case DOUBLE -> compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_DOUBLE;
    };
  }

  private static String unboxMethod(NumericKind kind) {
    return switch (kind) {
      case BYTE -> "byteValue";
      case SHORT -> "shortValue";
      case INT -> "intValue";
      case LONG -> "longValue";
      case FLOAT -> "floatValue";
      case DOUBLE -> "doubleValue";
    };
  }

  /** Box the on-stack primitive of {@code kind} into its wrapper (Wrapper.valueOf). */
  public static CodeDynamic box(QExprId loc, NumericKind kind) {
    var wrapper = compilerxyz.eval.dynamic.JavaInternalName.fromClass(wrapperClass(kind));
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        wrapper,
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(wrapper),
            compilerxyz.util.TList.of(primDesc(kind))));
  }

  /** Unbox the on-stack wrapper of {@code kind} to its primitive (wrapper.xValue()). */
  public static CodeDynamic unbox(QExprId loc, NumericKind kind) {
    var wrapper = compilerxyz.eval.dynamic.JavaInternalName.fromClass(wrapperClass(kind));
    return JavaBytecodeUtils.callMethod(
        loc,
        wrapper,
        unboxMethod(kind),
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            primDesc(kind), compilerxyz.util.TList.of()));
  }

  /** Cast an arbitrary numeric CodeValue to {@code to}, producing a dynamic value of {@code dest}. */
  public static EResult<CodeValue> castTo(
      ExprContext xx, QExprId loc, CodeValue cv, NumericKind to, Type dest) {
    var vOpt = valueOf(cv);
    if (vOpt.optIsNone()) {
      return EResult.err(loc, "Numerics::cast: operand has no compile-time value");
    }
    var classOpt = classify(xx, ((Opt.Some<Value>) vOpt).value());
    if (classOpt.optIsNone()) {
      return EResult.err(loc, "Numerics::cast: operand is not numeric");
    }
    var src = ((Opt.Some<Operand>) classOpt).value();
    var seq = new JavaBytecodeSequence();
    if (src.isConst()) {
      var n = constNumber(xx, src.value());
      addCarried(seq, cv);
      seq.add(pushConst(loc, to, n));
    } else {
      var consumed = consume(xx, loc, cv);
      if (consumed.hasErrors()) return consumed.reerr();
      seq.add(consumed.requireValue("Numerics::cast consume"));
      convertCast(seq, loc, src.kind(), to);
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(dest, to.slots()))));
  }

  // ------------------------------------------------------------------
  // operand pushing / conversion
  // ------------------------------------------------------------------

  private static EResult<JavaBytecodeSequence> pushConverted(
      ExprContext xx, QExprId loc, CodeValue cv, Operand op, NumericKind target) {
    var seq = new JavaBytecodeSequence();
    if (op.isConst()) {
      var n = constNumber(xx, op.value());
      addCarried(seq, cv);
      seq.add(pushNatural(loc, op.kind(), n));
      convertCategory(seq, loc, category(op.kind()), category(target));
    } else {
      var consumed = consume(xx, loc, cv);
      if (consumed.hasErrors()) return consumed.reerr();
      seq.add(consumed.requireValue("Numerics::pushConverted consume"));
      convertCategory(seq, loc, category(op.kind()), category(target));
    }
    return EResult.ok(seq);
  }

  /** Consume a numeric constant into runtime code: push its literal and box it to the wrapper. */
  public static EResult<CodeDynamic> constConsume(
      ExprContext xx, QExprId loc, CodeValue cv, NumericKind kind) {
    var vOpt = valueOf(cv);
    if (vOpt.optIsNone()) {
      return EResult.err(loc, "Numerics::constConsume: operand has no compile-time value");
    }
    var n = constNumber(xx, ((Opt.Some<Value>) vOpt).value());
    var seq = new JavaBytecodeSequence();
    addCarried(seq, cv);
    seq.add(pushNatural(loc, kind, n));
    seq.add(box(loc, kind));
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  /** Prepend any carried dynamic side-effect code a (possibly-const) operand holds. */
  private static void addCarried(JavaBytecodeSequence seq, CodeValue cv) {
    var carried = cv.code().dynamic();
    if (carried != null) seq.add(carried);
  }

  private static EResult<CodeDynamic> consume(ExprContext xx, QExprId loc, CodeValue cv) {
    var vOpt = valueOf(cv);
    var type = ((Opt.Some<Value>) vOpt).value().valueType();
    if (GenAspectConsumeDynamic.getMaybe(xx, type)
        instanceof Opt.Some<compilerxyz.gen.AspectConsumeDynamic> some) {
      return some.value().aspectConsumeDynamicToDynamic(xx, loc, cv);
    }
    return EResult.err(loc, "Numerics::consume: dynamic operand is not consumable");
  }

  /** Push a constant in its own kind's natural JVM stack representation. */
  private static CodeDynamic pushNatural(QExprId loc, NumericKind kind, Number n) {
    return switch (kind) {
      case BYTE, SHORT, INT -> JavaBytecodeUtils.literalIntShortByte(loc, n.intValue());
      case LONG -> JavaBytecodeUtils.literalLong(loc, n.longValue());
      case FLOAT -> JavaBytecodeUtils.literalFloat(loc, n.floatValue());
      case DOUBLE -> JavaBytecodeUtils.literalDouble(loc, n.doubleValue());
    };
  }

  /** Push a constant narrowed/widened into the target kind's representation. */
  private static CodeDynamic pushConst(QExprId loc, NumericKind to, Number n) {
    return switch (to) {
      case BYTE -> JavaBytecodeUtils.literalIntShortByte(loc, (byte) n.intValue());
      case SHORT -> JavaBytecodeUtils.literalIntShortByte(loc, (short) n.intValue());
      case INT -> JavaBytecodeUtils.literalIntShortByte(loc, n.intValue());
      case LONG -> JavaBytecodeUtils.literalLong(loc, n.longValue());
      case FLOAT -> JavaBytecodeUtils.literalFloat(loc, n.floatValue());
      case DOUBLE -> JavaBytecodeUtils.literalDouble(loc, n.doubleValue());
    };
  }

  private enum Cat {
    I,
    L,
    F,
    D
  }

  private static Cat category(NumericKind kind) {
    return switch (kind) {
      case BYTE, SHORT, INT -> Cat.I;
      case LONG -> Cat.L;
      case FLOAT -> Cat.F;
      case DOUBLE -> Cat.D;
    };
  }

  /** Cast conversion including narrowing to byte/short (I2B / I2S after the category conversion). */
  private static void convertCast(JavaBytecodeSequence seq, QExprId loc, NumericKind from, NumericKind to) {
    convertCategory(seq, loc, category(from), category(to));
    if (to == NumericKind.BYTE) {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.I2B));
    } else if (to == NumericKind.SHORT) {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.I2S));
    }
  }

  private static void convertCategory(JavaBytecodeSequence seq, QExprId loc, Cat from, Cat to) {
    if (from == to) return;
    int opcode =
        switch (from) {
          case I -> switch (to) {
            case L -> Opcodes.I2L;
            case F -> Opcodes.I2F;
            case D -> Opcodes.I2D;
            case I -> -1;
          };
          case L -> switch (to) {
            case I -> Opcodes.L2I;
            case F -> Opcodes.L2F;
            case D -> Opcodes.L2D;
            case L -> -1;
          };
          case F -> switch (to) {
            case I -> Opcodes.F2I;
            case L -> Opcodes.F2L;
            case D -> Opcodes.F2D;
            case F -> -1;
          };
          case D -> switch (to) {
            case I -> Opcodes.D2I;
            case L -> Opcodes.D2L;
            case F -> Opcodes.D2F;
            case D -> -1;
          };
        };
    seq.add(JavaBytecodeUtils.inst(loc, opcode));
  }

  // ------------------------------------------------------------------
  // opcode selection
  // ------------------------------------------------------------------

  private static int arithOpcode(NumericKind compute, Op op) {
    return switch (compute) {
      case INT, BYTE, SHORT -> switch (op) {
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
      case LONG -> switch (op) {
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
      case FLOAT -> switch (op) {
        case ADD -> Opcodes.FADD;
        case SUB -> Opcodes.FSUB;
        case MUL -> Opcodes.FMUL;
        case DIV -> Opcodes.FDIV;
        case MOD -> Opcodes.FREM;
        default -> throw new IllegalStateException("not a float arithmetic op: " + op);
      };
      case DOUBLE -> switch (op) {
        case ADD -> Opcodes.DADD;
        case SUB -> Opcodes.DSUB;
        case MUL -> Opcodes.DMUL;
        case DIV -> Opcodes.DDIV;
        case MOD -> Opcodes.DREM;
        default -> throw new IllegalStateException("not a double arithmetic op: " + op);
      };
    };
  }

  private static int negOpcode(NumericKind compute) {
    return switch (compute) {
      case INT, BYTE, SHORT -> Opcodes.INEG;
      case LONG -> Opcodes.LNEG;
      case FLOAT -> Opcodes.FNEG;
      case DOUBLE -> Opcodes.DNEG;
    };
  }

  // ------------------------------------------------------------------
  // comparison bytecode
  // ------------------------------------------------------------------

  private static CodeDynamic compareBytecode(QExprId loc, NumericKind compute, Op op) {
    var seq = new JavaBytecodeSequence();
    int branch;
    switch (compute) {
      case INT, BYTE, SHORT -> branch = intCmpBranch(op);
      case LONG -> {
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.LCMP));
        branch = zeroCmpBranch(op);
      }
      case FLOAT -> {
        seq.add(JavaBytecodeUtils.inst(loc, floatCmpOpcode(op, false)));
        branch = zeroCmpBranch(op);
      }
      case DOUBLE -> {
        seq.add(JavaBytecodeUtils.inst(loc, floatCmpOpcode(op, true)));
        branch = zeroCmpBranch(op);
      }
      default -> throw new IllegalStateException("unreachable");
    }
    var trueLabel = new LabelNode();
    var endLabel = new LabelNode();
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(branch, trueLabel))));
    seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, endLabel))));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(trueLabel)));
    seq.add(JavaBytecodeUtils.literalIntShortByte(loc, 1));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(endLabel)));
    return new CodeDynamic(loc, seq);
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

  /** FCMPL/FCMPG (or DCMPL/DCMPG) chosen so NaN comparisons yield false. */
  private static int floatCmpOpcode(Op op, boolean isDouble) {
    boolean g = op == Op.LT || op == Op.LE;
    if (isDouble) return g ? Opcodes.DCMPG : Opcodes.DCMPL;
    return g ? Opcodes.FCMPG : Opcodes.FCMPL;
  }

  // ------------------------------------------------------------------
  // constant folding helpers
  // ------------------------------------------------------------------

  private static Opt<Integer> foldIntOp(Op op, int a, int b) {
    return switch (op) {
      case ADD -> Opt.some(a + b);
      case SUB -> Opt.some(a - b);
      case MUL -> Opt.some(a * b);
      case DIV -> b == 0 ? Opt.none() : Opt.some(a / b);
      case MOD -> b == 0 ? Opt.none() : Opt.some(a % b);
      case AND -> Opt.some(a & b);
      case OR -> Opt.some(a | b);
      case XOR -> Opt.some(a ^ b);
      case SHL -> Opt.some(a << b);
      case SHR -> Opt.some(a >> b);
      case USHR -> Opt.some(a >>> b);
      default -> throw new IllegalStateException("not an int fold op: " + op);
    };
  }

  private static Opt<Long> foldLongOp(Op op, long a, long b) {
    return switch (op) {
      case ADD -> Opt.some(a + b);
      case SUB -> Opt.some(a - b);
      case MUL -> Opt.some(a * b);
      case DIV -> b == 0 ? Opt.none() : Opt.some(a / b);
      case MOD -> b == 0 ? Opt.none() : Opt.some(a % b);
      case AND -> Opt.some(a & b);
      case OR -> Opt.some(a | b);
      case XOR -> Opt.some(a ^ b);
      case SHL -> Opt.some(a << b);
      case SHR -> Opt.some(a >> b);
      case USHR -> Opt.some(a >>> b);
      default -> throw new IllegalStateException("not a long fold op: " + op);
    };
  }

  private static float foldFloatOp(Op op, float a, float b) {
    return switch (op) {
      case ADD -> a + b;
      case SUB -> a - b;
      case MUL -> a * b;
      case DIV -> a / b;
      case MOD -> a % b;
      default -> throw new IllegalStateException("not a float fold op: " + op);
    };
  }

  private static double foldDoubleOp(Op op, double a, double b) {
    return switch (op) {
      case ADD -> a + b;
      case SUB -> a - b;
      case MUL -> a * b;
      case DIV -> a / b;
      case MOD -> a % b;
      default -> throw new IllegalStateException("not a double fold op: " + op);
    };
  }

  private static boolean compareInt(Op op, int a, int b) {
    return switch (op) {
      case EQ -> a == b;
      case NE -> a != b;
      case LT -> a < b;
      case LE -> a <= b;
      case GT -> a > b;
      case GE -> a >= b;
      default -> throw new IllegalStateException("not a comparison op: " + op);
    };
  }

  private static boolean compareLong(Op op, long a, long b) {
    return switch (op) {
      case EQ -> a == b;
      case NE -> a != b;
      case LT -> a < b;
      case LE -> a <= b;
      case GT -> a > b;
      case GE -> a >= b;
      default -> throw new IllegalStateException("not a comparison op: " + op);
    };
  }

  private static boolean compareFloat(Op op, float a, float b) {
    return switch (op) {
      case EQ -> a == b;
      case NE -> a != b;
      case LT -> a < b;
      case LE -> a <= b;
      case GT -> a > b;
      case GE -> a >= b;
      default -> throw new IllegalStateException("not a comparison op: " + op);
    };
  }

  private static boolean compareDouble(Op op, double a, double b) {
    return switch (op) {
      case EQ -> a == b;
      case NE -> a != b;
      case LT -> a < b;
      case LE -> a <= b;
      case GT -> a > b;
      case GE -> a >= b;
      default -> throw new IllegalStateException("not a comparison op: " + op);
    };
  }
}
