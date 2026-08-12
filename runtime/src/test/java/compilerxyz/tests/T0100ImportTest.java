package compilerxyz.tests;

import static compilerxyz.tests.Helpers.jsonArr;
import static compilerxyz.tests.Helpers.jsonField;
import static compilerxyz.tests.Helpers.jsonNull;
import static compilerxyz.tests.Helpers.jsonNum;
import static compilerxyz.tests.Helpers.jsonObj;
import static compilerxyz.tests.Helpers.jsonStr;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.langhelp.Compile;
import compilerxyz.safejson.JsonValue;
import compilerxyz.syntax.SourceModule;
import compilerxyz.syntax.SourceSpecAbsolute;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import org.junit.jupiter.api.Test;
import compilerxyz.eval.ExprErr;

public final class T0100ImportTest {
  @Test
  public void test00100ImportLiteral() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "other",
        new SourceModule.V1(mb.str("Hello world (mortar)")));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.call(mb.log(), mb.importExpr(mb.str("other")))));
    var gxa = Helpers.newGxWithArtifacts(modules);
    var res = Compile.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
    assertTrue(switch (res) { case Result.Ok<?,?> o -> true; case Result.Err<?,?> e -> false; }, () -> "compile failed: " + res);
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
        new SourceModule.V1(
            mb.seqOf(
                mb.bind(
                    "ctor",
                    mb.lower(
                        mb.seqOf(
                            mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                            mb.bind(
                                "ndr",
                                mb.call(
                                    mb.field(mb.scopeUse("d"), mb.str("object")),
                                    mb.str("upper"))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("set_inner")),
                                mb.typesString()),
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
                                                            mb.typesConstkey(),
                                                            mb.typesExpr())),
                                                    mb.str("new")),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "to_dynamic",
                                                            mb.stage(
                                                                mb.delegateAspect(
                                                                    mb.typesString(),
                                                                    mb.evalAspectConsumeDynamic(),
                                                                    "to_dynamic",
                                                                    "xx",
                                                                    "loc",
                                                                    "cv"))),
                                                        new TestMb.RecordPair(
                                                            "jvm_class",
                                                            mb.stage(
                                                                mb.delegateAspect(
                                                                    mb.typesString(),
                                                                    mb.evalAspectConsumeDynamic(),
                                                                    "jvm_class",
                                                                    "xx")))))))))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "iface_ref", mb.evalAspectBind()),
                                        new TestMb.RecordPair(
                                            "record",
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.typesMap(),
                                                        mb.tupleOf(
                                                            mb.typesConstkey(),
                                                            mb.typesExpr())),
                                                    mb.str("new")),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "bind",
                                                            mb.stage(
                                                                mb.delegateAspect(
                                                                    mb.typesString(),
                                                                    mb.evalAspectBind(),
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
                                                            mb.typesConstkey(),
                                                            mb.typesExpr())),
                                                    mb.str("new")),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "bind_dynamic",
                                                            mb.stage(
                                                                mb.delegateAspect(
                                                                    mb.typesString(),
                                                                    mb.evalAspectBindDynamic(),
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
                                                            mb.typesConstkey(),
                                                            mb.typesExpr())),
                                                    mb.str("new")),
                                                mb.recordOf(
                                                    TList.of(
                                                        new TestMb.RecordPair(
                                                            "field",
                                                            mb.stage(
                                                                mb.delegateAspect(
                                                                    mb.typesString(),
                                                                    mb.evalAspectField(),
                                                                    "field",
                                                                    "xx",
                                                                    "loc",
                                                                    "self",
                                                                    "key"))),
                                                        new TestMb.RecordPair(
                                                            "type_opt",
                                                            mb.stage(
                                                                mb.call(
                                                                    mb.field(
                                                                        mb.call(
                                                                            mb.typesOption(),
                                                                            mb.evalType()),
                                                                        mb.str("none")),
                                                                    mb.literalNull()))),
                                                        new TestMb.RecordPair(
                                                            "keys",
                                                            mb.stage(
                                                                mb.seqOf(
                                                                    mb.return_(
                                                                        mb.call(
                                                                            mb.field(
                                                                                mb.call(
                                                                                    mb.typesList(),
                                                                                    mb.typesConstkey()),
                                                                                mb.str("new")),
                                                                            mb.tupleOf())))))))))))),
                            mb.call(
                                mb.field(mb.scopeUse("ndr"), mb.str("impl_interface")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair(
                                            "iface_ref", mb.evalAspectLift()),
                                        new TestMb.RecordPair(
                                            "record",
                                            mb.call(
                                                mb.field(
                                                    mb.call(
                                                        mb.typesMap(),
                                                        mb.tupleOf(
                                                            mb.typesConstkey(),
                                                            mb.typesExpr())),
                                                    mb.str("new")),
                                                mb.recordOf(TList.of())))))),
                            mb.bind(
                                "mnfr",
                                mb.call(
                                    mb.field(mb.scopeUse("ndr"), mb.str("func")),
                                    mb.str("to_string"))),
                            mb.call(
                                mb.field(mb.scopeUse("mnfr"), mb.str("impl")),
                                mb.recordOf(
                                    TList.of(
                                        new TestMb.RecordPair("arg", mb.typesNull()),
                                        new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
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
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("val", mb.importExpr(mb.str("other"))),
                mb.call(
                    mb.log(),
                    mb.call(
                        mb.field(mb.scopeUse("val"), mb.str("to_string")),
                        mb.tupleOf())))));
    var gxa = Helpers.newGxWithArtifacts(modules);
    var res = Compile.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main"));
    assertTrue(switch (res) { case Result.Ok<?,?> o -> true; case Result.Err<?,?> e -> false; }, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("HELLO", ok.logs().get(0).message());
    assertEquals(2, gxa.artifacts().data.size());
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
                jsonObj(
                    jsonField(
                        "source",
                        jsonObj(
                            jsonField(
                                "compile_id",
                                jsonObj(
                                    jsonField(
                                        "import_state", jsonObj(jsonField("value", jsonNum(0)))),
                                    jsonField("source", jsonObj(jsonField("value", jsonNum(0)))))),
                            jsonField("id", jsonNum(0)))))),
            jsonField("underlying_type", typeArtifact("string_dynamic")),
            jsonField(
                "methods",
                jsonArr(
                    jsonObj(
                        jsonField("name", jsonStr("to_string")),
                        jsonField("arg_type", typeArtifact("null_dynamic")),
                        jsonField("ret_type", typeArtifact("string_dynamic")),
                        jsonField("compiled_class_name", jsonObj(jsonField("none", jsonNull()))),
                        jsonField("compiled_bytecode", jsonObj(jsonField("none", jsonNull()))),
                        jsonField("captured_externals", jsonArr())))),
            jsonField(
                "interface_impls",
                jsonArr(
                    interfaceImpl(
                        "aspect_consume_dynamic",
                        methodImpl(0, externalType(), externalUnique(1024)),
                        methodImpl(1, externalType(), externalUnique(1024))),
                    interfaceImpl(
                        "aspect_bind", methodImpl(0, externalType(), externalUnique(1022))),
                    interfaceImpl(
                        "aspect_bind_dynamic",
                        methodImpl(0, externalType(), externalUnique(1023))),
                    interfaceImpl(
                        "aspect_field",
                        methodImpl(0, externalType(), externalUnique(1015)),
                        methodImpl(1),
                        methodImpl(2)),
                    interfaceImpl("aspect_lift"))),
            jsonField("compiled_binary_name", jsonObj(jsonField("some", jsonStr("SourceObject$0_0_0_0")))),
            jsonField("compiled_bytecode", jsonStr("BYTECODE"))),
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
    return jsonObj(
        jsonField(
            "body",
            jsonObj(
                jsonField("data", jsonNull()),
                jsonField(
                    "exporter",
                    jsonObj(
                        jsonField(
                            "body",
                            jsonObj(
                                jsonField("data", jsonStr(tag)),
                                jsonField(
                                    "exporter", jsonObj(jsonField("root", jsonObj()))))))))));
  }

  private static JsonValue defSetIdJson() {
    return jsonObj(
        jsonField(
            "compile_id",
            jsonObj(
                jsonField("import_state", jsonObj(jsonField("value", jsonNum(0)))),
                jsonField("source", jsonObj(jsonField("value", jsonNum(0)))))),
        jsonField("index", jsonNum(0)));
  }

  private static JsonValue artifactRef(long index) {
    return jsonObj(
        jsonField(
            "ref",
            jsonObj(
                jsonField(
                    "compile_id",
                    jsonObj(
                        jsonField("import_state", jsonNum(0)), jsonField("source", jsonNum(0)))),
                jsonField("index", jsonNum(index)))));
  }

  private static JsonValue externalType() {
    return jsonObj(
        jsonField(
            "type",
            jsonObj(
                jsonField("artifact_external_key", jsonObj(jsonField("id", jsonStr("KEY")))),
                jsonField("type_artifact", typeArtifact("string_dynamic")))));
  }

  private static JsonValue externalUnique(long builtinId) {
    return jsonObj(
        jsonField(
            "unique_ref",
            jsonObj(
                jsonField("artifact_external_key", jsonObj(jsonField("id", jsonStr("KEY")))),
                jsonField(
                    "unique",
                    jsonObj(jsonField("builtin", jsonObj(jsonField("id", jsonNum(builtinId)))))))));
  }

  private static JsonValue methodImpl(long index, JsonValue... externals) {
    return jsonObj(
        jsonField("interface_method_index", jsonNum(index)),
        jsonField("compiled_class_name", jsonObj(jsonField("none", jsonNull()))),
        jsonField("compiled_bytecode", jsonObj(jsonField("none", jsonNull()))),
        jsonField("captured_externals", jsonArr(externals)));
  }

  private static JsonValue interfaceImpl(String kind, JsonValue... methodImpls) {
    return jsonObj(
        jsonField("builtin_kind", jsonObj(jsonField("some", jsonStr(kind)))),
        jsonField("user_iface", jsonObj(jsonField("none", jsonNull()))),
        jsonField("method_impls", jsonArr(methodImpls)));
  }

  @Test
  public void test00300ImportTransitiveDefinitionset() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put(
        "module_b",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "nfr",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("func")),
                        mb.str("func_b"))),
                mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                            new TestMb.RecordPair("impl_body", mb.stage(mb.str("hello")))))),
                mb.call(
                    mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.scopeUse("f")))));
    modules.put(
        "module_a",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("func_b", mb.importExpr(mb.str("module_b"))),
                mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                mb.bind(
                    "nfr",
                    mb.call(
                        mb.field(mb.scopeUse("d"), mb.str("func")),
                        mb.str("func_a"))),
                mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                mb.call(
                    mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                    mb.recordOf(
                        TList.of(
                            new TestMb.RecordPair("arg", mb.typesNull()),
                            new TestMb.RecordPair("ret", mb.optSomeType(mb.typesString())),
                            new TestMb.RecordPair(
                                "impl_body",
                                mb.stage(
                                    mb.call(
                                        mb.lower(mb.scopeUse("func_b")),
                                        mb.literalNull())))))),
                mb.call(
                    mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                mb.return_(mb.scopeUse("f")))));
    modules.put(
        "main",
        new SourceModule.V1(
            mb.seqOf(
                mb.bind("func_a", mb.importExpr(mb.str("module_a"))),
                mb.call(
                    mb.log(),
                    mb.call(mb.scopeUse("func_a"), mb.literalNull())))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(switch (res) { case Result.Ok<?,?> o -> true; case Result.Err<?,?> e -> false; }, () -> "compile failed: " + res);
    @SuppressWarnings("unchecked")
    var ok = ((Result.Ok<Compile.CompileResult, TList<ExprErr>>) res).value();
    assertEquals(1, ok.logs().size());
    assertEquals("hello", ok.logs().get(0).message());
  }

  @Test
  public void test00400ImportCycle() {
    var mb = new TestMb();
    var modules = new TMap<String, SourceModule>();
    modules.put("module_a", new SourceModule.V1(mb.importExpr(mb.str("module_b"))));
    modules.put("module_b", new SourceModule.V1(mb.importExpr(mb.str("module_a"))));
    modules.put("main", new SourceModule.V1(mb.importExpr(mb.str("module_a"))));
    var gx = Helpers.newGx(modules);
    var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
    assertTrue(
        switch (res) { case Result.Ok<?,?> o -> false; case Result.Err<?,?> e -> true; }, () -> "expected import cycle error, got: " + res);
  }
}
