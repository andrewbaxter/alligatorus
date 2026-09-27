package compilerxyz.main.tests;

import compilerxyz.main.syntax.BinaryOperator;
import compilerxyz.main.syntax.ExprId;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceLiteral;
import compilerxyz.main.syntax.UnaryOperator;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class TestMb {
    private long nextPhraseId = 0;

    public ExprId id() {
        return new ExprId(nextPhraseId++);
    }

    public SourceExpr num(double v) {
        return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Number(v)));
    }

    public SourceExpr str(String s) {
        return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Str(s)));
    }

    public SourceExpr bool(boolean v) {
        return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Bool(v)));
    }

    public SourceExpr as(SourceExpr base, SourceExpr target) {
        return new SourceExpr(id(), new SourceExpr.As(base, target));
    }

    public SourceExpr bind(String key, SourceExpr value) {
        return new SourceExpr(id(), new SourceExpr.Bind(key, value));
    }

    public SourceExpr call(SourceExpr func, SourceExpr arg) {
        return new SourceExpr(id(), new SourceExpr.Call(func, arg));
    }

    public SourceExpr field(SourceExpr base, SourceExpr key) {
        return new SourceExpr(id(), new SourceExpr.Field(base, key));
    }

    public SourceExpr foreach(SourceExpr base, SourceExpr elementKey, SourceExpr body) {
        return new SourceExpr(id(), new SourceExpr.Foreach(base, elementKey, body));
    }

    public SourceExpr graft(SourceExpr expr) {
        return new SourceExpr(id(), new SourceExpr.Graft(expr));
    }

    public SourceExpr importExpr(SourceExpr spec) {
        var entry = new SourceExpr.RecordEntry(str("v"), call(builtin("source", "virtual"), spec));
        var rec = record(TList.of(entry));
        return field(call(builtin("import"), rec), str("v"));
    }

    public SourceExpr sourceLocal(SourceExpr path) {
        return call(builtin("source", "local"), path);
    }

    public SourceExpr sourceRemote(SourceExpr url, SourceExpr hash) {
        return call(builtin("source", "remote"), tuple(TList.of(url, hash)));
    }

    public SourceExpr importRecord(TList<SourceExpr.RecordEntry> entries) {
        return call(builtin("import"), record(entries));
    }

    public SourceExpr labeled(String label, SourceExpr inner) {
        return new SourceExpr(id(), new SourceExpr.Labeled(label, inner));
    }

    public SourceExpr return_(Opt<String> label, Opt<SourceExpr> value) {
        return new SourceExpr(id(), new SourceExpr.Return(label, value));
    }

    public SourceExpr return_(SourceExpr value) {
        return return_(Opt.none(), Opt.some(value));
    }

    public SourceExpr return_(String label, SourceExpr value) {
        return return_(Opt.some(label), Opt.some(value));
    }

    public SourceExpr returnLabel(String label) {
        return return_(Opt.some(label), Opt.none());
    }

    public SourceExpr returnEmpty() {
        return return_(Opt.none(), Opt.none());
    }

    public SourceExpr lower(SourceExpr expr) {
        return new SourceExpr(id(), new SourceExpr.Lower(expr));
    }

    public SourceExpr operatorBinary(BinaryOperator op, SourceExpr base, SourceExpr reference) {
        return new SourceExpr(id(), new SourceExpr.OperatorBinary(op, base, reference));
    }

    public SourceExpr operatorUnary(UnaryOperator op, SourceExpr base) {
        return new SourceExpr(id(), new SourceExpr.OperatorUnary(op, base));
    }

    public SourceExpr operatorAdd(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.ADD, base, reference);
    }

    public SourceExpr operatorSub(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.SUB, base, reference);
    }

    public SourceExpr operatorMul(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.MUL, base, reference);
    }

    public SourceExpr operatorDiv(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.DIV, base, reference);
    }

    public SourceExpr operatorMod(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.MOD, base, reference);
    }

    public SourceExpr operatorAnd(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.AND, base, reference);
    }

    public SourceExpr operatorOr(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.OR, base, reference);
    }

    public SourceExpr operatorXor(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.XOR, base, reference);
    }

    public SourceExpr operatorShl(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.SHL, base, reference);
    }

    public SourceExpr operatorShr(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.SHR, base, reference);
    }

    public SourceExpr operatorUshr(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.USHR, base, reference);
    }

    public SourceExpr operatorEq(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.EQ, base, reference);
    }

    public SourceExpr operatorNe(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.NE, base, reference);
    }

    public SourceExpr operatorLt(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.LT, base, reference);
    }

    public SourceExpr operatorLe(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.LE, base, reference);
    }

    public SourceExpr operatorGt(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.GT, base, reference);
    }

    public SourceExpr operatorGe(SourceExpr base, SourceExpr reference) {
        return operatorBinary(BinaryOperator.GE, base, reference);
    }

    public SourceExpr operatorNeg(SourceExpr base) {
        return operatorUnary(UnaryOperator.NEG, base);
    }

    public SourceExpr operatorBitNot(SourceExpr base) {
        return operatorUnary(UnaryOperator.BITNOT, base);
    }

    public SourceExpr record(TList<SourceExpr.RecordEntry> entries) {
        return new SourceExpr(id(), new SourceExpr.Record(entries));
    }

    public SourceExpr scopeUse(String key) {
        return new SourceExpr(id(), new SourceExpr.ScopeUse(str(key)));
    }

    public SourceExpr seq(TList<SourceExpr> exprs) {
        return new SourceExpr(id(), new SourceExpr.Seq(exprs));
    }

    public SourceExpr set(SourceExpr base, SourceExpr value) {
        return new SourceExpr(id(), new SourceExpr.Set(base, value));
    }

    public SourceExpr stage(SourceExpr expr) {
        return new SourceExpr(id(), new SourceExpr.Stage(expr));
    }

    public SourceExpr tryExpr(SourceExpr body, Opt<SourceExpr.TryCatch> catchClause, Opt<SourceExpr> finallyBlock) {
        return new SourceExpr(id(), new SourceExpr.Try(body, catchClause, finallyBlock));
    }

    public SourceExpr tuple(TList<SourceExpr> values) {
        return new SourceExpr(id(), new SourceExpr.Tuple(values));
    }

    public SourceExpr builtin(String... path) {
        var expr = scopeUse("builtins");
        for (var segment : path) {
            expr = field(expr, str(segment));
        }
        return expr;
    }

    public SourceExpr log() {
        return builtin("log");
    }

    public SourceExpr panic(SourceExpr message) {
        return call(builtin("panic"), message);
    }

    public SourceExpr throw_(SourceExpr value) {
        return call(builtin("throw"), value);
    }

    public SourceExpr bindingDropViaUse() {
        return call(
                builtin("eval", "drop_value"),
                recordOf(TList.of(
                        new RecordPair("xx", field(scopeUse("arg"), str("xx"))),
                        new RecordPair("loc", field(scopeUse("arg"), str("loc"))),
                        new RecordPair(
                                "cv",
                                call(
                                        field(evalBinding(), str("use")),
                                        tupleOf(
                                                scopeUse("outer"),
                                                recordOf(
                                                        TList.of(
                                                                new RecordPair("xx", field(scopeUse("arg"), str("xx"))),
                                                                new RecordPair(
                                                                        "loc", field(scopeUse("arg"), str("loc"))),
                                                                new RecordPair(
                                                                        "setter",
                                                                        call(
                                                                                builtin(
                                                                                        "eval",
                                                                                        "binding_setter_consumed"),
                                                                                tupleOf())),
                                                                new RecordPair(
                                                                        "code",
                                                                        call(
                                                                                builtin("eval", "code_none"),
                                                                                field(
                                                                                        scopeUse("arg"),
                                                                                        str("loc"))))))))))));
    }

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

    public SourceExpr evalAspectInitialBindDynamic() {
        return builtin("eval", "aspect_initial_bind_dynamic");
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

    public SourceExpr evalCodevalueDynamic() {
        return builtin("eval", "codevalue_dynamic");
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

    public SourceExpr typesObject() {
        return builtin("types", "object");
    }

    public SourceExpr typesIntBox() {
        return builtin("types", "int_box");
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

    public SourceExpr optSomeType(SourceExpr typeExpr) {
        return call(field(call(typesOption(), evalType()), str("some")), typeExpr);
    }

    public SourceExpr optNoneType() {
        return call(field(call(typesOption(), evalType()), str("none")), tupleOf());
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

    public SourceExpr matchOf(SourceExpr base, SourceExpr elementKey, TList<Branch> cases, Opt<SourceExpr> fallback) {
        var out = new TList<SourceExpr.MatchBranch>(cases.size());
        for (var b : cases) out.add(new SourceExpr.MatchBranch(b.condition(), b.body()));
        return new SourceExpr(id(), new SourceExpr.Match(base, elementKey, out, fallback));
    }

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
                                call(field(call(typesEresult(), typesCode()), str("errs")), scopeUse("__ce_p")))),
                Opt.none());
    }

    public SourceExpr tryCatchFinally(SourceExpr body, Opt<Catch> catchClause, Opt<SourceExpr> finallyBlock) {
        return tryExpr(
                body,
                catchClause.optIsNone()
                        ? Opt.none()
                        : Opt.some(new SourceExpr.TryCatch(
                                str(catchClause.optUnwrap().name()),
                                catchClause.optUnwrap().exceptionType(),
                                catchClause.optUnwrap().body())),
                finallyBlock);
    }

    public record RecordPair(String key, SourceExpr value) {}

    public record Branch(SourceExpr condition, SourceExpr body) {}

    public record Catch(String name, SourceExpr exceptionType, SourceExpr body) {}

    public SourceExpr delegateAspect(SourceExpr underlying, SourceExpr aspectRef, String method, String... argKeys) {
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
        var receiver = field(call(evalAspectLookup(), tupleOf(underlying, aspectRef, lookupXx)), str("value"));
        return call(
                field(aspectRef, str(method)), callArg.optIsSome() ? tupleOf(receiver, callArg.optUnwrap()) : receiver);
    }
}
