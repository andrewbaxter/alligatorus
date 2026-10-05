package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0048ExecTest {
    private static Result<Compile.CompileResult, TList<ExprErr>> compile(Path cacheDir, Path entry) {
        try (var cache = new CacheReal(cacheDir)) {
            return Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache), new SourceSpecAbsolute.Local(entry.toString()));
        }
    }

    private static Compile.CompileResult expectOk(Result<Compile.CompileResult, TList<ExprErr>> res) {
        return switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + err.error());
        };
    }

    private static TList<ExprErr> expectErr(Result<Compile.CompileResult, TList<ExprErr>> res) {
        return switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok ->
                throw new AssertionError(
                        "expected failure; got logs " + ok.value().logs());
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> err.error();
        };
    }

    static SourceExpr bytesOf(EcoMb e, String text) {
        return e.call(e.mb.evalStrUtf8(), e.s(text));
    }

    static SourceExpr exec(EcoMb e, String script, EcoMb.P[] env, EcoMb.P[] files) {
        return exec(e, script, env, files, e.optNone(e.listType(e.mb.typesInt())));
    }

    static SourceExpr exec(EcoMb e, String script, EcoMb.P[] env, EcoMb.P[] files, SourceExpr successCodes) {
        return e.call(
                e.mb.builtin("exec"),
                e.rec(
                        e.p("command", e.s("sh")),
                        e.p("args", e.strList(e.s("-c"), e.s(script))),
                        e.p("env", e.mapNew(e.mb.typesString(), e.mb.typesString(), env)),
                        e.p("files", e.mapNew(e.mb.typesString(), e.call(e.mb.typesArray(), e.mb.typesByte()), files)),
                        e.p("success_codes", successCodes)));
    }

    private static SourceExpr log(EcoMb e, SourceExpr value) {
        return e.call(e.mb.log(), value);
    }

    @Test
    public void test00000RunWithFilesAndEnv(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var main = tmp.resolve("main.at");
        T0047FileTest.writeModule(
                main,
                TList.of(
                        e.let(
                                "r",
                                exec(
                                        e,
                                        "cat in/a.txt; printf '%s' \"$GREETING\"; printf 'world\\n' > out/b.txt; exit 3",
                                        new EcoMb.P[] {e.p("GREETING", e.s("hi"))},
                                        new EcoMb.P[] {
                                            e.p("in/a.txt", bytesOf(e, "hello ")), e.p("out/.keep", bytesOf(e, ""))
                                        },
                                        e.optSome(
                                                e.listType(e.mb.typesInt()), e.listNew(e.mb.typesInt(), e.intOf(3))))),
                        log(e, e.callM0(e.v("r"), "stdout")),
                        log(e, e.toStr(e.callM0(e.v("r"), "exit_code"))),
                        e.let("d", e.callM0(e.v("r"), "dir")),
                        e.let("f", e.callM(e.v("d"), "open", e.s("out/b.txt"))),
                        log(e, e.toStr(e.fld(e.v("f"), "len"))),
                        log(e, e.ifElse(e.callM(e.v("d"), "exists", e.s("nope")), e.dstr("yes"), e.dstr("no"))),
                        log(e, e.join(e.callM(e.v("d"), "list", e.s("")), ","))));
        var logs = expectOk(compile(tmp.resolve("cache"), main)).logs();
        assertEquals(5, logs.size(), () -> "logs: " + logs);
        assertEquals("hello hi", logs.get(0).message());
        assertEquals("3", logs.get(1).message());
        assertEquals("6", logs.get(2).message());
        assertEquals("no", logs.get(3).message());
        assertEquals("in,out", logs.get(4).message());
    }

    @Test
    public void test00050NonzeroExitIsAnError(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var main = tmp.resolve("main.at");
        var call = exec(e, "echo oops >&2; exit 3", new EcoMb.P[] {}, new EcoMb.P[] {});
        T0047FileTest.writeModule(main, TList.of(log(e, e.callM0(call, "stdout"))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        assertTrue(
                errs.get(0).message().contains("exec: sh exited with code 3:\noops"),
                () -> "unexpected message: " + errs.get(0).message());
    }

    @Test
    public void test00100FileEscapesSandbox(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var main = tmp.resolve("main.at");
        var call = exec(e, "true", new EcoMb.P[] {}, new EcoMb.P[] {e.p("../x", bytesOf(e, "x"))});
        T0047FileTest.writeModule(main, TList.of(log(e, e.callM0(call, "stdout"))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        assertEquals(call.id().value(), errs.get(0).loc().expr().optUnwrap());
        assertTrue(
                errs.get(0).message().contains("exec: path escapes the sandbox directory: ../x"),
                () -> "unexpected message: " + errs.get(0).message());
    }

    @Test
    public void test00200MissingOutput(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        var main = tmp.resolve("main.at");
        var open = e.callM(e.callM0(exec(e, "true", new EcoMb.P[] {}, new EcoMb.P[] {}), "dir"), "open", e.s("gone"));
        T0047FileTest.writeModule(main, TList.of(log(e, e.toStr(e.fld(open, "len")))));
        var errs = expectErr(compile(tmp.resolve("cache"), main));
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        assertEquals(open.id().value(), errs.get(0).loc().expr().optUnwrap());
        assertTrue(
                errs.get(0).message().contains("dir.open: file not found: gone"),
                () -> "unexpected message: " + errs.get(0).message());
    }
}
