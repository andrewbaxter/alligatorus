package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0170NodejsTest {
    private static TestMb.RecordPair rp(String key, SourceExpr value) {
        return new TestMb.RecordPair(key, value);
    }

    private static SourceExpr ifaceMethods(TestMb mb, TList<TestMb.RecordPair> methods) {
        var staged = new TList<TestMb.RecordPair>(methods.size());
        for (var m : methods) {
            staged.add(new TestMb.RecordPair(m.key(), mb.stage(m.value())));
        }
        return mb.call(
                mb.field(mb.call(mb.typesMap(), mb.tupleOf(mb.typesConstkey(), mb.typesExpr())), mb.str("new")),
                mb.recordOf(staged));
    }

    private static SourceExpr strListNew(TestMb mb, SourceExpr... elements) {
        return mb.call(mb.field(mb.call(mb.typesList(), mb.typesString()), mb.str("new")), mb.tupleOf(elements));
    }

    private static SourceExpr renderInnerCall(TestMb mb, SourceExpr codeExpr) {
        return mb.call(
                mb.lower(mb.scopeUse("render_inner")),
                mb.recordOf(TList.of(
                        rp("context", mb.field(mb.scopeUse("arg"), mb.str("context"))), rp("code", codeExpr))));
    }

    private static SourceExpr valuePush(TestMb mb, SourceExpr element) {
        return mb.call(mb.field(mb.scopeUse("value"), mb.str("push")), element);
    }

    private static SourceExpr buildNodejsCode(TestMb mb) {
        return mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind("code_tu_builder", mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())),
                mb.bind("code_tu", mb.field(mb.scopeUse("code_tu_builder"), mb.str("type"))),
                mb.bind("code_data", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs code"))),
                mb.call(mb.field(mb.scopeUse("code_data"), mb.str("set_inner")), mb.scopeUse("code_tu")),
                mb.bind("code", mb.field(mb.scopeUse("code_data"), mb.str("type"))),
                mb.bind(
                        "code_tu_call",
                        mb.field(
                                mb.call(
                                        mb.field(mb.scopeUse("code_tu_builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                rp("tag", mb.str("call")),
                                                rp(
                                                        "type_ref",
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp("func", mb.scopeUse("code")),
                                                                        rp("arg", mb.scopeUse("code")))))))),
                                mb.str("value"))),
                mb.bind(
                        "code_tu_lit_str",
                        mb.field(
                                mb.call(
                                        mb.field(mb.scopeUse("code_tu_builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                rp("tag", mb.str("lit_str")), rp("type_ref", mb.typesString())))),
                                mb.str("value"))),
                mb.bind(
                        "code_tu_field",
                        mb.field(
                                mb.call(
                                        mb.field(mb.scopeUse("code_tu_builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                rp("tag", mb.str("field")),
                                                rp(
                                                        "type_ref",
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp("base", mb.scopeUse("code")),
                                                                        rp("field", mb.scopeUse("code")))))))),
                                mb.str("value"))),
                mb.bind(
                        "code_tu_scope_use",
                        mb.field(
                                mb.call(
                                        mb.field(mb.scopeUse("code_tu_builder"), mb.str("variant")),
                                        mb.recordOf(TList.of(
                                                rp("tag", mb.str("scope_use")), rp("type_ref", mb.typesString())))),
                                mb.str("value"))),
                mb.call(
                        mb.field(mb.scopeUse("code_data"), mb.str("impl_interface")),
                        mb.recordOf(TList.of(
                                rp("iface_ref", mb.builtin("eval", "code_dyn")),
                                rp("record", ifaceMethods(mb, TList.of()))))),
                mb.bind("code_get_builder", mb.call(mb.field(mb.scopeUse("code_data"), mb.str("func")), mb.str("get"))),
                mb.call(
                        mb.field(mb.scopeUse("code_get_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp("arg", mb.typesNull()),
                                rp("ret", mb.optSomeType(mb.scopeUse("code_tu"))),
                                rp("impl_body", mb.stage(mb.scopeUse("inner")))))),
                mb.bind("code_call_builder", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("code.call"))),
                mb.bind("code_call", mb.field(mb.scopeUse("code_call_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("code_call_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp(
                                        "arg",
                                        mb.recordOf(TList.of(
                                                rp("func", mb.scopeUse("code")), rp("arg", mb.scopeUse("code"))))),
                                rp("ret", mb.optSomeType(mb.scopeUse("code"))),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.call(
                                                mb.lower(mb.field(mb.scopeUse("code_data"), mb.str("new"))),
                                                mb.call(
                                                        mb.lower(mb.scopeUse("code_tu_call")),
                                                        mb.scopeUse("arg")))))))),
                mb.bind(
                        "code_lit_str_builder",
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("code.lit_str"))),
                mb.bind("code_lit_str", mb.field(mb.scopeUse("code_lit_str_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("code_lit_str_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp("arg", mb.typesString()),
                                rp("ret", mb.optSomeType(mb.scopeUse("code"))),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.call(
                                                mb.lower(mb.field(mb.scopeUse("code_data"), mb.str("new"))),
                                                mb.call(
                                                        mb.lower(mb.scopeUse("code_tu_lit_str")),
                                                        mb.scopeUse("arg")))))))),
                mb.bind(
                        "code_field_builder",
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("code.field"))),
                mb.bind("code_field", mb.field(mb.scopeUse("code_field_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("code_field_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp(
                                        "arg",
                                        mb.recordOf(TList.of(
                                                rp("base", mb.scopeUse("code")), rp("field", mb.scopeUse("code"))))),
                                rp("ret", mb.optSomeType(mb.scopeUse("code"))),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.call(
                                                mb.lower(mb.field(mb.scopeUse("code_data"), mb.str("new"))),
                                                mb.call(
                                                        mb.lower(mb.scopeUse("code_tu_field")),
                                                        mb.scopeUse("arg")))))))),
                mb.bind(
                        "code_scope_use_builder",
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("code.scope_use"))),
                mb.bind("code_scope_use", mb.field(mb.scopeUse("code_scope_use_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("code_scope_use_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp("arg", mb.typesString()),
                                rp("ret", mb.optSomeType(mb.scopeUse("code"))),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.call(
                                                mb.lower(mb.field(mb.scopeUse("code_data"), mb.str("new"))),
                                                mb.call(
                                                        mb.lower(mb.scopeUse("code_tu_scope_use")),
                                                        mb.scopeUse("arg")))))))),
                mb.bind(
                        "render_inner_builder",
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("Render Node.js code (inner)"))),
                mb.bind("render_inner", mb.field(mb.scopeUse("render_inner_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("render_inner_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp(
                                        "arg",
                                        mb.recordOf(TList.of(
                                                rp(
                                                        "context",
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp("expr_count", mb.typesInt()),
                                                                        rp(
                                                                                "out",
                                                                                mb.call(
                                                                                        mb.typesList(),
                                                                                        mb.typesString()))))),
                                                rp("code", mb.scopeUse("code"))))),
                                rp("ret", mb.optSomeType(mb.typesString())),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.seqOf(
                                                mb.bind(
                                                        "key",
                                                        mb.call(
                                                                mb.field(
                                                                        strListNew(
                                                                                mb,
                                                                                mb.str("e"),
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.field(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "context")),
                                                                                                        mb.str(
                                                                                                                "expr_count")),
                                                                                                mb.str("to_string")),
                                                                                        mb.tupleOf())),
                                                                        mb.str("join")),
                                                                mb.str(""))),
                                                mb.set(
                                                        mb.field(
                                                                mb.field(mb.scopeUse("arg"), mb.str("context")),
                                                                mb.str("expr_count")),
                                                        mb.operatorAdd(
                                                                mb.field(
                                                                        mb.field(mb.scopeUse("arg"), mb.str("context")),
                                                                        mb.str("expr_count")),
                                                                mb.num(1.))),
                                                mb.bind(
                                                        "out",
                                                        mb.field(
                                                                mb.field(mb.scopeUse("arg"), mb.str("context")),
                                                                mb.str("out"))),
                                                mb.call(mb.field(mb.scopeUse("out"), mb.str("push")), mb.str("let ")),
                                                mb.call(
                                                        mb.field(mb.scopeUse("out"), mb.str("push")),
                                                        mb.scopeUse("key")),
                                                mb.call(mb.field(mb.scopeUse("out"), mb.str("push")), mb.str(";\n{\n")),
                                                mb.bind("value", strListNew(mb, mb.scopeUse("key"), mb.str(" = "))),
                                                mb.matchOf(
                                                        mb.call(
                                                                mb.field(
                                                                        mb.field(mb.scopeUse("arg"), mb.str("code")),
                                                                        mb.str("get")),
                                                                mb.tupleOf()),
                                                        mb.str("code"),
                                                        TList.of(
                                                                new TestMb.Branch(
                                                                        mb.str("call"),
                                                                        mb.seqOf(
                                                                                mb.bind(
                                                                                        "func",
                                                                                        renderInnerCall(
                                                                                                mb,
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "code"),
                                                                                                        mb.str(
                                                                                                                "func")))),
                                                                                mb.bind(
                                                                                        "rendered_arg",
                                                                                        renderInnerCall(
                                                                                                mb,
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "code"),
                                                                                                        mb.str(
                                                                                                                "arg")))),
                                                                                valuePush(mb, mb.str("(")),
                                                                                valuePush(mb, mb.scopeUse("func")),
                                                                                valuePush(mb, mb.str(")(")),
                                                                                valuePush(
                                                                                        mb,
                                                                                        mb.scopeUse("rendered_arg")),
                                                                                valuePush(mb, mb.str(")")))),
                                                                new TestMb.Branch(
                                                                        mb.str("lit_str"),
                                                                        mb.seqOf(
                                                                                valuePush(
                                                                                        mb,
                                                                                        mb.call(
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "str_to_json"),
                                                                                                mb.scopeUse("code"))))),
                                                                new TestMb.Branch(
                                                                        mb.str("field"),
                                                                        mb.seqOf(
                                                                                mb.bind(
                                                                                        "base",
                                                                                        renderInnerCall(
                                                                                                mb,
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "code"),
                                                                                                        mb.str(
                                                                                                                "base")))),
                                                                                mb.bind(
                                                                                        "field",
                                                                                        renderInnerCall(
                                                                                                mb,
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "code"),
                                                                                                        mb.str(
                                                                                                                "field")))),
                                                                                valuePush(mb, mb.scopeUse("base")),
                                                                                valuePush(mb, mb.str(".")),
                                                                                valuePush(mb, mb.scopeUse("field")))),
                                                                new TestMb.Branch(
                                                                        mb.str("scope_use"),
                                                                        mb.seqOf(valuePush(mb, mb.scopeUse("code"))))),
                                                        Opt.none()),
                                                mb.call(
                                                        mb.field(mb.scopeUse("out"), mb.str("push")),
                                                        mb.call(
                                                                mb.field(mb.scopeUse("value"), mb.str("join")),
                                                                mb.str(""))),
                                                mb.call(mb.field(mb.scopeUse("out"), mb.str("push")), mb.str(";\n}\n")),
                                                mb.return_(mb.scopeUse("key")))))))),
                mb.bind(
                        "render_builder",
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("Render Node.js code"))),
                mb.bind("render", mb.field(mb.scopeUse("render_builder"), mb.str("func"))),
                mb.call(
                        mb.field(mb.scopeUse("render_builder"), mb.str("impl")),
                        mb.recordOf(TList.of(
                                rp("arg", mb.scopeUse("code")),
                                rp("ret", mb.optSomeType(mb.typesString())),
                                rp(
                                        "impl_body",
                                        mb.stage(mb.seqOf(
                                                mb.bind(
                                                        "context",
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp(
                                                                                "out",
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb.typesList(),
                                                                                                        mb
                                                                                                                .typesString()),
                                                                                                mb.str("new")),
                                                                                        mb.tupleOf())),
                                                                        rp(
                                                                                "expr_count",
                                                                                mb.as_(mb.num(0.), mb.typesInt()))))),
                                                mb.call(
                                                        mb.field(
                                                                mb.field(mb.scopeUse("context"), mb.str("out")),
                                                                mb.str("push")),
                                                        mb.call(
                                                                mb.lower(mb.scopeUse("render_inner")),
                                                                mb.recordOf(
                                                                        TList.of(
                                                                                rp("context", mb.scopeUse("context")),
                                                                                rp("code", mb.scopeUse("arg")))))),
                                                mb.return_(
                                                        mb.call(
                                                                mb.field(
                                                                        mb.field(mb.scopeUse("context"), mb.str("out")),
                                                                        mb.str("join")),
                                                                mb.str(""))))))))),
                mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.recordOf(TList.of(
                        rp("type", mb.scopeUse("code")),
                        rp("call", mb.scopeUse("code_call")),
                        rp("lit_str", mb.scopeUse("code_lit_str")),
                        rp("field", mb.scopeUse("code_field")),
                        rp("scope_use", mb.scopeUse("code_scope_use")),
                        rp("render", mb.scopeUse("render"))))));
    }

    private static SourceExpr codeOp(TestMb mb, String key, SourceExpr arg) {
        return mb.call(mb.field(mb.scopeUse("code"), mb.str(key)), arg);
    }

    @Test
    public void test00000NodejsCode() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("code", mb.lower(buildNodejsCode(mb))),
                        mb.call(
                                mb.log(),
                                codeOp(
                                        mb,
                                        "render",
                                        codeOp(
                                                mb,
                                                "call",
                                                mb.recordOf(
                                                        TList.of(
                                                                rp(
                                                                        "func",
                                                                        codeOp(
                                                                                mb,
                                                                                "field",
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                rp(
                                                                                                        "base",
                                                                                                        codeOp(
                                                                                                                mb,
                                                                                                                "scope_use",
                                                                                                                mb.str(
                                                                                                                        "console"))),
                                                                                                rp(
                                                                                                        "field",
                                                                                                        codeOp(
                                                                                                                mb,
                                                                                                                "lit_str",
                                                                                                                mb.str(
                                                                                                                        "log"))))))),
                                                                rp(
                                                                        "arg",
                                                                        codeOp(
                                                                                mb,
                                                                                "lit_str",
                                                                                mb.str("Hello world (node)")))))))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals(
                """
        let e0;
        {
        let e1;
        {
        let e2;
        {
        e2 = console;
        }
        let e3;
        {
        e3 = "log";
        }
        e1 = e2.e3;
        }
        let e4;
        {
        e4 = "Hello world (node)";
        }
        e0 = (e1)(e4);
        }
        e0""",
                ok.logs().get(0).message());
    }

    private static SourceExpr optionAspectNone(TestMb mb) {
        return mb.call(mb.field(mb.call(mb.typesOption(), mb.typesAspect()), mb.str("none")), mb.literalNull());
    }

    private static SourceExpr aspectDispatch(TestMb mb, SourceExpr aspectId) {
        return mb.ifOf(
                TList.of(new TestMb.Branch(
                        mb.operatorEq(mb.field(mb.scopeUse("arg"), mb.str("key")), aspectId),
                        mb.call(
                                mb.field(mb.call(mb.typesOption(), mb.typesAspect()), mb.str("some")),
                                mb.scopeUse("outer")))),
                Opt.some(optionAspectNone(mb)));
    }

    private static SourceExpr implInterface(
            TestMb mb, String targetVar, SourceExpr ifaceRef, TList<TestMb.RecordPair> methods) {
        return mb.call(
                mb.field(mb.scopeUse(targetVar), mb.str("impl_interface")),
                mb.recordOf(TList.of(rp("iface_ref", ifaceRef), rp("record", ifaceMethods(mb, methods)))));
    }

    private static TList<SourceExpr> implAsSelfType(TestMb mb, String targetVar, String desc, SourceExpr dispatch) {
        return TList.of(
                implInterface(mb, targetVar, mb.evalValue(), TList.of(rp("type", mb.scopeUse("outer")))),
                implInterface(
                        mb,
                        targetVar,
                        mb.evalType(),
                        TList.of(
                                rp("desc", mb.str(desc)),
                                rp("aspect", dispatch),
                                rp("can_cast_from", mb.bool(false)),
                                rp(
                                        "cast_from",
                                        eresultOk(mb, mb.typesCve(), mb.field(mb.scopeUse("arg"), mb.str("cv")))),
                                rp("can_cast_to", mb.bool(false)),
                                rp("cast_to", mb.panic(mb.str(desc + ".cast_to not implemented"))),
                                rp("is_equal", mb.bool(false)))));
    }

    private static SourceExpr nvalueToCode(TestMb mb, SourceExpr cveExpr) {
        return mb.as_(mb.field(cveExpr, mb.str("code")), mb.lower(mb.field(mb.scopeUse("code"), mb.str("type"))));
    }

    private static SourceExpr eresultOk(TestMb mb, SourceExpr resultType, SourceExpr value) {
        return mb.call(mb.field(mb.call(mb.typesEresult(), resultType), mb.str("ok")), value);
    }

    private static SourceExpr codeNone(TestMb mb) {
        return mb.call(mb.builtin("eval", "code_none"), mb.field(mb.scopeUse("arg"), mb.str("loc")));
    }

    private static SourceExpr targetPanic(TestMb mb, String method) {
        return mb.panic(mb.str("ntarget." + method + " not implemented"));
    }

    @Test
    public void test00010NodejsHelloworld() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var exprs = new TList<SourceExpr>();
        exprs.add(mb.bind("code", buildNodejsCode(mb)));
        exprs.add(mb.bind("d", mb.call(mb.define(), mb.tupleOf())));
        exprs.add(mb.bind("root_t", mb.call(mb.typesOption(), mb.typesExpr())));
        exprs.add(mb.bind("nvalue_base", mb.call(mb.field(mb.scopeUse("d"), mb.str("union")), mb.tupleOf())));
        exprs.add(mb.bind(
                "nvalue_binding",
                mb.field(
                        mb.call(
                                mb.field(mb.scopeUse("nvalue_base"), mb.str("variant")),
                                mb.recordOf(TList.of(rp("tag", mb.str("binding")), rp("type_ref", mb.typesString())))),
                        mb.str("value"))));
        exprs.add(mb.bind(
                "nvalue_expr",
                mb.field(
                        mb.call(
                                mb.field(mb.scopeUse("nvalue_base"), mb.str("variant")),
                                mb.recordOf(TList.of(rp("tag", mb.str("expr")), rp("type_ref", mb.typesNull())))),
                        mb.str("value"))));
        exprs.add(mb.bind("nvalue_base_t", mb.field(mb.scopeUse("nvalue_base"), mb.str("type"))));
        exprs.add(mb.bind("nvalue", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs value"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("nvalue"), mb.str("set_inner")),
                mb.recordOf(TList.of(rp("base", mb.scopeUse("nvalue_base_t")), rp("type", mb.evalType())))));

        exprs.add(implInterface(
                mb, "nvalue", mb.evalValue(), TList.of(rp("type", mb.field(mb.scopeUse("inner"), mb.str("type"))))));
        exprs.add(mb.bind(
                "aspect_consume_nodejs", mb.call(mb.field(mb.scopeUse("d"), mb.str("interface")), mb.tupleOf())));
        exprs.add(mb.call(mb.field(mb.scopeUse("aspect_consume_nodejs"), mb.str("set_is_aspect")), mb.bool(true)));
        exprs.add(mb.bind(
                "acn_to_code",
                mb.call(mb.field(mb.scopeUse("aspect_consume_nodejs"), mb.str("func")), mb.str("to_code"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("acn_to_code"), mb.str("impl")),
                mb.recordOf(TList.of(
                        rp("arg", mb.typesCve()),
                        rp("ret", mb.optSomeType(mb.field(mb.scopeUse("code"), mb.str("type"))))))));
        exprs.add(mb.bind("ntype_str", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs string"))));
        exprs.add(mb.call(mb.field(mb.scopeUse("ntype_str"), mb.str("set_inner")), mb.typesNull()));

        exprs.addAll(implAsSelfType(mb, "ntype_str", "nodejs string", optionAspectNone(mb)));
        exprs.add(implInterface(
                mb,
                "ntype_str",
                mb.field(mb.scopeUse("aspect_consume_nodejs"), mb.str("type")),
                TList.of(rp("to_code", nvalueToCode(mb, mb.scopeUse("arg"))))));
        exprs.add(mb.bind(
                "ntype_func", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs function"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("ntype_func"), mb.str("set_inner")),
                mb.recordOf(TList.of(rp("arg", mb.evalType()), rp("ret", mb.evalType())))));

        exprs.addAll(implAsSelfType(
                mb, "ntype_func", "nodejs function", aspectDispatch(mb, mb.builtin("eval", "aspect_function_id"))));
        exprs.add(
                implInterface(
                        mb,
                        "ntype_func",
                        mb.builtin("eval", "aspect_function"),
                        TList.of(
                                rp(
                                        "exception_type",
                                        mb.call(
                                                mb.field(mb.call(mb.typesOption(), mb.evalType()), mb.str("none")),
                                                mb.literalNull())),
                                rp(
                                        "call",
                                        mb.seqOf(
                                                mb.bind(
                                                        "func_code",
                                                        nvalueToCode(mb, mb.field(mb.scopeUse("arg"), mb.str("self")))),
                                                mb.bind(
                                                        "arg_value",
                                                        mb.field(
                                                                mb.call(
                                                                        mb.field(
                                                                                mb.field(
                                                                                        mb.scopeUse("arg"),
                                                                                        mb.str("argument")),
                                                                                mb.str("value")),
                                                                        mb.tupleOf()),
                                                                mb.str("value"))),
                                                mb.bind(
                                                        "acn_opt",
                                                        mb.call(
                                                                mb.evalAspectLookup(),
                                                                mb.tupleOf(
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.evalValue(), mb.str("type")),
                                                                                mb.scopeUse("arg_value")),
                                                                        mb.lower(
                                                                                mb.field(
                                                                                        mb.scopeUse(
                                                                                                "aspect_consume_nodejs"),
                                                                                        mb.str("type"))),
                                                                        mb.field(mb.scopeUse("arg"), mb.str("xx"))))),
                                                mb.bind(
                                                        "arg_code",
                                                        mb.ifOf(
                                                                TList.of(
                                                                        new TestMb.Branch(
                                                                                mb.field(
                                                                                        mb.scopeUse("acn_opt"),
                                                                                        mb.str("is_some")),
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "aspect_consume_nodejs"),
                                                                                                                mb.str(
                                                                                                                        "type"))),
                                                                                                mb.str("to_code")),
                                                                                        mb.tupleOf(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "acn_opt"),
                                                                                                        mb.str(
                                                                                                                "value")),
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "arg"),
                                                                                                        mb.str(
                                                                                                                "argument")))))),
                                                                Opt.some(
                                                                        mb.call(
                                                                                mb.lower(
                                                                                        mb.field(
                                                                                                mb.scopeUse("code"),
                                                                                                mb.str("lit_str"))),
                                                                                mb.call(
                                                                                        mb.builtin(
                                                                                                "eval",
                                                                                                "constkey_to_str"),
                                                                                        mb.call(
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "extract_constkey"),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "xx_",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "xx"))),
                                                                                                                rp(
                                                                                                                        "value",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "arg_value")))))))))),
                                                mb.bind(
                                                        "call_code",
                                                        mb.call(
                                                                mb.lower(mb.field(mb.scopeUse("code"), mb.str("call"))),
                                                                mb.recordOf(
                                                                        TList.of(
                                                                                rp("func", mb.scopeUse("func_code")),
                                                                                rp("arg", mb.scopeUse("arg_code")))))),
                                                mb.return_(
                                                        eresultOk(
                                                                mb,
                                                                mb.typesCve(),
                                                                mb.call(
                                                                        mb.evalCodevalue(),
                                                                        mb.recordOf(
                                                                                TList.of(
                                                                                        rp(
                                                                                                "code",
                                                                                                mb.as_(
                                                                                                        mb.scopeUse(
                                                                                                                "call_code"),
                                                                                                        mb
                                                                                                                .typesCode())),
                                                                                        rp(
                                                                                                "value",
                                                                                                mb.call(
                                                                                                        mb.lower(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "nvalue"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "new"))),
                                                                                                        mb.recordOf(
                                                                                                                TList
                                                                                                                        .of(
                                                                                                                                rp(
                                                                                                                                        "base",
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .lower(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "nvalue_expr")),
                                                                                                                                                        mb
                                                                                                                                                                .literalNull())),
                                                                                                                                rp(
                                                                                                                                        "type",
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "inner"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "ret")))))))))))))))));
        exprs.add(mb.bind(
                "ntype_object", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs object"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("ntype_object"), mb.str("set_inner")),
                mb.call(mb.typesMap(), mb.tupleOf(mb.typesString(), mb.evalType()))));

        exprs.addAll(implAsSelfType(
                mb, "ntype_object", "nodejs object", aspectDispatch(mb, mb.builtin("eval", "aspect_field_id"))));
        exprs.add(implInterface(
                mb,
                "ntype_object",
                mb.evalAspectField(),
                TList.of(
                        rp(
                                "field",
                                mb.seqOf(
                                        mb.bind(
                                                "key_str",
                                                mb.call(
                                                        mb.builtin("eval", "constkey_to_str"),
                                                        mb.call(
                                                                mb.builtin("eval", "extract_constkey"),
                                                                mb.recordOf(
                                                                        TList.of(
                                                                                rp(
                                                                                        "xx_",
                                                                                        mb.field(
                                                                                                mb.scopeUse("arg"),
                                                                                                mb.str("xx"))),
                                                                                rp(
                                                                                        "value",
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "key")),
                                                                                                                mb.str(
                                                                                                                        "value")),
                                                                                                        mb.tupleOf()),
                                                                                                mb.str("value")))))))),
                                        mb.bind(
                                                "base_code",
                                                nvalueToCode(mb, mb.field(mb.scopeUse("arg"), mb.str("self")))),
                                        mb.bind(
                                                "field_code",
                                                mb.call(
                                                        mb.lower(mb.field(mb.scopeUse("code"), mb.str("field"))),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp("base", mb.scopeUse("base_code")),
                                                                        rp(
                                                                                "field",
                                                                                mb.call(
                                                                                        mb.lower(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "code"),
                                                                                                        mb.str(
                                                                                                                "lit_str"))),
                                                                                        mb.scopeUse("key_str"))))))),
                                        mb.return_(
                                                eresultOk(
                                                        mb,
                                                        mb.typesCve(),
                                                        mb.call(
                                                                mb.evalCodevalue(),
                                                                mb.recordOf(
                                                                        TList.of(
                                                                                rp(
                                                                                        "code",
                                                                                        mb.as_(
                                                                                                mb.scopeUse(
                                                                                                        "field_code"),
                                                                                                mb.typesCode())),
                                                                                rp(
                                                                                        "value",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "nvalue"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "base",
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .lower(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "nvalue_expr")),
                                                                                                                                        mb
                                                                                                                                                .literalNull())),
                                                                                                                rp(
                                                                                                                        "type",
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "inner"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "get")),
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "key_str")))))))))))))),
                        rp(
                                "type_opt",
                                mb.call(
                                        mb.field(mb.call(mb.typesOption(), mb.evalType()), mb.str("some")),
                                        mb.call(
                                                mb.field(mb.scopeUse("inner"), mb.str("get")),
                                                mb.call(
                                                        mb.builtin("eval", "constkey_to_str"),
                                                        mb.field(mb.scopeUse("arg"), mb.str("key")))))),
                        rp(
                                "keys",
                                mb.seqOf(mb.return_(mb.call(
                                        mb.field(mb.call(mb.typesList(), mb.typesConstkey()), mb.str("new")),
                                        mb.tupleOf())))))));
        exprs.add(mb.bind("nbinding", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs binding"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("nbinding"), mb.str("set_inner")),
                mb.recordOf(TList.of(rp("name", mb.typesString()), rp("type", mb.evalType())))));

        exprs.add(implInterface(
                mb,
                "nbinding",
                mb.evalBinding(),
                TList.of(
                        rp("drop", mb.panic(mb.str("nbinding.drop not implemented"))),
                        rp(
                                "use",
                                mb.seqOf(
                                        mb.return_(
                                                mb.call(
                                                        mb.evalCodevalue(),
                                                        mb.recordOf(
                                                                TList.of(
                                                                        rp(
                                                                                "code",
                                                                                mb.as_(
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "code"),
                                                                                                                mb.str(
                                                                                                                        "scope_use"))),
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "inner"),
                                                                                                        mb.str(
                                                                                                                "name"))),
                                                                                        mb.typesCode())),
                                                                        rp(
                                                                                "value",
                                                                                mb.call(
                                                                                        mb.lower(
                                                                                                mb.field(
                                                                                                        mb.scopeUse(
                                                                                                                "nvalue"),
                                                                                                        mb.str("new"))),
                                                                                        mb.recordOf(
                                                                                                TList.of(
                                                                                                        rp(
                                                                                                                "base",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .lower(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "nvalue_binding")),
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "inner"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "name")))),
                                                                                                        rp(
                                                                                                                "type",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "inner"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "type"))))))))))))),
                        rp("fork", mb.scopeUse("outer")),
                        rp("merge", mb.panic(mb.str("nbinding.merge not implemented"))),
                        rp("equals", mb.seqOf(mb.return_(mb.operatorEq(mb.num(1.0), mb.num(1.0))))))));
        exprs.add(mb.bind("ntarget", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs target"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("ntarget"), mb.str("set_inner")),
                mb.recordOf(TList.of(rp("count", mb.typesInt())))));
        exprs.add(implInterface(
                mb,
                "ntarget",
                mb.evalTarget(),
                TList.of(
                        rp("bind", targetPanic(mb, "bind")),
                        rp(
                                "maybe_bind_sub",
                                eresultOk(
                                        mb,
                                        mb.call(mb.typesOption(), mb.builtin("types", "target_bind_sub")),
                                        mb.call(
                                                mb.field(
                                                        mb.call(
                                                                mb.typesOption(),
                                                                mb.builtin("types", "target_bind_sub")),
                                                        mb.str("none")),
                                                mb.literalNull()))),
                        rp("code_to_bind_sub", targetPanic(mb, "code_to_bind_sub")),
                        rp("bind_sub_record", targetPanic(mb, "bind_sub_record")),
                        rp("bind_sub_tuple", targetPanic(mb, "bind_sub_tuple")),
                        rp(
                                "cast_exception",
                                eresultOk(
                                        mb,
                                        mb.call(mb.typesOption(), mb.typesCode()),
                                        mb.call(
                                                mb.field(mb.call(mb.typesOption(), mb.typesCode()), mb.str("none")),
                                                mb.literalNull()))),
                        rp("seq", eresultOk(mb, mb.typesCode(), codeNone(mb))),
                        rp("jump", targetPanic(mb, "jump")),
                        rp("land", targetPanic(mb, "land")),
                        rp("guarded", targetPanic(mb, "guarded")),
                        rp("exc_cleanup", targetPanic(mb, "exc_cleanup")),
                        rp("exit_trampoline", targetPanic(mb, "exit_trampoline")),
                        rp("fresh", eresultOk(mb, mb.typesCode(), codeNone(mb))),
                        rp("drop", eresultOk(mb, mb.typesCode(), codeNone(mb))),
                        rp("return", targetPanic(mb, "return")),
                        rp("stage", targetPanic(mb, "stage")),
                        rp("if", targetPanic(mb, "if")),
                        rp("try_catch_finally", targetPanic(mb, "try_catch_finally")),
                        rp("match", targetPanic(mb, "match")))));

        exprs.add(mb.bind("napp", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("nodejs app"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("napp"), mb.str("set_inner")),
                mb.recordOf(TList.of(rp("root", mb.scopeUse("root_t"))))));

        exprs.add(mb.bind("napp_root", mb.call(mb.field(mb.scopeUse("napp"), mb.str("func")), mb.str("root"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("napp_root"), mb.str("impl")),
                mb.recordOf(TList.of(
                        rp("arg", mb.typesExpr()),
                        rp("ret", mb.optSomeType(mb.typesNull())),
                        rp(
                                "impl_body",
                                mb.stage(mb.set(
                                        mb.field(mb.scopeUse("inner"), mb.str("root")),
                                        mb.call(
                                                mb.field(mb.lower(mb.scopeUse("root_t")), mb.str("some")),
                                                mb.scopeUse("arg")))))))));
        exprs.add(mb.bind("napp_build", mb.call(mb.field(mb.scopeUse("napp"), mb.str("func")), mb.str("build"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("napp_build"), mb.str("impl")),
                mb.recordOf(TList.of(
                        rp("arg", mb.typesNull()),
                        rp("ret", mb.optSomeType(mb.typesString())),
                        rp(
                                "impl_body",
                                mb.stage(mb.seqOf(
                                        mb.bind("ro", mb.field(mb.scopeUse("inner"), mb.str("root"))),
                                        mb.return_(
                                                mb.ifOf(
                                                        TList.of(
                                                                new TestMb.Branch(
                                                                        mb.field(mb.scopeUse("ro"), mb.str("is_some")),
                                                                        mb.seqOf(
                                                                                mb.bind(
                                                                                        "root_expr",
                                                                                        mb.field(
                                                                                                mb.scopeUse("ro"),
                                                                                                mb.str("value"))),
                                                                                mb.bind(
                                                                                        "ntype_str_inst",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "ntype_str"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.literalNull())),
                                                                                mb.bind(
                                                                                        "ntype_log",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "ntype_func"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "arg",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "ntype_str_inst")),
                                                                                                                rp(
                                                                                                                        "ret",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "ntype_str_inst")))))),
                                                                                mb.bind(
                                                                                        "ntype_console",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "ntype_object"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesMap(),
                                                                                                                        mb
                                                                                                                                .tupleOf(
                                                                                                                                        mb
                                                                                                                                                .typesString(),
                                                                                                                                        mb
                                                                                                                                                .evalType())),
                                                                                                                mb.str(
                                                                                                                        "new")),
                                                                                                        mb.recordOf(
                                                                                                                TList
                                                                                                                        .of(
                                                                                                                                rp(
                                                                                                                                        "log",
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "ntype_log"))))))),
                                                                                mb.bind(
                                                                                        "console_entry",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "nbinding"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "name",
                                                                                                                        mb
                                                                                                                                .str(
                                                                                                                                        "console")),
                                                                                                                rp(
                                                                                                                        "type",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "ntype_console")))))),
                                                                                mb.bind(
                                                                                        "tgt",
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "ntarget"),
                                                                                                                mb.str(
                                                                                                                        "new"))),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "count",
                                                                                                                        mb
                                                                                                                                .as_(
                                                                                                                                        mb
                                                                                                                                                .num(
                                                                                                                                                        0.),
                                                                                                                                        mb
                                                                                                                                                .typesInt())))))),
                                                                                mb.bind(
                                                                                        "vx2",
                                                                                        mb.call(
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "eval_context"),
                                                                                                mb.tupleOf())),
                                                                                mb.bind(
                                                                                        "sc",
                                                                                        mb.call(
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "scope"),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "vx_",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "vx2")),
                                                                                                                rp(
                                                                                                                        "entries",
                                                                                                                        mb
                                                                                                                                .call(
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .call(
                                                                                                                                                                        mb
                                                                                                                                                                                .typesMap(),
                                                                                                                                                                        mb
                                                                                                                                                                                .tupleOf(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .typesConstkey(),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .evalBinding())),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "new")),
                                                                                                                                        mb
                                                                                                                                                .recordOf(
                                                                                                                                                        TList
                                                                                                                                                                .of(
                                                                                                                                                                        rp(
                                                                                                                                                                                "console",
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "console_entry")))))))))),
                                                                                mb.bind(
                                                                                        "ctx",
                                                                                        mb.call(
                                                                                                mb.builtin(
                                                                                                        "eval",
                                                                                                        "expr_context"),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "scope",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "sc")),
                                                                                                                rp(
                                                                                                                        "target",
                                                                                                                        mb
                                                                                                                                .as_(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "tgt"),
                                                                                                                                        mb
                                                                                                                                                .evalTarget())),
                                                                                                                rp(
                                                                                                                        "vx",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "vx2")),
                                                                                                                rp(
                                                                                                                        "exception_type",
                                                                                                                        mb
                                                                                                                                .typesString()))))),
                                                                                mb.bind(
                                                                                        "res",
                                                                                        mb.call(
                                                                                                mb.evalConstEval(),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                rp(
                                                                                                                        "xx_",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "ctx")),
                                                                                                                rp(
                                                                                                                        "staged",
                                                                                                                        mb
                                                                                                                                .scopeUse(
                                                                                                                                        "root_expr")))))),
                                                                                mb.return_(
                                                                                        mb.call(
                                                                                                mb.lower(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "code"),
                                                                                                                mb.str(
                                                                                                                        "render"))),
                                                                                                mb.as_(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "res"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "value")),
                                                                                                                mb.str(
                                                                                                                        "code")),
                                                                                                        mb.lower(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "code"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "type"))))))))),
                                                        Opt.some(mb.str("")))))))))));
        exprs.add(mb.bind("nodejs", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("nodejs"))));
        exprs.add(mb.call(
                mb.field(mb.scopeUse("nodejs"), mb.str("impl")),
                mb.recordOf(TList.of(
                        rp("arg", mb.typesNull()),
                        rp("ret", mb.optSomeType(mb.field(mb.scopeUse("napp"), mb.str("type")))),
                        rp(
                                "impl_body",
                                mb.stage(mb.call(
                                        mb.lower(mb.field(mb.scopeUse("napp"), mb.str("new"))),
                                        mb.recordOf(
                                                TList.of(
                                                        rp(
                                                                "root",
                                                                mb.call(
                                                                        mb.field(
                                                                                mb.lower(mb.scopeUse("root_t")),
                                                                                mb.str("none")),
                                                                        mb.literalNull())))))))))));
        exprs.add(mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()));
        exprs.add(mb.return_(mb.field(mb.scopeUse("nodejs"), mb.str("func"))));
        modules.put("nodejs", new SourceModule.V1(mb.lower(mb.seq(exprs))));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("nodejs", mb.importExpr(mb.str("nodejs"))),
                        mb.bind("app", mb.call(mb.scopeUse("nodejs"), mb.literalNull())),
                        mb.call(
                                mb.field(mb.scopeUse("app"), mb.str("root")),
                                mb.stage(mb.call(
                                        mb.field(mb.scopeUse("console"), mb.str("log")),
                                        mb.str("Hello world (node)")))),
                        mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("app"), mb.str("build")), mb.literalNull())))));

        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> resOk;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> o -> resOk = o;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> e -> {
                assertTrue(false, () -> "compile failed: " + res);
                return;
            }
        }
        var ok = resOk.value();
        assertEquals(1, ok.logs().size());
        assertEquals(
                """
        let e0;
        {
        let e1;
        {
        let e2;
        {
        e2 = console;
        }
        let e3;
        {
        e3 = "log";
        }
        e1 = e2.e3;
        }
        let e4;
        {
        e4 = "Hello world (node)";
        }
        e0 = (e1)(e4);
        }
        e0""",
                ok.logs().get(0).message());
    }
}
