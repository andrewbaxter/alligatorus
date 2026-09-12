package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0130DropThrowTest.canaryInstance;
import static compilerxyz.main.tests.T0130DropThrowTest.compileOk;
import static compilerxyz.main.tests.T0130DropThrowTest.dynTrue;
import static compilerxyz.main.tests.T0130DropThrowTest.makeCanaryInstance;
import static compilerxyz.main.tests.T0130DropThrowTest.messages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0131DropReturnTest {
    private static TList<String> run(TestMb mb, TList<SourceExpr> body) {
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.seqOf(body.toArray(new SourceExpr[0]))));
        return messages(compileOk(modules));
    }

    private static SourceExpr canaryDef(TestMb mb, String scope, String message) {
        return mb.bind(scope, mb.lower(makeCanaryInstance(mb, "Canary" + scope, message)));
    }

    private static SourceExpr returnOr(TestMb mb, Opt<SourceExpr> value, SourceExpr elseValue) {
        return mb.ifOf(TList.of(new TestMb.Branch(dynTrue(mb), mb.return_(Opt.some("m"), value))), Opt.some(elseValue));
    }

    private static SourceExpr throwOr(TestMb mb, SourceExpr elseValue) {
        return mb.ifOf(TList.of(new TestMb.Branch(dynTrue(mb), mb.throw_(mb.str("boom")))), Opt.some(elseValue));
    }

    private static SourceExpr dynStr(TestMb mb, String text) {
        return mb.as(mb.str(text), mb.typesString());
    }

    private static SourceExpr caught(TestMb mb, SourceExpr body) {
        return mb.tryCatchFinally(
                body,
                Opt.some(new TestMb.Catch(
                        "e",
                        mb.builtin("types", "string"),
                        mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                Opt.none());
    }

    @Test
    public void returnDropsPendingTupleElement() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        mb.labeled(
                                "m",
                                mb.seqOf(
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        canaryInstance(mb, "ca", "a"),
                                                        returnOr(mb, Opt.none(), dynStr(mb, "c")))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "after"), logs);
    }

    @Test
    public void returnDropsPendingRecordField() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        mb.labeled(
                                "m",
                                mb.seqOf(
                                        mb.bind(
                                                "r",
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "a", canaryInstance(mb, "ca", "a")),
                                                                new TestMb.RecordPair(
                                                                        "b",
                                                                        returnOr(mb, Opt.none(), dynStr(mb, "c")))))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "after"), logs);
    }

    @Test
    public void returnDropsNestedPendingInReverseOrder() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        canaryDef(mb, "cb", "dropped_b"),
                        mb.labeled(
                                "m",
                                mb.seqOf(
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        canaryInstance(mb, "ca", "a"),
                                                        mb.tupleOf(
                                                                canaryInstance(mb, "cb", "b"),
                                                                returnOr(mb, Opt.none(), dynStr(mb, "c"))))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_b", "dropped_a", "after"), logs);
    }

    @Test
    public void returnDropsPendingBeforeEnclosingBindings() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "cx", "dropped_x"),
                        canaryDef(mb, "ca", "dropped_a"),
                        mb.labeled(
                                "m",
                                mb.seqOf(
                                        mb.bind("x", canaryInstance(mb, "cx", "x")),
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        canaryInstance(mb, "ca", "a"),
                                                        returnOr(mb, Opt.none(), dynStr(mb, "c")))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "dropped_x", "after"), logs);
    }

    @Test
    public void returnKeepsCarriedValueAndDropsPending() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        canaryDef(mb, "cr", "dropped_r"),
                        mb.bind(
                                "r",
                                mb.labeled(
                                        "m",
                                        mb.seqOf(
                                                mb.bind(
                                                        "t",
                                                        mb.tupleOf(
                                                                canaryInstance(mb, "ca", "a"),
                                                                returnOr(
                                                                        mb,
                                                                        Opt.some(canaryInstance(mb, "cr", "R")),
                                                                        dynStr(mb, "C")))),
                                                mb.return_("m", canaryInstance(mb, "cr", "Z"))))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "after", "dropped_r"), logs);
    }

    @Test
    public void nestedTupleBindRunsInnerElements() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        canaryDef(mb, "cb", "dropped_b"),
                        mb.bind(
                                "t",
                                mb.tupleOf(
                                        canaryInstance(mb, "ca", "a"),
                                        mb.tupleOf(canaryInstance(mb, "cb", "b"), dynStr(mb, "x")))),
                        mb.call(mb.log(), mb.str("mid"))));
        assertEquals(TList.of("mid", "dropped_b", "dropped_a"), logs);
    }

    @Test
    public void tupleOfBoundStringAndIfValueBinds() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        mb.bind("s", dynStr(mb, "s")),
                        mb.bind(
                                "t",
                                mb.tupleOf(
                                        mb.scopeUse("s"),
                                        mb.ifOf(
                                                TList.of(new TestMb.Branch(dynTrue(mb), dynStr(mb, "x"))),
                                                Opt.some(dynStr(mb, "c"))))),
                        mb.call(mb.log(), mb.str("mid"))));
        assertEquals(TList.of("mid"), logs);
    }

    @Test
    public void returnDoesNotDoubleDropBindingOwnedOperand() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        mb.labeled(
                                "m",
                                mb.seqOf(
                                        mb.bind("a", canaryInstance(mb, "ca", "a")),
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        mb.scopeUse("a"), returnOr(mb, Opt.none(), dynStr(mb, "c")))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "after"), logs);
    }

    @Test
    public void throwDoesNotDoubleDropBindingOwnedOperand() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        caught(
                                mb,
                                mb.seqOf(
                                        mb.bind("a", canaryInstance(mb, "ca", "a")),
                                        mb.bind("t", mb.tupleOf(mb.scopeUse("a"), throwOr(mb, dynStr(mb, "c")))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "caught", "after"), logs);
    }

    @Test
    public void tupleOfBoundValueAndStringBinds() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        mb.bind("a", canaryInstance(mb, "ca", "a")),
                        mb.bind("t", mb.tupleOf(mb.scopeUse("a"), dynStr(mb, "c"))),
                        mb.call(mb.log(), mb.str("mid"))));
        assertEquals(TList.of("mid", "dropped_a", "dropped_a"), logs);
    }

    @Test
    public void throwDropsPendingTupleElement() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        caught(
                                mb,
                                mb.seqOf(
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        canaryInstance(mb, "ca", "a"), throwOr(mb, dynStr(mb, "c")))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_a", "caught", "after"), logs);
    }

    @Test
    public void throwDropsNestedPendingInReverseOrder() {
        var mb = new TestMb();
        var logs = run(
                mb,
                TList.of(
                        canaryDef(mb, "ca", "dropped_a"),
                        canaryDef(mb, "cb", "dropped_b"),
                        caught(
                                mb,
                                mb.seqOf(
                                        mb.bind(
                                                "t",
                                                mb.tupleOf(
                                                        canaryInstance(mb, "ca", "a"),
                                                        mb.tupleOf(
                                                                canaryInstance(mb, "cb", "b"),
                                                                throwOr(mb, dynStr(mb, "c"))))),
                                        mb.call(mb.log(), mb.str("unreached")))),
                        mb.call(mb.log(), mb.str("after"))));
        assertEquals(TList.of("dropped_b", "dropped_a", "caught", "after"), logs);
    }
}
