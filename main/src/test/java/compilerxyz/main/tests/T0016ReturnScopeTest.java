package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.syntax.SourceExpr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0016ReturnScopeTest {
    private static SourceExpr lib(EcoMb e, SourceExpr thenValue, SourceExpr tailValue) {
        return libTail(e, thenValue, e.ret(tailValue));
    }

    private static SourceExpr libTail(EcoMb e, SourceExpr thenValue, SourceExpr tail) {
        return e.lower(e.seq(
                e.let("d", e.define()),
                e.let("fb", e.defFunc(e.v("d"), "f")),
                e.funcImpl(
                        e.v("fb"),
                        e.mb.typesInt(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.seq(
                                e.ifOnly(
                                        e.mb.operatorLt(e.v("arg"), e.mb.as(e.n(5.), e.mb.typesInt())),
                                        e.ret(thenValue)),
                                tail))),
                e.finish(e.v("d")),
                e.ret(e.fld(e.v("fb"), "func"))));
    }

    private static String run(SourceExpr lib, Path tmp) throws IOException {
        var e = new EcoMb();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        T0200EcosystemLlvmTest.writeModule(src.resolve("lib.at"), lib);
        T0200EcosystemLlvmTest.writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("f", e.importLocal("lib", "lib.at")),
                        e.call(e.mb.log(), e.call(e.v("f"), e.mb.as(e.n(3.), e.mb.typesInt())))));
        var ok = T0200EcosystemLlvmTest.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        return ok.logs().get(0).message();
    }

    @Test
    public void test00000UnlabeledReturnSkipsIfLevel(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        assertEquals("lo", run(lib(e, e.cat(e.s("lo")), e.s("hi")), tmp), "return_ in if exits the enclosing block");
    }

    @Test
    public void test00100UnlabeledReturnSkipsIfLevelDynamic(@TempDir Path tmp) throws IOException {
        var e = new EcoMb();
        assertEquals("lo", run(lib(e, e.cat(e.s("lo")), e.cat(e.s("hi"))), tmp), "dynamic return value through if");
    }

    @Test
    public void test00200ValuedReturnWithValuelessFallthroughIsError(@TempDir Path tmp) {
        var e = new EcoMb();
        var err = assertThrows(
                AssertionError.class,
                () -> run(libTail(e, e.cat(e.s("lo")), e.cat(e.s("hi"))), tmp),
                "a dynamic string return plus a valueless fallthrough must fail");
        assertTrue(
                err.getMessage()
                        .contains("return sites yield a value but the end of the block is reachable without one"),
                err.getMessage());
    }
}
