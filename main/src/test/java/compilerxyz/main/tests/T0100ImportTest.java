package compilerxyz.main.tests;

import static compilerxyz.main.tests.Helpers.jsonArr;
import static compilerxyz.main.tests.Helpers.jsonField;
import static compilerxyz.main.tests.Helpers.jsonNum;
import static compilerxyz.main.tests.Helpers.jsonObj;
import static compilerxyz.main.tests.Helpers.jsonStr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0100ImportTest {
    @Test
    public void test00100ImportLiteral() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("other", new SourceModule.V1(TList.of(mb.return_(mb.str("Hello world (mortar)")))));
        modules.put("main", new SourceModule.V1(TList.of(mb.call(mb.log(), mb.importExpr(mb.str("other"))))));
        var gxa = Helpers.newGxWithArtifacts(modules);
        var res = Helpers.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("Hello world (mortar)", ok.logs().get(0).message());
        assertEquals(0, gxa.artifacts().data.size());
    }

    @Test
    public void test00200ImportDefinitionset() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "other",
                new SourceModule.V1(TList.of(
                        mb.bind(
                                "ctor",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "ndr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str("upper"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("inner", mb.typesString()),
                                                                new TestMb.RecordPair(
                                                                        "description", mb.optNoneString())))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectConsumeDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "to_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "to_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "jvm_class",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                                "jvm_class",
                                                                                                                                "xx")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("iface_ref", mb.evalAspectBind()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBind(),
                                                                                                                                "bind",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectBindDynamic()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "bind_dynamic",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                                "bind_dynamic",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "cv")))))))))),
                                        mb.call(
                                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "iface_ref", mb.evalAspectField()),
                                                                new TestMb.RecordPair(
                                                                        "record",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesMap(),
                                                                                                mb.tupleOf(
                                                                                                        mb
                                                                                                                .typesConstkey(),
                                                                                                        mb
                                                                                                                .typesExpr())),
                                                                                        mb.str("new")),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "field",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .delegateAspect(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectField(),
                                                                                                                                "field",
                                                                                                                                "xx",
                                                                                                                                "loc",
                                                                                                                                "self",
                                                                                                                                "key"))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "type_opt",
                                                                                                        mb.stage(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .call(
                                                                                                                                                        mb
                                                                                                                                                                .typesOption(),
                                                                                                                                                        mb
                                                                                                                                                                .evalType()),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "none")),
                                                                                                                        mb
                                                                                                                                .tupleOf()))),
                                                                                                new TestMb.RecordPair(
                                                                                                        "keys",
                                                                                                        mb.stage(
                                                                                                                mb
                                                                                                                        .seqOf(
                                                                                                                                mb
                                                                                                                                        .return_(
                                                                                                                                                mb
                                                                                                                                                        .call(
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .call(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesList(),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesConstkey()),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "new")),
                                                                                                                                                                mb
                                                                                                                                                                        .tupleOf())))))))))))),
                                        mb.bind(
                                                "mnfr",
                                                mb.call(
                                                        mb.field(mb.scopeUse("ndr"), mb.str("func")),
                                                        mb.str("to_string"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("mnfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesString())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.scopeUse("inner"),
                                                                                                mb.str("toUpperCase")),
                                                                                        mb.tupleOf())))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.field(mb.scopeUse("ndr"), mb.str("new")))))),
                        mb.return_(mb.call(mb.scopeUse("ctor"), mb.str("hello"))))));
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("val", mb.importExpr(mb.str("other"))),
                        mb.call(mb.log(), mb.call(mb.field(mb.scopeUse("val"), mb.str("to_string")), mb.tupleOf())))));
        var gxa = Helpers.newGxWithArtifacts(modules);
        var res = Helpers.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("HELLO", ok.logs().get(0).message());
        assertEquals(3, gxa.artifacts().data.size());
        var tupleArt = Helpers.artifactByTag(gxa.artifacts(), "tuple");
        assertEquals(
                new ArtifactBodyExporter.Body(new ArtifactBody(jsonStr("tuple"), new ArtifactBodyExporter.Root())),
                tupleArt.exporter());
        assertEquals(jsonObj(jsonField("elements", jsonArr())), tupleArt.data());
        var objArt = Helpers.artifactByTag(gxa.artifacts(), "source_object_const");
        assertEquals(
                new ArtifactBodyExporter.Body(
                        new ArtifactBody(jsonStr("source_object_const"), new ArtifactBodyExporter.Root())),
                objArt.exporter());
        assertEquals(
                jsonObj(
                        jsonField("def_set", artifactRef(1)),
                        jsonField("def_set_id", defSetIdJson()),
                        jsonField("name", jsonObj(jsonField("some", jsonStr("upper")))),
                        jsonField(
                                "identity",
                                jsonObj(jsonField(
                                        "source",
                                        jsonObj(
                                                jsonField(
                                                        "compile_id",
                                                        jsonObj(
                                                                jsonField("import_state", jsonNum(0)),
                                                                jsonField("source", jsonNum(0)))),
                                                jsonField("id", jsonNum(0)))))),
                        jsonField("underlying_type", typeArtifact("string_dynamic")),
                        jsonField(
                                "methods",
                                jsonArr(jsonObj(
                                        jsonField("name", jsonStr("to_string")),
                                        jsonField("arg_type", artifactRef(2)),
                                        jsonField(
                                                "ret_type", jsonObj(jsonField("some", typeArtifact("string_dynamic")))),
                                        jsonField("jvm_method_name", jsonStr("_0")),
                                        jsonField("captured_externals", jsonArr())))),
                        jsonField(
                                "interface_impls",
                                jsonArr(
                                        interfaceImpl(
                                                4024,
                                                methodImpl(
                                                        4024,
                                                        "to_dynamic",
                                                        "_iface0_method0",
                                                        externalType(),
                                                        externalUnique(1024)),
                                                methodImpl(
                                                        4024,
                                                        "jvm_class",
                                                        "_iface0_method1",
                                                        externalType(),
                                                        externalUnique(1024))),
                                        interfaceImpl(
                                                4022,
                                                methodImpl(
                                                        4022,
                                                        "bind",
                                                        "_iface1_method0",
                                                        externalType(),
                                                        externalUnique(1022))),
                                        interfaceImpl(
                                                4023,
                                                methodImpl(
                                                        4023,
                                                        "bind_dynamic",
                                                        "_iface2_method0",
                                                        externalType(),
                                                        externalUnique(1023))),
                                        interfaceImpl(
                                                4015,
                                                methodImpl(
                                                        4015,
                                                        "field",
                                                        "_iface3_method0",
                                                        externalType(),
                                                        externalUnique(1015)),
                                                methodImpl(4015, "type_opt", "_iface3_method1"),
                                                methodImpl(4015, "keys", "_iface3_method2")))),
                        jsonField("binary_name", jsonStr("SourceObject$0_0_0_0")),
                        jsonField(
                                "compiled_class",
                                jsonObj(jsonField(
                                        "some",
                                        jsonObj(
                                                jsonField("bytecode", jsonStr("BYTECODE")),
                                                jsonField("locs", jsonStr("LOCS")))))),
                        jsonField("meta_id", jsonObj(jsonField("some", jsonStr("META"))))),
                Helpers.jsonRedacted(objArt.data()));
        var defSetArt = Helpers.artifactByTag(gxa.artifacts(), "definition_set");
        assertEquals(
                new ArtifactBodyExporter.Body(
                        new ArtifactBody(jsonStr("definition_set"), new ArtifactBodyExporter.Root())),
                defSetArt.exporter());
        assertEquals(
                jsonObj(
                        jsonField("def_set_id", defSetIdJson()),
                        jsonField("dependencies", jsonArr()),
                        jsonField("functions", jsonArr()),
                        jsonField("interfaces", jsonArr()),
                        jsonField("objects", jsonArr(artifactRef(0))),
                        jsonField("unions", jsonArr())),
                defSetArt.data());
    }

    private static JsonValue typeArtifact(String tag) {
        return jsonObj(jsonField(
                "body",
                jsonObj(
                        jsonField("data", new JsonValueNull()),
                        jsonField(
                                "exporter",
                                jsonObj(jsonField(
                                        "body",
                                        jsonObj(
                                                jsonField("data", jsonStr(tag)),
                                                jsonField("exporter", jsonObj(jsonField("root", jsonObj()))))))))));
    }

    private static JsonValue defSetIdJson() {
        return jsonObj(
                jsonField(
                        "compile_id", jsonObj(jsonField("import_state", jsonNum(0)), jsonField("source", jsonNum(0)))),
                jsonField("index", jsonNum(0)));
    }

    private static JsonValue artifactRef(long index) {
        return jsonObj(jsonField(
                "ref",
                jsonObj(
                        jsonField(
                                "compile_id",
                                jsonObj(jsonField("import_state", jsonNum(0)), jsonField("source", jsonNum(0)))),
                        jsonField("index", jsonNum(index)))));
    }

    private static JsonValue externalType() {
        return jsonObj(jsonField(
                "exported",
                jsonObj(
                        jsonField("artifact", typeArtifact("string_dynamic")),
                        jsonField("lowered", new JsonValueBoolean(false)))));
    }

    private static JsonValue externalUnique(long builtinId) {
        return jsonObj(jsonField("unique_ref", builtinUnique(builtinId)));
    }

    private static JsonValue builtinUnique(long identityId) {
        return jsonObj(jsonField("builtin", jsonNum(identityId)));
    }

    private static JsonValue bodyArtifact(String tag, JsonValue data) {
        return jsonObj(jsonField(
                "body",
                jsonObj(
                        jsonField("data", data),
                        jsonField(
                                "exporter",
                                jsonObj(jsonField(
                                        "body",
                                        jsonObj(
                                                jsonField("data", jsonStr(tag)),
                                                jsonField("exporter", jsonObj(jsonField("root", jsonObj()))))))))));
    }

    private static JsonValue methodImpl(
            long ifaceIdentityId, String name, String jvmMethodName, JsonValue... externals) {
        return jsonObj(
                jsonField(
                        "interface_method",
                        bodyArtifact(
                                "source_interface_method",
                                jsonObj(jsonField(
                                        "builtin",
                                        jsonObj(
                                                jsonField("iface", builtinUnique(ifaceIdentityId)),
                                                jsonField("name", jsonStr(name))))))),
                jsonField("jvm_method_name", jsonStr(jvmMethodName)),
                jsonField("captured_externals", jsonArr(externals)));
    }

    private static JsonValue interfaceImpl(long identityId, JsonValue... methodImpls) {
        return jsonObj(
                jsonField(
                        "iface",
                        bodyArtifact(
                                "source_interface_dynamic", jsonObj(jsonField("builtin", builtinUnique(identityId))))),
                jsonField("method_impls", jsonArr(methodImpls)));
    }

    @Test
    public void test00300ImportTransitiveDefinitionset() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "module_b",
                new SourceModule.V1(TList.of(
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("func_b"))),
                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                        mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("arg", mb.tupleOf()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                                        new TestMb.RecordPair("impl_body", mb.stage(mb.str("hello"))),
                                        new TestMb.RecordPair("exception_type", mb.optNoneType()),
                                        new TestMb.RecordPair("description", mb.optNoneString())))),
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.scopeUse("f")))));
        modules.put(
                "module_a",
                new SourceModule.V1(TList.of(
                        mb.bind("func_b", mb.importExpr(mb.str("module_b"))),
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("nfr", mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("func_a"))),
                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                        mb.call(
                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("arg", mb.tupleOf()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                                        new TestMb.RecordPair(
                                                "impl_body",
                                                mb.stage(mb.call(mb.lower(mb.scopeUse("func_b")), mb.tupleOf()))),
                                        new TestMb.RecordPair("exception_type", mb.optNoneType()),
                                        new TestMb.RecordPair("description", mb.optNoneString())))),
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.scopeUse("f")))));
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("func_a", mb.importExpr(mb.str("module_a"))),
                        mb.call(mb.log(), mb.call(mb.scopeUse("func_a"), mb.tupleOf())))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> true;
                    case Result.Err<?, ?> e -> false;
                },
                () -> "compile failed: " + res);
        @SuppressWarnings("unchecked")
        var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
        assertEquals(1, ok.logs().size());
        assertEquals("hello", ok.logs().get(0).message());
    }

    @Test
    public void test00400ImportCycle() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put("module_a", new SourceModule.V1(TList.of(mb.return_(mb.importExpr(mb.str("module_b"))))));
        modules.put("module_b", new SourceModule.V1(TList.of(mb.return_(mb.importExpr(mb.str("module_a"))))));
        modules.put("main", new SourceModule.V1(TList.of(mb.return_(mb.importExpr(mb.str("module_a"))))));
        var gx = Helpers.newGx(modules);
        var res = Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Ok<?, ?> o -> false;
                    case Result.Err<?, ?> e -> true;
                },
                () -> "expected import cycle error, got: " + res);
    }
}
