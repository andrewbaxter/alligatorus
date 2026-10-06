package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0040ListsTest {
    @Test
    public void test00000List() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "my_list",
                                mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesString()), mb.str("new")),
                                        mb.tupleOf(mb.str("c"), mb.str("d")))),
                        mb.call(mb.field(mb.scopeUse("my_list"), mb.str("push")), mb.str("a")),
                        mb.call(mb.field(mb.scopeUse("my_list"), mb.str("push")), mb.str("b")),
                        mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("my_list"), mb.str("join")), mb.str("_"))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> resOk = ok;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + res);
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals("c_d_a_b", ok.logs().get(0).message());
    }

    @Test
    public void test00100CastToList() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind(
                "from_tuple",
                mb.as(mb.tupleOf(mb.num(1), mb.num(2), mb.num(3)), mb.call(mb.typesList(), mb.typesInt()))));
        exprs.add(mb.bind(
                "from_tuple_boxed",
                mb.as(mb.tupleOf(mb.str("a"), mb.str("b")), mb.call(mb.typesList(), mb.typesString()))));
        exprs.add(mb.bind(
                "from_array",
                mb.as(mb.call(mb.evalStrUtf8(), mb.str("h\u00e9")), mb.call(mb.typesList(), mb.typesUbyte()))));
        exprs.add(mb.bind("from_string", mb.as(mb.str("h\u00e9"), mb.call(mb.typesList(), mb.typesUbyte()))));
        exprs.add(mb.bind("from_string_signed", mb.as(mb.str("h\u00e9"), mb.call(mb.typesList(), mb.typesByte()))));
        exprs.add(mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("from_tuple_boxed"), mb.str("join")), mb.str("_"))));
        for (var name : new String[] {"from_tuple", "from_array", "from_string", "from_string_signed"}) {
            exprs.add(mb.foreach(
                    mb.scopeUse(name),
                    mb.str("e"),
                    mb.seqOf(mb.call(
                            mb.log(), mb.call(mb.field(mb.scopeUse("e"), mb.str("to_string")), mb.tupleOf())))));
        }
        modules.put("main", new SourceModule.V1(exprs));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> resOk = ok;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + res);
        }
        var messages = new TList<String>();
        for (var entry : resOk.value().logs()) {
            messages.add(entry.message());
        }
        assertEquals(
                TList.of("a_b", "1", "2", "3", "104", "195", "169", "104", "195", "169", "104", "-61", "-87"),
                messages);
    }
}
