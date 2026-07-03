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
import org.jspecify.annotations.Nullable;

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
  private static final ClassName TYPE_METHOD_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeMethodDynamic");
  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");
  private static final String DYN_RUNTIME_INTNAME = "compilerxyz/eval/dynamic/DynRuntime";

  private static final ClassName TUPLE = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
  private static final ClassName TUPLE_ELEMENT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.tuple", "TupleElement", "Dynamic");

  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");

  public record PushField(String name, TypeRef ret, CodeBlock body) {}

  public record PushStructSpec(
      String name,
      long builtinUniqueId,
      TList<NamedTypeRef> dataFields,
      TList<PushField> fields,
      TList<PushMethod> methods,
      @Nullable String extraAspectFn) {

    public PushStructSpec(
        String name,
        long builtinUniqueId,
        TList<NamedTypeRef> dataFields,
        TList<PushField> fields,
        TList<PushMethod> methods) {
      this(name, builtinUniqueId, dataFields, fields, methods, null);
    }
  }

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
      writeTypeSentinel(outRoot, formatter, spec);
    }
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
    var sentinel = ClassName.get(GEN_PACKAGE, "Type" + spec.name() + "Dynamic");
    var cls =
        TypeSpec.classBuilder(spec.name())
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addJavadoc(
                "Push-struct data carrier for {@code $L}. Generated by"
                    + " {@code PushStructsGen}; port of {@code source/buildsrc/push_structs.rs}'s"
                    + " {@code PushStructSpec} for {@code $L} (Rust builtin_unique_id $L). The"
                    + " Rust struct also implements the v8 {@code Push} trait for ABI crossing;"
                    + " the JVM target has no v8 binding so only the data carrier is emitted"
                    + " here — compile-time aspect dispatch lives on {@link Type$LDynamic}. The"
                    + " static {@link #dynamicType()} accessor mirrors Rust's"
                    + " {@code <Name as Push>::dynamic_type()} (see {@code source/src/eval/push.rs})"
                    + " and returns a fresh sentinel instance; callers that need the compile-time"
                    + " type of a builtin returning this struct use it in the same position as the"
                    + " Rust trait method.\n",
                spec.name(),
                spec.name(),
                spec.builtinUniqueId(),
                spec.name());

    var ctor = MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC);
    for (var df : spec.dataFields()) {
      cls.addField(
          FieldSpec.builder(df.type().path(), df.name(), Modifier.PUBLIC, Modifier.FINAL).build());
      ctor.addParameter(ParameterSpec.builder(df.type().path(), df.name()).build());
      ctor.addStatement("this.$N = $N", df.name(), df.name());
    }
    cls.addMethod(ctor.build());

    cls.addMethod(
        MethodSpec.methodBuilder("dynamicType")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TYPE)
            .addJavadoc(
                "Compile-time type sentinel for this push struct. Port of Rust's"
                    + " {@code <$L as Push>::dynamic_type()} from {@code source/src/eval/push.rs};"
                    + " returns a fresh {@link $T} instance.\n",
                spec.name(),
                sentinel)
            .addStatement("return new $T()", sentinel)
            .build());

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static void writeTypeSentinel(Path outRoot, Formatter formatter, PushStructSpec spec)
      throws IOException {
    var sentinelName = "Type" + spec.name() + "Dynamic";
    var sentinel = ClassName.get(GEN_PACKAGE, sentinelName);
    var cls =
        TypeSpec.classBuilder(sentinelName)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addSuperinterface(TYPE)
            .addSuperinterface(ASPECT_FIELD)
            .addSuperinterface(ASPECT_CONSUME_DYNAMIC)
            .addSuperinterface(ASPECT_BIND)
            .addSuperinterface(ASPECT_BIND_DYNAMIC)
            .addSuperinterface(ASPECT_STACK_VALUE_DYNAMIC)
            .addJavadoc(
                "Compile-time type sentinel for the push struct {@code $L}. Generated by"
                    + " {@code PushStructsGen}; port of the {@code Type<Name>Dynamic} emission"
                    + " block in {@code source/buildsrc/push_structs.rs}.\n\n"
                    + "<p>Aspects implemented:\n"
                    + "<ul>\n"
                    + "  <li>{@link AspectField} — dispatches positional field access"
                    + " ({@code _0}, {@code _1}, …) for the derived fields and methods.\n"
                    + "  <li>Per-spec identity (Rust builtin_unique_id $L), exposed via"
                    + " {@link #IDENTITY}.\n"
                    + "</ul>\n"
                    + "<p>Method invocation in Rust is fulfilled by a v8 callback closure; the JVM"
                    + " equivalent (a dynamic-target dispatch table keyed by positional field"
                    + " name) is not yet built. Until it lands, the {@code field()} dispatch only"
                    + " emits a primitive field-get {@code JavaBytecode.LoweredSequence} on the underlying"
                    + " dynamic code via {@link DynUtil#buildFieldGetSequence} (matching"
                    + " Rust) — the runtime resolution of the resulting reference will need to be"
                    + " wired through the dispatch table when it ports.\n",
                spec.name(),
                spec.builtinUniqueId());

    cls.addField(
        FieldSpec.builder(UNIQUE, "IDENTITY", Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer("new $T($LL)", UNIQUE_BUILTIN, spec.builtinUniqueId())
            .addJavadoc(
                "Per-spec type-identity (Rust {@code $L_type_identity}).\n",
                snakeCase(spec.name()))
            .build());

    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PUBLIC).build());

    cls.addMethod(
        MethodSpec.methodBuilder("typeDesc")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(String.class)
            .addStatement("return $S", spec.name())
            .build());

    cls.addMethod(
        MethodSpec.methodBuilder("valueTypeOf")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(TYPE)
            .addStatement("return this")
            .build());

    cls.addMethod(buildAspectDispatch(spec, sentinel));

    cls.addMethod(buildFieldMethod(spec, sentinel));
    cls.addMethod(buildFieldTypeOpt(spec));
    cls.addMethod(buildFieldKeys());
    cls.addMethod(buildToDynamicMethod());
    cls.addMethod(buildBindMethod());
    cls.addMethod(buildBindDynamicMethod());
    cls.addMethod(buildToValueMethod(sentinel));

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  private static MethodSpec buildAspectDispatch(PushStructSpec spec, ClassName sentinel) {
    var m =
        MethodSpec.methodBuilder("typeAspect")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(OPT, ASPECT))
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(UNIQUE, "key")
            .addStatement("if (key.equals(IDENTITY)) return $T.some(this)", OPT)
            .addStatement("if (key.equals($T.ID)) return $T.some(this)", ASPECT_FIELD, OPT)
            .addStatement(
                "if (key.equals($T.ID)) return $T.some(this)", ASPECT_CONSUME_DYNAMIC, OPT)
            .addStatement("if (key.equals($T.ID)) return $T.some(this)", ASPECT_BIND, OPT)
            .addStatement("if (key.equals($T.ID)) return $T.some(this)", ASPECT_BIND_DYNAMIC, OPT)
            .addStatement(
                "if (key.equals($T.ID)) return $T.some(this)", ASPECT_STACK_VALUE_DYNAMIC, OPT);
    if (spec.extraAspectFn() != null) {
      var fn = spec.extraAspectFn();
      var lastDot = fn.lastIndexOf('.');
      if (lastDot < 0) {
        throw new IllegalStateException(
            "extraAspectFn must be a fully-qualified Class.method reference, got: " + fn);
      }
      var className = ClassName.bestGuess(fn.substring(0, lastDot));
      var methodName = fn.substring(lastDot + 1);
      m.addStatement("return $T.$L(xx, key)", className, methodName);
    } else {
      m.addStatement("return $T.none()", OPT);
    }
    return m.build();
  }

  private static MethodSpec buildToDynamicMethod() {
    return MethodSpec.methodBuilder("toDynamic")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(ERESULT_RT, CODE_DYNAMIC))
        .addParameter(EXPR_CONTEXT, "xx")
        .addParameter(QEXPR_ID, "loc")
        .addParameter(CODE_VALUE, "cv")
        .addStatement("return $T.dynamicConsumeToDynamic(xx, loc, cv)", DYN_UTIL)
        .build();
  }

  private static MethodSpec buildBindMethod() {
    return MethodSpec.methodBuilder("bind")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(ERESULT_RT, ASPECT_BIND_RESULT))
        .addParameter(EXPR_CONTEXT, "xx")
        .addParameter(QEXPR_ID, "loc")
        .addParameter(CODE_VALUE, "cv")
        .addStatement("return $T.bindViaTarget(xx, loc, cv)", DYN_UTIL)
        .build();
  }

  private static MethodSpec buildBindDynamicMethod() {
    return MethodSpec.methodBuilder("bindDynamic")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(
            ParameterizedTypeName.get(
                ERESULT_RT, ParameterizedTypeName.get(OPT, TARGET_BIND_SUB)))
        .addParameter(EXPR_CONTEXT, "xx")
        .addParameter(QEXPR_ID, "loc")
        .addParameter(CODE_VALUE, "cv")
        .addStatement("return $T.bindDynamicDefault(xx, loc, cv)", DYN_UTIL)
        .build();
  }

  private static MethodSpec buildToValueMethod(ClassName sentinel) {
    return MethodSpec.methodBuilder("toValue")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(CODE_VALUE)
        .addParameter(QEXPR_ID, "loc")
        .addParameter(CODE_DYNAMIC, "value")
        .addStatement("return $T.dynamicStackToValue(loc, value, this)", DYN_UTIL)
        .build();
  }

  private static MethodSpec buildFieldMethod(PushStructSpec spec, ClassName sentinel) {
    var m =
        MethodSpec.methodBuilder("field")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(ERESULT, CODE_VALUE))
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(QEXPR_ID, "loc")
            .addParameter(CODE_VALUE, "self")
            .addParameter(CODE_VALUE, "key");

    m.addStatement("var erb = new $T(loc)", ERB);
    m.beginControlFlow(
        "if (!(key.value() instanceof $T.Some<$T> keySome))", OPT, VALUE);
    m.addStatement(
        "return $T.err(loc, $S)",
        ERESULT,
        spec.name() + "::field: key has no compile-time value");
    m.endControlFlow();
    m.addStatement("var ckOpt = erb.mustRes($T.extractConstKey(xx, loc, keySome.value()))", EVAL);
    m.addStatement("if (ckOpt.isEmpty()) return $T.errs(erb.errors())", ERESULT);
    m.addStatement("var ck = ckOpt.get()");
    m.beginControlFlow("if (!(ck instanceof $T ckStr))", CONST_KEY_STR);
    m.addStatement(
        "return $T.err(loc, $S + ck.toCanonical())",
        ERESULT,
        spec.name() + ": unknown field ");
    m.endControlFlow();
    m.addStatement("var keyStr = ckStr.value()");

    m.addStatement("var dyn = self.code().dynamic()");
    m.beginControlFlow("if (dyn == null)");
    m.addStatement(
        "return $T.err(loc, $S)", ERESULT, spec.name() + ": self has no dynamic code");
    m.endControlFlow();

    for (var f : spec.fields()) {
      m.addCode(buildFieldGetterArm(sentinel, pushMethodName(f.name()), f));
    }
    for (var meth : spec.methods()) {
      m.addCode(buildMethodArm(sentinel, pushMethodName(meth.name()), meth));
    }
    m.addStatement(
        "return $T.err(loc, $S + keyStr)",
        ERESULT,
        spec.name() + ": unknown field '");
    return m.build();
  }

  private static CodeBlock buildFieldGetterArm(ClassName sentinel, String keyName, PushField f) {
    return CodeBlock.builder()
        .beginControlFlow("if (keyStr.equals($S))", keyName)
        .addStatement(
            "return $T.ok(new $T(self.code(), $T.some(new $T(new $T(), $L))))",
            ERESULT,
            CODE_VALUE,
            OPT,
            VALUE_DYNAMIC,
            VALUE_DYNAMIC_SOURCE_EXPR,
            f.ret().dynamicType())
        .endControlFlow()
        .build();
  }

  private static CodeBlock buildMethodArm(ClassName sentinel, String keyName, PushMethod meth) {
    var dynRuntimeMethod = meth.dynRuntimeMethod();
    if (dynRuntimeMethod == null) {
      throw new IllegalStateException(
          "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
    }
    return CodeBlock.builder()
        .beginControlFlow("if (keyStr.equals($S))", keyName)
        .addStatement(
            "return $T.ok(new $T(self.code(), $T.some(new $T(new $T(), $L))))",
            ERESULT,
            CODE_VALUE,
            OPT,
            VALUE_DYNAMIC,
            VALUE_DYNAMIC_SOURCE_EXPR,
            methodFunctionType(sentinel, meth, dynRuntimeMethod))
        .endControlFlow()
        .build();
  }

  private static MethodSpec buildFieldTypeOpt(PushStructSpec spec) {
    var m =
        MethodSpec.methodBuilder("fieldTypeOpt")
            .addModifiers(Modifier.PUBLIC)
            .addAnnotation(Override.class)
            .returns(ParameterizedTypeName.get(OPT, TYPE))
            .addParameter(EXPR_CONTEXT, "xx")
            .addParameter(CONST_KEY, "key");
    m.beginControlFlow("if (!(key instanceof $T.Str keyStr))", CONST_KEY);
    m.addStatement("return $T.none()", OPT);
    m.endControlFlow();
    for (var f : spec.fields()) {
      m.beginControlFlow("if (keyStr.value().equals($S))", pushMethodName(f.name()));
      m.addStatement("return $T.some($L)", OPT, f.ret().dynamicType());
      m.endControlFlow();
    }
    var sentinelForTypeOpt = ClassName.get(GEN_PACKAGE, "Type" + spec.name() + "Dynamic");
    for (var meth : spec.methods()) {
      m.beginControlFlow("if (keyStr.value().equals($S))", pushMethodName(meth.name()));
      var dynRuntimeMethod = meth.dynRuntimeMethod();
      if (dynRuntimeMethod == null) {
        throw new IllegalStateException(
            "PushMethod '" + meth.name() + "' has no dynRuntimeMethod set");
      }
      m.addStatement("return $T.some($L)", OPT, methodFunctionType(sentinelForTypeOpt, meth, dynRuntimeMethod));
      m.endControlFlow();
    }
    m.addStatement("return $T.none()", OPT);
    return m.build();
  }

  private static MethodSpec buildFieldKeys() {
    return MethodSpec.methodBuilder("fieldKeys")
        .addModifiers(Modifier.PUBLIC)
        .addAnnotation(Override.class)
        .returns(ParameterizedTypeName.get(TLIST, CONST_KEY))
        .addParameter(EXPR_CONTEXT, "xx")
        .addStatement("return new $T<>()", TLIST)
        .build();
  }

  private static CodeBlock methodFunctionType(
      ClassName sentinel, PushMethod m, String dynRuntimeMethodName) {
    var argDynType = argDynamicTypeExpr(m.args());
    var retDynType = unitOrDynamic(m.ret());
    var descriptor = methodDescriptorExpr(m);
    return CodeBlock.of(
        "new $T(new $T(), $L, $L, $T.of($S), $S, $L, true)",
        TYPE_METHOD_DYNAMIC,
        sentinel,
        argDynType,
        retDynType,
        JAVA_INTERNAL_NAME,
        DYN_RUNTIME_INTNAME,
        dynRuntimeMethodName,
        descriptor);
  }

  private static CodeBlock methodDescriptorExpr(PushMethod m) {
    var argListExpr = CodeBlock.builder();
    argListExpr.add("$T.of($T.fromClass($T.class)", TLIST, JAVA_DATA_DESCRIPTOR, EXPR_CONTEXT);
    argListExpr.add(", $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, ClassName.OBJECT);
    for (var a : m.args()) {
      argListExpr.add(", $T.fromClass($T.class)", JAVA_DATA_DESCRIPTOR, ClassName.OBJECT);
    }
    argListExpr.add(")");
    return CodeBlock.of(
        "$T.fromParts($T.fromClass($T.class), $L)",
        JAVA_METHOD_DESCRIPTOR,
        JAVA_DATA_DESCRIPTOR,
        ClassName.OBJECT,
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
    return CodeBlock.of("new $T($L)", TUPLE, elements.build());
  }

  private static CodeBlock unitOrDynamic(String ret) {
    if (ret.equals("()") || ret.isEmpty()) {
      return CodeBlock.of("new $T($T.<$T>of())", TUPLE, TLIST,
          ClassName.get("compilerxyz.eval.value.tuple", "TupleElement"));
    }
    return CodeBlock.of("$L", ret);
  }

  private static final ClassName T_DEFINITION_SET_BUILDER_DYN =
      ClassName.get(GEN_PACKAGE, "TypeDefinitionSetBuilderDynamic");
  private static final ClassName T_OPTION_PUSH_DYN =
      ClassName.get(GEN_PACKAGE, "TypeOptionPushDynamic");
  private static final ClassName T_SOURCE_FUNC_RESULT_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceFuncResultDynamic");
  private static final ClassName T_SOURCE_OBJECT_RESULT_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceObjectResultDynamic");
  private static final ClassName T_SOURCE_OBJECT_METHOD_RESULT_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceObjectMethodResultDynamic");
  private static final ClassName T_SOURCE_INTERFACE_RESULT_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceInterfaceResultDynamic");
  private static final ClassName T_SOURCE_INTERFACE_METHOD_RESULT_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceInterfaceMethodResultDynamic");
  private static final ClassName T_SOURCE_UNION_BUILDER_DYN =
      ClassName.get(GEN_PACKAGE, "TypeSourceUnionBuilderDynamic");

  private static final ClassName DEFINITION_SET =
      ClassName.get("compilerxyz.eval", "DefinitionSet");
  private static final ClassName TYPE_RT = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName ANY_TYPE =
      ClassName.get("compilerxyz.eval.push", "AnyType");
  private static final ClassName STAGED_EXPR =
      ClassName.get("compilerxyz.eval.push", "StagedExpr");

  private static final ClassName TYPE_SOURCE_FUNCTION_CONST =
      ClassName.get("compilerxyz.eval.value.source_function", "TypeSourceFunctionConst");
  private static final ClassName TYPE_SOURCE_FUNCTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_function", "TypeSourceFunctionDynamic");
  private static final ClassName SOURCE_INTERFACE_REF =
      ClassName.get("compilerxyz.eval.value.source_interface", "SourceInterfaceRef");
  private static final ClassName TYPE_SOURCE_INTERFACE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_interface", "TypeSourceInterfaceDynamic");
  private static final ClassName SOURCE_INTERFACE_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "SourceInterfaceMethodData");
  private static final ClassName SOURCE_OBJECT_REF =
      ClassName.get("compilerxyz.eval.value.source_object", "SourceObjectRef");
  private static final ClassName SOURCE_DATA_METHOD_DATA =
      ClassName.get("compilerxyz.eval.value.source_object", "SourceDataMethodData");
  private static final ClassName TYPE_SOURCE_OBJECT_REF_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_object", "TypeSourceObjectRefDynamic");
  private static final ClassName TYPE_SOURCE_OBJECT_NEW_CONST =
      ClassName.get("compilerxyz.eval.value.source_object_new", "TypeSourceObjectNewConst");
  private static final ClassName TYPE_SOURCE_OBJECT_NEW_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_object_new", "TypeSourceObjectNewDynamic");
  private static final ClassName SOURCE_UNION_DATA =
      ClassName.get("compilerxyz.eval.value.source_union", "SourceUnionData");
  private static final ClassName SOURCE_UNION_REF =
      ClassName.get("compilerxyz.eval.value.source_union", "SourceUnionRef");
  private static final ClassName TYPE_SOURCE_UNION_REF_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_union", "TypeSourceUnionRefDynamic");
  private static final ClassName SOURCE_UNION_NEW_REF =
      ClassName.get("compilerxyz.eval.value.source_union_new", "SourceUnionNewRef");
  private static final ClassName TYPE_SOURCE_UNION_NEW_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.source_union_new", "TypeSourceUnionNewDynamic");
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

  public static TList<PushStructSpec> specs() {
    return specs(TList.of());
  }

  public static TList<PushStructSpec> specs(TList<InterfaceSpec> traitSpecs) {
    var any = TypeRef.unit(TYPE_ANY_DYNAMIC);
    var string = TypeRef.unit(TYPE_STRING_DYNAMIC);
    var bool = TypeRef.unit(TYPE_BOOL_DYNAMIC);
    var anyTypeArg = ref(ANY_TYPE, CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC));
    var stagedExprArg = ref(STAGED_EXPR, CodeBlock.of("new $T()", TYPE_EXPR));
    var uniqueArg = ref(UNIQUE_RT, CodeBlock.of("new $T()", TYPE_UNIQUE_RT));
    var ifaceRefArg =
        ref(SOURCE_INTERFACE_REF, CodeBlock.of("new $T()", TYPE_SOURCE_INTERFACE_DYNAMIC));
    var recordArg =
        new TypeRef(
            ParameterizedTypeName.get(
                ClassName.get("java.util", "Map"),
                ClassName.get("compilerxyz.eval", "ConstKey"),
                ClassName.get("compilerxyz.eval", "Expr")),
            recordArgDyn());

    var empty = CodeBlock.of("");

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
                    dynExpr(T_SOURCE_FUNC_RESULT_DYN),
                    empty,
                    "defSetBuilderFunc"),
                new PushMethod(
                    "object",
                    TList.of(new NamedTypeRef("name", string)),
                    dynExpr(T_SOURCE_OBJECT_RESULT_DYN),
                    empty,
                    "defSetBuilderObject"),
                new PushMethod(
                    "build", TList.of(), dynExpr(T_DEFINITION_SET_BUILDER_DYN), empty,
                    "defSetBuilderBuild"),
                new PushMethod(
                    "union", TList.of(), dynExpr(T_SOURCE_UNION_BUILDER_DYN), empty,
                    "defSetBuilderUnion"),
                new PushMethod(
                    "interface", TList.of(), dynExpr(T_SOURCE_INTERFACE_RESULT_DYN), empty,
                    "defSetBuilderInterface"),
                new PushMethod(
                    "finish", TList.of(), dynExpr(T_DEFINITION_SET_BUILDER_DYN), empty,
                    "defSetBuilderFinish")),
            "compilerxyz.eval.definition_set.DefinitionSetExtraAspect.extraAspect");

    var optionPush =
        new PushStructSpec(
            "OptionPush",
            3001L,
            TList.of(new NamedTypeRef("is_some_inner", dataPath(ClassName.get("java.lang", "Boolean")))),
            TList.of(),
            TList.of(
                new PushMethod(
                    "is_some_", TList.of(), dynExpr(TYPE_BOOL_DYNAMIC), empty,
                    "optionPushGet")));

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
                    dynExpr(T_OPTION_PUSH_DYN),
                    empty,
                    "typeRefPushAspect")));

    var sourceFuncResult =
        new PushStructSpec(
            "SourceFuncResult",
            3003L,
            TList.of(new NamedTypeRef("func_ref", dataPath(TYPE_SOURCE_FUNCTION_CONST))),
            TList.of(
                new PushField(
                    "func",
                    ref(
                        TYPE_SOURCE_FUNCTION_CONST,
                        CodeBlock.of("new $T()", TYPE_SOURCE_FUNCTION_DYNAMIC)),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcFuncSetDescription"),
                new PushMethod(
                    "impl_",
                    TList.of(
                        new NamedTypeRef("arg", anyTypeArg),
                        new NamedTypeRef("ret", anyTypeArg),
                        new NamedTypeRef("impl_body", stagedExprArg)),
                    "()",
                    empty,
                    "srcFuncSetTypesAndImpl"),
                new PushMethod(
                    "set_exception_type",
                    TList.of(new NamedTypeRef("exception_type", anyTypeArg)),
                    "()",
                    empty,
                    "srcFuncSetExcType")));

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
                    "srcObjectMethodSetTypesAndImpl")));

    var sourceInterfaceResult =
        new PushStructSpec(
            "SourceInterfaceResult",
            3005L,
            TList.of(new NamedTypeRef("interface_ref", dataPath(SOURCE_INTERFACE_REF))),
            TList.of(
                new PushField(
                    "type_",
                    ref(
                        SOURCE_INTERFACE_REF,
                        CodeBlock.of("new $T()", TYPE_SOURCE_INTERFACE_DYNAMIC)),
                    empty)),
            TList.of(
                new PushMethod(
                    "set_description",
                    TList.of(new NamedTypeRef("description", string)),
                    "()",
                    empty,
                    "srcInterfaceSetDescription"),
                new PushMethod(
                    "func",
                    TList.of(new NamedTypeRef("name", string)),
                    dynExpr(T_SOURCE_INTERFACE_METHOD_RESULT_DYN),
                    empty,
                    "srcInterfaceAddMethod"),
                new PushMethod(
                    "set_is_aspect",
                    TList.of(new NamedTypeRef("is_aspect", bool)),
                    "()",
                    empty,
                    "srcInterfaceSetIsAspect")));

    var sourceObjectMethods = new TList<PushMethod>();
    sourceObjectMethods.add(
        new PushMethod(
            "set_description",
            TList.of(new NamedTypeRef("description", string)),
            "()",
            empty,
            "srcObjectSetDescription"));
    sourceObjectMethods.add(
        new PushMethod(
            "set_inner",
            TList.of(new NamedTypeRef("type_ref", anyTypeArg)),
            "()",
            empty,
            "srcObjectSetInnerType"));
    sourceObjectMethods.add(
        new PushMethod(
            "func",
            TList.of(new NamedTypeRef("name", string)),
            dynExpr(T_SOURCE_OBJECT_METHOD_RESULT_DYN),
            empty,
            "srcObjectAddMethod"));
    sourceObjectMethods.add(
        new PushMethod(
            "impl_interface",
            TList.of(
                new NamedTypeRef("iface_ref", ifaceRefArg),
                new NamedTypeRef("record", recordArg)),
            "()",
            empty,
            "srcObjectImplInterface"));
    sourceObjectMethods.add(
        new PushMethod(
            "aspect",
            TList.of(new NamedTypeRef("record", recordArg)),
            "new " + TYPE_SOURCE_INTERFACE_DYNAMIC.canonicalName() + "()",
            empty,
            "srcObjectImplAspect"));

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
              runtimeName));
    }
    var sourceObjectResult =
        new PushStructSpec(
            "SourceObjectResult",
            3006L,
            TList.of(new NamedTypeRef("object_ref", dataPath(SOURCE_OBJECT_REF))),
            TList.of(
                new PushField(
                    "type_ref_",
                    ref(
                        SOURCE_OBJECT_REF,
                        CodeBlock.of("new $T()", TYPE_SOURCE_OBJECT_REF_DYNAMIC)),
                    empty),
                new PushField(
                    "new_",
                    ref(
                        TYPE_SOURCE_OBJECT_NEW_CONST,
                        CodeBlock.of("new $T()", TYPE_SOURCE_OBJECT_NEW_DYNAMIC)),
                    empty)),
            TList.<PushMethod>from(sourceObjectMethods));

    var sourceUnionBuilder =
        new PushStructSpec(
            "SourceUnionBuilder",
            3007L,
            TList.of(new NamedTypeRef("data", dataPath(SOURCE_UNION_DATA))),
            TList.of(
                new PushField(
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
                    "srcUnionAddVariant")));

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
                    "srcInterfaceMethodSetTypes")));

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
