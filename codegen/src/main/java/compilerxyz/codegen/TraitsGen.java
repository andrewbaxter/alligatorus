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
  private static final ClassName TYPE_UNIQUE =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");

  public static TList<InterfaceSpec> specs() {
    var any = TypeRef.leaf(TYPE);
    var string = TypeRef.leaf(TYPE_STRING_DYNAMIC);
    var bool = TypeRef.leaf(TYPE_BOOL_DYNAMIC);
    var aspect = TypeRef.leaf(ASPECT_INTERFACE);
    var cve = TypeRef.leaf(TYPE_CODE_VALUE_EXTERN_DYNAMIC);
    var eresultCve = TypeRef.composite1(TYPE_ERESULT_DYNAMIC, cve);
    var xxRef = TypeRef.leaf(EXPR_CONTEXT);
    var unique = TypeRef.leaf(TYPE_UNIQUE);

    var aspectOption = TypeRef.composite1(TYPE_OPTION_DYNAMIC, aspect);

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

    var qexprIdRef = TypeRef.leaf(ClassName.get("compilerxyz.eval", "QExprId"));
    var typeIdRef =
        TypeRef.leaf(ClassName.get("compilerxyz.eval.value.typeid", "TypeTypeIdDynamic"));
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
            null,
            "compilerxyz.eval.Type",
            TList.of(
                new Method(
                    "canCastFrom",
                    bool,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("source_type", any)))),
                new Method(
                    "castFrom",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve)))),
                new Method(
                    "canCastTo",
                    bool,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("dest_type", any)))),
                new Method(
                    "castTo",
                    eresultCve,
                    new MethodArgs(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("loc", qexprIdRef),
                            new NamedTypeRef("cv", cve),
                            new NamedTypeRef("dest_type", any)))),
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
