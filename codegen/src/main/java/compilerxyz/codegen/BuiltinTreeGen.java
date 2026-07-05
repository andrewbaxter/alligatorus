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
import compilerxyz.codegen.util.MethodSig;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
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

  private static final ClassName BUILTIN_TREE = ClassName.get("compilerxyz.eval", "BuiltinTree");
  private static final ClassName BUILTIN_TREE_BRANCH =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "Branch");
  private static final ClassName BUILTIN_TREE_LEAF =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "Leaf");
  private static final ClassName BUILTIN_TREE_CHILD =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "Child");
  private static final ClassName BUILTIN_LEAF_ANY_CONST =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "AnyConst");
  private static final ClassName BUILTIN_LEAF_INITIAL_BIND =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "InitialBind");
  private static final ClassName BUILTIN_LEAF_ASPECT_ID =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "AspectId");
  private static final ClassName BUILTIN_LEAF_INTERFACE =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "Interface");
  private static final ClassName BUILTIN_LEAF_FUNCTION =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "Function");
  private static final ClassName BUILTIN_LEAF_CONSTRUCTOR =
      ClassName.get("compilerxyz.eval", "BuiltinTree", "BuiltinLeaf", "Constructor");

  private static final ClassName BINDING_ID = ClassName.get("compilerxyz.eval", "BindingId");
  private static final ClassName BINDING_ID_BUILTIN =
      ClassName.get("compilerxyz.eval", "BindingId", "Builtin");
  private static final ClassName IMPORT_STATE_ID =
      ClassName.get("compilerxyz.eval", "ImportStateId");
  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TLIST = ClassName.get("compilerxyz.util", "TList");
  private static final ClassName EXPR_CONTEXT = ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName EXTERNAL = ClassName.get("compilerxyz.eval", "External");
  private static final ClassName EXTERNAL_FUNCTION =
      ClassName.get("compilerxyz.eval", "External", "FunctionExternal");
  private static final ClassName EXTERNAL_SOURCE_INTERFACE_REF =
      ClassName.get("compilerxyz.eval", "External", "SourceInterfaceRefExternal");
  private static final ClassName SOURCE_INTERFACE_REF =
      ClassName.get("compilerxyz.eval.value.source_interface", "SourceInterfaceRef");

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
  private static final ClassName TYPE_ANY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.any", "TypeAnyDynamic");
  private static final ClassName TYPE_CONSTKEY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.constkey", "TypeConstkeyDynamic");
  private static final ClassName TYPE_EXPR =
      ClassName.get("compilerxyz.eval.value.expr", "TypeExpr");
  private static final ClassName TYPE_ASPECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.aspect", "TypeAspectDynamic");
  private static final ClassName TYPE_CODE_DYN_HANDLE_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code", "TypeCodeDynHandleDynamic");
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

  private static final ClassName EXTERNAL_UNIQUE =
      ClassName.get("compilerxyz.eval", "External", "UniqueExternal");

  public sealed interface NodeSpec {}

  public record Child(String name, NodeSpec node) {}

  public record Branch(TList<Child> children) implements NodeSpec {}

  public record Leaf(LeafSpec leaf) implements NodeSpec {}

  public sealed interface LeafSpec {

    record AnyConst(CodeBlock typeExpr) implements LeafSpec {}

    record InitialBind(long dynamicKeyScopeId, CodeBlock typeExpr) implements LeafSpec {}

    record AspectId(long builtinScopeId, String aspectName) implements LeafSpec {}

    record Interface(InterfaceSpec spec) implements LeafSpec {}

    record Function(
        long builtinScopeId,
        TList<NamedTypeRef> args,
        TypeName ret,
        CodeBlock retDynamicType,
        CodeBlock body)
        implements LeafSpec {}

    record Constructor(
        long builtinScopeId,
        TList<NamedTypeRef> args,
        TypeName ret,
        CodeBlock retDynamicType,
        CodeBlock typeExpr,
        CodeBlock body)
        implements LeafSpec {}
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

    cls.addMethod(buildFactory(spec));

    var file = JavaFile.builder(GEN_PACKAGE, cls.build()).build();
    GenMain.write(outRoot, file, formatter);

    for (var helper : collectFunctionLeaves(spec)) {
      var helperCls =
              TypeSpec.classBuilder(builtinHelperName(helper.scopeId()))
                      .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                      .addMethod(MethodSpec.constructorBuilder().addModifiers(Modifier.PRIVATE).build())
              .addMethod(invokeMethod(helper))
              .build();
      var helperFile = JavaFile.builder(GEN_PACKAGE, helperCls).build();
      GenMain.write(outRoot, helperFile, formatter);
    }
  }

  private record FunctionHelper(long scopeId, TList<NamedTypeRef> args, CodeBlock body) {}

  private static TList<FunctionHelper> collectFunctionLeaves(NodeSpec root) {
    var out = new TList<FunctionHelper>();
    collectFunctionLeaves(root, out);
    return out;
  }

  private static void collectFunctionLeaves(NodeSpec node, TList<FunctionHelper> out) {
    switch (node) {
      case Branch b -> {
        for (var c : b.children()) collectFunctionLeaves(c.node(), out);
      }
      case Leaf l -> {
        switch (l.leaf()) {
          case LeafSpec.Function fn ->
              out.add(new FunctionHelper(fn.builtinScopeId(), fn.args(), fn.body()));
          case LeafSpec.Constructor c ->
              out.add(new FunctionHelper(c.builtinScopeId(), c.args(), c.body()));
          default -> {}
        }
      }
    }
  }

  private static String builtinHelperName(long scopeId) {
    return "Builtin_b" + scopeId;
  }

  private static MethodSpec invokeMethod(FunctionHelper h) {
    var m =
        MethodSpec.methodBuilder("invoke")
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
            .returns(TypeName.OBJECT)
            .addParameter(ParameterSpec.builder(EXPR_CONTEXT, "xx").build())
            .addParameter(ParameterSpec.builder(TypeName.OBJECT, "arg").build());

    var args = h.args();
    if (args.size() == 1) {
      var a = args.get(0);
      m.addStatement("$T $N = ($T) arg", a.type().path(), a.name(), a.type().path());
    } else if (args.size() > 1) {
      m.addStatement("$T[] _args = ($T[]) arg", TypeName.OBJECT, TypeName.OBJECT);
      for (var i = 0; i < args.size(); i++) {
        var a = args.get(i);
        m.addStatement("$T $N = ($T) _args[$L]", a.type().path(), a.name(), a.type().path(), i);
      }
    }
    m.addCode(h.body());
    return m.build();
  }

  private static MethodSpec buildFactory(NodeSpec root) {
    var build =
            MethodSpec.methodBuilder("build")
                    .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                    .returns(BUILTIN_TREE)
                    .addParameter(ParameterSpec.builder(IMPORT_STATE_ID, "importState").build())
                    .addParameter(ParameterSpec.builder(GEN_BUILTIN_INTERFACES, "interfaces").build());

    build.addCode("return $L;\n", nodeExpr(root));
    return build.build();
  }

  private static CodeBlock nodeExpr(NodeSpec node) {
    return switch (node) {
      case Branch b -> branchExpr(b);
      case Leaf l -> leafExpr(l);
    };
  }

  private static CodeBlock branchExpr(Branch b) {
    var inner = CodeBlock.builder();
    inner.add("$T.<$T>of(", TLIST, BUILTIN_TREE_CHILD);
    for (var i = 0; i < b.children().size(); i++) {
      var c = b.children().get(i);
      if (i > 0) inner.add(", ");
      inner.add("new $T($S, $L)", BUILTIN_TREE_CHILD, c.name(), nodeExpr(c.node()));
    }
    inner.add(")");
    return CodeBlock.of("new $T($L)", BUILTIN_TREE_BRANCH, inner.build());
  }

  private static CodeBlock leafExpr(Leaf l) {
    return CodeBlock.of("new $T($L)", BUILTIN_TREE_LEAF, leafSpecExpr(l.leaf()));
  }

  private static CodeBlock leafSpecExpr(LeafSpec spec) {
    return switch (spec) {
      case LeafSpec.AnyConst a ->
          CodeBlock.of("new $T(($T) $L)", BUILTIN_LEAF_ANY_CONST, TYPE, a.typeExpr());
      case LeafSpec.InitialBind ib ->
          CodeBlock.of(
              "new $T(new $T($LL), ($T) $L)",
              BUILTIN_LEAF_INITIAL_BIND,
              BINDING_ID_BUILTIN,
              ib.dynamicKeyScopeId(),
              TYPE,
              ib.typeExpr());
      case LeafSpec.AspectId ai -> {
        var aspectClass = ClassName.get(GEN_PACKAGE, "Aspect" + ai.aspectName());
        yield CodeBlock.of(
            "new $T(new $T($LL), $T.ID, new $T($T.ID))",
            BUILTIN_LEAF_ASPECT_ID,
            BINDING_ID_BUILTIN,
            ai.builtinScopeId(),
            aspectClass,
            EXTERNAL_UNIQUE,
            aspectClass);
      }
      case LeafSpec.Interface ifc ->
          CodeBlock.of(

              "$L",
              interfaceLeafExpr(ifc.spec()));
      case LeafSpec.Function fn -> {
        var helperClass = ClassName.get(GEN_PACKAGE, builtinHelperName(fn.builtinScopeId()));
        var helperInternalName =
            (GEN_PACKAGE + "." + builtinHelperName(fn.builtinScopeId())).replace('.', '/');
        yield CodeBlock.of(
            "new $T(new $T($LL), ($T) $T.builtinWithCtx($S, $S, $L, $L))",
            BUILTIN_LEAF_FUNCTION,
            BINDING_ID_BUILTIN,
            fn.builtinScopeId(),
            TYPE,
            ClassName.get("compilerxyz.eval.value.function", "TypeFunctionDynamic"),
            helperInternalName,
            "invoke",
            argDynamicTypeExpr(fn.args()),
            fn.retDynamicType());
      }
      case LeafSpec.Constructor c -> {
        yield CodeBlock.of(
            "new $T(new $T($LL), ($T) $L)",
            BUILTIN_LEAF_CONSTRUCTOR,
            BINDING_ID_BUILTIN,
            c.builtinScopeId(),
            TYPE,
            c.typeExpr());
      }
    };
  }

  private static CodeBlock interfaceLeafExpr(InterfaceSpec spec) {

    var sourceInterfaceConst =
        ClassName.get("compilerxyz.eval.value.source_interface", "TypeSourceInterfaceConst");
    return CodeBlock.of(
        "new $T(new $T($LL), ($T) new $T(interfaces.$N),"
            + " new $T(new $T(interfaces.$N)))",
        BUILTIN_LEAF_INTERFACE,
        BINDING_ID_BUILTIN,
        (long) spec.scopeId(),
        TYPE,
        sourceInterfaceConst,
        ifaceHolderFieldName(spec),
        EXTERNAL_SOURCE_INTERFACE_REF,
        SOURCE_INTERFACE_REF,
        ifaceHolderFieldName(spec));
  }

  private static CodeBlock argDynamicTypeExpr(TList<NamedTypeRef> args) {
    if (args.size() == 1) {
      return args.get(0).type().dynamicType();
    }
    var tuple = ClassName.get("compilerxyz.eval.value.tuple", "Tuple");
    var tupleElement = ClassName.get("compilerxyz.eval.value.tuple", "TupleElement");
    var tupleElementDynamic =
        ClassName.get("compilerxyz.eval.value.tuple", "TupleElement", "Dynamic");
    var opt = ClassName.get("compilerxyz.util", "Opt");
    var elements = CodeBlock.builder();
    elements.add("$T.<$T>of(", TLIST, tupleElement);
    for (var i = 0; i < args.size(); i++) {
      if (i > 0) elements.add(", ");
      elements.add("new $T($L)", tupleElementDynamic, args.get(i).type().dynamicType());
    }
    elements.add(")");
    return CodeBlock.of("new $T($T.none(), $L)", tuple, opt, elements.build());
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
    var any = TypeRef.unit(TYPE_ANY_DYNAMIC);
    var cve = TypeRef.unit(TYPE_CODE_VALUE_EXTERN_DYNAMIC);
    var xxRef =
        TypeRef.unit(
            ClassName.get(
                "compilerxyz.eval.value.code_value_extern", "TypeExprContextPushDynamic"));

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
            TList.of(
                new Method(
                    "call",
                    cve,
                    new MethodArgs.Source(
                        TList.of(new NamedTypeRef("inner", cve), new NamedTypeRef("argument", cve))))));

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
            TList.of(
                new Method(
                    "field",
                    cve,
                    new MethodArgs.Source(
                        TList.of(new NamedTypeRef("inner", cve), new NamedTypeRef("key", cve)))),
                new Method(
                    "field_type_opt",
                    any,
                    new MethodArgs.Source(TList.of(new NamedTypeRef("key", cve))))));

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
            TList.of(
                new Method(
                    "drop",
                    cve,
                    new MethodArgs.Source(
                        TList.of(new NamedTypeRef("xx", xxRef), new NamedTypeRef("inner", cve))))));

    var codeRef = ClassName.get("compilerxyz.eval", "Code");
    var qexprId = ClassName.get("compilerxyz.eval", "QExprId");
    var codeValue = ClassName.get("compilerxyz.eval", "CodeValue");
    var exprContext = ClassName.get("compilerxyz.eval", "ExprContext");
    var exprContextPushType =
        ClassName.get("compilerxyz.eval.value.code_value_extern", "ExprContextPush");

    var bindingXx = TypeRef.viaPushAs(exprContext, exprContextPushType);
    var bindingLoc = TypeRef.viaPush(qexprId);
    var bindingCode = TypeRef.viaPush(codeRef);
    var bindingCv = TypeRef.viaPush(codeValue);

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
            ClassName.get("compilerxyz.eval.value.binding", "TypeAnyBindingDynamic"),
            TList.of(
                new Method(
                    "bindingUse_",
                    bindingCv,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(new NamedTypeRef("code", bindingCode)))))));

    var targetBindSub =
        TypeRef.viaPush(
            ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub"));
    var codeBindingTuple =
        TypeRef.opaque(
            ParameterizedTypeName.get(ClassName.get("compilerxyz.util", "Pair"), codeRef, BINDING_ID));
    var eresultCode =
        TypeRef.opaque(
            ParameterizedTypeName.get(ClassName.get("compilerxyz.eval", "EResult"), codeRef));
    var eresultCodeValue =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.eval", "EResult"), codeValue));
    var eresultTargetBindSub =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.eval", "EResult"),
                ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub")));
    var eresultOptionTargetBindSub =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.eval", "EResult"),
                ParameterizedTypeName.get(
                    ClassName.get("compilerxyz.util", "Opt"),
                    ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub"))));
    var constKey = ClassName.get("compilerxyz.eval", "ConstKey");
    var expr = ClassName.get("compilerxyz.eval", "Expr");
    var listFields =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.util", "TList"),
                ParameterizedTypeName.get(
                    ClassName.get("compilerxyz.util", "Pair"),
                    constKey,
                    ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub"))));
    var listElements =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.util", "TList"),
                ClassName.get("compilerxyz.eval.dynamic.target", "TargetBindSub")));
    var listEntries =
        TypeRef.opaque(
            ParameterizedTypeName.get(ClassName.get("compilerxyz.util", "TList"), codeRef));
    var typeOpaque = TypeRef.opaque(TYPE);
    var listConditions =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.util", "TList"),
                ParameterizedTypeName.get(
                    ClassName.get("compilerxyz.util", "Pair"), codeValue, expr)));
    var optExpr =
        TypeRef.opaque(
            ParameterizedTypeName.get(ClassName.get("compilerxyz.util", "Opt"), expr));
    var caseListOpaque =
        TypeRef.opaque(
            ParameterizedTypeName.get(
                ClassName.get("compilerxyz.util", "TList"),
                ClassName.get("compilerxyz.eval.dynamic.target", "TargetCase")));
    var codeValueOpaque = TypeRef.opaque(codeValue);

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
            ClassName.get("compilerxyz.eval.value.binding", "TypeAnyTargetDynamic"),
            TList.of(
                new Method(
                    "bind",
                    codeBindingTuple,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(new NamedTypeRef("sub", targetBindSub))))),
                new Method(
                    "bind_sub_record",
                    targetBindSub,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("carry", bindingCode),
                                new NamedTypeRef("fields", listFields))))),
                new Method(
                    "bind_sub_tuple",
                    targetBindSub,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("carry", bindingCode),
                                new NamedTypeRef("elements", listElements))))),
                new Method(
                    "seq",
                    eresultCode,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(new NamedTypeRef("loc", bindingLoc)),
                            TList.of(new NamedTypeRef("entries", listEntries))))),
                new Method(
                    "code_to_bind_sub",
                    eresultTargetBindSub,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("code", bindingCode),
                                new NamedTypeRef("type_", typeOpaque))))),
                new Method(
                    "if_",
                    eresultCodeValue,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("conditions", listConditions),
                                new NamedTypeRef("else_", optExpr))))),
                new Method(
                    "match_",
                    eresultCodeValue,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("base", codeValueOpaque),
                                new NamedTypeRef(
                                    "element_key", TypeRef.viaPush(constKey)),
                                new NamedTypeRef("cases", caseListOpaque),
                                new NamedTypeRef("fallback", optExpr))))),
                new Method(
                    "maybe_bind_sub",
                    eresultOptionTargetBindSub,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(new NamedTypeRef("cv", codeValueOpaque))))),
                new Method(
                    "stage",
                    eresultCodeValue,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("tree", TypeRef.viaPush(expr)),
                                new NamedTypeRef(
                                    "lower_values",
                                    TypeRef.opaque(
                                        ParameterizedTypeName.get(
                                            ClassName.get("compilerxyz.util", "TList"), codeValue))),
                                new NamedTypeRef(
                                    "graft_values",
                                    TypeRef.opaque(
                                        ParameterizedTypeName.get(
                                            ClassName.get("compilerxyz.util", "TList"), codeValue))))))),
                new Method(
                    "cast_exception",
                    eresultCode,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("call_code", bindingCode),
                                new NamedTypeRef("catch_binding", TypeRef.opaque(BINDING_ID)),
                                new NamedTypeRef("cast_cv", codeValueOpaque))))),
                new Method(
                    "drop",
                    eresultCode,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(
                                new NamedTypeRef("xx", bindingXx),
                                new NamedTypeRef("loc", bindingLoc)),
                            TList.of(new NamedTypeRef("cv", codeValueOpaque))))),
                new Method(
                    "try_finally",
                    eresultCode,
                    new MethodArgs.Rust(
                        new MethodSig(
                            TList.of(new NamedTypeRef("loc", bindingLoc)),
                            TList.of(
                                new NamedTypeRef("body", bindingCode),
                                new NamedTypeRef("finally_", bindingCode)))))));

    var codeDyn =
        new InterfaceSpec(
            "code_dyn", 2007, 3007, "cd", "CodeDyn", null, true, null, TList.<Method>of());

    return TList.of(aspectFunction, aspectField, aspectDropDynamic, binding, target, codeDyn);
  }

  public static NodeSpec canonicalSpec() {
    return new Branch(
        TList.of(
            new Child(
                "throw",
                new Leaf(
                    new LeafSpec.InitialBind(
                        THROW_COMPILE_SCOPE_ID,
                        CodeBlock.of("new $T()", TYPE_THROW_DYNAMIC)))),
            new Child(
                "panic",
                new Leaf(
                    new LeafSpec.InitialBind(
                        PANIC_COMPILE_SCOPE_ID,
                        CodeBlock.of("new $T()", TYPE_PANIC_DYNAMIC)))),
            new Child(
                "log",
                new Leaf(
                    new LeafSpec.Function(
                        5001,
                        TList.of(
                            new NamedTypeRef(
                                "message",
                                new TypeRef(
                                    ClassName.get(String.class),
                                    CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC)))),
                        ClassName.VOID,
                        unitTupleDynamic(),
                        CodeBlock.builder()
                            .addStatement("long timestampMs = $T.currentTimeMillis()", System.class)
                            .addStatement(
                                "xx.vx.logs.add(new $T(timestampMs, message))",
                                ClassName.get("compilerxyz.eval", "LogEntry"))
                            .addStatement("return null")
                            .build()))),
            new Child(
                "define",
                new Leaf(
                    new LeafSpec.Function(
                        5002,
                        TList.of(),
                        ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder"),
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder")),
                        CodeBlock.builder()
                            .addStatement("var ex = xx.vx")
                            .addStatement(
                                "var compileId = new $T(ex.ix.importState, ex.ix.sourceId)",
                                ClassName.get("compilerxyz.eval", "CompileId"))
                            .addStatement(
                                "var defSetId = new $T(compileId, ex.ix.defSetCounter.next())",
                                ClassName.get("compilerxyz.eval", "DefSetId"))
                            .addStatement(
                                "var inner = new $T(defSetId)",
                                ClassName.get("compilerxyz.eval", "DefinitionSet"))
                            .addStatement("ex.definitionSets.put(defSetId, inner)")
                            .addStatement(
                                "return new $T(inner)",
                                ClassName.get(GEN_PACKAGE, "DefinitionSetBuilder"))
                            .build()))),
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
                        5010,
                        TList.of(
                            new NamedTypeRef(
                                "element_type",
                                TypeRef.unit(ClassName.get("compilerxyz.eval.push", "AnyType")))),
                        TYPE_LIST_DYNAMIC,
                        CodeBlock.of("new $T()", TYPE_LIST_DYNAMIC),
                        CodeBlock.of("new $T()", TYPE_LIST_CONSTRUCTOR_CONST),
                        pendingBody(
                            5010,
                            "types.list",
                            "TypeListDynamic instantiation from dynamic AnyType arg")))),
            new Child(
                "map",
                new Leaf(
                    new LeafSpec.Constructor(
                        5011,
                        TList.of(
                            new NamedTypeRef(
                                "key_type",
                                TypeRef.unit(ClassName.get("compilerxyz.eval.push", "AnyType"))),
                            new NamedTypeRef(
                                "value_type",
                                TypeRef.unit(ClassName.get("compilerxyz.eval.push", "AnyType")))),
                        TYPE_MAP_DYNAMIC,
                        CodeBlock.of("new $T()", TYPE_MAP_DYNAMIC),
                        CodeBlock.of("new $T()", TYPE_MAP_CONSTRUCTOR_CONST),
                        pendingBody(
                            5011,
                            "types.map",
                            "TypeMapDynamic instantiation from dynamic AnyType args")))),
            new Child(
                "option",
                new Leaf(
                    new LeafSpec.Constructor(
                        5012,
                        TList.of(
                            new NamedTypeRef(
                                "element_type",
                                TypeRef.unit(ClassName.get("compilerxyz.eval.push", "AnyType")))),
                        TYPE_OPTION_DYNAMIC,
                        CodeBlock.of("new $T()", TYPE_OPTION_DYNAMIC),
                        CodeBlock.of("new $T()", TYPE_OPTION_CONSTRUCTOR_CONST),
                        pendingBody(
                            5012,
                            "types.option",
                            "TypeOptionDynamic instantiation from dynamic AnyType arg")))),
            new Child(
                "string",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC)))),
            new Child(
                "number",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_NUMBER_DYNAMIC)))),
            new Child(
                "null",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC)))),
            new Child(
                "bool",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_BOOL_DYNAMIC)))),
            new Child(
                "any",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC)))),
            new Child(
                "constkey",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_CONSTKEY_DYNAMIC)))),
            new Child(
                "expr", new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_EXPR)))),
            new Child(
                "type",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_ANY_DYNAMIC)))),
            new Child(
                "aspect",
                new Leaf(new LeafSpec.AnyConst(CodeBlock.of("new $T()", TYPE_ASPECT_DYNAMIC)))),
            new Child(
                "codedyn",
                new Leaf(
                    new LeafSpec.AnyConst(
                        CodeBlock.of("new $T()", TYPE_CODE_DYN_HANDLE_DYNAMIC)))),
            new Child(
                "cve",
                new Leaf(
                    new LeafSpec.AnyConst(
                        CodeBlock.of("new $T()", TYPE_CODE_VALUE_EXTERN_DYNAMIC))))));
  }

  private static NodeSpec evalBranch() {
    var extras = extraInterfaceSpecs();
    var ifaceAspectFunction = findSpec(extras, "aspect_function");
    var ifaceAspectField = findSpec(extras, "aspect_field");
    var ifaceAspectDropDynamic = findSpec(extras, "aspect_drop_dynamic");
    var ifaceBinding = findSpec(extras, "binding");
    var ifaceTarget = findSpec(extras, "target");
    var ifaceCodeDyn = findSpec(extras, "code_dyn");

    return new Branch(
        TList.of(
            new Child(
                "aspect_lookup",
                new Leaf(
                    new LeafSpec.InitialBind(
                        ASPECT_COMPILE_SCOPE_ID,
                        CodeBlock.of("new $T()", TYPE_FUNCTION_ASPECT_DYNAMIC)))),
            new Child(
                "cast_value",
                new Leaf(
                    new LeafSpec.InitialBind(
                        CAST_VALUE_COMPILE_SCOPE_ID,
                        CodeBlock.of("new $T()", TYPE_FUNCTION_CAST_VALUE_DYNAMIC)))),

            new Child(
                "const_eval",
                new Leaf(
                    new LeafSpec.Function(
                        5008,
                        TList.of(
                            new NamedTypeRef(
                                "xx_",
                                new TypeRef(
                                    TypeName.OBJECT,
                                    CodeBlock.of(
                                        "$T.dynamicType()",
                                        ClassName.get(
                                            "compilerxyz.eval.value.code_value_extern",
                                            "ExprContextPush")))),
                            new NamedTypeRef(
                                "staged",
                                new TypeRef(
                                    TypeName.OBJECT,
                                    CodeBlock.of(
                                        "$T.dynamicType()",
                                        ClassName.get(
                                            "compilerxyz.eval.push", "StagedExpr"))))),
                        ClassName.get(
                            "compilerxyz.eval.value.code_value_extern", "CodeValueExtern"),
                        CodeBlock.of("new $T()", TYPE_CODE_VALUE_EXTERN_DYNAMIC),
                        constEvalBody()))),
            new Child(
                "codevalue",
                new Leaf(
                    new LeafSpec.Function(
                        5005,
                        TList.of(
                            new NamedTypeRef(
                                "code", TypeRef.viaPush(ClassName.get("compilerxyz.eval", "Code"))),
                            new NamedTypeRef(
                                "value",
                                TypeRef.viaPush(ClassName.get("compilerxyz.eval", "Value")))),
                        ClassName.get(
                            "compilerxyz.eval.value.code_value_extern", "CodeValueExtern"),
                        CodeBlock.of("new $T()", TYPE_CODE_VALUE_EXTERN_DYNAMIC),
                        pendingBody(
                            5005,
                            "codevalue",
                            "CodeValueExtern construction from dynamic args requires Push"
                                + " decode for Code + Value")))),
            new Child(
                "str_to_json",
                new Leaf(
                    new LeafSpec.Function(
                        5003,
                        TList.of(
                            new NamedTypeRef(
                                "s",
                                new TypeRef(
                                    ClassName.get(String.class),
                                    CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC)))),
                        ClassName.get(String.class),
                        CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC),
                        pendingBody(
                            5003,
                            "str_to_json",
                            "serde_json::to_string equivalent on JVM (com.fasterxml.jackson or"
                                + " similar) not yet selected for the runtime")))),
            new Child(
                "constkey_to_str",
                new Leaf(
                    new LeafSpec.Function(
                        5004,
                        TList.of(
                            new NamedTypeRef(
                                "key", TypeRef.viaPush(ClassName.get("compilerxyz.eval", "ConstKey")))),
                        ClassName.get(String.class),
                        CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC),
                        pendingBody(
                            5004,
                            "constkey_to_str",
                            "ConstKey.toStr() bridge from dynamic invocation not yet wired")))),
            new Child(
                "gen_binding_name",
                new Leaf(
                    new LeafSpec.Function(
                        5006,
                        TList.of(
                            new NamedTypeRef(
                                "_xx",
                                TypeRef.viaPush(
                                    ClassName.get(
                                        "compilerxyz.eval.value.code_value_extern",
                                        "ExprContextPush")))),
                        ClassName.get(String.class),
                        CodeBlock.of("new $T()", TYPE_STRING_DYNAMIC),
                        pendingBody(
                            5006,
                            "gen_binding_name",
                            "requires next_local_id allocation from xx.vx — pending Push decode"
                                + " parity")))),
            new Child(
                "new_expr_context",
                new Leaf(
                    new LeafSpec.Function(
                        5009,
                        TList.of(
                            new NamedTypeRef(
                                "target_arg",
                                TypeRef.viaPush(ClassName.get(GEN_PACKAGE, "Target"))),
                            new NamedTypeRef(
                                "scope_arg",
                                TypeRef.opaque(
                                    ParameterizedTypeName.get(
                                        ClassName.get("compilerxyz.util", "TMap"),
                                        ClassName.get("compilerxyz.eval", "ConstKey"),
                                        ClassName.get(GEN_PACKAGE, "Binding"))))),
                        ClassName.get(
                            "compilerxyz.eval.value.code_value_extern", "ExprContextPush"),
                        CodeBlock.of(
                            "$T.dynamicType()",
                            ClassName.get(
                                "compilerxyz.eval.value.code_value_extern",
                                "ExprContextPush")),
                        pendingBody(
                            5009,
                            "new_expr_context",
                            "requires RcBindingWrap + Scope construction — pending Push decode"
                                + " parity")))),
            new Child("aspect_function", new Leaf(new LeafSpec.Interface(ifaceAspectFunction))),
            new Child("aspect_field", new Leaf(new LeafSpec.Interface(ifaceAspectField))),
            new Child(
                "aspect_drop_dynamic",
                new Leaf(new LeafSpec.Interface(ifaceAspectDropDynamic))),
            new Child(
                "aspect_string_dynamic",
                new Leaf(new LeafSpec.AspectId(4001, "StringDynamic"))),
            new Child(
                "aspect_function_id",
                new Leaf(new LeafSpec.AspectId(4002, "Function"))),
            new Child(
                "aspect_field_id",
                new Leaf(new LeafSpec.AspectId(4003, "Field"))),
            new Child("binding", new Leaf(new LeafSpec.Interface(ifaceBinding))),
            new Child("target", new Leaf(new LeafSpec.Interface(ifaceTarget))),
            new Child("code_dyn", new Leaf(new LeafSpec.Interface(ifaceCodeDyn)))));
  }

  private static InterfaceSpec findSpec(TList<InterfaceSpec> specs, String name) {
    for (var s : specs) if (s.name().equals(name)) return s;
    throw new IllegalStateException("missing extra-interface spec: " + name);
  }

  private static CodeBlock unitTupleDynamic() {
    return CodeBlock.of("new $T()", TYPE_NULL_DYNAMIC);
  }

  private static CodeBlock constEvalBody() {
    var exprContext = ClassName.get("compilerxyz.eval", "ExprContext");
    var exprContextPush =
        ClassName.get("compilerxyz.eval.value.code_value_extern", "ExprContextPush");
    var stagedCell = ClassName.get("compilerxyz.eval.expr", "StagedExprCell");
    var expr = ClassName.get("compilerxyz.eval", "Expr");
    var constEval = ClassName.get("compilerxyz.eval", "ConstEval");
    var codeValueExternCell = ClassName.get("compilerxyz.eval.expr", "CodeValueExternCell");
    var b = CodeBlock.builder();
    b.addStatement("$T targetXx", exprContext);
    b.beginControlFlow("if (xx_ instanceof $T push)", exprContextPush);
    b.addStatement("targetXx = push.context()");
    b.nextControlFlow("else if (xx_ instanceof $T raw)", exprContext);
    b.addStatement("targetXx = raw");
    b.nextControlFlow("else");
    b.addStatement(
        "throw new $T($S + (xx_ == null ? $S : xx_.getClass()))",
        AssertionError.class,
        "builtin 5008 (const_eval): xx is not ExprContextPush, got ",
        "null");
    b.endControlFlow();
    b.addStatement("Object stagedObj = staged");
    b.addStatement("Object exprValue");
    b.beginControlFlow("if (stagedObj instanceof $T cell)", stagedCell);
    b.addStatement("exprValue = cell.peek()");
    b.beginControlFlow("if (exprValue == null)");
    b.addStatement(
        "throw new $T($S)",
        IllegalStateException.class,
        "builtin 5008 (const_eval): staged cell already null (was consumed elsewhere)");
    b.endControlFlow();
    b.nextControlFlow("else");
    b.addStatement("exprValue = stagedObj");
    b.endControlFlow();
    b.beginControlFlow("if (!(exprValue instanceof $T exprAst))", expr);
    b.addStatement(
        "throw new $T($S + (exprValue == null ? $S : exprValue.getClass()))",
        AssertionError.class,
        "builtin 5008 (const_eval): staged payload is not Expr, got ",
        "null");
    b.endControlFlow();
    b.addStatement("var res = $T.constEval(targetXx, exprAst)", constEval);
    b.beginControlFlow("if (res.hasErrors())");
    b.addStatement(
        "throw new $T($S + res.errorsOrNull())",
        IllegalStateException.class,
        "builtin 5008 (const_eval): const_eval produced no value; errors: ");
    b.endControlFlow();
    b.addStatement(
        "var resCv = res.requireValue($S)",
        "BuiltinTreeGen builtin 5008 const_eval result");
    b.addStatement("return new $T(resCv)", codeValueExternCell);
    return b.build();
  }

  private static CodeBlock pendingBody(long scopeId, String name, String reason) {
    return CodeBlock.builder()
        .addStatement(
            "throw new $T($S)",
            UnsupportedOperationException.class,
            "builtin scope id "
                + scopeId
                + " ("
                + name
                + "): port of builtin_tree.rs body pending — "
                + reason)
        .build();
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
          case LeafSpec.AnyConst a -> null;
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
