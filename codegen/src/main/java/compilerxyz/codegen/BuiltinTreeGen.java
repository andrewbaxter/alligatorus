package compilerxyz.codegen;

import com.palantir.javaformat.java.Formatter;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.ParameterSpec;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;

public final class BuiltinTreeGen {
  private BuiltinTreeGen() {}

  private static final String GEN_PACKAGE = "compilerxyz.gen";

  static final long ASPECT_COMPILE_SCOPE_ID = 7023;
  static final long CAST_VALUE_COMPILE_SCOPE_ID = 7024;
  static final long THROW_COMPILE_SCOPE_ID = 7025;
  static final long PANIC_COMPILE_SCOPE_ID = 7026;

  private static final ClassName BINDING_ID = ClassName.get("compilerxyz.eval", "BindingId");
  private static final ClassName BINDING = ClassName.get("compilerxyz.gen", "Binding");
  private static final ClassName ERESULT = ClassName.get("compilerxyz.eval", "EResult");
  private static final ClassName QEXPR_ID = ClassName.get("compilerxyz.eval", "QExprId");
  private static final ClassName CODE_RT = ClassName.get("compilerxyz.eval", "Code");
  private static final ClassName CODE_VALUE = ClassName.get("compilerxyz.eval", "CodeValue");
  private static final ClassName OPT = ClassName.get("compilerxyz.util", "Opt");
  private static final ClassName CONST_KEY = ClassName.get("compilerxyz.eval", "ConstKey");
  private static final ClassName CONST_KEY_STR =
      ClassName.get("compilerxyz.eval", "ConstKey", "Str");
  private static final ClassName TORDERED_MAP =
      ClassName.get("compilerxyz.util", "TOrderedMap");
  private static final ClassName ARGUMENT_ENTRY =
      ClassName.get("compilerxyz.eval.value.argrecord", "ArgumentEntry");
  private static final ClassName ARGUMENT_RECORD =
      ClassName.get("compilerxyz.eval.value.argrecord", "ArgumentRecord");
  private static final ClassName ARGUMENT_RECORD_BINDING =
      ClassName.get("compilerxyz.eval.value.argrecord", "ArgumentRecordBinding");
  private static final ClassName BINDING_UNIQUE_DUAL_MODE =
      ClassName.get("compilerxyz.eval.value.unique", "BindingUniqueDualMode");
  private static final ClassName TYPE_UNIQUE_RT =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName ASPECT_BIND =
      ClassName.get("compilerxyz.gen", "AspectBind");
  private static final ClassName ASPECT_INITIAL_BIND =
      ClassName.get("compilerxyz.gen", "AspectInitialBind");
  private static final ClassName VALUE_OBJECT_CONST =
      ClassName.get("compilerxyz.eval.value.object_", "ValueObjectConst");
  private static final ClassName OBJECT_CONST_OF_TYPE =
      ClassName.get("compilerxyz.eval.value.object_", "ObjectConstOfType");
  private static final ClassName TYPE_TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeTypeObjectDynamic");
  private static final CodeBlock OCT_TYPE_EXPR =
      CodeBlock.of("$T.dynamicType()", OBJECT_CONST_OF_TYPE);
  private static final CodeBlock TTOD_TYPE_EXPR =
      CodeBlock.of("$T.dynamicType()", TYPE_TYPE_OBJECT_DYNAMIC);
  private static final ClassName IMPORT_INSTALL =
      ClassName.get("compilerxyz.eval", "ImportInstall");
  private static final ClassName TYPE_INTERFACE_CONST =
      ClassName.get("compilerxyz.eval.value.source_interface", "TypeSourceInterfaceConst");
  private static final ClassName TYPE_FUNCTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeFunctionDynamic");
  private static final ClassName BINDING_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "BindingId", "Builtin");
  private static final ClassName IMPORT_STATE_ID =
      ClassName.get("compilerxyz.eval", "ImportStateId");
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName EXTERNAL = ClassName.get("compilerxyz.eval", "External");
  private static final ClassName INTERFACE_DATA =
      ClassName.get("compilerxyz.eval.value.source_interface", "InterfaceData");

  private static final ClassName GEN_BUILTIN_INTERFACES =
      ClassName.get(GEN_PACKAGE, "GenBuiltinInterfaces");

  private static final ClassName TYPE_THROW_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.throw_", "TypeThrowDynamic");
  private static final ClassName TYPE_PANIC_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.panic", "TypePanicDynamic");
  private static final ClassName TYPE_FUNCTION_ASPECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeFunctionAspectDynamic");
  private static final ClassName TYPE_FUNCTION_CAST_VALUE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.function", "TypeFunctionCastValueDynamic");

  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName TYPE_NUMBER_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.number", "TypeNumberDynamic");
  private static final ClassName TYPE_NULL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.null_", "TypeNullDynamic");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");
  private static final ClassName ASPECT_INTERFACE =
      ClassName.get("compilerxyz.eval.aspect", "Aspect");
  private static final ClassName CODE_DYN_HANDLE_TYPE =
      ClassName.get("compilerxyz.eval.value.code", "CodeDynHandleType");
  private static final ClassName TYPE_CODE_VALUE_EXTERN_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code_value_extern", "TypeCodeValueExternDynamic");
  private static final ClassName TYPE_LIST_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.list", "TypeListDynamic");
  private static final ClassName TYPE_LIST_CONSTRUCTOR_CONST =
      ClassName.get("compilerxyz.eval.value.list", "TypeListConstructorConst");
  private static final ClassName TYPE_MAP_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapDynamic");
  private static final ClassName TYPE_MAP_CONSTRUCTOR_CONST =
      ClassName.get("compilerxyz.eval.value.map", "TypeMapConstructorConst");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_OPTION_CONSTRUCTOR_CONST =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionConstructorConst");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");
  private static final ClassName TYPE_ERESULT_CONSTRUCTOR_CONST =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultConstructorConst");
  private static final ClassName CODE_INTERFACE = ClassName.get("compilerxyz.eval", "Code");
  private static final ClassName VALUE_INTERFACE = ClassName.get("compilerxyz.gen", "Value");

  private static final ClassName UNIQUE_CLASS = ClassName.get("compilerxyz.eval", "Unique");

  public sealed interface NodeSpec {}

  public record Child(String name, NodeSpec node) {}

  public record Branch(TList<Child> children) implements NodeSpec {}

  public record Leaf(LeafSpec leaf) implements NodeSpec {}

  public sealed interface LeafSpec {

    /**
     * Compile-time constant value. {@code innerExpr} is the wrapped Type reference; {@code
     * valueTypeExpr} is the source-level type of the resulting value (both required, no default).
     */
    record Const(CodeBlock innerExpr, CodeBlock valueTypeExpr) implements LeafSpec {}

    record InitialBind(long dynamicKeyScopeId, CodeBlock typeExpr) implements LeafSpec {}

    record AspectId(long builtinScopeId, String aspectName) implements LeafSpec {}

    record Interface(InterfaceSpec spec) implements LeafSpec {}

    record Function(
        long builtinScopeId,
        TList<NamedTypeRef> args,
        CodeBlock retDynamicType)
        implements LeafSpec {}

    record Constructor(long builtinScopeId, CodeBlock typeExpr) implements LeafSpec {}
  }

  public static void run(
      Path outRoot, Formatter formatter, TSet<Long> builtinUniqueIds, TSet<Long> scopeIds)
      throws IOException {
    var spec = canonicalSpec();
    validate(spec, scopeIds);

    var cls =
            TypeSpec.classBuilder("GenBuiltinTree")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL);

    cls.addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build());

    cls.addMethod(installRootFactory(spec));

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);
  }

  /**
   * Mutable counter for allocating unique local-variable names in the
   * generated installRoot method. Each emit method appends statements to
   * the shared body and returns the names of the (binding, type) locals it
   * left in scope.
   */
  private static final class EmitState {
    int counter = 0;
    final CodeBlock.Builder body = CodeBlock.builder();

    String next(String prefix) {
      return "_" + prefix + counter++;
    }
  }

  private static MethodSpec installRootFactory(NodeSpec root) {
    var m =
        MethodSpec.methodBuilder("installRoot")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(ParameterizedTypeName.get(ERESULT, BINDING))
            .addParameter(ParameterSpec.builder(EXPR_CONTEXT, "xx").build())
            .addParameter(ParameterSpec.builder(QEXPR_ID, "loc").build())
            .addParameter(ParameterSpec.builder(IMPORT_STATE_ID, "importState").build())
            .addParameter(ParameterSpec.builder(GEN_BUILTIN_INTERFACES, "interfaces").build())
            .addParameter(ParameterSpec.builder(TypeName.BOOLEAN, "registerDynamic").build());

    // Dynamic registration for Interface + AspectId leaves.
    var dynBlock = CodeBlock.builder();
    emitDynamicRegistrations(root, dynBlock);
    if (!dynBlock.build().isEmpty()) {
      m.beginControlFlow("if (registerDynamic)");
      m.addCode(dynBlock.build());
      m.endControlFlow();
    }

    // Emit the whole tree inline; result is (binding, type) local names.
    var st = new EmitState();
    var vars = emitNode(root, st);
    m.addCode(st.body.build());
    m.addStatement("return $T.ok($N)", ERESULT, vars[0]);
    return m.build();
  }

  /** Emit inline statements for a node; return [bindingVarName, typeVarName]. */
  private static String[] emitNode(NodeSpec node, EmitState st) {
    return switch (node) {
      case Branch b -> emitBranch(b, st);
      case Leaf l -> emitLeaf(l.leaf(), st);
    };
  }

  private static String[] emitBranch(Branch b, EmitState st) {
    var fields = st.next("fields");
    st.body.addStatement(
        "var $N = new $T<$T, $T>()", fields, TORDERED_MAP, CONST_KEY, ARGUMENT_ENTRY);
    for (var c : b.children()) {
      var childVars = emitNode(c.node(), st);
      st.body.addStatement(
          "$N.put(new $T($S), new $T($N, $N))",
          fields,
          CONST_KEY_STR,
          c.name(),
          ARGUMENT_ENTRY,
          childVars[0],
          childVars[1]);
    }
    var record = st.next("record");
    var binding = st.next("binding");
    var type = st.next("type");
    st.body.addStatement("var $N = new $T($N)", record, ARGUMENT_RECORD, fields);
    st.body.addStatement("$T $N = new $T($N)", BINDING, binding, ARGUMENT_RECORD_BINDING, record);
    st.body.addStatement("$T $N = $N", TYPE, type, record);
    return new String[] {binding, type};
  }

  private static String[] emitLeaf(LeafSpec spec, EmitState st) {
    return switch (spec) {
      case LeafSpec.Const c -> emitConstLeaf(c.innerExpr(), c.valueTypeExpr(), st);
      case LeafSpec.InitialBind ib ->
          emitInitialBindLike(ib.typeExpr(), ib.dynamicKeyScopeId(), st, "InitialBind");
      case LeafSpec.AspectId ai -> emitAspectIdLeaf(ai, st);
      case LeafSpec.Interface ifc -> {
        var typeExpr =
            CodeBlock.of(
                "new $T(interfaces.$N)",
                TYPE_INTERFACE_CONST,
                ifaceHolderFieldName(ifc.spec()));
        yield emitInitialBindLike(typeExpr, (long) ifc.spec().scopeId(), st, "Interface");
      }
      case LeafSpec.Function fn -> {
        var methodName = BUILTIN_METHOD_NAMES.get(fn.builtinScopeId());
        if (methodName == null) {
          throw new IllegalStateException(
              "BuiltinTreeGen: no BuiltinBodies method mapped for scope id "
                  + fn.builtinScopeId());
        }
        var typeExpr =
            CodeBlock.of(
                "new $T($L, $L, $T.of($S), $S, true)",
                TYPE_FUNCTION_DYNAMIC,
                argDynamicTypeExpr(fn.args()),
                fn.retDynamicType(),
                JAVA_INTERNAL_NAME,
                "compilerxyz/eval/builtin/BuiltinBodies",
                methodName);
        yield emitInitialBindLike(typeExpr, fn.builtinScopeId(), st, "Function");
      }
      case LeafSpec.Constructor c ->
          emitInitialBindLike(c.typeExpr(), c.builtinScopeId(), st, "Constructor");
    };
  }

  private static final java.util.Map<Long, String> BUILTIN_METHOD_NAMES =
      java.util.Map.ofEntries(
          java.util.Map.entry(5001L, "log"),
          java.util.Map.entry(5002L, "define"),
          java.util.Map.entry(5003L, "strToJson"),
          java.util.Map.entry(5004L, "constkeyToStr"),
          java.util.Map.entry(5005L, "codevalue"),
          java.util.Map.entry(5006L, "genBindingName"),
          java.util.Map.entry(5008L, "constEval"),
          java.util.Map.entry(5009L, "newExprContext"),
          java.util.Map.entry(5013L, "stack"),
          java.util.Map.entry(5015L, "bindResult"));

  private static final ClassName JAVA_INTERNAL_NAME =
      ClassName.get("compilerxyz.eval.dynamic", "JavaInternalName");
  private static final ClassName JAVA_METHOD_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaMethodDescriptor");
  private static final ClassName JAVA_DATA_DESCRIPTOR =
      ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor");

  private static ClassName argClassLiteral(NamedTypeRef a) {
    var path = a.type().path();
    if (path instanceof ClassName cn) return cn;
    if (path instanceof ParameterizedTypeName pt) return pt.rawType;
    throw new IllegalStateException(
        "BuiltinTreeGen: unsupported arg TypeName for JVM descriptor: " + path);
  }

  private static String[] emitConstLeaf(
      CodeBlock innerExpr, CodeBlock valueTypeExpr, EmitState st) {
    var type = st.next("type");
    var wrapper = st.next("wrapper");
    var cv = st.next("cv");
    var bindRes = st.next("bindRes");
    var br = st.next("br");
    var binding = st.next("binding");
    st.body.addStatement("$T $N = ($T) $L", TYPE, type, TYPE, innerExpr);
    st.body.addStatement("$T $N = ($T) $L", TYPE, wrapper, TYPE, valueTypeExpr);
    st.body.addStatement(
        "var $N = new $T($T.none(loc), $T.some(new $T($N, $N)))",
        cv,
        CODE_VALUE,
        CODE_RT,
        OPT,
        VALUE_OBJECT_CONST,
        wrapper,
        type);
    st.body.addStatement("var $N = $T.get(xx, loc, $N)", bindRes, ASPECT_BIND, wrapper);
    st.body.beginControlFlow("if ($N.hasErrors())", bindRes);
    st.body.addStatement("return $N.reerr()", bindRes);
    st.body.endControlFlow();
    st.body.addStatement(
        "var $N = $N.requireValue($S).aspectBindBind(xx, loc, $N)",
        br,
        bindRes,
        "installRoot Const bind aspect",
        cv);
    st.body.beginControlFlow("if ($N.hasErrors())", br);
    st.body.addStatement("return $N.reerr()", br);
    st.body.endControlFlow();
    st.body.addStatement(
        "$T $N = $N.requireValue($S).binding()",
        BINDING,
        binding,
        br,
        "installRoot Const bind result");
    return new String[] {binding, wrapper};
  }

  /**
   * Shared emit for InitialBind / Function / Constructor / Interface — all
   * follow the pattern "AspectInitialBind.get(type).initialBind(key)".
   */
  private static String[] emitInitialBindLike(
      CodeBlock typeExpr, long dynamicKeyScopeId, EmitState st, String tag) {
    var type = st.next("type");
    var ib = st.next("ib");
    var binding = st.next("binding");
    st.body.addStatement("$T $N = ($T) $L", TYPE, type, TYPE, typeExpr);
    st.body.addStatement("var $N = $T.get(xx, loc, $N)", ib, ASPECT_INITIAL_BIND, type);
    st.body.beginControlFlow("if ($N.hasErrors())", ib);
    st.body.addStatement("return $N.reerr()", ib);
    st.body.endControlFlow();
    st.body.addStatement(
        "$T $N = $N.requireValue($S).aspectInitialBindInitialBind(new $T($LL))",
        BINDING,
        binding,
        ib,
        "installRoot " + tag,
        BINDING_ID_BUILTIN,
        dynamicKeyScopeId);
    return new String[] {binding, type};
  }

  private static String[] emitAspectIdLeaf(LeafSpec.AspectId ai, EmitState st) {
    var aspectClass = ClassName.get(GEN_PACKAGE, "Aspect" + ai.aspectName());
    var binding = st.next("binding");
    var type = st.next("type");
    st.body.addStatement(
        "$T $N = new $T(new $T($LL), $T.ID)",
        BINDING,
        binding,
        BINDING_UNIQUE_DUAL_MODE,
        BINDING_ID_BUILTIN,
        ai.builtinScopeId(),
        aspectClass);
    st.body.addStatement("$T $N = new $T()", TYPE, type, TYPE_UNIQUE_RT);
    return new String[] {binding, type};
  }

  private static void emitDynamicRegistrations(NodeSpec node, CodeBlock.Builder out) {
    switch (node) {
      case Branch b -> {
        for (var c : b.children()) emitDynamicRegistrations(c.node(), out);
      }
      case Leaf l -> {
        switch (l.leaf()) {
          case LeafSpec.Interface ifc ->
              out.addStatement(
                  "xx.vx.ix.builtinDynamics.put($T.bindingIdDynamicKey(new $T($LL)),"
                      + " new $T($T.class, new $T(interfaces.$N)))",
                  IMPORT_INSTALL,
                  BINDING_ID_BUILTIN,
                  (long) ifc.spec().scopeId(),
                  EXTERNAL,
                  TYPE_INTERFACE_CONST,
                  TYPE_INTERFACE_CONST,
                  ifaceHolderFieldName(ifc.spec()));
          case LeafSpec.AspectId ai -> {
            var aspectClass = ClassName.get(GEN_PACKAGE, "Aspect" + ai.aspectName());
            out.addStatement(
                "xx.vx.ix.builtinDynamics.put($T.bindingIdDynamicKey(new $T($LL)),"
                    + " new $T($T.class, $T.ID))",
                IMPORT_INSTALL,
                BINDING_ID_BUILTIN,
                ai.builtinScopeId(),
                EXTERNAL,
                UNIQUE_CLASS,
                aspectClass);
          }
          default -> {}
        }
      }
    }
  }

  private static CodeBlock argDynamicTypeExpr(TList<NamedTypeRef> args) {
    if (args.size() == 1) {
      return args.get(0).type().dynamicType();
    }
    var opt = ClassName.get("compilerxyz.util", "Opt");
    if (args.isEmpty()) {
      var tuple = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
      var tupleElement = ClassName.get("compilerxyz.eval.value.tuple", "TupleElement");
      return CodeBlock.of(
          "new $T($T.none(), $T.<$T>of())", tuple, opt, TLIST, tupleElement);
    }
    var record = ClassName.get("compilerxyz.eval.value.record", "Record");
    var recordFieldDynamic =
        ClassName.get("compilerxyz.eval.value.record", "RecordField", "Dynamic");
    var pair = ClassName.get("compilerxyz.util", "Pair");
    var entries = CodeBlock.builder();
    entries.add("$T.ofEntries(", TORDERED_MAP);
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
    return CodeBlock.of("new $T($T.none(), $L)", record, opt, entries.build());
  }

  private static String ifaceHolderFieldName(InterfaceSpec spec) {
    var s = spec.name().replace('-', '_');
    return switch (s) {
      case "type", "class", "interface", "if", "else", "for", "while", "do", "return", "switch",
          "case", "default", "break", "continue", "new", "this", "super", "null", "true", "false",
          "void", "int", "long", "short", "byte", "char", "boolean", "float", "double", "static",
          "final", "public", "private", "protected", "abstract", "package", "import", "throws",
          "throw", "try", "catch", "finally", "enum", "record", "instanceof", "extends",
          "implements" -> s + "_";
      default -> s;
    };
  }

  public static TList<InterfaceSpec> extraInterfaceSpecs() {
    var cve = TypeRef.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC);
    var xxRef = TypeRef.leaf(ClassName.get("compilerxyz.eval", "ExprContext"));
    var qexprIdRef = TypeRef.leaf(ClassName.get("compilerxyz.eval", "QExprId"));
    var constKeyRef = TypeRef.leaf(ClassName.get("compilerxyz.eval", "ConstKey"));
    var typeRef = TypeRef.leaf(TYPE);
    var eresultCve = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, cve);
    var optType = TypeRef.composite1(TYPE_OPTION_DYNAMIC, typeRef);
    var dropRet = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, TypeRef.leaf(CODE_INTERFACE));

    var aspectFunction =
        new InterfaceSpec(
            "aspect_function",
            1016,
            3003,
            "af",
            "AspectFunction",
            null,
            true,
            null,
            null,
            "compilerxyz.gen.AspectFunction",
            TList.of(
                new Method(
                    "call",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("self_", cve),
                            new NamedTypeRef("argument", cve))))));

    var aspectField =
        new InterfaceSpec(
            "aspect_field",
            1030,
            3004,
            "afd",
            "AspectField",
            null,
            true,
            null,
            null,
            "compilerxyz.gen.AspectField",
            TList.of(
                new Method(
                    "field",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("self_", cve),
                            new NamedTypeRef("key", cve)))),
                new Method(
                    "fieldTypeOpt",
                    optType,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("key", constKeyRef))))));

    var aspectDropDynamic =
        new InterfaceSpec(
            "aspect_drop_dynamic",
            2008,
            3008,
            "dd",
            "AspectDropDynamic",
            null,
            true,
            null,
            null,
            "compilerxyz.gen.AspectDropDynamic",
            TList.of(
                new Method(
                    "drop",
                    dropRet,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve))))));

    var codeDynamicClass = ClassName.get("compilerxyz.eval.dynamic", "CodeDynamic");
    var codeDynamicRef = TypeRef.leaf(codeDynamicClass);
    var eresultCodeDyn = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, codeDynamicRef);
    var javaDataDescriptorRef =
        TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic", "JavaDataDescriptor"));

    var aspectConsumeDynamic =
        new InterfaceSpec(
            "aspect_consume_dynamic",
            2009,
            3010,
            "cd_",
            "AspectConsumeDynamic",
            null,
            true,
            null,
            null,
            "compilerxyz.gen.AspectConsumeDynamic",
            TList.of(
                new Method(
                    "toDynamic",
                    eresultCodeDyn,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve)))),
                new Method(
                    "consumeDynamicJvmClass",
                    javaDataDescriptorRef,
                    new MethodArgs(
                        TList.of(new NamedTypeRef("xx", xxRef))))));

    var codeRef = ClassName.get("compilerxyz.eval", "Code");
    var qexprId = ClassName.get("compilerxyz.eval", "QExprId");
    var codeValue = ClassName.get("compilerxyz.eval", "CodeValue");
    var exprContext = ClassName.get("compilerxyz.eval", "ExprContext");
    var bindingXx = TypeRef.leaf(exprContext);
    var bindingLoc = TypeRef.leaf(qexprId);
    var bindingCode = TypeRef.leaf(codeRef);
    var bindingCv = TypeRef.leaf(codeValue);

    var binding =
        new InterfaceSpec(
            "binding",
            2005,
            3005,
            "se",
            "Binding",
            null,
            true,
            "Binding",
            "TYPE_ANY_BINDING_DYNAMIC_TYPE_ID",
            TList.of(
                new Method(
                    "use",
                    bindingCv,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("code", bindingCode))))));

var targetBindSubCls =
        ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub");
    var targetBindSub = TypeRef.leaf(targetBindSubCls);
    var codeDynamicCls = ClassName.get("compilerxyz.eval.dynamic", "CodeDynamic");
    var bindResult =
        TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "BindResult"));
    var codeDynamic = TypeRef.leaf(codeDynamicCls);
    var codeRefLeaf = TypeRef.leaf(codeRef);
    var codeValueLeaf = TypeRef.leaf(codeValue);
    var eresultCode = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, codeRefLeaf);
    var eresultCodeValue = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, codeValueLeaf);
    var eresultCodeDynamic = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, codeDynamic);
    var eresultTargetBindSub = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, targetBindSub);
    var eresultOptionTargetBindSub =
        TypeRef.composite1(
            TYPE_ERESULT_DYNAMIC, TypeRef.composite1(TYPE_OPTION_DYNAMIC, targetBindSub));
    var constKey = ClassName.get("compilerxyz.eval", "ConstKey");
    var expr = ClassName.get("compilerxyz.eval", "Expr");
    var listRecordBindField =
        TypeRef.composite1(
            TYPE_LIST_DYNAMIC,
            TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "RecordBindField")));
    var listTupleBindSlot =
        TypeRef.composite1(
            TYPE_LIST_DYNAMIC,
            TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "TupleBindSlot")));
    var listEntries = TypeRef.composite1(TYPE_LIST_DYNAMIC, codeRefLeaf);
    var listCodeValue = TypeRef.composite1(TYPE_LIST_DYNAMIC, codeValueLeaf);
    var typeOpaque = TypeRef.leaf(TYPE);
    var listIfBranch =
        TypeRef.composite1(
            TYPE_LIST_DYNAMIC,
            TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "IfBranch")));
    var optExpr = TypeRef.composite1(TYPE_OPTION_DYNAMIC, TypeRef.leaf(expr));
    var optTryCatchClause =
        TypeRef.composite1(
            TYPE_OPTION_DYNAMIC,
            TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "TryCatchClause")));
    var optCodeDynamic = TypeRef.composite1(TYPE_OPTION_DYNAMIC, codeDynamic);
    var caseListOpaque =
        TypeRef.composite1(
            TYPE_LIST_DYNAMIC,
            TypeRef.leaf(ClassName.get("compilerxyz.eval.dynamic.target", "TargetCase")));
    var codeValueOpaque = TypeRef.leaf(codeValue);
    var exprOpaque = TypeRef.leaf(expr);
    var constKeyOpaque = TypeRef.leaf(constKey);
    var bindingIdOpaque = TypeRef.leaf(BINDING_ID);

    var target =
        new InterfaceSpec(
            "target",
            2006,
            3006,
            "tg",
            "Target",
            null,
            true,
            "Target",
            "TYPE_ANY_TARGET_DYNAMIC_TYPE_ID",
            TList.of(
                new Method(
                    "bind",
                    bindResult,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("sub", targetBindSub)))),
                new Method(
                    "maybeBindSub",
                    eresultOptionTargetBindSub,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("cv", codeValueOpaque)))),
                new Method(
                    "codeToBindSub",
                    eresultTargetBindSub,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("code", bindingCode),
                            new NamedTypeRef("type", typeOpaque)))),
                new Method(
                    "bindSubRecord",
                    targetBindSub,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("carry", bindingCode),
                            new NamedTypeRef("fields", listRecordBindField)))),
                new Method(
                    "bindSubTuple",
                    targetBindSub,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("carry", bindingCode),
                            new NamedTypeRef("slots", listTupleBindSlot)))),
                new Method(
                    "castException",
                    eresultCode,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("callCode", bindingCode),
                            new NamedTypeRef("catchBinding", bindingIdOpaque),
                            new NamedTypeRef("castCv", codeValueOpaque)))),
                new Method(
                    "seq",
                    eresultCode,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("entries", listEntries)))),
                new Method(
                    "tryFinally",
                    eresultCode,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("body", bindingCode),
                            new NamedTypeRef("finally", bindingCode)))),
                new Method(
                    "drop",
                    eresultCode,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("cv", codeValueOpaque)))),
                new Method(
                    "stage",
                    eresultCodeValue,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("tree", exprOpaque),
                            new NamedTypeRef("lowerValues", listCodeValue),
                            new NamedTypeRef("graftValues", listCodeValue)))),
                new Method(
                    "if",
                    eresultCodeValue,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("conditions", listIfBranch),
                            new NamedTypeRef("else", optExpr)))),
                new Method(
                    "tryCatchFinally",
                    eresultCodeDynamic,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("body", codeDynamic),
                            new NamedTypeRef("catch", optTryCatchClause),
                            new NamedTypeRef("finally", optCodeDynamic)))),
                new Method(
                    "match",
                    eresultCodeValue,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", bindingXx),
                            new NamedTypeRef("loc", bindingLoc),
                            new NamedTypeRef("base", codeValueOpaque),
                            new NamedTypeRef("elementKey", constKeyOpaque),
                            new NamedTypeRef("cases", caseListOpaque),
                            new NamedTypeRef("fallback", optExpr))))));

    var codeDyn =
        new InterfaceSpec(
            "code_dyn", 2007, 3007, "cd", "CodeDyn", null, true, "CodeDyn", TList.<Method>of());

    return TList.of(
        aspectFunction,
        aspectField,
        aspectDropDynamic,
        aspectConsumeDynamic,
        binding,
        target,
        codeDyn);
  }

  public static NodeSpec canonicalSpec() {
    return new Branch(
        TList.of(
            new Child(
                "throw",
                new Leaf(
                    new LeafSpec.InitialBind(
                        THROW_COMPILE_SCOPE_ID,
                        DynamicTypes.leaf(TYPE_THROW_DYNAMIC)))),
            new Child(
                "panic",
                new Leaf(
                    new LeafSpec.InitialBind(
                        PANIC_COMPILE_SCOPE_ID,
                        DynamicTypes.leaf(TYPE_PANIC_DYNAMIC)))),
            new Child(
                "log",
                new Leaf(
                    new LeafSpec.Function(
                        5001,
                        TList.of(
                            new NamedTypeRef(
                                "message",
                                TypeRef.leaf(TYPE_STRING_DYNAMIC))),
                        unitTupleDynamic()))),
            new Child(
                "define",
                new Leaf(
                    new LeafSpec.Function(
                        5002,
                        TList.of(),
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder"))))),
            new Child("types", typesBranch()),
            new Child("eval", evalBranch())));
  }

  private static NodeSpec typesBranch() {
    return new Branch(
        TList.of(
            new Child(
                "list",
                new Leaf(
                    new LeafSpec.Constructor(
                        5010, DynamicTypes.leaf(TYPE_LIST_CONSTRUCTOR_CONST)))),
            new Child(
                "map",
                new Leaf(
                    new LeafSpec.Constructor(
                        5011, DynamicTypes.leaf(TYPE_MAP_CONSTRUCTOR_CONST)))),
            new Child(
                "option",
                new Leaf(
                    new LeafSpec.Constructor(
                        5012, DynamicTypes.leaf(TYPE_OPTION_CONSTRUCTOR_CONST)))),
            new Child(
                "eresult",
                new Leaf(
                    new LeafSpec.Constructor(
                        5014, DynamicTypes.leaf(TYPE_ERESULT_CONSTRUCTOR_CONST)))),
            new Child(
                "code",
                new Leaf(
                    new LeafSpec.Const(DynamicTypes.leaf(CODE_INTERFACE), OCT_TYPE_EXPR))),
            new Child(
                "string",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_STRING_DYNAMIC), OCT_TYPE_EXPR))),
            new Child(
                "number",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_NUMBER_DYNAMIC), OCT_TYPE_EXPR))),
            new Child(
                "null",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_NULL_DYNAMIC), OCT_TYPE_EXPR))),
            new Child(
                "bool",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_BOOL_DYNAMIC), OCT_TYPE_EXPR))),
            new Child(
                "binding",
                new Leaf(
                    new LeafSpec.Const(
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get("compilerxyz.gen", "Binding")), OCT_TYPE_EXPR))),
            new Child(
                "bind_result",
                new Leaf(
                    new LeafSpec.Const(
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get(
                                "compilerxyz.eval.value.bind_result", "BindResultType")), OCT_TYPE_EXPR))),
            new Child(
                "constkey",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_CONSTKEY_DYNAMIC), OCT_TYPE_EXPR))),
            new Child(
                "expr", new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE_EXPR), OCT_TYPE_EXPR))),
            new Child(
                "type",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(TYPE), OCT_TYPE_EXPR))),
            new Child(
                "aspect",
                new Leaf(new LeafSpec.Const(DynamicTypes.leaf(ASPECT_INTERFACE), OCT_TYPE_EXPR))),
            new Child(
                "codedyn",
                new Leaf(
                    new LeafSpec.Const(
                        DynamicTypes.leaf(CODE_DYN_HANDLE_TYPE), OCT_TYPE_EXPR))),
            new Child(
                "cve",
                new Leaf(
                    new LeafSpec.Const(
                        DynamicTypes.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC), OCT_TYPE_EXPR)))));
  }

  private static NodeSpec evalBranch() {
    var extras = extraInterfaceSpecs();
    var ifaceAspectFunction = findSpec(extras, "aspect_function");
    var ifaceAspectField = findSpec(extras, "aspect_field");
    var ifaceAspectDropDynamic = findSpec(extras, "aspect_drop_dynamic");
    var ifaceAspectConsumeDynamic = findSpec(extras, "aspect_consume_dynamic");
    var ifaceBinding = findSpec(extras, "binding");
    var ifaceTarget = findSpec(extras, "target");
    var ifaceCodeDyn = findSpec(extras, "code_dyn");
    var traits = TraitsGen.specs();
    var ifaceValue = findSpec(traits, "value");
    var ifaceType = findSpec(traits, "type");

    return new Branch(
        TList.of(
            new Child(
                "aspect_lookup",
                new Leaf(
                    new LeafSpec.InitialBind(
                        ASPECT_COMPILE_SCOPE_ID,
                        DynamicTypes.leaf(TYPE_FUNCTION_ASPECT_DYNAMIC)))),
            new Child(
                "cast_value",
                new Leaf(
                    new LeafSpec.InitialBind(
                        CAST_VALUE_COMPILE_SCOPE_ID,
                        DynamicTypes.leaf(TYPE_FUNCTION_CAST_VALUE_DYNAMIC)))),

            new Child(
                "const_eval",
                new Leaf(
                    new LeafSpec.Function(
                        5008,
                        TList.of(
                            new NamedTypeRef(
                                "xx_",
                                TypeRef.leaf(ClassName.get("compilerxyz.eval", "ExprContext"))),
                            new NamedTypeRef("staged", TypeRef.leaf(TYPE_EXPR))),
                        DynamicTypes.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC)))),
            new Child(
                "codevalue",
                new Leaf(
                    new LeafSpec.Function(
                        5005,
                        TList.of(
                            new NamedTypeRef(
                                "code", TypeRef.leaf(CODE_INTERFACE)),
                            new NamedTypeRef(
                                "value", TypeRef.leaf(VALUE_INTERFACE))),
                        DynamicTypes.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC)))),
            new Child(
                "stack",
                new Leaf(
                    new LeafSpec.Function(
                        5013,
                        TList.of(
                            new NamedTypeRef(
                                "type", TypeRef.leaf(ClassName.get("compilerxyz.eval", "Type")))),
                        DynamicTypes.leaf(VALUE_INTERFACE)))),
            new Child(
                "str_to_json",
                new Leaf(
                    new LeafSpec.Function(
                        5003,
                        TList.of(
                            new NamedTypeRef(
                                "s",
                                TypeRef.leaf(TYPE_STRING_DYNAMIC))),
                        DynamicTypes.leaf(TYPE_STRING_DYNAMIC)))),
            new Child(
                "constkey_to_str",
                new Leaf(
                    new LeafSpec.Function(
                        5004,
                        TList.of(
                            new NamedTypeRef(
                                "key", TypeRef.leaf(ClassName.get("compilerxyz.eval", "ConstKey")))),
                        DynamicTypes.leaf(TYPE_STRING_DYNAMIC)))),
            new Child(
                "gen_binding_name",
                new Leaf(
                    new LeafSpec.Function(
                        5006,
                        TList.of(
                            new NamedTypeRef(
                                "_xx",
                                TypeRef.leaf(ClassName.get("compilerxyz.eval", "ExprContext")))),
                        DynamicTypes.leaf(TYPE_STRING_DYNAMIC)))),
            new Child(
                "new_expr_context",
                new Leaf(
                    new LeafSpec.Function(
                        5009,
                        TList.of(
                            new NamedTypeRef(
                                "target_arg",
                                TypeRef.leaf(ClassName.get("compilerxyz.gen", "Target"))),
                            new NamedTypeRef(
                                "scope_arg",
                                TypeRef.composite2(
                                    TYPE_MAP_DYNAMIC,
                                    TypeRef.leaf(ClassName.get("compilerxyz.eval", "ConstKey")),
                                    TypeRef.leaf(ClassName.get("compilerxyz.gen", "Binding"))))),
                        DynamicTypes.leaf(ClassName.get("compilerxyz.eval", "ExprContext"))))),
            new Child("aspect_function", new Leaf(new LeafSpec.Interface(ifaceAspectFunction))),
            new Child("aspect_field", new Leaf(new LeafSpec.Interface(ifaceAspectField))),
            new Child(
                "aspect_drop_dynamic",
                new Leaf(new LeafSpec.Interface(ifaceAspectDropDynamic))),
            new Child(
                "aspect_consume_dynamic",
                new Leaf(
                    new LeafSpec.Const(
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get("compilerxyz.gen", "AspectConsumeDynamic")), TTOD_TYPE_EXPR))),
            new Child(
                "bind_result",
                new Leaf(
                    new LeafSpec.Function(
                        5015,
                        TList.of(
                            new NamedTypeRef("code", TypeRef.leaf(CODE_INTERFACE)),
                            new NamedTypeRef(
                                "binding",
                                TypeRef.leaf(ClassName.get("compilerxyz.gen", "Binding")))),
                        DynamicTypes.leaf(
                            ClassName.get(
                                "compilerxyz.eval.value.bind_result", "BindResultType"))))),
            new Child(
                "aspect_bind",
                new Leaf(
                    new LeafSpec.Const(
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get("compilerxyz.gen", "AspectBind")), TTOD_TYPE_EXPR))),
            new Child(
                "aspect_string_dynamic",
                new Leaf(new LeafSpec.AspectId(4001, "StringDynamic"))),
            new Child(
                "aspect_function_id",
                new Leaf(new LeafSpec.AspectId(4002, "Function"))),
            new Child(
                "aspect_field_id",
                new Leaf(new LeafSpec.AspectId(4003, "Field"))),
            new Child(
                "aspect_bind_id",
                new Leaf(new LeafSpec.AspectId(4004, "Bind"))),
            new Child(
                "aspect_drop_dynamic_id",
                new Leaf(new LeafSpec.AspectId(4005, "DropDynamic"))),
            new Child(
                "aspect_consume_dynamic_id",
                new Leaf(new LeafSpec.AspectId(4006, "ConsumeDynamic"))),
            new Child("binding", new Leaf(new LeafSpec.Interface(ifaceBinding))),
            new Child("target", new Leaf(new LeafSpec.Interface(ifaceTarget))),
            new Child("code_dyn", new Leaf(new LeafSpec.Interface(ifaceCodeDyn))),
            new Child("value", new Leaf(new LeafSpec.Interface(ifaceValue))),
            new Child("type_", new Leaf(new LeafSpec.Interface(ifaceType)))));
  }

  private static InterfaceSpec findSpec(TList<InterfaceSpec> specs, String name) {
    for (var s : specs) if (s.name().equals(name)) return s;
    throw new IllegalStateException("missing extra-interface spec: " + name);
  }

  private static CodeBlock unitTupleDynamic() {
    return DynamicTypes.leaf(TYPE_NULL_DYNAMIC);
  }

  private static void validate(NodeSpec root, TSet<Long> scopeIds) {

    for (var id :
        new long[] {
          ASPECT_COMPILE_SCOPE_ID,
          CAST_VALUE_COMPILE_SCOPE_ID,
          THROW_COMPILE_SCOPE_ID,
          PANIC_COMPILE_SCOPE_ID
        }) {
      if (!scopeIds.add(id)) {
        throw new IllegalStateException(
            "duplicate scope_id " + id + " (in builtin_tree, compile-scope marker)");
      }
    }
    var seenNames = new TSet<String>();
    collectAndValidate(root, "", seenNames, scopeIds);

  }

  private static void collectAndValidate(
      NodeSpec node, String path, TSet<String> seenNames, TSet<Long> scopeIds) {
    switch (node) {
      case Branch b -> {
        var seenChildren = new TSet<String>();
        for (var c : b.children()) {
          if (!seenChildren.add(c.name())) {
            throw new IllegalStateException(
                "duplicate child name at " + (path.isEmpty() ? "<root>" : path) + ": " + c.name());
          }
          collectAndValidate(
              c.node(),
              path.isEmpty() ? c.name() : path + "/" + c.name(),
              seenNames,
              scopeIds);
        }
      }
      case Leaf l -> validateLeaf(l.leaf(), path, scopeIds);
    }
  }

  private static void validateLeaf(LeafSpec spec, String path, TSet<Long> scopeIds) {

    Long id =
        switch (spec) {
          case LeafSpec.Const c -> null;
          case LeafSpec.InitialBind ib -> null;
          case LeafSpec.AspectId ai -> ai.builtinScopeId();
          case LeafSpec.Interface ifc -> null;
          case LeafSpec.Function fn -> fn.builtinScopeId();
          case LeafSpec.Constructor c -> c.builtinScopeId();
        };
    if (id == null) return;
    if (!scopeIds.add(id)) {
      throw new IllegalStateException(
          "duplicate scope_id " + id + " (in builtin_tree at path " + path + ")");
    }
  }
}
