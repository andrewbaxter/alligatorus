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
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.DynamicTypes;
import compilerxyz.codegen.experimental.BuiltinPathScanner;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TSet;
import java.io.IOException;
import java.nio.file.Path;
import javax.lang.model.element.Modifier;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
      ClassName.get("compilerxyz.eval.value.unique", "TypeUniqueDynamic");
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
  private static final CodeBlock TOD_SELF_TYPE_EXPR =
      CodeBlock.of("$T.constType()", TYPE_OBJECT_DYNAMIC);
  private static final CodeBlock TYPE_WRAPPER_EXPR =
      CodeBlock.of(
          "$T.metaOf($T.class, $S)",
          TYPE_OBJECT_CONST_CLS,
          ClassName.get("compilerxyz.eval", "Type"),
          "type");
  private static final ClassName TYPE_UNIQUE_CONST =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUniqueConst");
  private static final CodeBlock TUC_TYPE_EXPR =
      CodeBlock.of("new $T()", TYPE_UNIQUE_CONST);
  private static final ClassName IMPORT_INSTALL =
      ClassName.get("compilerxyz.eval", "ImportInstall");
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
  private static final ClassName TYPE_INT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.int_", "TypeIntDynamic");
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
  private static final ClassName TYPE_LIST_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.list", "TypeListBoxedDynamic");
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

    public static LeafSpec functionByName(
        String methodName,
        String ownerInternalName,
        boolean needsExprContext,
        TList<NamedTypeRef> args,
        CodeBlock retDynamicType) {
      var innerExpr = CodeBlock.of(
          "new $T($L, $T.some($L), $T.of($S), $S, $L)",
          TYPE_FUNCTION_DYNAMIC,
          paramsExpr(args),
          OPT,
          retDynamicType,
          JAVA_INTERNAL_NAME,
          ownerInternalName,
          methodName,
          needsExprContext);
      return new LeafSpec(innerExpr, TYPE_WRAPPER_EXPR);
    }
  }

  public static void run(
      Path outRoot, Formatter formatter, TSet<Long> scopeIds)
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
            .addParameter(ParameterSpec.builder(IMPORT_STATE_ID, "importState").build());

    for (var linkName : linkClassNames) {
      m.addStatement("$T.link()", ClassName.get(GEN_PACKAGE, linkName));
    }
    var st = new EmitState();
    var vars = emitNode(root, st);
    m.addCode(st.body.build());
    m.addStatement("return $T.ok($N)", ERESULT, vars[0]);
    return m.build();
  }

  private static TList<String> linkClassNames = new TList<>();

  public static void setLinkClassNames(TList<String> names) {
    linkClassNames = names;
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


  private static CodeBlock paramsExpr(TList<NamedTypeRef> args) {
    var functionParam =
        ClassName.get("compilerxyz.eval.value.function", "FunctionParam");
    var b = CodeBlock.builder();
    b.add("$T.<$T>of(", TLIST, functionParam);
    for (var i = 0; i < args.size(); i++) {
      var a = args.get(i);
      if (i > 0) b.add(", ");
      b.add("new $T($S, $L)", functionParam, a.name(), a.type().dynamicType());
    }
    b.add(")");
    return b.build();
  }

  private static TList<BuiltinPathScanner.Entry> scannedPathEntries = new TList<>();
  private static Set<String> builtinInterfaceHostNames = Set.of();

  public static void setScannedPathEntries(TList<BuiltinPathScanner.Entry> entries) {
    scannedPathEntries = entries;
  }

  public static void setBuiltinInterfaceHostNames(Set<String> names) {
    builtinInterfaceHostNames = names;
  }

  public static NodeSpec canonicalSpec() {
    // Collect (path, leaf) pairs from all sources.
    var pathToLeaf = new LinkedHashMap<String, LeafSpec>();

    // Scanner-produced @BuiltinPath entries. @Builtin interface classes yield
    // a TypeObjectDynamic (via Gen<X>.dynamicType()), so their source-visible
    // wrapper is TypeObjectDynamic.constType() rather than the plain Type
    // wrapper.
    for (var entry : scannedPathEntries) {
      LeafSpec spec = switch (entry) {
        case BuiltinPathScanner.TypeLeaf tl -> {
          var host = tl.typeClass().canonicalName();
          var wrapper =
              builtinInterfaceHostNames.contains(host) ? TOD_SELF_TYPE_EXPR : TYPE_WRAPPER_EXPR;
          yield LeafSpec.of(DynamicTypes.leaf(tl.typeClass()), wrapper);
        }
        case BuiltinPathScanner.FunctionLeaf fl ->
            LeafSpec.functionByName(
                fl.methodName(),
                fl.ownerInternalName(),
                fl.needsExprContext(),
                fl.args(),
                fl.retType().dynamicType());
      };
      if (pathToLeaf.put(entry.entryPath(), spec) != null) {
        throw new IllegalStateException("duplicate @BuiltinPath entry: " + entry.entryPath());
      }
    }

    // 3) Hand-coded specials — TTOD-style, .ID-style, and remaining entries
    //    that don't fit type-leaf / function-leaf / interface-leaf.
    addSpecial(pathToLeaf, "eval.aspect_drop_dynamic_light",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectDropDynamicLight")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_initial_bind",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectInitialBind")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_lift",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectLift")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_bind_dynamic",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectBindDynamic")), TOD_SELF_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_consume_dynamic",
        CodeBlock.of("$T.dynamicType()",
            ClassName.get("compilerxyz.gen", "GenAspectConsumeDynamic")), TOD_SELF_TYPE_EXPR);
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
    addSpecial(pathToLeaf, "eval.aspect_set_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectSet")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_operator_add_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectOperatorAdd")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_drop_dynamic_light_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectDropDynamicLight")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_stack_value_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectStackValueDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_field_use_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectFieldUseDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_element_use_dynamic_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectElementUseDynamic")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.aspect_initial_bind_id",
        CodeBlock.of("$T.AspectId", ClassName.get("compilerxyz.gen", "GenAspectInitialBind")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.type_int_dynamic_identity",
        CodeBlock.of(
            "$T.TYPE_INT_DYNAMIC_IDENTITY",
            ClassName.get("compilerxyz.gen", "GenTrivialIdentityAspects")),
        TUC_TYPE_EXPR);
    addSpecial(pathToLeaf, "eval.type_long_dynamic_identity",
        CodeBlock.of(
            "$T.TYPE_LONG_DYNAMIC_IDENTITY",
            ClassName.get("compilerxyz.gen", "GenTrivialIdentityAspects")),
        TUC_TYPE_EXPR);

    return buildTree(pathToLeaf);
  }

  private static void addSpecial(
      LinkedHashMap<String, LeafSpec> m,
      String path,
      CodeBlock data,
      CodeBlock type) {
    if (m.put(path, LeafSpec.of(data, type)) != null) {
      throw new IllegalStateException("duplicate special path: " + path);
    }
  }

  /** Static hard-coded ORDER for children of well-known branches, to preserve generated-file layout. */
  private static final Map<String, List<String>> BRANCH_ORDER =
      Map.of(
          "",
              List.of("throw", "panic", "log", "define", "types", "eval"),
          "types",
              List.of(
                  "list", "map", "option", "eresult", "code", "string", "int", "null",
                  "bool", "binding", "bind_result", "constkey", "expr", "type", "aspect",
                  "codedyn", "cve"),
          "eval",
              List.of(
                  "aspect_lookup", "cast_value", "const_eval", "codevalue", "stack",
                  "str_to_json", "constkey_to_str", "gen_binding_name", "new_expr_context",
                  "aspect_function", "aspect_field", "aspect_drop_dynamic",
                  "aspect_drop_dynamic_light", "aspect_bind_dynamic",
                  "aspect_consume_dynamic", "bind_result", "aspect_bind",
                  "aspect_initial_bind", "aspect_lift",
                  "aspect_string_dynamic", "aspect_function_id", "aspect_field_id",
                  "aspect_bind_id", "aspect_drop_dynamic_id", "aspect_consume_dynamic_id",
                  "aspect_bind_dynamic_id", "aspect_set_id", "aspect_operator_add_id",
                  "aspect_drop_dynamic_light_id", "aspect_stack_value_dynamic_id",
                  "aspect_field_use_dynamic", "aspect_element_use_dynamic",
                  "aspect_field_use_dynamic_id", "aspect_element_use_dynamic_id",
                  "aspect_initial_bind_id", "binding", "target", "code_dyn", "value", "type"));

  private record TreeNode(
      LinkedHashMap<String, TreeNode> children,
      Opt<LeafSpec> leaf) {
    TreeNode() {
      this(new LinkedHashMap<>(), Opt.none());
    }
    TreeNode(LeafSpec leaf) {
      this(new LinkedHashMap<>(), Opt.some(leaf));
    }
  }

  private static NodeSpec buildTree(LinkedHashMap<String, LeafSpec> pathToLeaf) {
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
        if (next.leaf().optIsSome()) {
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
    if (n.leaf().optIsSome()) return n.leaf().optUnwrap();
    var children = new TList<Child>();
    var order = BRANCH_ORDER.get(branchPath);
    var remaining = new LinkedHashMap<>(n.children());
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
