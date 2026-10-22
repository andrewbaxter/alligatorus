package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0143ExpansionLocTest {
    @Test
    public void test00000ExpandedErrorNestsUnderCallSite(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var le = new TestMbShorthand();
        var missing = le.v("missing");
        Helpers.writeModule(
                src.resolve("lib.at"),
                TList.of(le.ret(le.lower(le.seq(
                        le.let("d", le.define()),
                        le.let("bad", le.callM(le.v("d"), "const_func", le.s("bad"))),
                        le.callM(
                                le.v("bad"),
                                "impl",
                                le.rec(
                                        le.p(
                                                "impl_body",
                                                le.stage(
                                                        le.call(
                                                                le.mb.evalConstEval(),
                                                                le.rec(
                                                                        le.p("xx", le.fld(le.v("arg"), "xx")),
                                                                        le.p("loc", le.fld(le.v("arg"), "loc")),
                                                                        le.p("staged", le.stage(missing)))))),
                                        le.p("exception_type", le.mb.optNoneType()),
                                        le.p("description", le.optNone(le.mb.typesString())))),
                        le.finish(le.v("d")),
                        le.ret(le.rec(le.p("bad", le.fld(le.v("bad"), "func")))))))));
        var e = new TestMbShorthand();
        var call = e.call(e.fld(e.v("m"), "bad"), e.s("x"));
        Helpers.writeModule(src.resolve("main.at"), TList.of(e.let("m", e.importLocal("lib", "lib.at")), call));
        TList<ExprErr> errs;
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache),
                    new SourceSpecAbsolute.Local(src.resolve("main.at").toString()));
            errs = switch (res) {
                case Result.Ok<Compile.CompileResult, TList<ExprErr>> o ->
                    throw new AssertionError("expected the expansion to fail; got: " + res);
                case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> err.error();
            };
        }
        assertEquals(1, errs.size(), () -> "expected one error; got: " + errs);
        var loc = errs.get(0).loc();
        assertEquals(missing.id().value(), loc.expr().optUnwrap(), () -> "innermost frame: " + loc);
        assertTrue(loc.site().optIsSome(), () -> "expanded error must carry its call site: " + loc);
        var site = loc.site().optUnwrap();
        assertEquals(call.id().value(), site.expr().optUnwrap(), () -> "call site frame: " + loc);
        assertNotEquals(loc.source(), site.source());
        assertTrue(site.site().optIsNone(), () -> "the call site is a root: " + loc);
    }
}
