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
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class PushStructsGen {
  private PushStructsGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName VALUE = ClassName.get("compilerxyz.gen", "Value");
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
  private static final ClassName TYPE_METHOD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
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

    if (spec.name().equals("DefinitionSetBuilder")) {
      addDefinitionSetBuilderMethods(cls);
    } else if (spec.name().equals("SourceFuncResult")) {
      addSourceFuncResultMethods(cls);
    } else if (spec.name().equals("SourceInterfaceResult")) {
      addSourceInterfaceResultMethods(cls);
    } else if (spec.name().equals("SourceObjectBuilder")) {
      addSourceObjectBuilderMethods(cls);
    } else if (spec.name().equals("SourceUnionBuilder")) {
      addSourceUnionBuilderMethods(cls);
    } else if (spec.name().equals("SourceInterfaceMethodResult")) {
      addSourceInterfaceMethodResultMethods(cls);
    } else if (spec.name().equals("SourceObjectMethodResult")) {
      addSourceObjectMethodResultMethods(cls);
    }

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static void addDefinitionSetBuilderMethods(TypeSpec.Builder cls) {
    var sourceFuncResult = ClassName.get(GEN_PACKAGE, "SourceFuncResult");
    var sourceObjectBuilder = ClassName.get(GEN_PACKAGE, "SourceObjectBuilder");
    var sourceUnionBuilder = ClassName.get(GEN_PACKAGE, "SourceUnionBuilder");
    var sourceInterfaceResult = ClassName.get(GEN_PACKAGE, "SourceInterfaceResult");
    var sourceFunctionData =
        ClassName.get("compilerxyz.eval.value.source_function", "SourceFunctionData");
    var typeSourceFunctionConst =
        ClassName.get("compilerxyz.eval.value.source_function", "TypeSourceFunctionConst");
    var typeSourceObjectDynamic =
        ClassName.get("compilerxyz.eval.value.source_object", "TypeSourceObjectDynamic");
    var typeSourceUnionDynamic =
        ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic");
    var interfaceData =
        ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
    var dynRuntime = ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");
    var objectClass = ClassName.get(Object.class);
    var stringClass = ClassName.get(String.class);

    cls.addMethod(
        MethodSpec.methodBuilder("func")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceFuncResult)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "name")
            .addStatement(
                "var data = new $T(this.inner.defSetId, name, this.inner)", sourceFunctionData)
            .addStatement("data.funcIdx = this.inner.functions.size()")
            .addStatement("this.inner.functions.add(data)")
            .addStatement("return new $T(new $T(data))", sourceFuncResult, typeSourceFunctionConst)
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("object")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceObjectBuilder)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "name")
            .addStatement("var identity = $T.freshUnique(xx)", dynRuntime)
            .addStatement(
                "var data = new $T(this.inner.defSetId, name, identity, this.inner)",
                typeSourceObjectDynamic)
            .addStatement("data.objIdx = this.inner.objects.size()")
            .addStatement("this.inner.objects.add(data)")
            .addStatement("return new $T(data)", sourceObjectBuilder)
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("union")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceUnionBuilder)
            .addParameter(EXPR_CONTEXT, "xx")
            .addStatement("var identity = $T.freshUnique(xx)", dynRuntime)
            .addStatement("var data = new $T(this.inner, identity)", typeSourceUnionDynamic)
            .addStatement("this.inner.unions.add(data)")
            .addStatement("return new $T(data)", sourceUnionBuilder)
            .build());

    var sourceInterfaceEmitter =
        ClassName.get("compilerxyz.eval.dynamic", "SourceInterfaceEmitter");
    cls.addMethod(
        MethodSpec.methodBuilder("interface_")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceInterfaceResult)
            .addParameter(EXPR_CONTEXT, "xx")
            .addStatement("var identity = $T.freshUnique(xx)", dynRuntime)
            .addStatement(
                "var iface = new $T(this.inner.defSetId, identity, null, this.inner)",
                interfaceData)
            .addStatement("var ifaceIndex = (long) this.inner.interfaces.size()")
            .addStatement("this.inner.interfaces.add(iface)")
            .addStatement(
                "iface.compiledBinaryName = $T.binaryNameFor(this.inner.defSetId, ifaceIndex)",
                sourceInterfaceEmitter)
            .addStatement("return new $T(iface)", sourceInterfaceResult)
            .build());

    var qexprIdCls = ClassName.get("compilerxyz.eval", "QExprId");
    var sourceIdCls = ClassName.get("compilerxyz.eval", "SourceId");
    cls.addMethod(
        MethodSpec.methodBuilder("finish")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addStatement(
                "var _loc = new $T(new $T(-1L), -1L)", qexprIdCls, sourceIdCls)
            .beginControlFlow(
                "for (long _i = 0; _i < this.inner.interfaces.size(); _i++)")
            .addStatement("var _iface = this.inner.interfaces.get((int) _i)")
            .addStatement("var _r = _iface.finish(_loc, _i)")
            .beginControlFlow("if (_r.hasErrors())")
            .addStatement(
                "throw new IllegalStateException(\"DefinitionSetBuilder.finish: \" + _r.errorsOrNull())")
            .endControlFlow()
            .endControlFlow()
            .addStatement("this.inner.finished = true")
            .build());
  }

  private static void addSourceFuncResultMethods(TypeSpec.Builder cls) {
    var typeSourceFunctionConst =
        ClassName.get("compilerxyz.eval.value.source_function", "TypeSourceFunctionConst");
    var stagedExprCell =
        ClassName.get("compilerxyz.eval.expr", "StagedExprCell");
    var valueClass = ClassName.get("compilerxyz.gen", "Value");
    var objectClass = ClassName.get(Object.class);
    var stringClass = ClassName.get(String.class);
    var illegalState = ClassName.get(IllegalStateException.class);

    cls.addMethod(
        MethodSpec.methodBuilder("get_ref")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeSourceFunctionConst)
            .addStatement("return this.func_ref")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("set_description")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "description")
            .beginControlFlow("if (this.func_ref.data().description != null)")
            .addStatement("throw new $T($S)", illegalState, "Function already has a description")
            .endControlFlow()
            .addStatement("this.func_ref.data().description = description")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("impl")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "argType")
            .addParameter(TYPE, "retType")
            .addParameter(stagedExprCell, "implCell")
            .addStatement("var data = this.func_ref.data()")
            .beginControlFlow("if (data.argType != null)")
            .addStatement("throw new $T($S)", illegalState, "Function already has an arg type")
            .endControlFlow()
            .addStatement("data.argType = argType")
            .beginControlFlow("if (data.retType != null)")
            .addStatement("throw new $T($S)", illegalState, "Function already has a ret type")
            .endControlFlow()
            .addStatement("data.retType = retType")
            .beginControlFlow("if (data.implExpr != null)")
            .addStatement("throw new $T($S)", illegalState, "Function already has an implementation")
            .endControlFlow()
            .addStatement("data.implExpr = ($T) implCell.take()", valueClass)
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("set_exception_type")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "excType")
            .beginControlFlow("if (this.func_ref.data().exceptionType != null)")
            .addStatement("throw new $T($S)", illegalState, "Function already has an exception type")
            .endControlFlow()
            .addStatement("this.func_ref.data().exceptionType = excType")
            .build());
  }

  private static void addSourceInterfaceResultMethods(TypeSpec.Builder cls) {
    var interfaceData =
        ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
    var interfaceMethodData =
        ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceMethodData");
    var sourceInterfaceMethodResult =
        ClassName.get(GEN_PACKAGE, "SourceInterfaceMethodResult");
    var definitionSet = ClassName.get("compilerxyz.eval", "DefinitionSet");
    var dynRuntime = ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");
    var objectClass = ClassName.get(Object.class);
    var stringClass = ClassName.get(String.class);
    var booleanClass = ClassName.get(Boolean.class);
    var illegalState = ClassName.get(IllegalStateException.class);

    cls.addMethod(
        MethodSpec.methodBuilder("get_ref")
            .addModifiers(Modifier.PUBLIC)
            .returns(interfaceData)
            .addStatement("return this.interface_ref")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("set_description")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "description")
            .beginControlFlow("if (this.interface_ref.description != null)")
            .addStatement("throw new $T($S)", illegalState, "Interface already has a description")
            .endControlFlow()
            .addStatement("this.interface_ref.description = description")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("func")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceInterfaceMethodResult)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "name")
            .addStatement("var ifaceData = this.interface_ref")
            .addStatement("var method = new $T(name)", interfaceMethodData)
            .addStatement("ifaceData.methods.add(method)")
            .addStatement("return new $T(method)", sourceInterfaceMethodResult)
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("set_is_aspect")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(booleanClass, "isAspect")
            .addStatement("this.interface_ref.isAspect = isAspect")
            .build());
  }

  private static void addSourceObjectBuilderMethods(TypeSpec.Builder cls) {
    var sourceObjectMethodResult =
        ClassName.get(GEN_PACKAGE, "SourceObjectMethodResult");
    var sourceDataMethodData =
        ClassName.get("compilerxyz.eval.value.source_object", "SourceDataMethodData");
    var sourceObjectInterfaceImplData =
        ClassName.get(
            "compilerxyz.eval.value.source_object", "SourceObjectInterfaceImplData");
    var sourceObjectInterfaceMethodImplData =
        ClassName.get(
            "compilerxyz.eval.value.source_object", "SourceObjectInterfaceMethodImplData");
    var interfaceData =
        ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");
    var interfaceSpec =
        ClassName.get("compilerxyz.eval.value.interface_spec", "InterfaceSpec");
    var typeInterfaceConst =
        ClassName.get("compilerxyz.eval.value.source_interface", "TypeSourceInterfaceConst");
    var typeObjectDynamic =
        ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
    var constKey = ClassName.get("compilerxyz.eval", "ConstKey");
    var constKeyStr = ClassName.get("compilerxyz.eval", "ConstKey", "Str");
    var expr = ClassName.get("compilerxyz.eval", "Expr");
    var optSome = ClassName.get("compilerxyz.util", "Opt", "Some");
    var dynRuntime = ClassName.get("compilerxyz.eval.dynamic", "DynRuntime");
    var objectClass = ClassName.get(Object.class);
    var stringClass = ClassName.get(String.class);
    var mapClass =
        ParameterizedTypeName.get(
            ClassName.get("java.util", "Map"), objectClass, objectClass);
    var illegalState = ClassName.get(IllegalStateException.class);

    cls.addMethod(
        MethodSpec.methodBuilder("set_description")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "description")
            .beginControlFlow("if (this.type_.description != null)")
            .addStatement("throw new $T($S)", illegalState, "Object already has a description")
            .endControlFlow()
            .addStatement("this.type_.description = description")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("set_inner")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "typeRef")
            .beginControlFlow("if (this.type_.underlyingType != null)")
            .addStatement("throw new $T($S)", illegalState, "Object already has an inner type")
            .endControlFlow()
            .addStatement("this.type_.underlyingType = typeRef")
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("func")
            .addModifiers(Modifier.PUBLIC)
            .returns(sourceObjectMethodResult)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "name")
            .addStatement("var dtData = this.type_")
            .addStatement("var method = new $T(name)", sourceDataMethodData)
            .addStatement("method.methodIdx = dtData.methods.size()")
            .addStatement("dtData.methods.add(method)")
            .addStatement("return new $T(method)", sourceObjectMethodResult)
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("impl_interface")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(interfaceData, "ifaceData")
            .addParameter(mapClass, "recordArg")
            .addStatement("var record = $T.recordToConstKeyExprMap(recordArg)", dynRuntime)
            .addStatement("var dtData = this.type_")
            .addStatement("var impl = new $T(ifaceData)", sourceObjectInterfaceImplData)
            .addStatement("impl.implIdx = dtData.interfaceImpls.size()")
            .beginControlFlow("for (int idx = 0; idx < ifaceData.methods.size(); idx++)")
            .addStatement("var im = ifaceData.methods.get(idx)")
            .addStatement("var ckey = new $T(im.name)", constKeyStr)
            .addStatement("var implExpr = record.get(ckey)")
            .beginControlFlow("if (implExpr == null)")
            .addStatement(
                "throw new $T($S + im.name + $S)",
                illegalState,
                "Missing implementation for interface method '",
                "'")
            .endControlFlow()
            .addStatement("var mImpl = new $T(idx)", sourceObjectInterfaceMethodImplData)
            .addStatement("mImpl.implExpr = implExpr")
            .addStatement("mImpl.methodImplIdx = impl.methodImpls.size()")
            .addStatement("impl.methodImpls.add(mImpl)")
            .endControlFlow()
            .addStatement("dtData.interfaceImpls.add(impl)")
            .build());

  }

  private static void addSourceUnionBuilderMethods(TypeSpec.Builder cls) {
    var typeSourceUnionNewDynamic =
        ClassName.get(
            "compilerxyz.eval.value.source_union_new", "TypeSourceUnionNewDynamic");
    var sourceUnionVariant =
        ClassName.get("compilerxyz.eval.value.source_union", "SourceUnionVariant");
    var constKeyStr = ClassName.get("compilerxyz.eval", "ConstKey", "Str");
    var prefixes = ClassName.get("compilerxyz.eval.dynamic", "Prefixes");
    var dynUtil = ClassName.get("compilerxyz.eval.dynamic", "DynUtil");
    var stringClass = ClassName.get(String.class);

    cls.addMethod(
        MethodSpec.methodBuilder("variant")
            .addModifiers(Modifier.PUBLIC)
            .returns(typeSourceUnionNewDynamic)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(stringClass, "tag")
            .addParameter(TYPE, "typeRef")
            .addStatement("var jsTag = $T.TAG_PREFIX + this.type_.nextTagId.next()", prefixes)
            .addStatement("var variantIndex = this.type_.variants.size()")
            .addStatement("var payloadType = $T.substituteBoxed(xx, typeRef)", dynUtil)
            .addStatement(
                "this.type_.variants.add(new $T(new $T(tag), jsTag, payloadType))",
                sourceUnionVariant,
                constKeyStr)
            .addStatement(
                "return new $T(this.type_, variantIndex)", typeSourceUnionNewDynamic)
            .build());
  }

  private static void addSourceInterfaceMethodResultMethods(TypeSpec.Builder cls) {
    var objectClass = ClassName.get(Object.class);
    var illegalState = ClassName.get(IllegalStateException.class);

    cls.addMethod(
        MethodSpec.methodBuilder("impl")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "argType")
            .addParameter(TYPE, "retType")
            .beginControlFlow("if (this.method.argType != null)")
            .addStatement(
                "throw new $T($S)", illegalState, "Interface method already has an arg type")
            .endControlFlow()
            .addStatement("this.method.argType = argType")
            .beginControlFlow("if (this.method.retType != null)")
            .addStatement(
                "throw new $T($S)", illegalState, "Interface method already has a ret type")
            .endControlFlow()
            .addStatement("this.method.retType = retType")
            .build());
  }

  private static void addSourceObjectMethodResultMethods(TypeSpec.Builder cls) {
    var stagedExprCell = ClassName.get("compilerxyz.eval.expr", "StagedExprCell");
    var valueClass = ClassName.get("compilerxyz.gen", "Value");
    var objectClass = ClassName.get(Object.class);
    var illegalState = ClassName.get(IllegalStateException.class);

    cls.addMethod(
        MethodSpec.methodBuilder("impl")
            .addModifiers(Modifier.PUBLIC)
            .returns(com.squareup.javapoet.TypeName.VOID)
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(TYPE, "argType")
            .addParameter(TYPE, "retType")
            .addParameter(stagedExprCell, "implCell")
            .beginControlFlow("if (this.method.argType != null)")
            .addStatement("throw new $T($S)", illegalState, "Method already has an arg type")
            .endControlFlow()
            .addStatement("this.method.argType = argType")
            .beginControlFlow("if (this.method.retType != null)")
            .addStatement("throw new $T($S)", illegalState, "Method already has a ret type")
            .endControlFlow()
            .addStatement("this.method.retType = retType")
            .beginControlFlow("if (this.method.implExpr != null)")
            .addStatement("throw new $T($S)", illegalState, "Method already has an implementation")
            .endControlFlow()
            .addStatement("this.method.implExpr = ($T) implCell.take()", valueClass)
            .build());
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

    var isSelfReferential = INSTANCE_METHOD_STRUCTS.contains(spec.name());
    var methodsList = CodeBlock.builder();
    if (isSelfReferential) {
      methodsList.add("$N", "__methods");
    } else {
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
            methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
      }
      methodsList.add(")");
    }

    var builder =
        MethodSpec.methodBuilder(factoryName)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE)
            .beginControlFlow("if ($N == null)", fieldName);
    if (isSelfReferential) {
      builder.addStatement("var __methods = new $T<$T>()", TLIST, OBJECT_METHOD);
    }
    builder.addStatement(
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
        OPT);
    if (isSelfReferential) {
      for (var meth : spec.methods()) {
        var dynRuntimeMethod = meth.dynRuntimeMethod();
        if (dynRuntimeMethod == null) {
          throw new IllegalStateException(
              "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
        }
        builder.addStatement(
            "__methods.add(new $T($S, $L))",
            OBJECT_METHOD,
            pushMethodName(meth.name()),
            methodFunctionType(spec, specClass, meth, dynRuntimeMethod));
      }
    }
    return builder.endControlFlow().addStatement("return $N", fieldName).build();
  }


  private static com.squareup.javapoet.TypeName rawTypeName(com.squareup.javapoet.TypeName t) {
    if (t instanceof com.squareup.javapoet.ParameterizedTypeName p) return p.rawType;
    return t;
  }

  private static final TSet<String> INSTANCE_METHOD_STRUCTS =
      TSet.of(
          "DefinitionSetBuilder",
          "SourceFuncResult",
          "SourceInterfaceMethodResult",
          "SourceInterfaceResult",
          "SourceObjectBuilder",
          "SourceObjectMethodResult",
          "SourceUnionBuilder");

  private static CodeBlock methodFunctionType(
      PushStructSpec spec, ClassName receiverClass, PushMethod m, String dynRuntimeMethodName) {
    var argDynType = argDynamicTypeExpr(m.args());
    var retDynType = unitOrDynamic(m.ret());
    if (INSTANCE_METHOD_STRUCTS.contains(spec.name())) {
      var receiverInternalName = receiverClass.canonicalName().replace('.', '/');
      var descriptor = methodDescriptorExprVirtual(receiverClass, m);
      var receiverField = lowerFirst(receiverClass.simpleName()) + "_instance";
      return CodeBlock.of(
          "new $T($N, $L, $L, $T.of($S), $S, $L, false, true)",
          TYPE_METHOD_DYNAMIC,
          receiverField,
          argDynType,
          retDynType,
          JAVA_INTERNAL_NAME,
          receiverInternalName,
          jvmMethodName(m.name()),
          descriptor);
    }
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

  private static String lowerFirst(String s) {
    var chars = s.toCharArray();
    chars[0] = Character.toLowerCase(chars[0]);
    return new String(chars);
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

  private static CodeBlock methodDescriptorExprVirtual(ClassName receiverClass, PushMethod m) {
    var argListExpr = CodeBlock.builder();
    argListExpr.add("$T.of($T.fromClass($T.class)", TLIST, JAVA_DATA_DESCRIPTOR, EXPR_CONTEXT);
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
    if (args.isEmpty()) {
      return CodeBlock.of(
          "new $T($T.none(), $T.<$T>of())",
          TUPLE,
          OPT,
          TLIST,
          ClassName.get("compilerxyz.eval.value.tuple", "TupleElement"));
    }
    var record = ClassName.get("compilerxyz.eval.value.record", "Record");
    var recordFieldDynamic =
        ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
    var torderedMap = ClassName.get("compilerxyz.util", "TOrderedMap");
    var pair = ClassName.get("compilerxyz.util", "Pair");
    var entries = CodeBlock.builder();
    entries.add("$T.ofEntries(", torderedMap);
    for (var i = 0; i < args.size(); i++) {
      var a = args.get(i);
      if (i > 0) entries.add(", ");
      entries.add(
          "new $T<>(new $T($S), new $T($L))",
          pair,
          CONST_KEY_STR,
          a.name(),
          recordFieldDynamic,
          a.type().dynamicType());
    }
    entries.add(")");
    return CodeBlock.of("new $T($T.none(), $L)", record, OPT, entries.build());
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
  private static final ClassName SOURCE_OBJECT_REF_TYPE =
      ClassName.get("compilerxyz.eval.value.source_object", "SourceObjectRefType");
  private static final ClassName TYPE_SOURCE_OBJECT_NEW_CONST =
      ClassName.get("compilerxyz.eval.value.source_object_new", "TypeSourceObjectNewDynamic");
  private static final ClassName SOURCE_OBJECT_NEW_REF_TYPE =
      ClassName.get("compilerxyz.eval.value.source_object_new", "SourceObjectNewRefType");
  private static final ClassName SOURCE_UNION_DATA =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic");
  private static final ClassName SOURCE_UNION_REF =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionDynamic");
  private static final ClassName SOURCE_UNION_REF_TYPE =
      ClassName.get("compilerxyz.eval.value.source_union", "SourceUnionRefType");
  private static final ClassName SOURCE_UNION_NEW_REF =
      ClassName.get("compilerxyz.eval.value.source_union_new", "TypeSourceUnionNewDynamic");
  private static final ClassName SOURCE_UNION_NEW_REF_TYPE =
      ClassName.get("compilerxyz.eval.value.source_union_new", "SourceUnionNewRefType");
  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName VALUE_STRING_CONST =
      ClassName.get("compilerxyz.eval.value.string", "ValueStringConst");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_UNIQUE_RT =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName UNIQUE_RT = ClassName.get("compilerxyz.eval", "Unique");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");

  private static CodeBlock recordArgDyn() {
    return DynamicTypes.composite(
        TYPE_MAP_DYNAMIC, DynamicTypes.leaf(TYPE_CONSTKEY_DYNAMIC), DynamicTypes.leaf(TYPE_EXPR));
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
    var string = TypeRef.leaf(TYPE_STRING_DYNAMIC);
    var bool = TypeRef.leaf(TYPE_BOOL_DYNAMIC);
    var anyTypeArg = TypeRef.leaf(TYPE_RT);
    var stagedExprArg = TypeRef.leaf(TYPE_EXPR);
    var uniqueArg = TypeRef.leaf(TYPE_UNIQUE_RT);
    var ifaceRefArg = TypeRef.leaf(INTERFACE_REF);
    var recordArg =
        new TypeRef(
            ParameterizedTypeName.get(
                ClassName.get("java.util", "Map"),
                ClassName.get("compilerxyz.eval", "ConstKey"),
                ClassName.get("compilerxyz.eval", "Expr")),
            recordArgDyn());

    var empty = CodeBlock.of("");

    var retVoid = TypeRef.opaque(TypeName.VOID);
    var retSourceFuncResult = TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceFuncResult"));
    var retSourceObjectBuilder = TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceObjectBuilder"));
    var retSourceInterfaceResult =
        TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceInterfaceResult"));
    var retSourceInterfaceMethodResult =
        TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceInterfaceMethodResult"));
    var retSourceObjectMethodResult =
        TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceObjectMethodResult"));
    var retSourceUnionBuilder = TypeRef.leaf(ClassName.get(GEN_PACKAGE, "SourceUnionBuilder"));
    var retDefinitionSetBuilder =
        TypeRef.leaf(ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder"));
    var retSourceUnionNewRef = TypeRef.leaf(SOURCE_UNION_NEW_REF);

    var definitionSetBuilder =
        new PushStructSpec(
            "DefinitionSetBuilder",
            3009L,
            TList.of(new NamedTypeRef("inner", TypeRef.leaf(DEFINITION_SET))),
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
                    "defSetBuilderFinish", retVoid)));

    var sourceFuncResult =
        new PushStructSpec(
            "SourceFuncResult",
            3003L,
            TList.of(new NamedTypeRef("func_ref", TypeRef.leaf(TYPE_SOURCE_FUNCTION_CONST))),
            TList.of(
                new PushField(
                    "func",
                    "func_ref",
                    TypeRef.leaf(TYPE_SOURCE_FUNCTION_CONST),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcFuncSetDescription",
                    retVoid),
                new PushMethod(
                    "impl_",
                    TList.of(
                        new NamedTypeRef("arg", anyTypeArg),
                        new NamedTypeRef("ret", anyTypeArg),
                        new NamedTypeRef("impl_body", stagedExprArg)),
                    "()",
                    empty,
                    "srcFuncSetTypesAndImpl",
                    retVoid),
                new PushMethod(
                    "set_exception_type",
                    TList.of(new NamedTypeRef("exception_type", anyTypeArg)),
                    "()",
                    empty,
                    "srcFuncSetExcType",
                    retVoid)));

    var sourceObjectMethodResult =
        new PushStructSpec(
            "SourceObjectMethodResult",
            3004L,
            TList.of(new NamedTypeRef("method", TypeRef.leaf(SOURCE_DATA_METHOD_DATA))),
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
                    retVoid)));

    var sourceInterfaceResult =
        new PushStructSpec(
            "SourceInterfaceResult",
            3005L,
            TList.of(new NamedTypeRef("interface_ref", TypeRef.leaf(INTERFACE_REF))),
            TList.of(
                new PushField(
                    "type_",
                    "interface_ref",
                    TypeRef.leaf(INTERFACE_REF),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcInterfaceSetDescription",
                    retVoid),
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
                    retVoid)));

    var sourceObjectMethods = new TList<PushMethod>();
    sourceObjectMethods.add(
        new PushMethod(
            "set_description",
            TList.of(new NamedTypeRef("description", string)),
            "()",
            empty,
            "srcObjectSetDescription",
            retVoid));
    sourceObjectMethods.add(
        new PushMethod(
            "set_inner",
            TList.of(new NamedTypeRef("type_ref", anyTypeArg)),
            "()",
            empty,
            "srcObjectSetInnerType",
            retVoid));
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
            retVoid));
    var retTypeSourceObjectNewConst = TypeRef.leaf(TYPE_SOURCE_OBJECT_NEW_CONST);
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
              retVoid));
    }
    var sourceObjectResult =
        new PushStructSpec(
            "SourceObjectBuilder",
            3006L,
            TList.of(
                new NamedTypeRef("type_", TypeRef.leaf(SOURCE_OBJECT_REF)),
                new NamedTypeRef(
                    "new_ref",
                    retTypeSourceObjectNewConst,
                    CodeBlock.of("new $T(type_)", TYPE_SOURCE_OBJECT_NEW_CONST))),
            TList.of(
                new PushField(
                    "type_ref_",
                    "type_",
                    TypeRef.leaf(SOURCE_OBJECT_REF),
                    empty),
                new PushField(
                    "new",
                    "new_ref",
                    retTypeSourceObjectNewConst,
                    empty)),
            TList.<PushMethod>from(sourceObjectMethods));

    var sourceUnionBuilder =
        new PushStructSpec(
            "SourceUnionBuilder",
            3007L,
            TList.of(new NamedTypeRef("type_", TypeRef.leaf(SOURCE_UNION_DATA))),
            TList.of(
                new PushField(
                    "type_",
                    "type_",
                    TypeRef.leaf(SOURCE_UNION_REF),
                    empty)),
            TList.of(
                new PushMethod(
                    "variant",
                    TList.of(
                        new NamedTypeRef("tag", string),
                        new NamedTypeRef("type_ref", anyTypeArg)),
                    SOURCE_UNION_NEW_REF_TYPE.canonicalName() + ".dynamicType()",
                    empty,
                    "srcUnionAddVariant",
                    retSourceUnionNewRef)));

    var sourceInterfaceMethodResult =
        new PushStructSpec(
            "SourceInterfaceMethodResult",
            3008L,
            TList.of(new NamedTypeRef("method", TypeRef.leaf(SOURCE_INTERFACE_METHOD_DATA))),
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
                    retVoid)));

    return TList.of(
        definitionSetBuilder,
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

  private static String jvmMethodName(String sourceName) {
    var base = pushMethodName(sourceName);
    return switch (base) {
      case "interface", "new", "class", "return", "if", "else", "for", "while",
              "switch", "case", "default", "break", "continue", "true", "false", "null",
              "void", "int", "long", "boolean", "float", "double", "char", "byte", "short",
              "this", "super", "static", "final", "public", "private", "protected",
              "abstract", "synchronized", "volatile", "transient", "native", "throws",
              "throw", "try", "catch", "finally", "import", "package", "extends", "implements",
              "instanceof", "do", "goto", "const", "enum" -> base + "_";
      default -> base;
    };
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
