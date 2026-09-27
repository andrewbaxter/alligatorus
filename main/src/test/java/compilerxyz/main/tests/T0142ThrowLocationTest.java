package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0142ThrowLocationTest {
    private static ExprErr singleErr(TMap<String, SourceModule> modules) {
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        var errs =
                switch (res) {
                    case Result.Ok<Compile.CompileResult, TList<ExprErr>> o ->
                        throw new AssertionError("expected evaluation to fail; got: " + res);
                    case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> e.error();
                };
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        return errs.get(0);
    }

    @Test
    public void test00000JvmExceptionPointsAtThrowingExpr() {
        var mb = new TestMb();
        var div = mb.operatorDiv(mb.as(mb.num(1.0), mb.typesInt()), mb.scopeUse("z"));
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("z", mb.as(mb.num(0.0), mb.typesInt())),
                        mb.set(mb.scopeUse("z"), mb.operatorSub(mb.scopeUse("z"), mb.num(0.0))),
                        mb.call(mb.log(), mb.call(mb.field(div, mb.str("to_string")), mb.tupleOf())))));
        var err = singleErr(modules);
        assertEquals(div.id().value(), err.loc().expr(), () -> "error should point at the division; got: " + err);
        assertTrue(
                err.message().contains("java.lang.ArithmeticException"),
                () -> "expected the JVM exception to surface; got: " + err.message());
    }

    @Test
    public void test00010PanicInDefinedFunctionPointsInsideBody() {
        var mb = new TestMb();
        var panic = mb.panic(mb.str("inside"));
        var callF = mb.call(mb.scopeUse("f"), mb.tupleOf());
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "f",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "nfr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair("ret", mb.optNoneType()),
                                                                new TestMb.RecordPair("impl_body", mb.stage(panic))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        callF)));
        var err = singleErr(modules);
        assertEquals(
                panic.id().value(), err.loc().expr(), () -> "panic should point inside the function body; got: " + err);
        assertTrue(err.message().contains("panic: inside"), () -> "expected panic message; got: " + err.message());
    }

    @Test
    public void test00020PanicInImportedFunctionPointsIntoItsModule() {
        var mb = new TestMb();
        var panic = mb.panic(mb.str("imported"));
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "lib",
                new SourceModule.V1(mb.lower(mb.seqOf(
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                        mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("arg", mb.tupleOf()),
                                        new TestMb.RecordPair("ret", mb.optNoneType()),
                                        new TestMb.RecordPair("impl_body", mb.stage(panic))))),
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.scopeUse("f"))))));
        modules.put("main", new SourceModule.V1(mb.call(mb.importExpr(mb.str("lib")), mb.tupleOf())));
        var err = singleErr(modules);
        assertEquals(
                panic.id().value(), err.loc().expr(), () -> "panic should point into the imported body; got: " + err);
        assertTrue(err.message().contains("panic: imported"), () -> "expected panic message; got: " + err.message());
    }

    @Test
    public void test00030FinishErrorPointsAtFinishCall() {
        var mb = new TestMb();
        var finishCall = mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf());
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.lower(mb.seqOf(
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                        finishCall,
                        mb.return_(mb.field(mb.scopeUse("nfr"), mb.str("func")))))));
        var err = singleErr(modules);
        assertEquals(
                finishCall.id().value(),
                err.loc().expr(),
                () -> "finish error should point at d.finish(); got: " + err);
        assertTrue(
                err.message().contains("has no implementation"),
                () -> "expected missing-implementation error; got: " + err.message());
    }

    @Test
    public void test00040ExportOfUnfinishedSetRejected() {
        var mb = new TestMb();
        var module = mb.lower(mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                mb.call(
                        mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                new TestMb.RecordPair("ret", mb.optNoneType()),
                                new TestMb.RecordPair("impl_body", mb.stage(mb.tupleOf()))))),
                mb.return_(mb.field(mb.scopeUse("nfr"), mb.str("func")))));
        var modules = new TMap<String, SourceModule>();
        modules.put("lib", new SourceModule.V1(module));
        modules.put("main", new SourceModule.V1(mb.importExpr(mb.str("lib"))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        var combined = res.toString();
        assertTrue(
                combined.contains("cannot be exported: its definition set has not been finished"),
                () -> "expected unfinished-set export error; got: " + combined);
    }
}
