package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0015DefineFuncBodyTest {

    @Test
    public void test00000StatementSeqBodiesExecute(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var le = new EcoMb();
        T0200EcosystemLlvmTest.writeModule(
                src.resolve("lib.at"),
                le.lower(le.seq(
                        le.let("d", le.define()),
                        le.let("obj", le.defObject(le.v("d"), "poker")),
                        le.callM(le.v("obj"), "set_inner", le.rec(le.p("out", le.strListType()))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke",
                                le.mb.tupleOf(),
                                le.mb.optNoneType(),
                                le.stage(le.seq(
                                        le.let("a", le.s("x")), le.push(le.fld(le.v("inner"), "out"), le.s("poked"))))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke2",
                                le.mb.tupleOf(),
                                le.mb.optNoneType(),
                                le.stage(le.push(le.fld(le.v("inner"), "out"), le.s("poked2")))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke4",
                                le.mb.tupleOf(),
                                le.mb.optNoneType(),
                                le.stage(le.seq(
                                        le.let("a", le.s("x")),
                                        le.push(le.fld(le.v("inner"), "out"), le.s("poked4"))))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke5",
                                le.mb.tupleOf(),
                                le.mb.optNoneType(),
                                le.stage(le.seq(
                                        le.push(le.fld(le.v("inner"), "out"), le.s("poked5a")),
                                        le.push(le.fld(le.v("inner"), "out"), le.s("poked5b"))))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke_ret_str",
                                le.mb.tupleOf(),
                                le.retSome(le.mb.typesString()),
                                le.stage(le.seq(
                                        le.push(le.fld(le.v("inner"), "out"), le.s("poked_rs")), le.ret(le.s("x"))))),
                        le.methodImpl(
                                le.v("obj"),
                                "poke_arg_str",
                                le.mb.typesString(),
                                le.mb.optNoneType(),
                                le.stage(le.push(le.fld(le.v("inner"), "out"), le.v("arg")))),
                        le.methodImpl(
                                le.v("obj"),
                                "show",
                                le.mb.tupleOf(),
                                le.retSome(le.mb.typesString()),
                                le.stage(le.join(le.fld(le.v("inner"), "out"), ","))),
                        le.let("mk", le.defFunc(le.v("d"), "mk")),
                        le.funcImpl(
                                le.v("mk"),
                                le.mb.tupleOf(),
                                le.retSome(le.fld(le.v("obj"), "type")),
                                le.stage(le.call(
                                        le.lower(le.fld(le.v("obj"), "new")),
                                        le.rec(le.p("out", le.listNew(le.mb.typesString(), le.s("start"))))))),
                        le.finish(le.v("d")),
                        le.ret(le.fld(le.v("mk"), "func")))));
        var e = new EcoMb();
        T0200EcosystemLlvmTest.writeModule(
                src.resolve("main.at"),
                e.seq(
                        e.let("mk", e.importLocal("lib", "lib.at")),
                        e.let("o", e.call(e.v("mk"), e.tup())),
                        e.callM(e.v("o"), "poke", e.tup()),
                        e.callM(e.v("o"), "poke2", e.tup()),
                        e.callM(e.v("o"), "poke2", e.tup()),
                        e.callM(e.v("o"), "poke4", e.tup()),
                        e.callM(e.v("o"), "poke5", e.tup()),
                        e.callM(e.v("o"), "poke_ret_str", e.tup()),
                        e.callM(e.v("o"), "poke_arg_str", e.s("poked_as")),
                        e.call(e.mb.log(), e.callM(e.v("o"), "show", e.tup()))));
        var ok = T0200EcosystemLlvmTest.compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(
                "start,poked,poked2,poked2,poked4,poked5a,poked5b,poked_rs,poked_as",
                ok.logs().get(0).message(),
                "statement-seq bodies must run");
    }
}
