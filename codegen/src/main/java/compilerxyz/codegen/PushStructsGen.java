package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.PushMethod;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class PushStructsGen {
  private PushStructsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName VALUE = ClassName.get("compilerxyz.eval", "Value");
  private static final ClassName UNIQUE = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName UNIQUE_BUILTIN =
      ClassName.get("compilerxyz.eval", "Unique", "Builtin");
  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");
  private static final ClassName CODE = ClassName.get("compilerxyz.eval", "Code");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR =
      ClassName.get("compilerxyz.eval", "ConstKey", "Str");
  private static final ClassName EVAL = ClassName.get("compilerxyz.eval", "Eval");
  private static final ClassName ERB = ClassName.get("compilerxyz.eval", "Erb");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");

  private static final ClassName ASPECT = ClassName.get("compilerxyz.eval.aspect", "Aspect");
  private static final ClassName ASPECT_FIELD =
      ClassName.get("compilerxyz.gen", "AspectField");
  private static final ClassName ASPECT_CONSUME_DYNAMIC =
      ClassName.get("compilerxyz.gen", "AspectConsumeDynamic");
  private static final ClassName ASPECT_BIND =
      ClassName.get("compilerxyz.gen", "AspectBind");
  private static final ClassName ASPECT_BIND_RESULT =
      ClassName.get("compilerxyz.gen", "AspectBind", "BindResult");
  private static final ClassName ASPECT_BIND_DYNAMIC =
      ClassName.get("compilerxyz.gen", "AspectBindDynamic");
  private static final ClassName ASPECT_STACK_VALUE_DYNAMIC =
      ClassName.get("compilerxyz.gen", "AspectStackValueDynamic");
  private static final ClassName TARGET_BIND_SUB =
      ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub");
  private static final ClassName DYN_UTIL =
      ClassName.get("compilerxyz.eval.dynamic", "DynUtil");
  private static final ClassName ERESULT_RT = ClassName.get("compilerxyz.eval", "EResult");

  private static final ClassName CODE_DYNAMIC =
      ClassName.get("compilerxyz.eval.dynamic", "CodeDynamic");

  private static final ClassName VALUE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value", "ValueDynamic");
  private static final ClassName VALUE_DYNAMIC_SOURCE_EXPR =
      ClassName.get("compilerxyz.eval.value", "ValueDynamic", "Source", "Expr");

  private static final ClassName TYPE_FUNCTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeFunctionDynamic");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final String DYN_RUNTIME_INTNAME = "compilerxyz/eval/dynamic/DynRuntime";
  private static final ClassName JAVA_BYTECODE_SEQUENCE =
      ClassName.get("compilerxyz.eval.dynamic", "JavaBytecodeSequence");
  private static final ClassName JAVA_BYTECODE_UTILS =
      ClassName.get("compilerxyz.eval.dynamic", "JavaBytecodeUtils");
  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName OBJECT_DROP_KIND =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDropKind");
  private static final ClassName TYPE_OBJECT_CAST_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastRule");
  private static final ClassName TYPE_OBJECT_CAST_FROM_RULE =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectCastFromRule");
  private static final ClassName OBJECT_DATA_FIELD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectDataField");
  private static final ClassName OBJECT_METHOD =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectMethod");
  private static final ClassName TYPE_ID = ClassName.get("compilerxyz.eval", "TypeId");

  private static final ClassName TUPLE = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
  private static final ClassName TUPLE_ELEMENT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement", "Dynamic");

  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");

  public record PushField(String name, String javaFieldName, TypeRef ret, CodeBlock body) {}

  public record PushStructSpec(
      String name,
      long builtinUniqueId,
      TList<NamedTypeRef> dataFields,
      TList<PushField> fields,
      TList<PushMethod> methods) {}

  public static void run(
      Path outRoot,
      Formatter formatter,
      TList<PushStructSpec> specs,
      TSet<Long> builtinUniqueIds,
      TSet<Long> scopeIds)
      throws IOException {
    validate(specs, builtinUniqueIds);
    for (var spec : specs) {
      writeDataClass(outRoot, formatter, spec);
    }
    writeGenObjectTypes(outRoot, formatter, specs);
  }

  private static void validate(TList<PushStructSpec> specs, TSet<Long> builtinUniqueIds) {
    var names = new TSet<String>();
    for (var spec : specs) {
      if (!names.add(spec.name())) {
        throw new IllegalStateException("duplicate push struct name: " + spec.name());
      }
      if (!builtinUniqueIds.add(spec.builtinUniqueId())) {
        throw new IllegalStateException(
            "duplicate builtin_unique_id " + spec.builtinUniqueId() + " (in push_structs)");
      }
      var memberNames = new TSet<String>();
      for (var f : spec.fields()) {
        if (!memberNames.add(pushMethodName(f.name()))) {
          throw new IllegalStateException(
              "duplicate field/method name on " + spec.name() + ": " + f.name());
        }
      }
      for (var m : spec.methods()) {
        if (!memberNames.add(pushMethodName(m.name()))) {
          throw new IllegalStateException(
              "duplicate field/method name on " + spec.name() + ": " + m.name());
        }
      }
    }
  }

  private static void writeDataClass(Path outRoot, Formatter formatter, PushStructSpec spec)
      throws IOException {
    var chars = spec.name().toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    var factoryName = new String(chars);
    var genObjectTypes = ClassName.get(GEN_PACKAGE, "GenObjectTypes");
    var cls =
        TypeSpec.classBuilder(spec.name())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    for (var df : spec.dataFields()) {
      cls.addField(
          FieldSpec.builder(df.type().path(), df.name(), Modifier.PUBLIC, Modifier.FINAL).build());
      if (df.derivedFrom() != null) {
        ctor.addStatement("this.$N = $L", df.name(), df.derivedFrom());
      } else {
        ctor.addParameter(ParameterSpec.builder(df.type().path(), df.name()).build());
        ctor.addStatement("this.$N = $N", df.name(), df.name());
      }
    }
    cls.addMethod(ctor.build());

    cls.addMethod(
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE)
            .addStatement("return $T.$L()", genObjectTypes, factoryName)
            .build());

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static void writeGenObjectTypes(
      Path outRoot, Formatter formatter, TList<PushStructSpec> specs) throws IOException {
    var cls =
        TypeSpec.classBuilder("GenObjectTypes")
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL);
    cls.addMethod(
        MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());
    for (var spec : specs) {
      var chars = spec.name().toCharArray();
      chars[0] = Character.toLowerCase(chars[0]);
      var fieldName = new String(chars) + "_instance";
      cls.addField(
          FieldSpec.builder(TYPE, fieldName, Modifier.PRIVATE, Modifier.STATIC).build());
      cls.addMethod(buildObjectTypeFactory(spec, fieldName));
    }
    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static MethodSpec buildObjectTypeFactory(PushStructSpec spec, String fieldName) {
    var chars = spec.name().toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    var factoryName = new String(chars);
    var specClass = ClassName.get(GEN_PACKAGE, spec.name());
    var jvmInternal = specClass.canonicalName().replace('.', '/');

    var dataFieldsList = CodeBlock.builder();
    dataFieldsList.add("$T.<$T>of(", TLIST, OBJECT_DATA_FIELD);
    for (int i = 0; i < spec.fields().size(); i++) {
      var f = spec.fields().get(i);
      if (i > 0) dataFieldsList.add(", ");
      dataFieldsList.add(
          "new $T($S, $S, $T.fromClass($T.class), $L)",
          OBJECT_DATA_FIELD,
          pushMethodName(f.name()),
          f.javaFieldName(),
          JAVA_DATA_DESCRIPTOR,
          rawTypeName(f.ret().path()),
          f.ret().dynamicType());
    }
    dataFieldsList.add(")");

    var methodsList = CodeBlock.builder();
    methodsList.add("$T.<$T>of(", TLIST, OBJECT_METHOD);
    for (int i = 0; i < spec.methods().size(); i++) {
      var meth = spec.methods().get(i);
      var dynRuntimeMethod = meth.dynRuntimeMethod();
      if (dynRuntimeMethod == null) {
        throw new IllegalStateException(
            "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
      }
      if (i > 0) methodsList.add(", ");
      methodsList.add(
          "new $T($S, $L)",
          OBJECT_METHOD,
          pushMethodName(meth.name()),
          methodFunctionType(null, specClass, meth, dynRuntimeMethod));
    }
    methodsList.add(")");

    return MethodSpec.methodBuilder(factoryName)
        .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        .returns(TYPE)
        .beginControlFlow("if ($N == null)", fieldName)
        .addStatement(
            "$N = new $T(new $T.Builtin($LL), $S, $T.of($S), $T.fromClass($T.class), $L, $L, $T.DYNAMIC, $T.<$T>of(), $T.<$T>of(), $T.<$T>of(), $T.none(), $T.none(), $T.none())",
            fieldName,
            TYPE_OBJECT_DYNAMIC,
            TYPE_ID,
            spec.builtinUniqueId(),
            spec.name(),
            JAVA_INTERNAL_NAME,
            jvmInternal,
            JAVA_DATA_DESCRIPTOR,
            specClass,
            dataFieldsList.build(),
            methodsList.build(),
            OBJECT_DROP_KIND,
            TLIST,
            UNIQUE,
            TLIST,
            TYPE_OBJECT_CAST_RULE,
            TLIST,
            TYPE_OBJECT_CAST_FROM_RULE,
            OPT,
            OPT,
            OPT)
        .endControlFlow()
        .addStatement("return $N", fieldName)
        .build();
  }


  private static com.squareup.javapoet.TypeName rawTypeName(com.squareup.javapoet.TypeName t) {
    if (t instanceof com.squareup.javapoet.ParameterizedTypeName p) return p.rawType;
    return t;
  }

  private static CodeBlock methodFunctionType(
      ClassName sentinel, ClassName receiverClass, PushMethod m, String dynRuntimeMethodName) {
    var argDynType = argDynamicTypeExpr(m.args());
    var retDynType = unitOrDynamic(m.ret());
    var descriptor = methodDescriptorExpr(receiverClass, m);
    return CodeBlock.of(
        "new $T($L, $L, $T.of($S), $S, $L, true)",
        TYPE_FUNCTION_DYNAMIC,
        argDynType,
        retDynType,
        JAVA_INTERNAL_NAME,
        DYN_RUNTIME_INTNAME,
        dynRuntimeMethodName,
        descriptor);
  }

  private static CodeBlock methodDescriptorExpr(ClassName receiverClass, PushMethod m) {
    var argListExpr = CodeBlock.builder();
    argListExpr.add("$T.of($T.fromClass($T.class)", TLIST, JAVA_DATA_DESCRIPTOR, EXPR_CONTEXT);
    argListExpr.add(", $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, receiverClass);
    for (var a : m.args()) {
      argListExpr.add(", $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, rawTypeName(a.type().path()));
    }
    argListExpr.add(")");
    return CodeBlock.of(
        "$T.fromParts($T.fromClass($T.class), $L)",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        rawTypeName(m.retClass().path()),
        argListExpr.build());
  }

  private static CodeBlock argDynamicTypeExpr(TList<NamedTypeRef> args) {
    if (args.size() == 1) {
      return args.get(0).type().dynamicType();
    }
    var elements = CodeBlock.builder();
    elements.add("$T.<$T>of(", TLIST, ClassName.get("compilerxyz.eval.value.tuple", "TupleElement"));
    for (var i = 0; i < args.size(); i++) {
      if (i > 0) elements.add(", ");
      elements.add("new $T($L)", TUPLE_ELEMENT_DYNAMIC, args.get(i).type().dynamicType());
    }
    elements.add(")");
    return CodeBlock.of("new $T($T.none(), $L)", TUPLE, OPT, elements.build());
  }

  private static CodeBlock unitOrDynamic(String ret) {
    if (ret.equals("()") || ret.isEmpty()) {
      return CodeBlock.of("new $T($T.none(), $T.<$T>of())", TUPLE, OPT, TLIST,
          ClassName.get("compilerxyz.eval.value.tuple", "TupleElement"));
    }
    return CodeBlock.of("$L", ret);
  }

  private static final ClassName DEFINITION_SET =
      ClassName.get("compilerxyz.eval", "DefinitionSet");
  private static final ClassName TYPE_RT = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName ANY_TYPE =
      ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName STAGED_EXPR =
      ClassName.get("compilerxyz.eval.expr", "StagedExprCell");

  private static final ClassName TYPE_SOURCE_FUNCTION_CONST =
      ClassName.get("compilerxyz.eval.value.source_function", "TypeSourceFunctionConst");
  private static final ClassName SOURCE_FUNCTION_TYPE =
      ClassName.get("compilerxyz.eval.value.source_function", "SourceFunctionType");
  private static final ClassName INTERFACE_REF =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
  private static final ClassName SOURCE_INTERFACE_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceMethodData");
  private static final ClassName SOURCE_OBJECT_REF =
      ClassName.get("compilerxyz.eval.value.source_object", "TypeSourceObjectDynamic");
  private static final ClassName SOURCE_DATA_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_object", "SourceDataMethodData");
  private static final ClassName TYPE_SOURCE_OBJECT_REF_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_object", "TypeTypeSourceObjectDynamic");
  private static final ClassName TYPE_SOURCE_OBJECT_NEW_CONST =
      ClassName.get("compilerxyz.eval.value.source_object_new", "TypeSourceObjectNewDynamic");
  private static final ClassName TYPE_SOURCE_OBJECT_NEW_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_object_new", "TypeTypeSourceObjectNewDynamic");
  private static final ClassName SOURCE_UNION_DATA =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic");
  private static final ClassName SOURCE_UNION_REF =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic");
  private static final ClassName TYPE_SOURCE_UNION_REF_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeTypeSourceUnionDynamic");
  private static final ClassName SOURCE_UNION_NEW_REF =
      ClassName.get("compilerxyz.eval.value.source_union_new", "TypeSourceUnionNewDynamic");
  private static final ClassName TYPE_SOURCE_UNION_NEW_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_union_new", "TypeTypeSourceUnionNewDynamic");
  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName VALUE_STRING_CONST =
      ClassName.get("compilerxyz.eval.value.string", "ValueStringConst");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_ANY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.any", "TypeAnyDynamic");
  private static final ClassName TYPE_UNIQUE_RT =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName UNIQUE_RT = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");

  private static TypeRef dataPath(ClassName path) {
    return new TypeRef(path, CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC));
  }

  private static TypeRef ref(ClassName path, CodeBlock dynamicType) {
    return new TypeRef(path, dynamicType);
  }

  private static CodeBlock recordArgDyn() {
    return CodeBlock.of(
        "new $T(new $T(), new $T())", TYPE_MAP_DYNAMIC, TYPE_CONSTKEY_DYNAMIC, TYPE_EXPR);
  }

  private static String dynExpr(ClassName c) {
    return "new " + c.canonicalName() + "()";
  }

  private static String objRef(String specName) {
    var chars = specName.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return GEN_PACKAGE + ".GenObjectTypes." + new String(chars) + "()";
  }

  public static TList<PushStructSpec> specs() {
    return specs(TList.of());
  }

  public static TList<PushStructSpec> specs(TList<InterfaceSpec> traitSpecs) {
    var any = ref(ClassName.OBJECT, CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC));
    var string = ref(ClassName.get(String.class), CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC));
    var bool = ref(ClassName.get(Boolean.class), CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC));
    var anyTypeArg = ref(ANY_TYPE, CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC));
    var stagedExprArg = ref(STAGED_EXPR, CodeBlock.of("new $T()", TYPE_EXPR));
    var uniqueArg = ref(UNIQUE_RT, CodeBlock.of("new $T()", TYPE_UNIQUE_RT));
    var ifaceRefArg = ref(ClassName.OBJECT, CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC));
    var recordArg =
        new TypeRef(
            ParameterizedTypeName.get(
                ClassName.get("java.util", "Map"),
                ClassName.get("compilerxyz.eval", "ConstKey"),
                ClassName.get("compilerxyz.eval", "Expr")),
            recordArgDyn());

    var empty = CodeBlock.of("");

    var retObject = TypeRef.opaque(ClassName.OBJECT);
    var retSourceFuncResult = TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceFuncResult"));
    var retSourceObjectBuilder = TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceObjectBuilder"));
    var retSourceInterfaceResult =
        TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceInterfaceResult"));
    var retSourceInterfaceMethodResult =
        TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceInterfaceMethodResult"));
    var retSourceObjectMethodResult =
        TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceObjectMethodResult"));
    var retSourceUnionBuilder = TypeRef.opaque(ClassName.get(GEN_PACKAGE, "SourceUnionBuilder"));
    var retDefinitionSetBuilder =
        TypeRef.opaque(ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder"));
    var retOptionPush = TypeRef.opaque(ClassName.get(GEN_PACKAGE, "OptionPush"));
    var retInterfaceRef = TypeRef.opaque(INTERFACE_REF);
    var retSourceUnionNewRef = TypeRef.opaque(SOURCE_UNION_NEW_REF);

    var definitionSetBuilder =
        new PushStructSpec(
            "DefinitionSetBuilder",
            3009L,
            TList.of(new NamedTypeRef("inner", dataPath(DEFINITION_SET))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "func",
                    TList.of(new NamedTypeRef("name", string)),
                    objRef("SourceFuncResult"),
                    empty,
                    "defSetBuilderFunc",
                    retSourceFuncResult),
                new PushMethod(
                    "object",
                    TList.of(new NamedTypeRef("name", string)),
                    objRef("SourceObjectBuilder"),
                    empty,
                    "defSetBuilderObject",
                    retSourceObjectBuilder),
                new PushMethod(
                    "union", TList.of(), objRef("SourceUnionBuilder"), empty,
                    "defSetBuilderUnion", retSourceUnionBuilder),
                new PushMethod(
                    "interface", TList.of(), objRef("SourceInterfaceResult"), empty,
                    "defSetBuilderInterface", retSourceInterfaceResult),
                new PushMethod(
                    "finish", TList.of(), "()", empty,
                    "defSetBuilderFinish", retObject)));

    var optionPush =
        new PushStructSpec(
            "OptionPush",
            3001L,
            TList.of(new NamedTypeRef("is_some_inner", dataPath(ClassName.get("java.lang", "Boolean")))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "is_some_", TList.of(), dynExpr(TYPE_BOOL_DYNAMIC), empty,
                    "optionPushGet", retObject)));

    var typeRefPush =
        new PushStructSpec(
            "TypeRefPush",
            3002L,
            TList.of(new NamedTypeRef("type_ref", dataPath(TYPE_RT))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "aspect",
                    TList.of(new NamedTypeRef("unique", uniqueArg)),
                    objRef("OptionPush"),
                    empty,
                    "typeRefPushAspect",
                    retOptionPush)));

    var sourceFuncResult =
        new PushStructSpec(
            "SourceFuncResult",
            3003L,
            TList.of(new NamedTypeRef("func_ref", dataPath(TYPE_SOURCE_FUNCTION_CONST))),
            TList.of(
                new PushField(
                    "func",
                    "func_ref",
                    ref(
                        TYPE_SOURCE_FUNCTION_CONST,
                        CodeBlock.of("$T.dynamicType()", SOURCE_FUNCTION_TYPE)),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcFuncSetDescription",
                    retObject),
                new PushMethod(
                    "impl_",
                    TList.of(
                        new NamedTypeRef("arg", anyTypeArg),
                        new NamedTypeRef("ret", anyTypeArg),
                        new NamedTypeRef("impl_body", stagedExprArg)),
                    "()",
                    empty,
                    "srcFuncSetTypesAndImpl",
                    retObject),
                new PushMethod(
                    "set_exception_type",
                    TList.of(new NamedTypeRef("exception_type", anyTypeArg)),
                    "()",
                    empty,
                    "srcFuncSetExcType",
                    retObject)));

    var sourceObjectMethodResult =
        new PushStructSpec(
            "SourceObjectMethodResult",
            3004L,
            TList.of(new NamedTypeRef("method", dataPath(SOURCE_DATA_METHOD_DATA))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "impl_",
                    TList.of(
                        new NamedTypeRef("arg", anyTypeArg),
                        new NamedTypeRef("ret", anyTypeArg),
                        new NamedTypeRef("impl_body", stagedExprArg)),
                    "()",
                    empty,
                    "srcObjectMethodSetTypesAndImpl",
                    retObject)));

    var sourceInterfaceResult =
        new PushStructSpec(
            "SourceInterfaceResult",
            3005L,
            TList.of(new NamedTypeRef("interface_ref", dataPath(INTERFACE_REF))),
            TList.of(
                new PushField(
                    "type_",
                    "interface_ref",
                    ref(
                        INTERFACE_REF,
                        CodeBlock.of("$T.dynamicType()", INTERFACE_REF)),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcInterfaceSetDescription",
                    retObject),
                new PushMethod(
                    "func",
                    TList.of(new NamedTypeRef("name", string)),
                    objRef("SourceInterfaceMethodResult"),
                    empty,
                    "srcInterfaceAddMethod",
                    retSourceInterfaceMethodResult),
                new PushMethod(
                    "set_is_aspect",
                    TList.of(new NamedTypeRef("is_aspect", bool)),
                    "()",
                    empty,
                    "srcInterfaceSetIsAspect",
                    retObject)));

    var sourceObjectMethods = new TList<PushMethod>();
    sourceObjectMethods.add(
        new PushMethod(
            "set_description",
            TList.of(new NamedTypeRef("description", string)),
            "()",
            empty,
            "srcObjectSetDescription",
            retObject));
    sourceObjectMethods.add(
        new PushMethod(
            "set_inner",
            TList.of(new NamedTypeRef("type_ref", anyTypeArg)),
            "()",
            empty,
            "srcObjectSetInnerType",
            retObject));
    sourceObjectMethods.add(
        new PushMethod(
            "func",
            TList.of(new NamedTypeRef("name", string)),
            objRef("SourceObjectMethodResult"),
            empty,
            "srcObjectAddMethod",
            retSourceObjectMethodResult));
    sourceObjectMethods.add(
        new PushMethod(
            "impl_interface",
            TList.of(
                new NamedTypeRef("iface_ref", ifaceRefArg),
                new NamedTypeRef("record", recordArg)),
            "()",
            empty,
            "srcObjectImplInterface",
            retObject));
    sourceObjectMethods.add(
        new PushMethod(
            "aspect",
            TList.of(new NamedTypeRef("record", recordArg)),
            INTERFACE_REF.canonicalName() + ".dynamicType()",
            empty,
            "srcObjectImplAspect",
            retInterfaceRef));
    var retTypeSourceObjectNewConst = TypeRef.opaque(TYPE_SOURCE_OBJECT_NEW_CONST);
    sourceObjectMethods.add(
        new PushMethod(
            "new_",
            TList.of(),
            dynExpr(TYPE_SOURCE_OBJECT_NEW_DYNAMIC),
            empty,
            "srcObjectResultNew",
            retTypeSourceObjectNewConst));
    for (var traitSpec : traitSpecs) {
      if (traitSpec.registerInScope()) continue;
      var runtimeName =
          traitSpec.name().equals("value")
              ? "srcObjectImplValue"
              : traitSpec.name().equals("type")
                  ? "srcObjectImplTypeInterface"
                  : null;
      if (runtimeName == null) {
        throw new IllegalStateException(
            "PushStructsGen: no DynRuntime method mapped for trait '" + traitSpec.name() + "'");
      }
      sourceObjectMethods.add(
          new PushMethod(
              traitSpec.name(),
              TList.of(new NamedTypeRef("record", recordArg)),
              "()",
              empty,
              runtimeName,
              retObject));
    }
    var sourceObjectResult =
        new PushStructSpec(
            "SourceObjectBuilder",
            3006L,
            TList.of(new NamedTypeRef("type_", dataPath(SOURCE_OBJECT_REF))),
            TList.of(
                new PushField(
                    "type_ref_",
                    "type_",
                    ref(
                        SOURCE_OBJECT_REF,
                        CodeBlock.of("new $T()", TYPE_SOURCE_OBJECT_REF_DYNAMIC)),
                    empty)),
            TList.<PushMethod>from(sourceObjectMethods));

    var sourceUnionBuilder =
        new PushStructSpec(
            "SourceUnionBuilder",
            3007L,
            TList.of(new NamedTypeRef("type_", dataPath(SOURCE_UNION_DATA))),
            TList.of(
                new PushField(
                    "type_",
                    "type_",
                    ref(
                        SOURCE_UNION_REF,
                        CodeBlock.of("new $T()", TYPE_SOURCE_UNION_REF_DYNAMIC)),
                    empty)),
            TList.of(
                new PushMethod(
                    "variant",
                    TList.of(
                        new NamedTypeRef("tag", string),
                        new NamedTypeRef("type_ref", anyTypeArg)),
                    "new " + TYPE_SOURCE_UNION_NEW_DYNAMIC.canonicalName() + "()",
                    empty,
                    "srcUnionAddVariant",
                    retSourceUnionNewRef)));

    var sourceInterfaceMethodResult =
        new PushStructSpec(
            "SourceInterfaceMethodResult",
            3008L,
            TList.of(new NamedTypeRef("method", dataPath(SOURCE_INTERFACE_METHOD_DATA))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "impl_",
                    TList.of(
                        new NamedTypeRef("arg", anyTypeArg),
                        new NamedTypeRef("ret", anyTypeArg)),
                    "()",
                    empty,
                    "srcInterfaceMethodSetTypes",
                    retObject)));

    return TList.of(
        definitionSetBuilder,
        optionPush,
        typeRefPush,
        sourceFuncResult,
        sourceObjectMethodResult,
        sourceInterfaceResult,
        sourceObjectResult,
        sourceUnionBuilder,
        sourceInterfaceMethodResult);
  }

  private static String pushMethodName(String name) {
    return name.endsWith("_") ? name.substring(0, name.length() - 1) : name;
  }

  private static String snakeCase(String camel) {
    var out = new StringBuilder();
    for (var i = 0; i < camel.length(); i++) {
      var c = camel.charAt(i);
      if (Character.isUpperCase(c) && i > 0) out.append('_');
      out.append(Character.toLowerCase(c));
    }
    return out.toString();
  }
}
