package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0015DefineFuncBodyTest {
    private static SourceExpr lib(EcoMb e) {
        return e.lower(e.seq(
                e.let("d", e.define()),
                e.let("obj", e.defObject(e.v("d"), "poker")),
                e.callM(e.v("obj"), "set_inner", e.rec(e.p("out", e.strListType()))),
                e.methodImpl(
                        e.v("obj"),
                        "poke",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesNull()),
                        e.stage(e.seq(e.let("a", e.s("x")), e.push(e.fld(e.v("inner"), "out"), e.s("poked"))))),
                e.methodImpl(
                        e.v("obj"),
                        "poke2",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesNull()),
                        e.stage(e.push(e.fld(e.v("inner"), "out"), e.s("poked2")))),
                e.methodImpl(
                        e.v("obj"),
                        "poke4",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesNull()),
                        e.stage(e.seq(
                                e.let("a", e.s("x")), e.push(e.fld(e.v("inner"), "out"), e.s("poked4")), e.nil()))),
                e.methodImpl(
                        e.v("obj"),
                        "poke5",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesNull()),
                        e.stage(e.seq(
                                e.push(e.fld(e.v("inner"), "out"), e.s("poked5a")),
                                e.push(e.fld(e.v("inner"), "out"), e.s("poked5b"))))),
                e.methodImpl(
                        e.v("obj"),
                        "poke_ret_str",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.seq(e.push(e.fld(e.v("inner"), "out"), e.s("poked_rs")), e.ret(e.s("x"))))),
                e.methodImpl(
                        e.v("obj"),
                        "poke_arg_str",
                        e.mb.typesString(),
                        e.retSome(e.mb.typesNull()),
                        e.stage(e.push(e.fld(e.v("inner"), "out"), e.v("arg")))),
                e.methodImpl(
                        e.v("obj"),
                        "show",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.join(e.fld(e.v("inner"), "out"), ","))),
                e.let("mk", e.defFunc(e.v("d"), "mk")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesNull(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.stage(e.call(
                                e.lower(e.fld(e.v("obj"), "new")),
                                e.rec(e.p("out", e.listNew(e.mb.typesString(), e.s("start"))))))),
                e.finish(e.v("d")),
                e.ret(e.fld(e.v("mk"), "func"))));
    }

    @Test
    public void test00000StatementSeqBodiesExecute(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        T0200EcosystemLlvmTest.writeModule(src.resolve("lib.at"), lib(new EcoMb()));
        var e = new EcoMb();
        T0200EcosystemLlvmTest.writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("mk", e.importLocal("lib", "lib.at")),
                        e.let("o", e.call(e.v("mk"), e.nil())),
                        e.callM(e.v("o"), "poke", e.nil()),
                        e.callM(e.v("o"), "poke2", e.nil()),
                        e.callM(e.v("o"), "poke2", e.tup()),
                        e.callM(e.v("o"), "poke4", e.nil()),
                        e.callM(e.v("o"), "poke5", e.nil()),
                        e.callM(e.v("o"), "poke_ret_str", e.nil()),
                        e.callM(e.v("o"), "poke_arg_str", e.s("poked_as")),
                        e.call(e.mb.log(), e.callM(e.v("o"), "show", e.nil()))));
        var ok = T0200EcosystemLlvmTest.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(
                "start,poked,poked2,poked2,poked4,poked5a,poked5b,poked_rs,poked_as",
                ok.logs().get(0).message(),
                "statement-seq bodies must run");
    }

    @Test
    public void test00100BindNullResult(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        T0200EcosystemLlvmTest.writeModule(src.resolve("lib.at"), lib(new EcoMb()));
        var e = new EcoMb();
        T0200EcosystemLlvmTest.writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("mk", e.importLocal("lib", "lib.at")),
                        e.let("o", e.call(e.v("mk"), e.nil())),
                        e.let("ignore", e.callM(e.v("o"), "poke", e.nil())),
                        e.call(e.mb.log(), e.callM(e.v("o"), "show", e.nil()))));
        var ok = T0200EcosystemLlvmTest.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals("start,poked", ok.logs().get(0).message(), "null result binds");
    }
}
