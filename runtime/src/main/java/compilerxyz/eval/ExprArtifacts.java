package compilerxyz.eval;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class ExprArtifacts {
  private ExprArtifacts() {}

  public static EResult<Expr> exprFromArtifact(ExprContext xx, ArtifactExpr art) {
    var loc = new QExprId(new SourceId(art.loc().source()), art.loc().expr());
    var v = art.variant();
    switch (v) {
      case ArtifactExpr.Variant.As a -> {
        var base = exprFromArtifact(xx, a.base());
        if (base.hasErrors()) return base.reerr();
        var target = exprFromArtifact(xx, a.target());
        if (target.hasErrors()) return target.reerr();
        return EResult.ok(new Expr(loc, new Expr.As(base.valueOrNull(), target.valueOrNull())));
      }
      case ArtifactExpr.Variant.Bind b -> {
        var base = exprFromArtifact(xx, b.base());
        if (base.hasErrors()) return base.reerr();
        var value = exprFromArtifact(xx, b.value());
        if (value.hasErrors()) return value.reerr();
        return EResult.ok(new Expr(loc, new Expr.Bind(base.valueOrNull(), value.valueOrNull())));
      }
      case ArtifactExpr.Variant.Call c -> {
        var func = exprFromArtifact(xx, c.func());
        if (func.hasErrors()) return func.reerr();
        var arg = exprFromArtifact(xx, c.arg());
        if (arg.hasErrors()) return arg.reerr();
        return EResult.ok(new Expr(loc, new Expr.Call(func.valueOrNull(), arg.valueOrNull())));
      }
      case ArtifactExpr.Variant.Field f -> {
        var base = exprFromArtifact(xx, f.base());
        if (base.hasErrors()) return base.reerr();
        var key = exprFromArtifact(xx, f.key());
        if (key.hasErrors()) return key.reerr();
        return EResult.ok(new Expr(loc, new Expr.Field(base.valueOrNull(), key.valueOrNull())));
      }
      case ArtifactExpr.Variant.Foreach fe -> {
        var base = exprFromArtifact(xx, fe.base());
        if (base.hasErrors()) return base.reerr();
        var body = exprFromArtifact(xx, fe.body());
        if (body.hasErrors()) return body.reerr();
        var elementKey = exprFromArtifact(xx, fe.elementKey());
        if (elementKey.hasErrors()) return elementKey.reerr();
        return EResult.ok(
            new Expr(
                loc,
                new Expr.Foreach(
                    base.valueOrNull(), elementKey.valueOrNull(), body.valueOrNull())));
      }
      case ArtifactExpr.Variant.Graft g -> {
        var inner = exprFromArtifact(xx, g.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new Expr(loc, new Expr.Graft.Pending(inner.valueOrNull())));
      }
      case ArtifactExpr.Variant.If i -> {
        var conds = new TList<Expr.ConditionBranch>(i.conditions().size());
        for (var b : i.conditions()) {
          var c = exprFromArtifact(xx, b.condition());
          if (c.hasErrors()) return c.reerr();
          var bo = exprFromArtifact(xx, b.body());
          if (bo.hasErrors()) return bo.reerr();
          conds.add(new Expr.ConditionBranch(c.valueOrNull(), bo.valueOrNull()));
        }
        Opt<Expr> else_;
        if (i.else_() instanceof Opt.Some<ArtifactExpr> some) {
          var e = exprFromArtifact(xx, some.value());
          if (e.hasErrors()) return e.reerr();
          else_ = Opt.some(e.valueOrNull());
        } else {
          else_ = Opt.none();
        }
        return EResult.ok(new Expr(loc, new Expr.If(conds, else_)));
      }
      case ArtifactExpr.Variant.Import im -> {
        var spec = exprFromArtifact(xx, im.spec());
        if (spec.hasErrors()) return spec.reerr();
        return EResult.ok(new Expr(loc, new Expr.Import(spec.valueOrNull())));
      }
      case ArtifactExpr.Variant.Literal l -> {
        var errors = new TList<ExprErr>();
        var imported = ArtifactSupport.importArtifact(xx, loc, l.value());
        imported.drainInto(errors);
        var exporterAny = imported.valueOrNull();
        if (exporterAny == null) return EResult.errs(errors);
        var asVal = exporterAny.exporterAnyAsValue();
        asVal.drainInto(errors);
        var litVal = asVal.valueOrNull();
        if (litVal == null) return EResult.errs(errors);
        var expr = new Expr(loc, new Expr.Literal(Expr.LitValue.shared(litVal)));
        return errors.isEmpty() ? EResult.ok(expr) : EResult.mixed(expr, errors);
      }
      case ArtifactExpr.Variant.Lower lo -> {
        var inner = exprFromArtifact(xx, lo.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new Expr(loc, new Expr.Lower(inner.valueOrNull())));
      }
      case ArtifactExpr.Variant.Match m -> {
        var base = exprFromArtifact(xx, m.base());
        if (base.hasErrors()) return base.reerr();
        var cases = new TList<Expr.MatchBranch>(m.cases().size());
        for (var c : m.cases()) {
          var cond = exprFromArtifact(xx, c.condition());
          if (cond.hasErrors()) return cond.reerr();
          var body = exprFromArtifact(xx, c.body());
          if (body.hasErrors()) return body.reerr();
          cases.add(new Expr.MatchBranch(cond.valueOrNull(), body.valueOrNull()));
        }
        var elementKey = exprFromArtifact(xx, m.elementKey());
        if (elementKey.hasErrors()) return elementKey.reerr();
        Opt<Expr> fallback;
        if (m.fallback() instanceof Opt.Some<ArtifactExpr> some) {
          var f = exprFromArtifact(xx, some.value());
          if (f.hasErrors()) return f.reerr();
          fallback = Opt.some(f.valueOrNull());
        } else {
          fallback = Opt.none();
        }
        return EResult.ok(
            new Expr(loc, new Expr.Match(base.valueOrNull(), cases, elementKey.valueOrNull(), fallback)));
      }
      case ArtifactExpr.Variant.OperatorAdd oa -> {
        var base = exprFromArtifact(xx, oa.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprFromArtifact(xx, oa.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new Expr(loc, new Expr.OperatorAdd(base.valueOrNull(), reference.valueOrNull())));
      }
      case ArtifactExpr.Variant.OperatorEq oe -> {
        var base = exprFromArtifact(xx, oe.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprFromArtifact(xx, oe.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new Expr(loc, new Expr.OperatorEq(base.valueOrNull(), reference.valueOrNull())));
      }
      case ArtifactExpr.Variant.OperatorLt ol -> {
        var base = exprFromArtifact(xx, ol.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprFromArtifact(xx, ol.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new Expr(loc, new Expr.OperatorLt(base.valueOrNull(), reference.valueOrNull())));
      }
      case ArtifactExpr.Variant.Record r -> {
        var entries = new TList<Expr.RecordEntry>(r.entries().size());
        for (var e : r.entries()) {
          var k = exprFromArtifact(xx, e.key());
          if (k.hasErrors()) return k.reerr();
          var val = exprFromArtifact(xx, e.value());
          if (val.hasErrors()) return val.reerr();
          entries.add(new Expr.RecordEntry(k.valueOrNull(), val.valueOrNull()));
        }
        return EResult.ok(new Expr(loc, new Expr.Record(entries)));
      }
      case ArtifactExpr.Variant.ScopeUse su -> {
        var key = exprFromArtifact(xx, su.key());
        if (key.hasErrors()) return key.reerr();
        return EResult.ok(new Expr(loc, new Expr.ScopeUse(key.valueOrNull())));
      }
      case ArtifactExpr.Variant.Seq s -> {
        var exprs = new TList<Expr>(s.exprs().size());
        for (var e : s.exprs()) {
          var r = exprFromArtifact(xx, e);
          if (r.hasErrors()) return r.reerr();
          exprs.add(r.valueOrNull());
        }
        return EResult.ok(new Expr(loc, new Expr.Seq(exprs)));
      }
      case ArtifactExpr.Variant.Set se -> {
        var base = exprFromArtifact(xx, se.base());
        if (base.hasErrors()) return base.reerr();
        var value = exprFromArtifact(xx, se.value());
        if (value.hasErrors()) return value.reerr();
        return EResult.ok(new Expr(loc, new Expr.Set(base.valueOrNull(), value.valueOrNull())));
      }
      case ArtifactExpr.Variant.Stage st -> {
        var inner = exprFromArtifact(xx, st.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new Expr(loc, new Expr.Stage(inner.valueOrNull())));
      }
      case ArtifactExpr.Variant.Try t -> {
        var body = exprFromArtifact(xx, t.body());
        if (body.hasErrors()) return body.reerr();
        Opt<Expr.TryCatch> catchClause;
        if (t.catchClause() instanceof Opt.Some<ArtifactExpr.TryCatchA> some) {
          var c = some.value();
          var name = exprFromArtifact(xx, c.bindingName());
          if (name.hasErrors()) return name.reerr();
          var ex = exprFromArtifact(xx, c.exceptionType());
          if (ex.hasErrors()) return ex.reerr();
          var b = exprFromArtifact(xx, c.body());
          if (b.hasErrors()) return b.reerr();
          catchClause =
              Opt.some(
                  new Expr.TryCatch(name.valueOrNull(), ex.valueOrNull(), b.valueOrNull()));
        } else {
          catchClause = Opt.none();
        }
        Opt<Expr> finallyBlock;
        if (t.finallyBlock() instanceof Opt.Some<ArtifactExpr> some) {
          var f = exprFromArtifact(xx, some.value());
          if (f.hasErrors()) return f.reerr();
          finallyBlock = Opt.some(f.valueOrNull());
        } else {
          finallyBlock = Opt.none();
        }
        return EResult.ok(new Expr(loc, new Expr.Try(body.valueOrNull(), catchClause, finallyBlock)));
      }
      case ArtifactExpr.Variant.Tuple tu -> {
        var elements = new TList<Expr>(tu.elements().size());
        for (var e : tu.elements()) {
          var r = exprFromArtifact(xx, e);
          if (r.hasErrors()) return r.reerr();
          elements.add(r.valueOrNull());
        }
        return EResult.ok(new Expr(loc, new Expr.Tuple(elements)));
      }
    }
  }

  public static EResult<ArtifactExpr> exprToArtifact(ExprContext xx, Expr expr) {
    var loc = new ArtifactExpr.ArtifactQExprId(expr.loc().source().value(), expr.loc().expr());
    var v = expr.variant();
    switch (v) {
      case Expr.As a -> {
        var base = exprToArtifact(xx, a.base());
        if (base.hasErrors()) return base.reerr();
        var target = exprToArtifact(xx, a.target());
        if (target.hasErrors()) return target.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.As(base.valueOrNull(), target.valueOrNull())));
      }
      case Expr.Bind b -> {
        var base = exprToArtifact(xx, b.base());
        if (base.hasErrors()) return base.reerr();
        var value = exprToArtifact(xx, b.value());
        if (value.hasErrors()) return value.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.Bind(base.valueOrNull(), value.valueOrNull())));
      }
      case Expr.Call c -> {
        var func = exprToArtifact(xx, c.func());
        if (func.hasErrors()) return func.reerr();
        var arg = exprToArtifact(xx, c.arg());
        if (arg.hasErrors()) return arg.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.Call(func.valueOrNull(), arg.valueOrNull())));
      }
      case Expr.Field f -> {
        var base = exprToArtifact(xx, f.base());
        if (base.hasErrors()) return base.reerr();
        var key = exprToArtifact(xx, f.key());
        if (key.hasErrors()) return key.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.Field(base.valueOrNull(), key.valueOrNull())));
      }
      case Expr.Foreach fe -> {
        var base = exprToArtifact(xx, fe.base());
        if (base.hasErrors()) return base.reerr();
        var body = exprToArtifact(xx, fe.body());
        if (body.hasErrors()) return body.reerr();
        var elementKey = exprToArtifact(xx, fe.elementKey());
        if (elementKey.hasErrors()) return elementKey.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc,
                new ArtifactExpr.Variant.Foreach(
                    base.valueOrNull(), elementKey.valueOrNull(), body.valueOrNull())));
      }
      case Expr.Graft.Pending p -> {
        var inner = exprToArtifact(xx, p.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Graft(inner.valueOrNull())));
      }
      case Expr.Graft.Evaluated ev -> {
        return EResult.err(
            expr.loc(),
            "ExprArtifacts.exprToArtifact: cannot serialize Graft after it has been evaluated by"
                + " stage processing");
      }
      case Expr.If i -> {
        var conds = new TList<ArtifactExpr.CondBranch>(i.conditions().size());
        for (var b : i.conditions()) {
          var c = exprToArtifact(xx, b.condition());
          if (c.hasErrors()) return c.reerr();
          var bo = exprToArtifact(xx, b.body());
          if (bo.hasErrors()) return bo.reerr();
          conds.add(new ArtifactExpr.CondBranch(c.valueOrNull(), bo.valueOrNull()));
        }
        Opt<ArtifactExpr> else_;
        if (i.else_() instanceof Opt.Some<Expr> some) {
          var e = exprToArtifact(xx, some.value());
          if (e.hasErrors()) return e.reerr();
          else_ = Opt.some(e.valueOrNull());
        } else {
          else_ = Opt.none();
        }
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.If(conds, else_)));
      }
      case Expr.Import im -> {
        var spec = exprToArtifact(xx, im.spec());
        if (spec.hasErrors()) return spec.reerr();
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Import(spec.valueOrNull())));
      }
      case Expr.Literal l -> {
        var peeked = l.value().peek();
        if (!(peeked instanceof Opt.Some<Value> someV)) {
          return EResult.err(expr.loc(), "Cannot serialize consumed Literal");
        }
        var value = someV.value();
        var errors = new TList<ExprErr>();
        var type = value.valueTypeOf();
        var aspRes = AspectArtifact.get(xx, expr.loc(), type);
        aspRes.drainInto(errors);
        var asp = aspRes.valueOrNull();
        if (asp == null) return EResult.errs(errors);
        var artRes =
            asp.exporter().artifactExporterExport(xx, expr.loc(), ArtifactSupport.valueAsExporterAny(value));
        artRes.drainInto(errors);
        var art = artRes.valueOrNull();
        if (art == null) return EResult.errs(errors);
        var result = new ArtifactExpr(loc, new ArtifactExpr.Variant.Literal(art));
        return errors.isEmpty() ? EResult.ok(result) : EResult.mixed(result, errors);
      }
      case Expr.Lower lo -> {
        var inner = exprToArtifact(xx, lo.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Lower(inner.valueOrNull())));
      }
      case Expr.Match m -> {
        var base = exprToArtifact(xx, m.base());
        if (base.hasErrors()) return base.reerr();
        var cases = new TList<ArtifactExpr.MatchBranchA>(m.cases().size());
        for (var c : m.cases()) {
          var cond = exprToArtifact(xx, c.condition());
          if (cond.hasErrors()) return cond.reerr();
          var body = exprToArtifact(xx, c.body());
          if (body.hasErrors()) return body.reerr();
          cases.add(new ArtifactExpr.MatchBranchA(cond.valueOrNull(), body.valueOrNull()));
        }
        var elementKey = exprToArtifact(xx, m.elementKey());
        if (elementKey.hasErrors()) return elementKey.reerr();
        Opt<ArtifactExpr> fallback;
        if (m.fallback() instanceof Opt.Some<Expr> some) {
          var f = exprToArtifact(xx, some.value());
          if (f.hasErrors()) return f.reerr();
          fallback = Opt.some(f.valueOrNull());
        } else {
          fallback = Opt.none();
        }
        return EResult.ok(
            new ArtifactExpr(
                loc,
                new ArtifactExpr.Variant.Match(
                    base.valueOrNull(), cases, elementKey.valueOrNull(), fallback)));
      }
      case Expr.OperatorAdd oa -> {
        var base = exprToArtifact(xx, oa.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprToArtifact(xx, oa.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc,
                new ArtifactExpr.Variant.OperatorAdd(base.valueOrNull(), reference.valueOrNull())));
      }
      case Expr.OperatorEq oe -> {
        var base = exprToArtifact(xx, oe.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprToArtifact(xx, oe.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc,
                new ArtifactExpr.Variant.OperatorEq(base.valueOrNull(), reference.valueOrNull())));
      }
      case Expr.OperatorLt ol -> {
        var base = exprToArtifact(xx, ol.base());
        if (base.hasErrors()) return base.reerr();
        var reference = exprToArtifact(xx, ol.reference());
        if (reference.hasErrors()) return reference.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc,
                new ArtifactExpr.Variant.OperatorLt(base.valueOrNull(), reference.valueOrNull())));
      }
      case Expr.Record r -> {
        var entries = new TList<ArtifactExpr.RecordEntryA>(r.entries().size());
        for (var e : r.entries()) {
          var k = exprToArtifact(xx, e.key());
          if (k.hasErrors()) return k.reerr();
          var val = exprToArtifact(xx, e.value());
          if (val.hasErrors()) return val.reerr();
          entries.add(new ArtifactExpr.RecordEntryA(k.valueOrNull(), val.valueOrNull()));
        }
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Record(entries)));
      }
      case Expr.ScopeUse su -> {
        var key = exprToArtifact(xx, su.key());
        if (key.hasErrors()) return key.reerr();
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.ScopeUse(key.valueOrNull())));
      }
      case Expr.Seq s -> {
        var exprs = new TList<ArtifactExpr>(s.exprs().size());
        for (var e : s.exprs()) {
          var r = exprToArtifact(xx, e);
          if (r.hasErrors()) return r.reerr();
          exprs.add(r.valueOrNull());
        }
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Seq(exprs)));
      }
      case Expr.Set se -> {
        var base = exprToArtifact(xx, se.base());
        if (base.hasErrors()) return base.reerr();
        var value = exprToArtifact(xx, se.value());
        if (value.hasErrors()) return value.reerr();
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.Set(base.valueOrNull(), value.valueOrNull())));
      }
      case Expr.Stage st -> {
        var inner = exprToArtifact(xx, st.expr());
        if (inner.hasErrors()) return inner.reerr();
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Stage(inner.valueOrNull())));
      }
      case Expr.Try t -> {
        var body = exprToArtifact(xx, t.body());
        if (body.hasErrors()) return body.reerr();
        Opt<ArtifactExpr.TryCatchA> catchClause;
        if (t.catchClause() instanceof Opt.Some<Expr.TryCatch> some) {
          var c = some.value();
          var name = exprToArtifact(xx, c.bindingName());
          if (name.hasErrors()) return name.reerr();
          var ex = exprToArtifact(xx, c.exceptionType());
          if (ex.hasErrors()) return ex.reerr();
          var b = exprToArtifact(xx, c.body());
          if (b.hasErrors()) return b.reerr();
          catchClause =
              Opt.some(
                  new ArtifactExpr.TryCatchA(
                      name.valueOrNull(), ex.valueOrNull(), b.valueOrNull()));
        } else {
          catchClause = Opt.none();
        }
        Opt<ArtifactExpr> finallyBlock;
        if (t.finallyBlock() instanceof Opt.Some<Expr> some) {
          var f = exprToArtifact(xx, some.value());
          if (f.hasErrors()) return f.reerr();
          finallyBlock = Opt.some(f.valueOrNull());
        } else {
          finallyBlock = Opt.none();
        }
        return EResult.ok(
            new ArtifactExpr(
                loc, new ArtifactExpr.Variant.Try(body.valueOrNull(), catchClause, finallyBlock)));
      }
      case Expr.Tuple tu -> {
        var elements = new TList<ArtifactExpr>(tu.elements().size());
        for (var e : tu.elements()) {
          var r = exprToArtifact(xx, e);
          if (r.hasErrors()) return r.reerr();
          elements.add(r.valueOrNull());
        }
        return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Tuple(elements)));
      }
    }
  }
}
