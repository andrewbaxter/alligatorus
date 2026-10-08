package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0018MergeCodegenTest {
    private static SourceExpr lib(TestMbShorthand e, SourceExpr retType, SourceExpr body) {
        return e.lower(e.seq(
                e.let("ta", e.mb.typesString()),
                e.let("tb", e.mb.typesInt()),
                e.let("d", e.define()),
                e.let("fb", e.defFunc(e.v("d"), "f")),
                e.funcImpl(e.v("fb"), e.mb.typesInt(), e.retSome(retType), e.mb.optNoneType(), e.stage(body)),
                e.finish(e.v("d")),
                e.ret(e.fld(e.v("fb"), "func"))));
    }

    private static String run(SourceExpr lib, SourceExpr use, Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        Helpers.writeModule(src.resolve("lib.at"), TList.of(e.ret(lib)));
        Helpers.writeModule(
                src.resolve("main.at"),
                TList.of(
                        e.let("f", e.importLocal("lib", "lib.at")),
                        e.let("r", e.call(e.v("f"), e.mb.as(e.n(3.), e.mb.typesInt()))),
                        e.call(e.mb.log(), use)));
        var ok = Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        return ok.logs().get(0).message();
    }

    private static SourceExpr lt5(TestMbShorthand e) {
        return e.mb.operatorLt(e.v("arg"), e.mb.as(e.n(5.), e.mb.typesInt()));
    }

    private static String failure(SourceExpr lib, SourceExpr use, Path tmp) {
        var err = assertThrows(AssertionError.class, () -> run(lib, use, tmp));
        return String.valueOf(err.getMessage());
    }

    @Test
    public void test00000ConstTypesThroughIfIsError(@TempDir Path tmp) {
        var e = new TestMbShorthand();
        var lib = lib(
                e,
                e.mb.evalType(),
                e.seq(e.let("t", e.ifElse(lt5(e), e.lowerV("ta"), e.lowerV("tb"))), e.ret(e.v("t"))));
        var message = failure(lib, e.s("ok"), tmp);
        assertTrue(message.contains("Incompatible types"), message);
    }

    @Test
    public void test00010SameConstThroughIfIsError(@TempDir Path tmp) {
        var e = new TestMbShorthand();
        var lib = lib(
                e,
                e.mb.evalType(),
                e.seq(e.let("t", e.ifElse(lt5(e), e.lowerV("ta"), e.lowerV("ta"))), e.ret(e.v("t"))));
        var message = failure(lib, e.s("ok"), tmp);
        assertTrue(message.contains("Incompatible types"), message);
    }

    @Test
    public void test00020ConstDoublesThroughIfIsError(@TempDir Path tmp) {
        var e = new TestMbShorthand();
        var lib = lib(e, e.typesDouble(), e.ret(e.ifElse(lt5(e), e.n(1.5), e.n(2.5))));
        var message = failure(lib, e.s("ok"), tmp);
        assertTrue(message.contains("Incompatible types"), message);
    }

    @Test
    public void test00030ConstDoublesAtReturnSitesIsError(@TempDir Path tmp) {
        var e = new TestMbShorthand();
        var lib = lib(e, e.typesDouble(), e.seq(e.ifOnly(lt5(e), e.ret(e.n(1.5))), e.ret(e.n(2.5))));
        var message = failure(lib, e.s("ok"), tmp);
        assertTrue(message.contains("Incompatible types"), message);
    }

    private static SourceExpr partialRecord(TestMbShorthand e, SourceExpr a) {
        return e.rec(e.p("a", a), e.p("b", e.cat(a, e.s("!"))));
    }

    @Test
    public void test00100RecordFromForeachAndTail(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var lib = lib(
                e,
                e.rec(e.p("a", e.mb.typesString()), e.p("b", e.mb.typesString())),
                e.seq(
                        e.foreach(
                                e.listNew(e.mb.typesString(), e.s("x"), e.s("y")),
                                "s",
                                e.ifOnly(lt5(e), e.ret(partialRecord(e, e.v("s"))))),
                        e.ret(partialRecord(e, e.dstr("none")))));
        assertEquals("x", run(lib, e.fld(e.v("r"), "a"), tmp));
    }

    @Test
    public void test00110RecordFromSingleSite(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var lib = lib(
                e,
                e.rec(e.p("a", e.mb.typesString()), e.p("b", e.mb.typesString())),
                e.seq(e.let("s", e.ifElse(lt5(e), e.dstr("lo"), e.dstr("hi"))), e.ret(partialRecord(e, e.v("s")))));
        assertEquals("lo!", run(lib, e.fld(e.v("r"), "b"), tmp));
    }

    @Test
    public void test00120RecordsWithDifferingHeldConstsIsError(@TempDir Path tmp) {
        var e = new TestMbShorthand();
        var lib = lib(
                e,
                e.rec(e.p("k", e.typesDouble()), e.p("v", e.mb.typesString())),
                e.seq(
                        e.ifOnly(lt5(e), e.ret(e.rec(e.p("k", e.n(1.5)), e.p("v", e.dstr("lo"))))),
                        e.ret(e.rec(e.p("k", e.n(2.5)), e.p("v", e.dstr("hi"))))));
        var message = failure(lib, e.fld(e.v("r"), "v"), tmp);
        assertTrue(message.contains("compile failed"), message);
    }
}
