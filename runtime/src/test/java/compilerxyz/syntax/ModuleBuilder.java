package compilerxyz.syntax;

import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.jspecify.annotations.Nullable;

public class ModuleBuilder {
  private long nextPhraseId = 0;

  public ExprId id() {
    return new ExprId(nextPhraseId++);
  }

  public SourceExpr literalNull() {
    return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Null()));
  }

  public SourceExpr num(double v) {
    return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Number(v)));
  }

  public SourceExpr str(String s) {
    return new SourceExpr(id(), new SourceExpr.Literal(new SourceLiteral.Str(s)));
  }

  public SourceExpr bool(boolean v) {
    return v ? operatorLt(num(0.0), num(1.0)) : operatorLt(num(1.0), num(0.0));
  }

  public SourceExpr as_(SourceExpr base, SourceExpr target) {
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

  public SourceExpr if_(
      TList<SourceExpr.IfBranch> cases, @Nullable SourceExpr elseBody) {
    return new SourceExpr(
        id(), new SourceExpr.If(cases, elseBody == null ? Opt.none() : Opt.some(elseBody)));
  }

  public SourceExpr importExpr(SourceExpr spec) {
    var entry =
        new SourceExpr.RecordEntry(str("v"), call(builtin("source", "virtual"), spec));
    var rec = record(TList.of(entry));
    return field(call(builtin("import"), rec), str("v"));
  }

  public SourceExpr sourceVirtual(SourceExpr name) {
    return call(builtin("source", "virtual"), name);
  }

  public SourceExpr sourceLocal(SourceExpr path) {
    return call(builtin("source", "local"), path);
  }

  public SourceExpr sourceRemote(SourceExpr url, SourceExpr hash) {
    return call(builtin("source", "remote"), tuple(TList.of(url, hash)));
  }

  public SourceExpr sourceSha256(SourceExpr bytes) {
    return call(builtin("source", "sha256"), bytes);
  }

  public SourceExpr importRecord(TList<SourceExpr.RecordEntry> entries) {
    return call(builtin("import"), record(entries));
  }

  public SourceExpr labeled(String label, SourceExpr inner) {
    return new SourceExpr(id(), new SourceExpr.Labeled(label, inner));
  }

  public SourceExpr return_(@Nullable String label, @Nullable SourceExpr value) {
    return new SourceExpr(
        id(),
        new SourceExpr.Return(
            label == null ? Opt.none() : Opt.some(label),
            value == null ? Opt.none() : Opt.some(value)));
  }

  public SourceExpr return_(SourceExpr value) {
    return return_(null, value);
  }

  public SourceExpr returnEmpty() {
    return return_(null, null);
  }

  public SourceExpr lower(SourceExpr expr) {
    return new SourceExpr(id(), new SourceExpr.Lower(expr));
  }

  public SourceExpr match_(
      SourceExpr base,
      SourceExpr elementKey,
      TList<SourceExpr.MatchBranch> cases,
      @Nullable SourceExpr fallback) {
    return new SourceExpr(
        id(),
        new SourceExpr.Match(
            base, elementKey, cases, fallback == null ? Opt.none() : Opt.some(fallback)));
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

  public SourceExpr tryExpr(
      SourceExpr body,
      SourceExpr.@Nullable TryCatch catchClause,
      @Nullable SourceExpr finallyBlock) {
    return new SourceExpr(
        id(),
        new SourceExpr.Try(
            body,
            catchClause == null ? Opt.none() : Opt.some(catchClause),
            finallyBlock == null ? Opt.none() : Opt.some(finallyBlock)));
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
}
