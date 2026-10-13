package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0037ConstHostTest {

    private static SourceExpr evalIn(TestMbShorthand e, SourceExpr staged) {
        return e.call(e.evalBuiltin("const_eval"), e.rec(e.p("xx", e.fld(e.v("arg"), "xx")), e.p("staged", staged)));
    }

    private static SourceExpr argumentAs(TestMbShorthand e, SourceExpr type) {
        var hostValue = e.fld(e.call(e.fld(e.fld(e.v("arg"), "argument"), "value"), e.tup()), "value");
        var unwrapped = e.fld(
                e.call(e.mb.mustReinterpret(), e.tup(hostValue, e.mb.builtin("types", "value_object_const"))), "data");
        return e.call(e.mb.mustReinterpret(), e.tup(unwrapped, type));
    }

    private static TList<SourceExpr> mainModule(TestMbShorthand e, SourceExpr... extra) {
        var out = new TList<SourceExpr>();
        out.add(e.let("m", e.importLocal("lib", "lib.at")));
        out.add(e.call(e.mb.log(), e.call(e.fld(e.v("m"), "keys"), e.fld(e.v("m"), "m"))));
        out.add(e.call(e.mb.log(), e.call(e.fld(e.v("m"), "total"), e.fld(e.v("m"), "ints"))));
        out.add(e.foreach(e.fld(e.v("m"), "strs"), "s", e.call(e.mb.log(), e.v("s"))));
        out.add(e.foreach(e.fld(e.v("m"), "ints"), "i", e.call(e.mb.log(), e.toStr(e.v("i")))));
        for (var name : new String[] {"o", "no"}) {
            out.add(e.matchOf(
                    e.fld(e.v("m"), name),
                    "v",
                    e.p("some", e.call(e.mb.log(), e.v("v"))),
                    e.p("none", e.call(e.mb.log(), e.s("none")))));
        }
        out.add(e.matchOf(
                e.fld(e.v("m"), "r"),
                "v",
                e.p("ok", e.call(e.mb.log(), e.v("v"))),
                e.p("errs", e.call(e.mb.log(), e.s("errs")))));
        out.add(e.call(e.mb.log(), e.toStr(e.fld(e.fld(e.v("m"), "bytes"), "len"))));
        for (var x : extra) out.add(x);
        return out;
    }

    @Test
    public void test00000ConstCollectionsRoundTrip(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var le = new TestMbShorthand();
        var keysBody = le.seq(
                le.let("mv", argumentAs(le, le.mapType(le.mb.typesString(), le.evalBuiltin("type")))),
                le.let("ks", le.listNew(le.mb.typesString())),
                le.foreach(le.v("mv"), "en", le.push(le.v("ks"), le.fld(le.v("en"), "key"))),
                le.ret(evalIn(le, le.stage(le.lower(le.join(le.v("ks"), ","))))));
        var totalBody = le.seq(
                le.let("lv", argumentAs(le, le.listType(le.mb.typesInt()))),
                le.ret(evalIn(le, le.stage(le.lower(le.toStr(le.fld(le.v("lv"), "len")))))));
        var defs = new TList<SourceExpr>();
        defs.add(le.let("d", le.define()));
        var names = TList.of("keys", "total");
        var bodies = TList.of(keysBody, totalBody);
        for (int i = 0; i < names.size(); i++) {
            defs.add(le.let(names.get(i), le.callM(le.v("d"), "const_func", le.s(names.get(i)))));
            defs.add(le.callM(
                    le.v(names.get(i)),
                    "impl",
                    le.rec(
                            le.p("impl_body", le.stage(bodies.get(i))),
                            le.p("exception_type", le.mb.optNoneType()),
                            le.p("description", le.optNone(le.mb.typesString())))));
        }
        defs.add(le.finish(le.v("d")));
        defs.add(le.ret(le.rec(
                le.p("keys", le.fld(le.v("keys"), "func")),
                le.p("total", le.fld(le.v("total"), "func")),
                le.p("ints", le.listNew(le.mb.typesInt(), le.n(1), le.n(2), le.n(3))),
                le.p("strs", le.strList(le.s("a"), le.s("b"))),
                le.p(
                        "m",
                        le.mapNew(
                                le.mb.typesString(),
                                le.evalBuiltin("type"),
                                le.p("x", le.mb.typesInt()),
                                le.p("y", le.mb.typesString()))),
                le.p("o", le.optSome(le.mb.typesString(), le.s("some-x"))),
                le.p("no", le.optNone(le.mb.typesString())),
                le.p("r", le.eresultOk(le.mb.typesString(), le.s("fine"))),
                le.p(
                        "bytes",
                        le.mb.as(
                                le.call(le.mb.evalStrUtf8(), le.s("hé")),
                                le.call(le.mb.typesArray(), le.mb.typesByte()))))));
        var lib = new SourceExpr[defs.size()];
        for (int i = 0; i < defs.size(); i++) lib[i] = defs.get(i);
        Helpers.writeModule(src.resolve("lib.at"), TList.of(le.ret(le.lower(le.seq(lib)))));
        var expected = TList.of("x,y", "3", "a", "b", "1", "2", "3", "some-x", "none", "fine", "3");

        var e = new TestMbShorthand();
        Helpers.writeModule(src.resolve("main.at"), mainModule(e));
        var first = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var firstLogs = new TList<String>();
        for (var entry : first.logs()) firstLogs.add(entry.message());
        assertEquals(expected, firstLogs, "lowered collections must round trip through export and import");

        var e2 = new TestMbShorthand();
        Helpers.writeModule(src.resolve("main2.at"), mainModule(e2, e2.call(e2.mb.log(), e2.s("again"))));
        var second = Helpers.compileOk(src.resolve("main2.at"), tmp.resolve("cache"));
        var secondLogs = new TList<String>();
        for (var entry : second.logs()) secondLogs.add(entry.message());
        var expectedAgain = new TList<String>();
        expectedAgain.addAll(expected);
        expectedAgain.add("again");
        assertEquals(expectedAgain, secondLogs, "the cached library must import its collections from artifacts");
    }
}
