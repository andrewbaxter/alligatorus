package compilerxyz.langhelp;

import compilerxyz.eval.Expr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.null_.TypeNullConst;
import compilerxyz.eval.value.int_.TypeIntConst;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.syntax.SourceExpr;
import compilerxyz.syntax.SourceLiteral;
import compilerxyz.syntax.SourceModule;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class SourceModuleToModule {
  private SourceModuleToModule() {}

  public static Expr.Module sourceModuleToModule(SourceId sourceId, SourceModule module) {
    return switch (module) {
      case SourceModule.V1 v1 -> new Expr.Module.V1(sourceExprToExpr(sourceId, v1.expr()));
    };
  }

  private static Expr sourceExprToExpr(SourceId sourceId, SourceExpr e) {
    var loc = new QExprId(sourceId, e.id().value());
    var variant =
        switch (e.variant()) {
          case SourceExpr.Literal lit -> new Expr.Literal(literalToLitValue(lit.value()));
          case SourceExpr.ScopeUse u ->
              new Expr.ScopeUse(sourceExprToExpr(sourceId, u.key()));
          case SourceExpr.Call c ->
              new Expr.Call(
                  sourceExprToExpr(sourceId, c.func()), sourceExprToExpr(sourceId, c.arg()));
          case SourceExpr.Set s ->
              new Expr.Set(
                  sourceExprToExpr(sourceId, s.base()), sourceExprToExpr(sourceId, s.value()));
          case SourceExpr.Field f ->
              new Expr.Field(
                  sourceExprToExpr(sourceId, f.base()), sourceExprToExpr(sourceId, f.key()));
          case SourceExpr.Seq s -> {
            var exprs = new TList<Expr>(s.exprs().size());
            for (var sub : s.exprs()) exprs.add(sourceExprToExpr(sourceId, sub));
            yield new Expr.Seq(exprs);
          }
          case SourceExpr.Bind b -> {
            var nameLiteral =
                new Expr(
                    loc,
                    new Expr.Literal(Expr.LitValue.shared(
                        new ValueObjectConst(new TypeStringConst(), b.name()))));
            yield new Expr.Bind(nameLiteral, sourceExprToExpr(sourceId, b.value()));
          }
          case SourceExpr.Stage s -> new Expr.Stage(sourceExprToExpr(sourceId, s.expr()));
          case SourceExpr.Lower l -> new Expr.Lower(sourceExprToExpr(sourceId, l.expr()));
          case SourceExpr.Graft g ->
              new Expr.Graft.Pending(sourceExprToExpr(sourceId, g.expr()));
          case SourceExpr.Tuple t -> {
            var elements = new TList<Expr>(t.exprs().size());
            for (var sub : t.exprs()) elements.add(sourceExprToExpr(sourceId, sub));
            yield new Expr.Tuple(elements);
          }
          case SourceExpr.Record r -> {
            var entries = new TList<Expr.RecordEntry>(r.entries().size());
            for (var entry : r.entries()) {
              entries.add(
                  new Expr.RecordEntry(
                      sourceExprToExpr(sourceId, entry.key()),
                      sourceExprToExpr(sourceId, entry.value())));
            }
            yield new Expr.Record(entries);
          }
          case SourceExpr.If i -> {
            var branches = new TList<Expr.ConditionBranch>(i.conditions().size());
            for (var branch : i.conditions()) {
              branches.add(
                  new Expr.ConditionBranch(
                      sourceExprToExpr(sourceId, branch.condition()),
                      sourceExprToExpr(sourceId, branch.body())));
            }
            yield new Expr.If(branches, mapOpt(sourceId, i.fallback()));
          }
          case SourceExpr.OperatorLt op ->
              new Expr.OperatorLt(
                  sourceExprToExpr(sourceId, op.base()),
                  sourceExprToExpr(sourceId, op.reference()));
          case SourceExpr.OperatorEq op ->
              new Expr.OperatorEq(
                  sourceExprToExpr(sourceId, op.base()),
                  sourceExprToExpr(sourceId, op.reference()));
          case SourceExpr.OperatorAdd op ->
              new Expr.OperatorAdd(
                  sourceExprToExpr(sourceId, op.base()),
                  sourceExprToExpr(sourceId, op.reference()));
          case SourceExpr.OperatorMod op ->
              new Expr.OperatorMod(
                  sourceExprToExpr(sourceId, op.base()),
                  sourceExprToExpr(sourceId, op.reference()));
          case SourceExpr.Foreach f ->
              new Expr.Foreach(
                  sourceExprToExpr(sourceId, f.base()),
                  sourceExprToExpr(sourceId, f.elementKey()),
                  sourceExprToExpr(sourceId, f.body()));
          case SourceExpr.Match m -> {
            var cases = new TList<Expr.MatchBranch>(m.cases().size());
            for (var c : m.cases()) {
              cases.add(
                  new Expr.MatchBranch(
                      sourceExprToExpr(sourceId, c.condition()),
                      sourceExprToExpr(sourceId, c.body())));
            }
            yield new Expr.Match(
                sourceExprToExpr(sourceId, m.base()),
                cases,
                sourceExprToExpr(sourceId, m.elementKey()),
                mapOpt(sourceId, m.fallback()));
          }
          case SourceExpr.As a ->
              new Expr.As(
                  sourceExprToExpr(sourceId, a.base()), sourceExprToExpr(sourceId, a.target()));
          case SourceExpr.Labeled lb ->
              new Expr.Labeled(lb.label(), sourceExprToExpr(sourceId, lb.inner()));
          case SourceExpr.Return r ->
              new Expr.Return(r.label(), mapOpt(sourceId, r.value()));
          case SourceExpr.Try t -> {
            var catchPort =
                t.catchClause()
                    .optMap(
                        c ->
                            new Expr.TryCatch(
                                sourceExprToExpr(sourceId, c.bindingName()),
                                sourceExprToExpr(sourceId, c.exceptionType()),
                                sourceExprToExpr(sourceId, c.body())));
            yield new Expr.Try(
                sourceExprToExpr(sourceId, t.body()),
                catchPort,
                mapOpt(sourceId, t.finallyBlock()));
          }
        };
    return new Expr(loc, variant);
  }

  private static Expr.LitValue literalToLitValue(SourceLiteral literal) {
    return switch (literal) {
      case SourceLiteral.Null n -> Expr.LitValue.shared(
          new ValueObjectConst(new TypeNullConst(), null));
      case SourceLiteral.Str s -> Expr.LitValue.shared(
          new ValueObjectConst(new TypeStringConst(), s.value()));
      case SourceLiteral.Number n -> Expr.LitValue.shared(
          new ValueObjectConst(new TypeIntConst(), (long) n.value()));
    };
  }

  private static Opt<Expr> mapOpt(SourceId sourceId, Opt<SourceExpr> opt) {
    return opt.optMap(e -> sourceExprToExpr(sourceId, e));
  }
}
