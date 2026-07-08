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

  private static final ClassName TYPE_ANY_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.any", "TypeAnyDynamic");
  private static final ClassName TYPE_STRING_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.string", "TypeStringDynamic");
  private static final ClassName TYPE_BOOL_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.bool_", "TypeBoolDynamic");
  private static final ClassName TYPE_ASPECT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.aspect", "TypeAspectDynamic");
  private static final ClassName TYPE_CODE_VALUE_EXTERN_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code_value_extern", "TypeCodeValueExternDynamic");
  private static final ClassName EXPR_CONTEXT =
      ClassName.get("compilerxyz.eval", "ExprContext");
  private static final ClassName TYPE_EXPR_CONTEXT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.code_value_extern", "TypeExprContextDynamic");
  private static final ClassName TYPE_REF_PUSH =
      ClassName.get("compilerxyz.gen", "TypeRefPush");
  private static final ClassName TYPE_UNIQUE =
      ClassName.get("compilerxyz.eval.value.unique", "TypeUnique");
  private static final ClassName TYPE_OPTION_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.option", "TypeOptionDynamic");
  private static final ClassName TYPE_ERESULT_DYNAMIC =
      ClassName.get("compilerxyz.eval.value.eresult", "TypeEResultDynamic");

  public static TList<InterfaceSpec> specs() {
    var any = TypeRef.unit(TYPE_ANY_DYNAMIC);
    var string = TypeRef.unit(TYPE_STRING_DYNAMIC);
    var bool = TypeRef.unit(TYPE_BOOL_DYNAMIC);
    var aspect = TypeRef.unit(TYPE_ASPECT_DYNAMIC);
    var cve = TypeRef.unit(TYPE_CODE_VALUE_EXTERN_DYNAMIC);
    var eresultCve =
        new TypeRef(
            TYPE_ERESULT_DYNAMIC,
            CodeBlock.of(
                "new $T(new $T())", TYPE_ERESULT_DYNAMIC, TYPE_CODE_VALUE_EXTERN_DYNAMIC));
    var xxRef =
        new TypeRef(EXPR_CONTEXT, CodeBlock.of("new $T()", TYPE_EXPR_CONTEXT_DYNAMIC));
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
            null,
            TList.of(new Method("type", any, new MethodArgs.Source(TList.of()))));

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
                    new MethodArgs.Source(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("source_type", typeRefPush),
                            new NamedTypeRef("cv", cve)))),
                new Method(
                    "cast_from",
                    eresultCve,
                    new MethodArgs.Source(
                        TList.of(
                            new NamedTypeRef("xx", xxRef),
                            new NamedTypeRef("source_type", typeRefPush),
                            new NamedTypeRef("cv", cve)))),
                new Method("desc", string, new MethodArgs.Source(TList.of())),
                new Method(
                    "aspect",
                    aspectOption,
                    new MethodArgs.Source(TList.of(new NamedTypeRef("key", unique))))));

    return TList.of(valueSpec, typeSpec);
  }
}
