package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0036ExprBuildTest {

    private static SourceExpr evalIn(TestMbShorthand e, SourceExpr staged) {
        return e.call(
                e.evalBuiltin("const_eval"),
                e.rec(e.p("xx", e.fld(e.v("arg"), "xx")), e.p("loc", e.fld(e.v("arg"), "loc")), e.p("staged", staged)));
    }

    private static SourceExpr built(TestMbShorthand e, String kind, TestMbShorthand.P... args) {
        var all = new TestMbShorthand.P[args.length + 1];
        all[0] = e.p("loc", e.fld(e.v("arg"), "loc"));
        System.arraycopy(args, 0, all, 1, args.length);
        return e.call(e.evalBuiltin("expr_" + kind), e.rec(all));
    }

    @Test
    public void test00000BuiltExpressions(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var le = new TestMbShorthand();
        var recordBody = le.seq(
                le.let("entries", le.listNew(le.mb.builtin("types", "expr_record_entry"))),
                le.foreach(
                        le.strList(le.s("alpha"), le.s("beta")),
                        "k",
                        le.push(
                                le.v("entries"),
                                le.call(
                                        le.evalBuiltin("expr_record_entry"),
                                        le.rec(
                                                le.p("key", le.stage(le.lower(le.v("k")))),
                                                le.p("value", le.stage(le.lower(le.v("k")))))))),
                le.ret(evalIn(le, built(le, "record", le.p("entries", le.v("entries"))))));
        var tupleBody = le.seq(le.ret(evalIn(
                le,
                built(
                        le,
                        "tuple",
                        le.p("elements", le.listNew(le.mb.typesExpr(), le.stage(le.s("t0")), le.stage(le.s("t1"))))))));
        var seqBody = le.seq(le.ret(evalIn(
                le,
                built(
                        le,
                        "seq",
                        le.p(
                                "exprs",
                                le.listNew(
                                        le.mb.typesExpr(),
                                        le.stage(le.call(le.mb.log(), le.s("in seq"))),
                                        le.stage(le.ret(le.s("last")))))))));
        var defs = new TList<SourceExpr>();
        defs.add(le.let("d", le.define()));
        var names = TList.of("record", "tuple", "seq");
        var bodies = TList.of(recordBody, tupleBody, seqBody);
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
        var exports = new TestMbShorthand.P[names.size()];
        for (int i = 0; i < names.size(); i++) exports[i] = le.p(names.get(i), le.fld(le.v(names.get(i)), "func"));
        defs.add(le.ret(le.rec(exports)));
        var lib = new SourceExpr[defs.size()];
        for (int i = 0; i < defs.size(); i++) lib[i] = defs.get(i);
        Helpers.writeModule(src.resolve("lib.at"), TList.of(le.ret(le.lower(le.seq(lib)))));
        var e = new TestMbShorthand();
        Helpers.writeModule(
                src.resolve("main.at"),
                TList.of(
                        e.let("m", e.importLocal("lib", "lib.at")),
                        e.let("r", e.call(e.fld(e.v("m"), "record"), e.tup())),
                        e.call(e.mb.log(), e.fld(e.v("r"), "alpha")),
                        e.call(e.mb.log(), e.fld(e.v("r"), "beta")),
                        e.let("t", e.call(e.fld(e.v("m"), "tuple"), e.tup())),
                        e.call(e.mb.log(), e.mb.field(e.v("t"), e.mb.num(1))),
                        e.call(e.mb.log(), e.call(e.fld(e.v("m"), "seq"), e.tup()))));
        var ok = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var messages = new TList<String>();
        for (var entry : ok.logs()) {
            messages.add(entry.message());
        }
        assertEquals(
                TList.of("alpha", "beta", "t1", "in seq", "last"),
                messages,
                "expressions built from data must evaluate like written ones");
    }
}
