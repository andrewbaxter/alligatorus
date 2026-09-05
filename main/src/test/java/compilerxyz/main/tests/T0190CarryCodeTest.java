package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0190CarryCodeTest {

    private static SourceExpr push(TestMb mb, String value) {
        return mb.call(mb.field(mb.scopeUse("l"), mb.str("push")), mb.str(value));
    }

    private static SourceExpr carrying(TestMb mb, String pushed, SourceExpr constValue) {
        return carryingLabeled(mb, "blk", pushed, constValue);
    }

    private static SourceExpr carryingLabeled(TestMb mb, String label, String pushed, SourceExpr constValue) {
        return mb.labeled(label, mb.seqOf(push(mb, pushed), mb.return_(label, constValue)));
    }

    private static TList<String> run(TestMb mb, SourceExpr... body) {
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind(
                "l",
                mb.call(mb.field(mb.call(mb.typesList(), mb.typesString()), mb.str("new")), mb.tupleOf(mb.str("a")))));
        for (var e : body) exprs.add(e);
        exprs.add(mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("l"), mb.str("join")), mb.str("_"))));

        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.seq(exprs)));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> okRes = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + e);
                throw new AssertionError();
            }
        }
        var ok = okRes.value();
        var out = new TList<String>();
        for (var log : ok.logs()) out.add(log.message());
        return out;
    }

    private static void assertPushed(TList<String> logs) {
        assertTrue(
                logs.contains("a_b"),
                () -> "carried dynamic code was discarded; expected a log of \"a_b\", got " + logs);
    }

    @Test
    public void test00100CallArgument() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.call(mb.log(), carrying(mb, "b", mb.str("arg")))));
    }

    @Test
    public void test00200StatementPosition() {
        var mb = new TestMb();
        assertPushed(run(mb, carrying(mb, "b", mb.str("dropped"))));
    }

    @Test
    public void test00300BindThenUse() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.bind("x", carrying(mb, "b", mb.str("bound"))), mb.call(mb.log(), mb.scopeUse("x"))));
    }

    private static SourceExpr intToString(TestMb mb, SourceExpr intExpr) {
        return mb.call(mb.field(intExpr, mb.str("to_string")), mb.tupleOf());
    }

    @Test
    public void test00400OperatorAddLeft() {
        var mb = new TestMb();
        assertPushed(run(
                mb, mb.call(mb.log(), intToString(mb, mb.operatorAdd(carrying(mb, "b", mb.num(1.0)), mb.num(2.0))))));
    }

    @Test
    public void test00500OperatorAddRight() {
        var mb = new TestMb();
        assertPushed(run(
                mb, mb.call(mb.log(), intToString(mb, mb.operatorAdd(mb.num(2.0), carrying(mb, "b", mb.num(1.0)))))));
    }

    @Test
    public void test00600IfCondition() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.ifOf(
                        TList.of(new TestMb.Branch(
                                mb.operatorEq(carrying(mb, "b", mb.num(1.0)), mb.num(1.0)),
                                mb.call(mb.log(), mb.str("then")))),
                        Opt.some(mb.call(mb.log(), mb.str("else"))))));
    }

    @Test
    public void test00700RecordFieldAccess() {
        var mb = new TestMb();
        var rec = mb.record(TList.of(new SourceExpr.RecordEntry(mb.str("k"), carrying(mb, "b", mb.str("field")))));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(rec, mb.str("k")))));
    }

    @Test
    public void test00800RecordSiblingField() {
        var mb = new TestMb();
        var rec = mb.record(TList.of(
                new SourceExpr.RecordEntry(mb.str("carried"), carrying(mb, "b", mb.str("sib"))),
                new SourceExpr.RecordEntry(mb.str("other"), mb.str("other"))));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(rec, mb.str("other")))));
    }

    @Test
    public void test00900AsCast() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.call(mb.log(), intToString(mb, mb.as(carrying(mb, "b", mb.num(1.0)), mb.typesInt())))));
    }

    @Test
    public void test01000NestedBlock() {
        var mb = new TestMb();
        var inner = carryingLabeled(mb, "inner", "b", mb.str("inner-value"));
        var outer = mb.labeled("outer", mb.seqOf(mb.return_("outer", inner)));
        assertPushed(run(mb, mb.call(mb.log(), outer)));
    }

    private static SourceExpr carryingDynamicStr(TestMb mb, String pushed, String value) {
        return carrying(mb, pushed, mb.as(mb.str(value), mb.typesString()));
    }

    private static SourceExpr carryingDynamicInt(TestMb mb, String pushed, double value) {
        return carrying(mb, pushed, mb.as(mb.num(value), mb.typesInt()));
    }

    @Test
    public void test02000DynamicCallArgument() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.call(mb.log(), carryingDynamicStr(mb, "b", "arg"))));
    }

    @Test
    public void test02100DynamicStatementPosition() {
        var mb = new TestMb();
        assertPushed(run(mb, carryingDynamicStr(mb, "b", "dropped")));
    }

    @Test
    public void test02200DynamicBindThenUse() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.bind("x", carryingDynamicStr(mb, "b", "bound")), mb.call(mb.log(), mb.scopeUse("x"))));
    }

    @Test
    public void test02300DynamicOperatorAddLeft() {
        var mb = new TestMb();
        assertPushed(run(
                mb, mb.call(mb.log(), intToString(mb, mb.operatorAdd(carryingDynamicInt(mb, "b", 1.0), mb.num(2.0))))));
    }

    @Test
    public void test02400DynamicOperatorAddRight() {
        var mb = new TestMb();
        assertPushed(run(
                mb, mb.call(mb.log(), intToString(mb, mb.operatorAdd(mb.num(2.0), carryingDynamicInt(mb, "b", 1.0))))));
    }

    @Test
    public void test02450DynamicOperatorAddRightDynamicBase() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.call(
                        mb.log(),
                        intToString(
                                mb,
                                mb.operatorAdd(mb.as(mb.num(2.0), mb.typesInt()), carryingDynamicInt(mb, "b", 1.0))))));
    }

    @Test
    public void test02460ConstBaseDynamicReferenceLt() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.ifOf(
                        TList.of(new TestMb.Branch(
                                mb.operatorLt(mb.num(0.0), carryingDynamicInt(mb, "b", 1.0)),
                                mb.call(mb.log(), mb.str("lt-then")))),
                        Opt.some(mb.call(mb.log(), mb.str("lt-else"))))));
    }

    @Test
    public void test02470ConstBaseDynamicReferenceMod() {
        var mb = new TestMb();
        assertPushed(run(
                mb, mb.call(mb.log(), intToString(mb, mb.operatorMod(mb.num(7.0), carryingDynamicInt(mb, "b", 4.0))))));
    }

    @Test
    public void test02500DynamicIfCondition() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.ifOf(
                        TList.of(new TestMb.Branch(
                                mb.operatorEq(carryingDynamicInt(mb, "b", 1.0), mb.num(1.0)),
                                mb.call(mb.log(), mb.str("then")))),
                        Opt.some(mb.call(mb.log(), mb.str("else"))))));
    }

    @Test
    public void test02600DynamicRecordFieldAccess() {
        var mb = new TestMb();
        var rec = mb.record(TList.of(new SourceExpr.RecordEntry(mb.str("k"), carryingDynamicStr(mb, "b", "field"))));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(rec, mb.str("k")))));
    }

    @Test
    public void test02700DynamicRecordSiblingField() {
        var mb = new TestMb();
        var rec = mb.record(TList.of(
                new SourceExpr.RecordEntry(mb.str("carried"), carryingDynamicStr(mb, "b", "sib")),
                new SourceExpr.RecordEntry(mb.str("other"), mb.str("other"))));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(rec, mb.str("other")))));
    }

    @Test
    public void test02800DynamicNestedBlock() {
        var mb = new TestMb();
        var inner = carryingLabeled(mb, "inner", "b", mb.as(mb.str("inner-value"), mb.typesString()));
        var outer = mb.labeled("outer", mb.seqOf(mb.return_("outer", inner)));
        assertPushed(run(mb, mb.call(mb.log(), outer)));
    }

    @Test
    public void test02900DynamicTupleElement() {
        var mb = new TestMb();
        var tup = mb.tupleOf(carryingDynamicStr(mb, "b", "elem"), mb.str("other"));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(tup, mb.num(0.0)))));
    }

    @Test
    public void test03000DynamicMethodArgument() {
        var mb = new TestMb();
        var logs = run(mb, mb.call(mb.field(mb.scopeUse("l"), mb.str("push")), carrying(mb, "b", mb.str("c"))));
        assertTrue(
                logs.contains("a_b_c"),
                () -> "carried dynamic code was discarded; expected a log of \"a_b_c\", got " + logs);
    }

    private static SourceExpr intList(TestMb mb) {
        return mb.call(mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")), mb.tupleOf(mb.num(1.0)));
    }

    @Test
    public void test03100ForeachBase() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.bind("other", intList(mb)),
                mb.foreach(
                        carrying(mb, "b", mb.scopeUse("other")),
                        mb.str("e"),
                        mb.call(mb.log(), intToString(mb, mb.scopeUse("e"))))));
    }

    @Test
    public void test03200ForeachBody() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.bind("other", intList(mb)),
                mb.foreach(mb.scopeUse("other"), mb.str("e"), mb.call(mb.log(), carrying(mb, "b", mb.str("body"))))));
    }

    @Test
    public void test03300ListConstructorTypeArgument() {
        var mb = new TestMb();
        var listType = mb.call(mb.typesList(), carrying(mb, "b", mb.typesString()));
        assertPushed(run(mb, mb.bind("other", mb.call(mb.field(listType, mb.str("new")), mb.tupleOf(mb.str("x"))))));
    }

    @Test
    public void test03050ForeachStringList() {
        var mb = new TestMb();
        assertPushed(
                run(mb, push(mb, "b"), mb.foreach(mb.scopeUse("l"), mb.str("e"), mb.call(mb.log(), mb.scopeUse("e")))));
    }

    @Test
    public void test03600TryCatchFinallyBody() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.tryExpr(
                        mb.seqOf(mb.call(mb.log(), carrying(mb, "b", mb.str("try"))), mb.tupleOf()),
                        Opt.some(new SourceExpr.TryCatch(
                                mb.str("err"),
                                mb.typesString(),
                                mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                        Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("fin")), mb.tupleOf())))));
    }

    @Test
    public void test03700TryCatchFinallyCarryInFinally() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.tryExpr(
                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.tupleOf()),
                        Opt.some(new SourceExpr.TryCatch(
                                mb.str("err"),
                                mb.typesString(),
                                mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                        Opt.some(mb.seqOf(mb.call(mb.log(), carrying(mb, "b", mb.str("fin"))), mb.tupleOf())))));
    }

    @Test
    public void test03800TryFinallyCarryInFinally() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.tryExpr(
                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.tupleOf()),
                        Opt.none(),
                        Opt.some(mb.seqOf(mb.call(mb.log(), carrying(mb, "b", mb.str("fin"))), mb.tupleOf())))));
    }

    @Test
    public void test03500TryFinallyPlainDynamic() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.tryExpr(mb.call(mb.log(), mb.str("body")), Opt.none(), Opt.some(push(mb, "b")))));
    }

    @Test
    public void test03400TryBody() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.tryExpr(
                        mb.seqOf(mb.call(mb.log(), carrying(mb, "b", mb.str("try"))), mb.tupleOf()),
                        Opt.none(),
                        Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("fin")), mb.tupleOf())))));
    }

    private static SourceExpr defineUpperFunc(TestMb mb) {
        return mb.bind(
                "my_func",
                mb.lower(mb.seqOf(
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                        mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("arg", mb.typesString()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                                        new TestMb.RecordPair(
                                                "impl_body",
                                                mb.stage(
                                                        mb.call(
                                                                mb.field(mb.scopeUse("arg"), mb.str("toUpperCase")),
                                                                mb.tupleOf())))))),
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.scopeUse("f")))));
    }

    @Test
    public void test03900UserFunctionArgument() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                defineUpperFunc(mb),
                mb.call(mb.log(), mb.call(mb.scopeUse("my_func"), carrying(mb, "b", mb.str("hi"))))));
    }

    @Test
    public void test03950UserFunctionDynamicArgument() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                defineUpperFunc(mb),
                mb.call(mb.log(), mb.call(mb.scopeUse("my_func"), carryingDynamicStr(mb, "b", "hi")))));
    }

    @Test
    public void test04000SetValue() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.bind("x", mb.as(mb.num(4.0), mb.typesInt())),
                mb.set(mb.scopeUse("x"), carrying(mb, "b", mb.num(7.0))),
                mb.call(mb.log(), intToString(mb, mb.scopeUse("x")))));
    }

    @Test
    public void test04050SetDynamicValue() {
        var mb = new TestMb();
        assertPushed(run(
                mb,
                mb.bind("x", mb.as(mb.num(4.0), mb.typesInt())),
                mb.set(mb.scopeUse("x"), carryingDynamicInt(mb, "b", 7.0)),
                mb.call(mb.log(), intToString(mb, mb.scopeUse("x")))));
    }

    @Test
    public void test01100TupleElement() {
        var mb = new TestMb();
        var tup = mb.tupleOf(carrying(mb, "b", mb.str("elem")), mb.str("other"));
        assertPushed(run(mb, mb.call(mb.log(), mb.field(tup, mb.num(0.0)))));
    }

    @Test
    public void test00900UnitToNullCastKeepsCarry() {
        var mb = new TestMb();
        assertPushed(run(mb, mb.as(carrying(mb, "b", mb.tupleOf()), mb.typesNull())));
    }
}
