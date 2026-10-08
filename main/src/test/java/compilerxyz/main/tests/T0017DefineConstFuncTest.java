package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0017DefineConstFuncTest {

    private static SourceExpr constFuncImpl(TestMbShorthand e, SourceExpr builder, SourceExpr body) {
        return e.callM(
                builder,
                "impl",
                e.rec(
                        e.p("impl_body", e.stage(body)),
                        e.p("exception_type", e.mb.optNoneType()),
                        e.p("description", e.optNone(e.mb.typesString()))));
    }

    @Test
    public void test00000ConstFuncCallAndImport(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var le = new TestMbShorthand();
        Helpers.writeModule(
                src.resolve("lib.at"),
                TList.of(le.ret(le.lower(le.seq(
                        le.let("d", le.define()),
                        le.let("ident", le.callM(le.v("d"), "const_func", le.s("ident"))),
                        constFuncImpl(
                                le, le.v("ident"), le.eresultOk(le.mb.typesCve(), le.fld(le.v("arg"), "argument"))),
                        le.let("via", le.callM(le.v("d"), "const_func", le.s("via"))),
                        constFuncImpl(
                                le,
                                le.v("via"),
                                le.call(
                                        le.lower(le.fld(le.v("ident"), "func_impl")),
                                        le.rec(
                                                le.p("xx", le.fld(le.v("arg"), "xx")),
                                                le.p("loc", le.fld(le.v("arg"), "loc")),
                                                le.p("argument", le.fld(le.v("arg"), "argument"))))),
                        le.finish(le.v("d")),
                        le.ret(le.rec(
                                le.p("ident", le.fld(le.v("ident"), "func")),
                                le.p("via", le.fld(le.v("via"), "func")))))))));
        var e = new TestMbShorthand();
        Helpers.writeModule(
                src.resolve("main.at"),
                TList.of(
                        e.let("m", e.importLocal("lib", "lib.at")),
                        e.call(e.mb.log(), e.call(e.fld(e.v("m"), "ident"), e.s("direct"))),
                        e.call(e.mb.log(), e.call(e.fld(e.v("m"), "via"), e.s("nested")))));
        var ok = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var messages = new TList<String>();
        for (var entry : ok.logs()) {
            messages.add(entry.message());
        }
        assertEquals(TList.of("direct", "nested"), messages, "const funcs must expand at the call site");
    }
}
