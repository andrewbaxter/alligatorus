package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.codegen.util.DynamicTypes;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

final class AspectObjectsGen {
  private AspectObjectsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName TYPE_METHOD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName OBJECT_DATA_FIELD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField");
  private static final ClassName OBJECT_DROP_KIND =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDropKind");
  private static final ClassName OBJECT_ASPECT_SOURCE =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource");
  private static final ClassName OBJECT_ASPECT_SOURCE_STATIC =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectAspectSource", "Static");
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName TYPE_OBJECT_LIFT =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectLift");
  private static final ClassName CAST_RULE_CALL_TO_INTERFACE_SPEC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule", "CallToInterfaceSpec");
  private static final ClassName CAST_RULE_CALL_BY_DEST_IDENTITY =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule", "CallByDestIdentity");

  private static final ClassName TYPE_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "TypeId", "Builtin");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName TORDERED_MAP = ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR = ClassName.get("compilerxyz.eval", "ConstKey", "Str");
  private static final ClassName RECORD = ClassName.get("compilerxyz.eval.value.record", "Record");
  private static final ClassName RECORD_FIELD =
      ClassName.get("compilerxyz.eval.value.record", "RecordField");
  private static final ClassName RECORD_FIELD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");

  private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");

  private static final ClassName ASPECT_BIND = ClassName.get("compilerxyz.gen", "AspectBind");
  private static final ClassName GEN_IDENTITIES =
      ClassName.get("compilerxyz.gen", "GenIdentities");
  private static final ClassName DYN_RUNTIME_HELPER =
      ClassName.get(
          "compilerxyz.eval.value.object_", "TypeObjectCastRule", "DynRuntimeHelper");
  private static final ClassName DYN_RUNTIME =
      ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");
  private static final ClassName TYPE_OBJECT_DYNAMIC_TYPE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName INTERFACE_SPEC_TYPE_TYPE =
      ClassName.get("compilerxyz.eval.value.interface_spec", "TypeInterfaceSpec");
  private static final ClassName INTERFACE_DATA_CLASS =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");

  private static final ClassName TYPE_CODE_VALUE_EXTERN_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code_value_extern", "TypeCodeValueExternDynamic");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");
  private static final ClassName BIND_RESULT_TYPE =
      ClassName.get("compilerxyz.eval.value.bind_result", "BindResultType");

  static void run(Path outRoot, Formatter formatter) throws IOException {
    var cls =
        TypeSpec.classBuilder("GenAspectBindObject")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    cls.addMethod(
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE_OBJECT_DYNAMIC)
            .addStatement("return Holder.INSTANCE")
            .build());

    var holder =
        TypeSpec.classBuilder("Holder").addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL);
    holder.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
    holder.addField(
        FieldSpec.builder(TYPE_OBJECT_DYNAMIC, "INSTANCE", Modifier.STATIC, Modifier.FINAL)
            .initializer("build()")
            .build());

    var build = MethodSpec.methodBuilder("build").addModifiers(Modifier.PRIVATE, Modifier.STATIC).returns(TYPE_OBJECT_DYNAMIC);
    build.addStatement(
        "var jvmInterface = $T.of($S)", JAVA_INTERNAL_NAME, "compilerxyz/gen/AspectBind");
    build.addStatement(
        "var jvmInterfaceDesc = $T.fromInternalName(jvmInterface)", JAVA_DATA_DESCRIPTOR);

    build.addStatement("var methods = new $T<$T>()", TLIST, OBJECT_METHOD);
    build.addStatement("var castRules = new $T<$T>()", TLIST, TYPE_OBJECT_CAST_RULE);
    build.addStatement(
        "castRules.add(new $T(new $T($T.fromClass($T.class), $S, "
            + "$T.fromParts($T.fromClass($T.class), "
            + "$T.of($T.fromClass($T.class))), false)))",
        CAST_RULE_CALL_TO_INTERFACE_SPEC,
        DYN_RUNTIME_HELPER,
        JAVA_INTERNAL_NAME,
        DYN_RUNTIME,
        "objectToInterfaceSpec",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        INTERFACE_SPEC_TYPE_TYPE,
        TLIST,
        JAVA_DATA_DESCRIPTOR,
        TYPE_OBJECT_DYNAMIC_TYPE);
    build.addStatement(
        "castRules.add(new $T($T.INTERFACE_REF_TYPE_IDENTITY, new $T($T.fromClass($T.class), $S, "
            + "$T.fromParts($T.fromClass($T.class), "
            + "$T.of($T.fromClass($T.class))), false)))",
        CAST_RULE_CALL_BY_DEST_IDENTITY,
        GEN_IDENTITIES,
        DYN_RUNTIME_HELPER,
        JAVA_INTERNAL_NAME,
        DYN_RUNTIME,
        "objectToInterfaceData",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        INTERFACE_DATA_CLASS,
        TLIST,
        JAVA_DATA_DESCRIPTOR,
        TYPE_OBJECT_DYNAMIC_TYPE);

    var typeIdInit = CodeBlock.of("new $T(9101L)", TYPE_ID_BUILTIN);
    var dataFieldsInit = CodeBlock.of("new $T<$T>()", TLIST, OBJECT_DATA_FIELD);
    var identityAspectsInit = CodeBlock.of("new $T<$T>()", TLIST, UNIQUE);
    var castFromRulesInit = CodeBlock.of("new $T<$T>()", TLIST, TYPE_OBJECT_CAST_FROM_RULE);
    var liftNone = CodeBlock.of("$T.<$T>none()", OPT, TYPE_OBJECT_LIFT);
    var aspectSourceInit =
        CodeBlock.of(
            "$T.<$T>some(new $T($T.ID))",
            OPT,
            OBJECT_ASPECT_SOURCE,
            OBJECT_ASPECT_SOURCE_STATIC,
            ASPECT_BIND);
    var interfaceRepresentedInit = CodeBlock.of("$T.some(jvmInterface)", OPT);

    build.addStatement(
        "var instance = new $T($L, $S, jvmInterface, jvmInterfaceDesc, $L, methods, "
            + "$T.DYNAMIC, $L, castRules, $L, $L, $L, $L)",
        TYPE_OBJECT_DYNAMIC,
        typeIdInit,
        "aspect_bind",
        dataFieldsInit,
        OBJECT_DROP_KIND,
        identityAspectsInit,
        castFromRulesInit,
        liftNone,
        aspectSourceInit,
        interfaceRepresentedInit);

    build.addStatement(
        "var bindDesc = $T.fromParts($T.fromClass($T.class), $T.of("
            + "$T.fromClass($T.class), $T.fromClass($T.class), $T.fromClass($T.class)))",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        ERESULT,
        TLIST,
        JAVA_DATA_DESCRIPTOR,
        EXPR_CONTEXT,
        JAVA_DATA_DESCRIPTOR,
        QEXPR_ID,
        JAVA_DATA_DESCRIPTOR,
        CODE_VALUE);
    build.addStatement("var argRecordFields = new $T<$T, $T>()", TORDERED_MAP, CONST_KEY, RECORD_FIELD);
    build.addStatement(
        "argRecordFields.put(new $T($S), new $T($L))",
        CONST_KEY_STR,
        "xx",
        RECORD_FIELD_DYNAMIC,
        DynamicTypes.leaf(EXPR_CONTEXT));
    build.addStatement(
        "argRecordFields.put(new $T($S), new $T($L))",
        CONST_KEY_STR,
        "loc",
        RECORD_FIELD_DYNAMIC,
        DynamicTypes.leaf(QEXPR_ID));
    build.addStatement(
        "argRecordFields.put(new $T($S), new $T($L))",
        CONST_KEY_STR,
        "cv",
        RECORD_FIELD_DYNAMIC,
        DynamicTypes.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC));
    build.addStatement("var argRecord = new $T($T.none(), argRecordFields)", RECORD, OPT);
    build.addStatement(
        "var retType = $L",
        DynamicTypes.composite(TYPE_ERESULT_DYNAMIC, DynamicTypes.leaf(BIND_RESULT_TYPE)));
    build.addStatement(
        "var bindMethod = new $T(instance, argRecord, retType, jvmInterface, $S, bindDesc, true, false)",
        TYPE_METHOD_DYNAMIC,
        "bind");
    build.addStatement("methods.add(new $T($S, bindMethod))", OBJECT_METHOD, "bind");
    build.addStatement("return instance");

    holder.addMethod(build.build());
    cls.addType(holder.build());

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }
}
