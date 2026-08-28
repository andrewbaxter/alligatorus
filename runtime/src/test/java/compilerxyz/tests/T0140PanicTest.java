package compilerxyz.tests;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.langhelp.Compile;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0140PanicTest {
    private static SourceExpr panickyFn(TestMb mb, String message) {
        return mb.lower(mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("arg", mb.typesNull()),
                                new TestMb.RecordPair("ret", mb.optSomeType(mb.typesNull())),
                                new TestMb.RecordPair("impl_body", mb.stage(mb.panic(mb.str(message))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.scopeUse("f"))));
    }

    @Test
    public void test01500PanicTerminatesCall() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(
                        mb.seqOf(mb.bind("f", panickyFn(mb, "boom")), mb.call(mb.scopeUse("f"), mb.tupleOf()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok -> false;
                    case Result.Err<?, ?> err -> true;
                },
                () -> "panic from called function should propagate as uncaught runtime exception; got: " + res);
        var combined = res.toString();
        assertTrue(combined.contains("boom"), () -> "expected panic message to surface; got: " + combined);
    }

    @Test
    public void test01510UserTryCatchDoesNotCatchPanic() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("f", panickyFn(mb, "uncatchable")),
                        mb.tryCatchFinally(
                                mb.call(mb.scopeUse("f"), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught")))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok -> false;
                    case Result.Err<?, ?> err -> true;
                },
                () -> "panic should propagate through user try/catch; got: " + res);
        var combined = res.toString();
        assertTrue(combined.contains("uncatchable"), () -> "expected panic message to surface; got: " + combined);
        assertFalse(combined.contains("\"caught\""), () -> "catch body must not have run; got: " + combined);
    }

    @Test
    public void test01520ExceptionCastDoesNotCatchPanic() {
        var mb = new TestMb();
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
                                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                                mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.typesNull()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesNull())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(mb.panic(mb.str("inner panic"))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        mb.tryCatchFinally(
                                mb.call(mb.scopeUse("f"), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught")))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> ok -> false;
                    case Result.Err<?, ?> err -> true;
                },
                () -> "panic inside a function declaring its own exception type should still propagate"
                        + " through the auto-injected cast_exception try/catch; got: "
                        + res);
        var combined = res.toString();
        assertTrue(combined.contains("inner panic"), () -> "expected panic message to surface; got: " + combined);
    }
}
