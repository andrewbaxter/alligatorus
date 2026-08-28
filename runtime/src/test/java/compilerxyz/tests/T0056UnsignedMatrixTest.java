package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import compilerxyz.eval.ExprErr;

public final class T0056UnsignedMatrixTest {

    private static final class Cases {
        final TestMb mb;
        final List<SourceExpr> stmts = new ArrayList<>();
        final List<String> expected = new ArrayList<>();

        Cases(TestMb mb) {
            this.mb = mb;
        }

        SourceExpr ubyteV(double v) {
            return mb.as_(mb.num(v), mb.typesUbyte());
        }

        SourceExpr ushortV(double v) {
            return mb.as_(mb.num(v), mb.builtin("types", "ushort"));
        }

        SourceExpr uintV(double v) {
            return mb.as_(mb.num(v), mb.typesUint());
        }

        SourceExpr ulongV(double v) {
            return mb.as_(mb.num(v), mb.typesUlong());
        }

        void num(SourceExpr expr, String exp) {
            stmts.add(mb.call(mb.log(), mb.call(mb.field(expr, mb.str("to_string")), mb.tupleOf())));
            expected.add(exp);
        }

        void bool(SourceExpr cond, boolean exp) {
            stmts.add(
                    mb.ifOf(
                            TList.of(new TestMb.Branch(cond, mb.call(mb.log(), mb.str("T")))),
                            Opt.some(mb.call(mb.log(), mb.str("F")))));
            expected.add(exp ? "T" : "F");
        }
    }

    private void check(Cases c) {
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(c.mb.seqOf(c.stmts.toArray(new SourceExpr[0]))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<?, ?> o -> {
                @SuppressWarnings("unchecked")
                var cast = (Result.Ok<Compile.CompileResult, TList<ExprErr>>) o;
                resOk = cast;
            }
            case Result.Err<?, ?> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                throw new RuntimeException("unreachable");
            }
        }
        var ok = resOk.value();
        var got = new ArrayList<String>();
        for (var l : ok.logs()) got.add(l.message());
        assertEquals(c.expected, got);
    }

    @Test
    public void test00000UnsignedArithmetic() {
        var mb = new TestMb();
        var c = new Cases(mb);
        c.num(mb.operatorAdd(c.ubyteV(200), c.ubyteV(50)), "250");
        c.num(mb.operatorSub(c.ubyteV(200), c.ubyteV(50)), "150");
        c.num(mb.operatorMul(c.ubyteV(10), c.ubyteV(20)), "200");
        c.num(mb.operatorDiv(c.ubyteV(200), c.ubyteV(3)), "66");
        c.num(mb.operatorMod(c.ubyteV(200), c.ubyteV(3)), "2");
        c.num(mb.operatorAdd(c.ushortV(40000), c.ushortV(100)), "40100");
        c.num(mb.operatorDiv(c.ushortV(40000), c.ushortV(7)), "5714");
        c.num(mb.operatorAdd(c.uintV(3000000000.0), c.uintV(100)), "3000000100");
        c.num(mb.operatorDiv(c.uintV(3000000000.0), c.uintV(2)), "1500000000");
        c.num(mb.operatorMod(c.uintV(3000000000.0), c.uintV(7)), "4");
        c.num(mb.operatorDiv(c.ulongV(10000000000.0), c.ulongV(3)), "3333333333");
        check(c);
    }

    @Test
    public void test00100UnsignedComparisons() {
        var mb = new TestMb();
        var c = new Cases(mb);
        c.bool(mb.operatorGt(c.uintV(3000000000.0), c.uintV(1)), true);
        c.bool(mb.operatorLt(c.uintV(3000000000.0), c.uintV(1)), false);
        c.bool(mb.operatorGe(c.uintV(3000000000.0), c.uintV(3000000000.0)), true);
        c.bool(mb.operatorEq(c.uintV(3000000000.0), c.uintV(3000000000.0)), true);
        c.bool(mb.operatorNe(c.uintV(3000000000.0), c.uintV(1)), true);
        c.bool(mb.operatorGt(c.ubyteV(200), c.ubyteV(100)), true);
        c.bool(mb.operatorLt(c.ushortV(40000), c.ushortV(50000)), true);
        c.bool(mb.operatorGt(mb.operatorBitNot(c.ulongV(0)), c.ulongV(1)), true);
        check(c);
    }

    @Test
    public void test00200ShiftsAndToString() {
        var mb = new TestMb();
        var c = new Cases(mb);
        c.num(c.ubyteV(200), "200");
        c.num(c.ushortV(40000), "40000");
        c.num(c.uintV(3000000000.0), "3000000000");
        c.num(mb.operatorShr(c.uintV(3000000000.0), c.uintV(1)), "1500000000");
        c.num(mb.operatorUshr(c.uintV(3000000000.0), c.uintV(1)), "1500000000");
        c.num(mb.operatorShl(c.uintV(1), c.uintV(4)), "16");
        c.num(mb.operatorBitNot(c.ulongV(0)), "18446744073709551615");
        c.num(mb.operatorDiv(mb.operatorBitNot(c.ulongV(0)), c.ulongV(2)), "9223372036854775807");
        check(c);
    }

    @Test
    public void test00300BitwiseAndUnary() {
        var mb = new TestMb();
        var c = new Cases(mb);
        c.num(mb.operatorAnd(c.uintV(12), c.uintV(10)), "8");
        c.num(mb.operatorOr(c.uintV(12), c.uintV(10)), "14");
        c.num(mb.operatorXor(c.uintV(12), c.uintV(10)), "6");
        c.num(mb.operatorBitNot(c.uintV(0)), "4294967295");
        c.num(mb.operatorAnd(c.ubyteV(200), c.ubyteV(15)), "8");
        check(c);
    }

    @Test
    public void test00400Conversions() {
        var mb = new TestMb();
        var c = new Cases(mb);
        c.num(mb.as_(c.ubyteV(200), mb.typesUint()), "200");
        c.num(mb.as_(c.ushortV(40000), mb.typesUint()), "40000");
        c.num(mb.as_(c.uintV(3000000000.0), mb.typesUlong()), "3000000000");
        c.num(mb.as_(c.ubyteV(200), mb.typesInt()), "200");
        c.num(mb.as_(c.uintV(3000000000.0), mb.typesLong()), "3000000000");
        c.num(mb.as_(mb.as_(mb.num(-1.0), mb.typesByte()), mb.typesUbyte()), "255");
        c.num(mb.as_(mb.as_(mb.num(-1.0), mb.typesInt()), mb.typesUint()), "4294967295");
        check(c);
    }

    @Test
    public void test00500MixedSignednessRejected() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(
                                                mb.operatorAdd(
                                                        mb.as_(mb.num(1.0), mb.typesUint()),
                                                        mb.as_(mb.num(1.0), mb.typesInt())),
                                                mb.str("to_string")),
                                        mb.tupleOf()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> false;
                    case Result.Err<?, ?> e -> true;
                },
                () -> "expected mixed signed/unsigned add to fail, got " + res);
        assertTrue(
                res.toString().contains("mix signed and unsigned"),
                () -> "expected a signedness error, got " + res);
    }
}
