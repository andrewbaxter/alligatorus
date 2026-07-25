package compilerxyz.tests;

import compilerxyz.syntax.ModuleBuilder;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public final class TestMb extends ModuleBuilder {
  public SourceExpr define() {
    return builtin("define");
  }

  public SourceExpr evalAspectField() {
    return builtin("eval", "aspect_field");
  }

  public SourceExpr evalAspectFieldId() {
    return builtin("eval", "aspect_field_id");
  }

  public SourceExpr evalAspectFunction() {
    return builtin("eval", "aspect_function");
  }

  public SourceExpr evalAspectFunctionId() {
    return builtin("eval", "aspect_function_id");
  }

  public SourceExpr evalAspectLookup() {
    return builtin("eval", "aspect_lookup");
  }

  public SourceExpr evalAspectStringDynamic() {
    return builtin("eval", "aspect_string_dynamic");
  }

  public SourceExpr evalAspectDropDynamic() {
    return builtin("eval", "aspect_drop_dynamic");
  }

  public SourceExpr evalAspectDropDynamicLight() {
    return builtin("eval", "aspect_drop_dynamic_light");
  }

  public SourceExpr evalAspectConsumeDynamic() {
    return builtin("eval", "aspect_consume_dynamic");
  }

  public SourceExpr evalAspectBindDynamic() {
    return builtin("eval", "aspect_bind_dynamic");
  }

  public SourceExpr evalAspectFieldUseDynamic() {
    return builtin("eval", "aspect_field_use_dynamic");
  }

  public SourceExpr evalAspectFieldUseDynamicId() {
    return builtin("eval", "aspect_field_use_dynamic_id");
  }

  public SourceExpr evalAspectElementUseDynamic() {
    return builtin("eval", "aspect_element_use_dynamic");
  }

  public SourceExpr evalAspectElementUseDynamicId() {
    return builtin("eval", "aspect_element_use_dynamic_id");
  }

  public SourceExpr evalAspectSet() {
    return builtin("eval", "aspect_set");
  }

  public SourceExpr evalOperatorAdd() {
    return builtin("eval", "operator_add");
  }

  public SourceExpr evalOperatorMod() {
    return builtin("eval", "operator_mod");
  }

  public SourceExpr mustReinterpret() {
    return builtin("must_reinterpret");
  }

  public SourceExpr evalValue() {
    return builtin("eval", "value");
  }

  public SourceExpr evalValueSource() {
    return builtin("eval", "value_source");
  }

  public SourceExpr evalType() {
    return builtin("eval", "type");
  }

  public SourceExpr evalBinding() {
    return builtin("eval", "binding");
  }

  public SourceExpr evalAspectBind() {
    return builtin("eval", "aspect_bind");
  }

  public SourceExpr evalAspectInitialBind() {
    return builtin("eval", "aspect_initial_bind");
  }

  public SourceExpr evalAspectLift() {
    return builtin("eval", "aspect_lift");
  }

  public SourceExpr evalAspectInt() {
    return builtin("eval", "aspect_int");
  }

  public SourceExpr evalTypeIntDynamicIdentity() {
    return builtin("eval", "type_int_dynamic_identity");
  }

  public SourceExpr evalAspectBindId() {
    return builtin("eval", "aspect_bind_id");
  }

  public SourceExpr evalAspectDropDynamicId() {
    return builtin("eval", "aspect_drop_dynamic_id");
  }

  public SourceExpr evalAspectConsumeDynamicId() {
    return builtin("eval", "aspect_consume_dynamic_id");
  }

  public SourceExpr evalAspectBindDynamicId() {
    return builtin("eval", "aspect_bind_dynamic_id");
  }

  public SourceExpr evalAspectSetId() {
    return builtin("eval", "aspect_set_id");
  }

  public SourceExpr evalOperatorAddId() {
    return builtin("eval", "aspect_operator_add_id");
  }

  public SourceExpr evalOperatorModId() {
    return builtin("eval", "aspect_operator_mod_id");
  }

  public SourceExpr evalAspectDropDynamicLightId() {
    return builtin("eval", "aspect_drop_dynamic_light_id");
  }

  public SourceExpr evalAspectStackValueDynamicId() {
    return builtin("eval", "aspect_stack_value_dynamic_id");
  }

  public SourceExpr evalAspectInitialBindId() {
    return builtin("eval", "aspect_initial_bind_id");
  }

  public SourceExpr evalFreshUnique() {
    return builtin("eval", "fresh_unique");
  }

  public SourceExpr evalBindResult() {
    return builtin("eval", "bind_result");
  }

  public SourceExpr evalCastValue() {
    return builtin("eval", "cast_value");
  }

  public SourceExpr evalCodeDyn() {
    return builtin("eval", "code_dyn");
  }

  public SourceExpr evalCodevalue() {
    return builtin("eval", "codevalue");
  }

  public SourceExpr evalConstEval() {
    return builtin("eval", "const_eval");
  }

  public SourceExpr evalConstkeyToStr() {
    return builtin("eval", "constkey_to_str");
  }

  public SourceExpr evalGenBindingName() {
    return builtin("eval", "gen_binding_name");
  }

  public SourceExpr evalNewExprContext() {
    return builtin("eval", "new_expr_context");
  }

  public SourceExpr evalStack() {
    return builtin("eval", "stack");
  }

  public SourceExpr evalStrToJson() {
    return builtin("eval", "str_to_json");
  }

  public SourceExpr evalTarget() {
    return builtin("eval", "target");
  }

  public SourceExpr typesAspect() {
    return builtin("types", "aspect");
  }

  public SourceExpr typesBool() {
    return builtin("types", "bool");
  }

  public SourceExpr typesBindResult() {
    return builtin("types", "bind_result");
  }

  public SourceExpr typesConstkey() {
    return builtin("types", "constkey");
  }

  public SourceExpr typesCve() {
    return builtin("types", "cve");
  }

  public SourceExpr typesEresult() {
    return builtin("types", "eresult");
  }

  public SourceExpr typesCode() {
    return builtin("types", "code");
  }

  public SourceExpr typesExpr() {
    return builtin("types", "expr");
  }

  public SourceExpr typesList() {
    return builtin("types", "list");
  }

  public SourceExpr typesMap() {
    return builtin("types", "map");
  }

  public SourceExpr typesNull() {
    return builtin("types", "null");
  }

  public SourceExpr typesInt() {
    return builtin("types", "int");
  }

  public SourceExpr typesOption() {
    return builtin("types", "option");
  }

  public SourceExpr typesString() {
    return builtin("types", "string");
  }

  @SafeVarargs
  public final SourceExpr seqOf(SourceExpr... exprs) {
    return seq(TList.of(exprs));
  }

  @SafeVarargs
  public final SourceExpr tupleOf(SourceExpr... values) {
    return tuple(TList.of(values));
  }

  public SourceExpr recordOf(TList<RecordPair> entries) {
    var out = new TList<SourceExpr.RecordEntry>(entries.size());
    for (var e : entries) {
      out.add(new SourceExpr.RecordEntry(str(e.key()), e.value()));
    }
    return record(out);
  }

  public SourceExpr ifOf(TList<Branch> cases, @Nullable SourceExpr elseBody) {
    var out = new TList<SourceExpr.IfBranch>(cases.size());
    for (var b : cases) out.add(new SourceExpr.IfBranch(b.condition(), b.body()));
    return if_(out, elseBody);
  }

  public SourceExpr matchOf(
      SourceExpr base, SourceExpr elementKey, TList<Branch> cases, @Nullable SourceExpr fallback) {
    var out = new TList<SourceExpr.MatchBranch>(cases.size());
    for (var b : cases) out.add(new SourceExpr.MatchBranch(b.condition(), b.body()));
    return match_(base, elementKey, out, fallback);
  }

  public SourceExpr tryCatchFinally(
      SourceExpr body,
      @Nullable Catch catchClause,
      @Nullable SourceExpr finallyBlock) {
    SourceExpr.TryCatch cc = null;
    if (catchClause != null) {
      cc = new SourceExpr.TryCatch(
          str(catchClause.name()), catchClause.exceptionType(), catchClause.body());
    }
    return tryExpr(body, cc, finallyBlock);
  }

  public record RecordPair(String key, SourceExpr value) {}

  public record Branch(SourceExpr condition, SourceExpr body) {}

  public record Catch(String name, SourceExpr exceptionType, SourceExpr body) {}

  /**
   * Build a source expression that delegates a method call to an aspect on {@code underlying}.
   * Emits: {@code aspect_lookup(underlying, aspectRef, arg.xx).value.<method>({argKeys: arg.argKeys...})}.
   */
  public SourceExpr delegateAspect(
      SourceExpr underlying, SourceExpr aspectRef, String method, String... argKeys) {
    var args = new TList<RecordPair>(argKeys.length);
    for (var k : argKeys) {
      args.add(new RecordPair(k, field(scopeUse("arg"), str(k))));
    }
    return call(
        field(
            field(
                call(
                    evalAspectLookup(),
                    tupleOf(underlying, aspectRef, field(scopeUse("arg"), str("xx")))),
                str("value")),
            str(method)),
        recordOf(args));
  }
}
