package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0080UnionTest {
    @Test
    public void test00000UnionMatch() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "strnum",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "new_str",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("strtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesString()))))),
                                        mb.bind(
                                                "new_num",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("numtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesInt()))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "type",
                                                                mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                                        new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                                        new TestMb.RecordPair("num", mb.scopeUse("new_num")))))))),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(
                                        new TestMb.Branch(
                                                mb.str("strtag"),
                                                mb.seqOf(mb.call(mb.log(), mb.scopeUse("y")), mb.tupleOf())),
                                        new TestMb.Branch(
                                                mb.str("numtag"),
                                                mb.seqOf(
                                                        mb.call(
                                                                mb.log(),
                                                                mb.call(
                                                                        mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                                                        mb.tupleOf())),
                                                        mb.tupleOf()))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(res.resultIsOk(), () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("7", ok.logs().get(0).message());
    }

    @Test
    public void test00010UnionMatchFallback() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "strnum",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "new_str",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("strtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesString()))))),
                                        mb.bind(
                                                "new_num",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("numtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesInt()))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "type",
                                                                mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                                        new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                                        new TestMb.RecordPair("num", mb.scopeUse("new_num")))))))),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y")))),
                                Opt.some(mb.call(mb.log(), mb.str("fallback")))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(res.resultIsOk(), () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("fallback", ok.logs().get(0).message());
    }

    @Test
    public void test00020UnionNonexhaustive() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "strnum",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "new_str",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("strtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesString()))))),
                                        mb.bind(
                                                "new_num",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("numtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesInt()))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "type",
                                                                mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                                        new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                                        new TestMb.RecordPair("num", mb.scopeUse("new_num")))))))),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(new TestMb.Branch(
                                        mb.str("numtag"),
                                        mb.call(
                                                mb.log(),
                                                mb.call(
                                                        mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                                        mb.tupleOf())))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(res.resultIsErr(), () -> "expected compile error but got: " + res);
    }

    @Test
    public void test00030UnionExhaustiveWithFallback() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "strnum",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "builder",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                                        mb.bind(
                                                "new_str",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("strtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesString()))))),
                                        mb.bind(
                                                "new_num",
                                                mb.call(
                                                        mb.field(mb.scopeUse("builder"), mb.str("variant")),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        new TestMb.RecordPair("tag", mb.str("numtag")),
                                                                        new TestMb.RecordPair(
                                                                                "type_ref", mb.typesInt()))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.recordOf(
                                                TList.of(
                                                        new TestMb.RecordPair(
                                                                "type",
                                                                mb.field(mb.scopeUse("builder"), mb.str("type"))),
                                                        new TestMb.RecordPair("str", mb.scopeUse("new_str")),
                                                        new TestMb.RecordPair("num", mb.scopeUse("new_num")))))))),
                        mb.bind(
                                "x",
                                mb.as(
                                        mb.call(mb.field(mb.scopeUse("strnum"), mb.str("num")), mb.num(7.)),
                                        mb.field(mb.scopeUse("strnum"), mb.str("type")))),
                        mb.matchOf(
                                mb.scopeUse("x"),
                                mb.str("y"),
                                TList.of(
                                        new TestMb.Branch(mb.str("strtag"), mb.call(mb.log(), mb.scopeUse("y"))),
                                        new TestMb.Branch(
                                                mb.str("numtag"),
                                                mb.call(
                                                        mb.log(),
                                                        mb.call(
                                                                mb.field(mb.scopeUse("y"), mb.str("to_string")),
                                                                mb.tupleOf())))),
                                Opt.some(mb.call(mb.log(), mb.str("fallback")))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(res.resultIsErr(), () -> "expected compile error but got: " + res);
    }
}
