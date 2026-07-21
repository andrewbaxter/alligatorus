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
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.codegen.experimental.BuiltinPathScanner;
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
      ClassName.get("compilerxyz.gen", "GenAspectBind");
  private static final ClassName ASPECT_INITIAL_BIND =
      ClassName.get("compilerxyz.gen", "GenAspectInitialBind");
  private static final ClassName VALUE_OBJECT_CONST =
      ClassName.get("compilerxyz.eval.value.object_", "ValueObjectConst");
  private static final ClassName TYPE_OBJECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectDynamic");
  private static final ClassName TYPE_OBJECT_CONST_CLS =
      ClassName.get("compilerxyz.eval.value.object_", "TypeObjectConst");
  private static final ClassName GEN_TYPE_IDS_CLS =
      ClassName.get("compilerxyz.gen", "GenTypeIds");
  private static final CodeBlock TOD_SELF_TYPE_EXPR =
      CodeBlock.of("$T.constType()", TYPE_OBJECT_DYNAMIC);
  private static final CodeBlock TYPE_WRAPPER_EXPR =
      CodeBlock.of(
          "$T.metaOf($T.class, $T.TYPE_TYPE_ID, $S)",
          TYPE_OBJECT_CONST_CLS,
          ClassName.get("compilerxyz.eval", "Type"),
          GEN_TYPE_IDS_CLS,
          "type");
  private static final ClassName TYPE_UNIQUE_CONST =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUniqueConst");
  private static final CodeBlock TUC_TYPE_EXPR =
      CodeBlock.of("new $T()", TYPE_UNIQUE_CONST);
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

  public record LeafSpec(CodeBlock data, CodeBlock type) implements NodeSpec {

    public static LeafSpec of(CodeBlock data, CodeBlock type) {
      return new LeafSpec(data, type);
    }

    public static LeafSpec interface_(InterfaceSpec spec) {
      var innerExpr =
          CodeBlock.of("new $T(interfaces.$N)", TYPE_INTERFACE_CONST, ifaceHolderFieldName(spec));
      var wrapperExpr = CodeBlock.of("$T.dynamicType()", TYPE_INTERFACE_CONST);
      return new LeafSpec(innerExpr, wrapperExpr);
    }

    public static LeafSpec functionByName(
        String methodName, TList<NamedTypeRef> args, CodeBlock retDynamicType) {
      var innerExpr = CodeBlock.of(
          "new $T($L, $L, $T.of($S), $S, true)",
          TYPE_FUNCTION_DYNAMIC,
          argDynamicTypeExpr(args),
          retDynamicType,
          JAVA_INTERNAL_NAME,
          "compilerxyz/eval/builtin/BuiltinBodies",
          methodName);
      return new LeafSpec(innerExpr, TYPE_WRAPPER_EXPR);
    }
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
            .addParameter(ParameterSpec.builder(GEN_BUILTIN_INTERFACES, "interfaces").build());

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
      case LeafSpec ls -> emitLeaf(ls, st);
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
    return emitConstLeaf(spec.data(), spec.type(), st);
  }

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
    var binding = st.next("binding");
    st.body.addStatement("$T $N = $L", Object.class, type, innerExpr);
    st.body.addStatement("$T $N = ($T) $L", TYPE, wrapper, TYPE, valueTypeExpr);
    st.body.addStatement(
        "$T $N = new compilerxyz.eval.binding.BindingObjectConst($N, $N)",
        BINDING,
        binding,
        wrapper,
        type);
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

  private static TList<InterfaceSpec> scannedExtras = new TList<>();
  private static TList<BuiltinPathScanner.Entry> scannedPathEntries = new TList<>();

  public static void setScannedInterfaceSpecs(TList<InterfaceSpec> specs) {
    scannedExtras = specs;
  }

  public static void setScannedPathEntries(TList<BuiltinPathScanner.Entry> entries) {
    scannedPathEntries = entries;
  }

  public static TList<InterfaceSpec> extraInterfaceSpecs() {
    var out = new TList<InterfaceSpec>();
    out.addAll(scannedExtras);
    out.addAll(hardCodedExtraInterfaceSpecs());
    return out;
  }

  private static TList<InterfaceSpec> hardCodedExtraInterfaceSpecs() {
    // AspectFunction migrated: scanned from handwritten/AspectFunction.java @SourceInterface
    // AspectField migrated: scanned from handwritten/AspectField.java @SourceInterface
    // AspectDropDynamic migrated: scanned from handwritten/AspectDropDynamic.java @SourceInterface
    // AspectBindDynamic migrated: scanned from handwritten/AspectBindDynamic.java @SourceInterface
    // AspectConsumeDynamic migrated: scanned from handwritten/AspectConsumeDynamic.java @SourceInterface

    var codeRef = ClassName.get("compilerxyz.eval", "Code");
    var qexprId = ClassName.get("compilerxyz.eval", "QExprId");
    var codeValue = ClassName.get("compilerxyz.eval", "CodeValue");
    var exprContext = ClassName.get("compilerxyz.eval", "ExprContext");
    var bindingXx = TypeRef.leaf(exprContext);
    var bindingLoc = TypeRef.leaf(qexprId);
    var bindingCode = TypeRef.leaf(codeRef);
    var bindingCv = TypeRef.leaf(codeValue);

    // Binding (id 2005) migrated: scanned from gen/Binding.java @Builtin

    // Target (id 2006) migrated: scanned from gen/Target.java @Builtin

    // CodeDyn (id 2007) migrated: scanned from gen/CodeDyn.java @Builtin

    return TList.of();
  }

  public static NodeSpec canonicalSpec() {
    // Collect (path, leaf) pairs from all sources.
    var pathToLeaf = new java.util.LinkedHashMap<String, LeafSpec>();

    // Build a lookup: host-interface binary name -> InterfaceSpec, so that a
    // @BuiltinPath scanned on an interface class becomes an interface-const
    // leaf (TypeSourceInterfaceConst wrapping the interfaces holder field)
    // rather than a plain type leaf (which would require the class to expose
    // a static dynamicType()).
    var extras = extraInterfaceSpecs();
    var interfaceByHostName = new java.util.HashMap<String, InterfaceSpec>();
    for (var s : extras) {
      var host = s.effectiveHostInterfaceBinaryName();
      if (host != null) interfaceByHostName.put(host, s);
      // Aspect specs are keyed by the generated compilerxyz.gen.AspectXxx name,
      // but @BuiltinPath is applied on the handwritten source class
      // (compilerxyz.handwritten.AspectXxx). Register that alias too.
      if (host != null && host.startsWith("compilerxyz.gen.Aspect")) {
        var handwritten = "compilerxyz.handwritten." + host.substring("compilerxyz.gen.".length());
        interfaceByHostName.put(handwritten, s);
      }
    }

    // 1) Scanner-produced @BuiltinPath entries.
    for (var entry : scannedPathEntries) {
      LeafSpec spec = switch (entry) {
        case BuiltinPathScanner.TypeLeaf tl -> {
          var host = tl.typeClass().canonicalName();
          var ifaceSpec = interfaceByHostName.get(host);
          if (ifaceSpec != null) yield LeafSpec.interface_(ifaceSpec);
          yield LeafSpec.of(DynamicTypes.leaf(tl.typeClass()), TYPE_WRAPPER_EXPR);
        }
        case BuiltinPathScanner.FunctionLeaf fl ->
            LeafSpec.functionByName(fl.methodName(), fl.args(), fl.retType().dynamicType());
      };
      if (pathToLeaf.put(entry.path(), spec) != null) {
        throw new IllegalStateException("duplicate @BuiltinPath entry: " + entry.path());
      }
    }

    // 3) Hand-coded specials — TTOD-style, .ID-style, and remaining entries
    //    that don't fit type-leaf / function-leaf / interface-leaf.
    addSpecial(pathToLeaf, "eval.aspect_drop_dynamic_light",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectDropDynamicLight")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_bind_dynamic",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectBindDynamic")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_consume_dynamic",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectConsumeDynamic")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_bind",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectBind")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_string_dynamic",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectStringDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_function_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectFunction")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_field_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectField")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_bind_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectBind")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_drop_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectDropDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_consume_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectConsumeDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_bind_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectBindDynamic")),
        TUC_TYPE_EXPR);

    return buildTree(pathToLeaf);
  }

  private static void addSpecial(
      java.util.LinkedHashMap<String, LeafSpec> m,
      String path,
      CodeBlock data,
      CodeBlock type) {
    if (m.put(path, LeafSpec.of(data, type)) != null) {
      throw new IllegalStateException("duplicate special path: " + path);
    }
  }

  /** Static hard-coded ORDER for children of well-known branches, to preserve generated-file layout. */
  private static final java.util.Map<String, java.util.List<String>> BRANCH_ORDER =
      java.util.Map.of(
          "",
              java.util.List.of("throw", "panic", "log", "define", "types", "eval"),
          "types",
              java.util.List.of(
                  "list", "map", "option", "eresult", "code", "string", "number", "null",
                  "bool", "binding", "bind_result", "constkey", "expr", "type", "aspect",
                  "codedyn", "cve"),
          "eval",
              java.util.List.of(
                  "aspect_lookup", "cast_value", "const_eval", "codevalue", "stack",
                  "str_to_json", "constkey_to_str", "gen_binding_name", "new_expr_context",
                  "aspect_function", "aspect_field", "aspect_drop_dynamic",
                  "aspect_drop_dynamic_light", "aspect_bind_dynamic",
                  "aspect_consume_dynamic", "bind_result", "aspect_bind",
                  "aspect_string_dynamic", "aspect_function_id", "aspect_field_id",
                  "aspect_bind_id", "aspect_drop_dynamic_id", "aspect_consume_dynamic_id",
                  "aspect_bind_dynamic_id",
                  "binding", "target", "code_dyn", "value", "type"));

  private record TreeNode(
      java.util.LinkedHashMap<String, TreeNode> children,
      @org.jspecify.annotations.Nullable LeafSpec leaf) {
    TreeNode() {
      this(new java.util.LinkedHashMap<>(), null);
    }
    TreeNode(LeafSpec leaf) {
      this(new java.util.LinkedHashMap<>(), leaf);
    }
  }

  private static NodeSpec buildTree(java.util.LinkedHashMap<String, LeafSpec> pathToLeaf) {
    var root = new TreeNode();
    for (var e : pathToLeaf.entrySet()) {
      var parts = e.getKey().split("\\.");
      var cur = root;
      for (int i = 0; i < parts.length - 1; i++) {
        var name = parts[i];
        var next = cur.children().get(name);
        if (next == null) {
          next = new TreeNode();
          cur.children().put(name, next);
        }
        if (next.leaf() != null) {
          throw new IllegalStateException(
              "path collision (leaf where branch needed): " + e.getKey());
        }
        cur = next;
      }
      var leafName = parts[parts.length - 1];
      if (cur.children().containsKey(leafName)) {
        throw new IllegalStateException("path collision: " + e.getKey());
      }
      cur.children().put(leafName, new TreeNode(e.getValue()));
    }
    return toNodeSpec(root, "");
  }

  private static NodeSpec toNodeSpec(TreeNode n, String branchPath) {
    if (n.leaf() != null) return n.leaf();
    var children = new TList<Child>();
    var order = BRANCH_ORDER.get(branchPath);
    var remaining = new java.util.LinkedHashMap<>(n.children());
    if (order != null) {
      for (var name : order) {
        var child = remaining.remove(name);
        if (child == null) continue;
        children.add(new Child(name, toNodeSpec(child,
            branchPath.isEmpty() ? name : branchPath + "." + name)));
      }
    }
    for (var e : remaining.entrySet()) {
      children.add(new Child(e.getKey(), toNodeSpec(e.getValue(),
          branchPath.isEmpty() ? e.getKey() : branchPath + "." + e.getKey())));
    }
    return new Branch(children);
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
      case LeafSpec ls -> {}
    }
  }
}
