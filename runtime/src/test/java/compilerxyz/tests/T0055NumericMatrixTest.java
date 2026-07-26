package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import compilerxyz.eval.ExprErr;

/**
 * Matrix coverage for every numeric operator across same-type and widening (mixed-type) operands of
 * each of the six numeric primitive types.
 */
public final class T0055NumericMatrixTest {

  /** Accumulates (statement, expected-log) pairs and compiles them as one module. */
  private static final class Cases {
    final TestMb mb;
    final List<SourceExpr> stmts = new ArrayList<>();
    final List<String> expected = new ArrayList<>();

    Cases(TestMb mb) {
      this.mb = mb;
    }

    SourceExpr byteV(double v) {
      return mb.as_(mb.num(v), mb.typesByte());
    }

    SourceExpr shortV(double v) {
      return mb.as_(mb.num(v), mb.typesShort());
    }

    SourceExpr intV(double v) {
      return mb.as_(mb.num(v), mb.typesInt());
    }

    SourceExpr longV(double v) {
      return mb.as_(mb.num(v), mb.typesLong());
    }

    SourceExpr floatV(double v) {
      return mb.as_(mb.num(v), mb.typesFloat());
    }

    SourceExpr doubleV(double v) {
      return mb.as_(mb.num(v), mb.typesDouble());
    }

    /** Log the numeric result's to_string and record the expectation. */
    void num(SourceExpr expr, String exp) {
      stmts.add(mb.call(mb.log(), mb.call(mb.field(expr, mb.str("to_string")), mb.tupleOf())));
      expected.add(exp);
    }

    /** Log "T"/"F" from a comparison's boolean result and record the expectation. */
    void bool(SourceExpr cond, boolean exp) {
      stmts.add(
          mb.ifOf(
              TList.of(new TestMb.Branch(cond, mb.call(mb.log(), mb.str("T")))),
              mb.call(mb.log(), mb.str("F"))));
      expected.add(exp ? "T" : "F");
    }
  }

  private void check(Cases c) {
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "main", new SourceModule.V1(c.mb.seqOf(c.stmts.toArray(new SourceExpr[0]))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(res instanceof Result.Ok<?, ?>, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    var got = new ArrayList<String>();
    for (var l : ok.logs()) got.add(l.message());
    assertEquals(c.expected, got);
  }

  @Test
  public void test00000ArithmeticSameType() {
    var mb = new TestMb();
    var c = new Cases(mb);
    // integral (7,3): + - * / %
    c.num(mb.operatorAdd(c.intV(7), c.intV(3)), "10");
    c.num(mb.operatorSub(c.intV(7), c.intV(3)), "4");
    c.num(mb.operatorMul(c.intV(7), c.intV(3)), "21");
    c.num(mb.operatorDiv(c.intV(7), c.intV(3)), "2");
    c.num(mb.operatorMod(c.intV(7), c.intV(3)), "1");
    c.num(mb.operatorAdd(c.longV(7), c.longV(3)), "10");
    c.num(mb.operatorDiv(c.longV(7), c.longV(3)), "2");
    // byte/short promote to int
    c.num(mb.operatorAdd(c.byteV(7), c.byteV(3)), "10");
    c.num(mb.operatorMul(c.shortV(7), c.shortV(3)), "21");
    // floating (7.5, 2.0)
    c.num(mb.operatorAdd(c.floatV(7.5), c.floatV(2.0)), "9.5");
    c.num(mb.operatorSub(c.floatV(7.5), c.floatV(2.0)), "5.5");
    c.num(mb.operatorMul(c.floatV(7.5), c.floatV(2.0)), "15.0");
    c.num(mb.operatorDiv(c.floatV(7.5), c.floatV(2.0)), "3.75");
    c.num(mb.operatorMod(c.floatV(7.5), c.floatV(2.0)), "1.5");
    c.num(mb.operatorAdd(c.doubleV(7.5), c.doubleV(2.0)), "9.5");
    c.num(mb.operatorMul(c.doubleV(7.5), c.doubleV(2.0)), "15.0");
    c.num(mb.operatorDiv(c.doubleV(7.5), c.doubleV(2.0)), "3.75");
    check(c);
  }

  @Test
  public void test00100ArithmeticMixedWidening() {
    var mb = new TestMb();
    var c = new Cases(mb);
    // int(7) + wider(3) -> wider type
    c.num(mb.operatorAdd(c.intV(7), c.longV(3)), "10"); // long
    c.num(mb.operatorAdd(c.intV(7), c.floatV(3.0)), "10.0"); // float
    c.num(mb.operatorAdd(c.intV(7), c.doubleV(3.0)), "10.0"); // double
    c.num(mb.operatorAdd(c.longV(7), c.floatV(3.0)), "10.0"); // float
    c.num(mb.operatorAdd(c.longV(7), c.doubleV(3.0)), "10.0"); // double
    c.num(mb.operatorAdd(c.floatV(7.5), c.doubleV(2.0)), "9.5"); // double
    // small ints promote; wider operand wins
    c.num(mb.operatorAdd(c.byteV(7), c.intV(3)), "10"); // int
    c.num(mb.operatorAdd(c.shortV(7), c.longV(3)), "10"); // long
    c.num(mb.operatorMul(c.byteV(7), c.doubleV(3.0)), "21.0"); // double
    // base as the wider operand, reference narrower
    c.num(mb.operatorSub(c.doubleV(7.5), c.intV(2)), "5.5"); // double
    c.num(mb.operatorAdd(c.longV(7), c.intV(3)), "10"); // long
    check(c);
  }

  @Test
  public void test00200BitwiseAndShift() {
    var mb = new TestMb();
    var c = new Cases(mb);
    // & | ^ on 12,10
    c.num(mb.operatorAnd(c.intV(12), c.intV(10)), "8");
    c.num(mb.operatorOr(c.intV(12), c.intV(10)), "14");
    c.num(mb.operatorXor(c.intV(12), c.intV(10)), "6");
    c.num(mb.operatorAnd(c.longV(12), c.longV(10)), "8");
    c.num(mb.operatorOr(c.longV(12), c.longV(10)), "14");
    c.num(mb.operatorXor(c.byteV(12), c.byteV(10)), "6"); // promotes to int
    c.num(mb.operatorAnd(c.intV(12), c.longV(10)), "8"); // mixed -> long
    // shifts
    c.num(mb.operatorShl(c.intV(1), c.intV(4)), "16");
    c.num(mb.operatorShr(c.intV(256), c.intV(2)), "64");
    c.num(mb.operatorUshr(c.intV(-1), c.intV(28)), "15");
    c.num(mb.operatorShl(c.longV(1), c.intV(4)), "16"); // long shifted, int amount
    c.num(mb.operatorShr(c.longV(256), c.intV(2)), "64");
    check(c);
  }

  @Test
  public void test00300Unary() {
    var mb = new TestMb();
    var c = new Cases(mb);
    c.num(mb.operatorNeg(c.intV(5)), "-5");
    c.num(mb.operatorNeg(c.longV(5)), "-5");
    c.num(mb.operatorNeg(c.floatV(5.0)), "-5.0");
    c.num(mb.operatorNeg(c.doubleV(5.0)), "-5.0");
    c.num(mb.operatorNeg(c.byteV(5)), "-5"); // promotes to int
    c.num(mb.operatorBitNot(c.intV(5)), "-6");
    c.num(mb.operatorBitNot(c.longV(5)), "-6");
    c.num(mb.operatorBitNot(c.byteV(5)), "-6"); // promotes to int
    check(c);
  }

  @Test
  public void test00400Comparisons() {
    var mb = new TestMb();
    var c = new Cases(mb);
    c.bool(mb.operatorLt(c.intV(3), c.intV(5)), true);
    c.bool(mb.operatorLt(c.intV(5), c.intV(3)), false);
    c.bool(mb.operatorEq(c.intV(3), c.intV(3)), true);
    c.bool(mb.operatorNe(c.intV(3), c.intV(3)), false);
    c.bool(mb.operatorNe(c.intV(3), c.intV(5)), true);
    c.bool(mb.operatorLe(c.intV(5), c.intV(5)), true);
    c.bool(mb.operatorGe(c.intV(5), c.intV(6)), false);
    c.bool(mb.operatorGt(c.intV(5), c.intV(3)), true);
    c.bool(mb.operatorGe(c.intV(3), c.intV(3)), true);
    // same-type for each numeric kind
    c.bool(mb.operatorLt(c.longV(3), c.longV(5)), true);
    c.bool(mb.operatorLt(c.floatV(3.5), c.floatV(5.5)), true);
    c.bool(mb.operatorGt(c.doubleV(5.5), c.doubleV(3.5)), true);
    c.bool(mb.operatorEq(c.byteV(3), c.byteV(3)), true);
    c.bool(mb.operatorEq(c.shortV(3), c.shortV(4)), false);
    // mixed widening comparisons
    c.bool(mb.operatorLt(c.intV(3), c.doubleV(5.0)), true);
    c.bool(mb.operatorEq(c.floatV(1.0), c.doubleV(1.0)), true);
    c.bool(mb.operatorGt(c.longV(5), c.intV(3)), true);
    c.bool(mb.operatorLe(c.byteV(2), c.longV(2)), true);
    check(c);
  }
}
