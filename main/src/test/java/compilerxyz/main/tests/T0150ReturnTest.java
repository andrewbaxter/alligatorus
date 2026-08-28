package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0150ReturnTest {
    @Test
    public void test01600ReturnEmptyFromBlock() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.call(mb.log(), mb.str("before")), mb.returnEmpty(), mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected unreachable-code error: " + res);
    }

    @Test
    public void test01610ReturnLabeledSkipsLog() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.call(mb.log(), mb.str("before")),
                        mb.labeled("outer", mb.seqOf(mb.call(mb.log(), mb.str("inside")), mb.returnLabel("outer"))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes49;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes49 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes49.value();
        assertEquals(3, ok.logs().size());
        assertEquals("before", ok.logs().get(0).message());
        assertEquals("inside", ok.logs().get(1).message());
        assertEquals("after", ok.logs().get(2).message());
    }

    @Test
    public void test01615ReturnInOperandRejected() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.seqOf(mb.operatorAdd(mb.num(1.0), mb.return_(mb.num(5.0))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "return in an operand should fail compile: " + res);
    }

    @Test
    public void test01620ReturnUnknownLabelErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(mb.returnLabel("nowhere")));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected unknown-label error: " + res);
    }

    @Test
    public void test01625ReturnToSiblingLabelErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.labeled("other", mb.call(mb.log(), mb.str("in-other"))),
                        mb.labeled("outer", mb.seqOf(mb.returnLabel("other"))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected non-enclosing-label error: " + res);
    }

    @Test
    public void test01630ReturnFromForeachSkipsRest() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(3.)))),
                        mb.foreach(
                                mb.scopeUse("my_list"),
                                mb.str("e"),
                                mb.seqOf(
                                        mb.call(mb.log(), mb.str("iter")),
                                        mb.returnEmpty(),
                                        mb.call(mb.log(), mb.str("SHOULD NOT LOG")))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected unreachable-after-return error: " + res);
    }

    @Test
    public void test01635ReturnWithValueFromBlock() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(
                        mb.call(mb.log(), mb.labeled("out", mb.seqOf(mb.return_("out", mb.str("returned-value")))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes144;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes144 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes144.value();
        assertEquals(1, ok.logs().size());
        assertEquals("returned-value", ok.logs().get(0).message());
    }

    @Test
    public void test01640ReturnFromIf() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.labeled(
                        "out",
                        mb.seqOf(
                                mb.call(mb.log(), mb.str("before")),
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(
                                                mb.bool(true),
                                                mb.seqOf(
                                                        mb.call(mb.log(), mb.str("in-if")),
                                                        mb.returnLabel("out"),
                                                        mb.call(mb.log(), mb.str("SHOULD NOT LOG"))))),
                                        Opt.some(mb.call(mb.log(), mb.str("else")))),
                                mb.call(mb.log(), mb.str("SHOULD NOT LOG AFTER"))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected unreachable errors: " + res);
    }

    @Test
    public void test01650LabeledExitFromForeachSkipsLogInside() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesInt()), mb.str("new")),
                                        mb.tupleOf(mb.num(1.), mb.num(2.), mb.num(3.)))),
                        mb.labeled(
                                "outer",
                                mb.foreach(
                                        mb.scopeUse("my_list"),
                                        mb.str("e"),
                                        mb.seqOf(mb.call(mb.log(), mb.str("iter")), mb.returnLabel("outer")))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes203;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes203 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes203.value();
        assertEquals(2, ok.logs().size());
        assertEquals("iter", ok.logs().get(0).message());
        assertEquals("after", ok.logs().get(1).message());
    }

    @Test
    public void test01660LabeledExitFromIfSkipsLogAfter() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.labeled(
                                "outer",
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(
                                                mb.bool(true),
                                                mb.seqOf(mb.call(mb.log(), mb.str("in-if")), mb.returnLabel("outer")))),
                                        Opt.some(mb.call(mb.log(), mb.str("SHOULD NOT LOG else"))))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes230;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes230 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes230.value();
        assertEquals(2, ok.logs().size());
        assertEquals("in-if", ok.logs().get(0).message());
        assertEquals("after", ok.logs().get(1).message());
    }

    @Test
    public void test01670LabeledExitFromMatchSkipsLogAfter() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "strnum",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "new_str",
                                                mb.field(
                                                        mb.call(
                                                                mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                                mb.recordOf(
                                                                        TList.of(
                                                                                new TestMb.RecordPair(
                                                                                        "tag", mb.str("strtag")),
                                                                                new TestMb.RecordPair(
                                                                                        "type_ref",
                                                                                        mb.typesString())))),
                                                        mb.str("value"))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "type",
                                                                mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                                        new TestMb.RecordPair("str", mb.scopeUse("new_str")))))))),
                        mb.bind("x", mb.call(mb.field(mb.scopeUse("strnum"), mb.str("str")), mb.str("value"))),
                        mb.labeled(
                                "outer",
                                mb.matchOf(
                                        mb.scopeUse("x"),
                                        mb.str("y"),
                                        TList.of(new TestMb.Branch(
                                                mb.str("strtag"),
                                                mb.seqOf(
                                                        mb.call(mb.log(), mb.str("in-match")),
                                                        mb.returnLabel("outer")))),
                                        Opt.none())),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes290;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes290 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes290.value();
        assertEquals(2, ok.logs().size());
        assertEquals("in-match", ok.logs().get(0).message());
        assertEquals("after", ok.logs().get(1).message());
    }

    @Test
    public void test01675IfWithReturningBranchCascade() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.labeled(
                        "out",
                        mb.seqOf(
                                mb.call(mb.log(), mb.str("before")),
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(mb.bool(false), mb.returnLabel("out"))),
                                        Opt.some(mb.call(mb.log(), mb.str("else")))),
                                mb.call(mb.log(), mb.str("after"))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes319;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes319 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes319.value();
        assertEquals(3, ok.logs().size());
        assertEquals("before", ok.logs().get(0).message());
        assertEquals("else", ok.logs().get(1).message());
        assertEquals("after", ok.logs().get(2).message());
    }

    @Test
    public void test01677LabeledReturnFromTryBodyRunsFinally() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.labeled(
                                "outer",
                                mb.tryCatchFinally(
                                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.returnLabel("outer")),
                                        Opt.none(),
                                        Opt.some(mb.call(mb.log(), mb.str("finally"))))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes348;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes348 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes348.value();
        var messages = new TList<String>(ok.logs().size());
        for (var l : ok.logs()) messages.add(l.message());
        assertEquals(
                TList.of("body", "finally", "after"),
                messages,
                () -> "labeled return out of a try body must run the finally; got logs=" + messages);
    }

    @Test
    public void test01678LabeledReturnFinallyWithBranches() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var dynTrue = mb.operatorEq(mb.as_(mb.num(1.0), mb.typesLong()), mb.as_(mb.num(1.0), mb.typesLong()));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.labeled(
                                "outer",
                                mb.tryCatchFinally(
                                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.returnLabel("outer")),
                                        Opt.none(),
                                        Opt.some(mb.ifOf(
                                                TList.of(
                                                        new TestMb.Branch(
                                                                dynTrue, mb.call(mb.log(), mb.str("fin-then")))),
                                                Opt.some(mb.call(mb.log(), mb.str("fin-else"))))))),
                        mb.call(mb.log(), mb.str("after")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes388;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes388 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes388.value();
        var messages = new TList<String>(ok.logs().size());
        for (var l : ok.logs()) messages.add(l.message());
        assertEquals(
                TList.of("body", "fin-then", "after"),
                messages,
                () -> "branchy finally must run once on the labeled-return path; got logs=" + messages);
    }

    @Test
    public void test01680ImplicitEmptyTupleReturnFromNoValueReturn() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("t", mb.labeled("out", mb.seqOf(mb.returnEmpty()))),
                        mb.call(mb.log(), mb.str("done")))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes414;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes414 = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = okRes414.value();
        assertEquals(1, ok.logs().size());
        assertEquals("done", ok.logs().get(0).message());
    }
}
