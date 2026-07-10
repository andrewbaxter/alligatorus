package compilerxyz.codegen;

import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.codegen.util.InterfaceSpec;
import compilerxyz.codegen.util.Method;
import compilerxyz.codegen.util.MethodArgs;
import compilerxyz.codegen.util.NamedTypeRef;
import compilerxyz.codegen.util.TypeRef;
import compilerxyz.util.TList;

public final class TraitsGen {
  private TraitsGen() {}

  private static final ClassName TYPE = ClassName.get("compilerxyz.eval", "Type");
  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName ASPECT_INTERFACE =
      ClassName.get("compilerxyz.eval.aspect", "Aspect");
  private static final ClassName TYPE_CODE_VALUE_EXTERN_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code_value_extern", "TypeCodeValueExternDynamic");
  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName TYPE_REF_PUSH =
      ClassName.get("compilerxyz.gen", "TypeRefPush");
  private static final ClassName TYPE_UNIQUE =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");

  public static TList<InterfaceSpec> specs() {
    var any = TypeRef.opaque(TYPE);
    var string = TypeRef.unit(TYPE_STRING_DYNAMIC);
    var bool = TypeRef.unit(TYPE_BOOL_DYNAMIC);
    var aspect = TypeRef.viaPush(ASPECT_INTERFACE);
    var cve = TypeRef.unit(TYPE_CODE_VALUE_EXTERN_DYNAMIC);
    var eresultCve =
        new TypeRef(
            TYPE_ERESULT_DYNAMIC,
            CodeBlock.of(
                "new $T(new $T())", TYPE_ERESULT_DYNAMIC, TYPE_CODE_VALUE_EXTERN_DYNAMIC));
    var xxRef = TypeRef.viaPush(EXPR_CONTEXT);
    var typeRefPush = TypeRef.viaPush(TYPE_REF_PUSH);
    var unique = TypeRef.unit(TYPE_UNIQUE);

    var aspectOption =
        new TypeRef(
            TYPE_OPTION_DYNAMIC,
            CodeBlock.of("new $T($L)", TYPE_OPTION_DYNAMIC, aspect.dynamicType()));

    var valueSpec =
        new InterfaceSpec(
            "value",
            2001,
            3001,
            "v",
            "Value",
            null,
            false,
            "Value",
            "TYPE_VALUE_DYNAMIC_TYPE_ID",
            TList.of(new Method("type", any, new MethodArgs(TList.of()))));

    var qexprIdRef = TypeRef.viaPush(ClassName.get("compilerxyz.eval", "QExprId"));
    var typeIdRef =
        TypeRef.opaque(ClassName.get("compilerxyz.eval", "TypeId"));
    var typeSpec =
        new InterfaceSpec(
            "type",
            2002,
            3002,
            "t",
            "Type",
            null,
            false,
            null,
            TList.of(
                new Method(
                    "can_cast_from",
                    bool,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("source_type", typeRefPush)))),
                new Method(
                    "cast_from",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve)))),
                new Method(
                    "can_cast_to",
                    bool,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("dest_type", typeRefPush)))),
                new Method(
                    "cast_to",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve),
                            new NamedTypeRef("dest_type", typeRefPush)))),
                new Method("desc", string, new MethodArgs(TList.of())),
                new Method("id", typeIdRef, new MethodArgs(TList.of())),
                new Method(
                    "aspect",
                    aspectOption,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("key", unique))))));

    return TList.of(valueSpec, typeSpec);
  }
}
