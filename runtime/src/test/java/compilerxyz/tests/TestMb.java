package compilerxyz.tests;

import compilerxyz.syntax.ModuleBuilder;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class TestMb extends ModuleBuilder {
  public SourceExpr define() {
    return builtin("define");
  }

  public SourceExpr evalAspectField() {
    return builtin("eval", "aspect_field");
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

  public SourceExpr evalAspectSet() {
    return builtin("eval", "aspect_set");
  }

  public SourceExpr evalOperatorAdd() {
    return builtin("eval", "operator_add");
  }

  public SourceExpr mustReinterpret() {
    return builtin("must_reinterpret");
  }

  public SourceExpr evalValue() {
    return builtin("eval", "value");
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

  public SourceExpr evalTypeLongDynamicIdentity() {
    return builtin("eval", "type_long_dynamic_identity");
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

  public SourceExpr evalBindResult() {
    return builtin("eval", "bind_result");
  }

  public SourceExpr evalCodevalue() {
    return builtin("eval", "codevalue");
  }

  public SourceExpr evalConstEval() {
    return builtin("eval", "const_eval");
  }

  public SourceExpr evalStack() {
    return builtin("eval", "stack");
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

  public SourceExpr typesByte() {
    return builtin("types", "byte");
  }

  public SourceExpr typesLong() {
    return builtin("types", "long");
  }

  public SourceExpr typesUbyte() {
    return builtin("types", "ubyte");
  }

  public SourceExpr typesUint() {
    return builtin("types", "uint");
  }

  public SourceExpr typesUlong() {
    return builtin("types", "ulong");
  }

  public SourceExpr typesOption() {
    return builtin("types", "option");
  }

  /** {@code option(type).some(typeExpr)} — an optional return type that yields a value. */
  public SourceExpr optSomeType(SourceExpr typeExpr) {
    return call(field(call(typesOption(), evalType()), str("some")), typeExpr);
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

  public SourceExpr ifOf(TList<Branch> cases, Opt<SourceExpr> elseBody) {
    var out = new TList<SourceExpr.IfBranch>(cases.size());
    for (var b : cases) out.add(new SourceExpr.IfBranch(b.condition(), b.body()));
    return new SourceExpr(id(), new SourceExpr.If(out, elseBody));
  }

  public SourceExpr matchOf(
      SourceExpr base, SourceExpr elementKey, TList<Branch> cases, Opt<SourceExpr> fallback) {
    var out = new TList<SourceExpr.MatchBranch>(cases.size());
    for (var b : cases) out.add(new SourceExpr.MatchBranch(b.condition(), b.body()));
    return new SourceExpr(id(), new SourceExpr.Match(base, elementKey, out, fallback));
  }

  /**
   * Turn a {@code const_eval(...)} result — an {@code eresult(cve)} — into an
   * {@code eresult(code)} by matching: on {@code ok} take the value's {@code
   * code}; on {@code errs} propagate the errors. This is how a meta-function
   * (e.g. a drop body) surfaces const-eval errors through its own return.
   */
  public SourceExpr constEvalCode(SourceExpr constEvalCall) {
    return matchOf(
        constEvalCall,
        str("__ce_p"),
        TList.of(
            new Branch(
                str("ok"),
                call(
                    field(call(typesEresult(), typesCode()), str("ok")),
                    field(scopeUse("__ce_p"), str("code")))),
            new Branch(
                str("errs"),
                call(
                    field(call(typesEresult(), typesCode()), str("errs")),
                    scopeUse("__ce_p")))),
        Opt.none());
  }

  public SourceExpr tryCatchFinally(
      SourceExpr body,
      Opt<Catch> catchClause,
      Opt<SourceExpr> finallyBlock) {
    return tryExpr(
        body,
        catchClause.optIsNone()
            ? Opt.none()
            : Opt.some(
                new SourceExpr.TryCatch(
                    str(catchClause.optUnwrap().name()),
                    catchClause.optUnwrap().exceptionType(),
                    catchClause.optUnwrap().body())),
        finallyBlock);
  }

  public record RecordPair(String key, SourceExpr value) {}

  public record Branch(SourceExpr condition, SourceExpr body) {}

  public record Catch(String name, SourceExpr exceptionType, SourceExpr body) {}

  /**
   * Build a source expression that delegates a method call to an aspect on {@code underlying}.
   * Emits: {@code aspectRef.<method>((aspect_lookup(underlying, aspectRef, arg.xx).value,
   * {argKeys: arg.argKeys...}))} — the interface-type method reference called with the
   * looked-up aspect value as receiver (bare for no-arg methods).
   */
  public SourceExpr delegateAspect(
      SourceExpr underlying, SourceExpr aspectRef, String method, String... argKeys) {
    SourceExpr lookupXx;
    Opt<SourceExpr> callArg;
    if (argKeys.length == 0) {
      lookupXx = field(scopeUse("arg"), str("xx"));
      callArg = Opt.none();
    } else if (argKeys.length == 1) {
      lookupXx = scopeUse("arg");
      callArg = Opt.some(scopeUse("arg"));
    } else {
      lookupXx = field(scopeUse("arg"), str("xx"));
      var args = new TList<RecordPair>(argKeys.length);
      for (var k : argKeys) {
        args.add(new RecordPair(k, field(scopeUse("arg"), str(k))));
      }
      callArg = Opt.some(recordOf(args));
    }
    var receiver =
        field(
            call(evalAspectLookup(), tupleOf(underlying, aspectRef, lookupXx)),
            str("value"));
    return call(
        field(aspectRef, str(method)),
        callArg.optIsSome() ? tupleOf(receiver, callArg.optUnwrap()) : receiver);
  }
}
