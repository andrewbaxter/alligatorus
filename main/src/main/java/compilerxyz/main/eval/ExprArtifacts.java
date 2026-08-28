package compilerxyz.main.eval;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
                return EResult.ok(new Expr(
                        loc,
                        new Expr.As(
                                base.requireValue("ExprArtifacts::exprFromArtifact As base"),
                                target.requireValue("ExprArtifacts::exprFromArtifact As target"))));
            }
            case ArtifactExpr.Variant.Bind b -> {
                var base = exprFromArtifact(xx, b.base());
                if (base.hasErrors()) return base.reerr();
                var value = exprFromArtifact(xx, b.value());
                if (value.hasErrors()) return value.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Bind(
                                base.requireValue("ExprArtifacts::exprFromArtifact Bind base"),
                                value.requireValue("ExprArtifacts::exprFromArtifact Bind value"))));
            }
            case ArtifactExpr.Variant.Call c -> {
                var func = exprFromArtifact(xx, c.func());
                if (func.hasErrors()) return func.reerr();
                var arg = exprFromArtifact(xx, c.arg());
                if (arg.hasErrors()) return arg.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Call(
                                func.requireValue("ExprArtifacts::exprFromArtifact Call func"),
                                arg.requireValue("ExprArtifacts::exprFromArtifact Call arg"))));
            }
            case ArtifactExpr.Variant.Field f -> {
                var base = exprFromArtifact(xx, f.base());
                if (base.hasErrors()) return base.reerr();
                var key = exprFromArtifact(xx, f.key());
                if (key.hasErrors()) return key.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Field(
                                base.requireValue("ExprArtifacts::exprFromArtifact Field base"),
                                key.requireValue("ExprArtifacts::exprFromArtifact Field key"))));
            }
            case ArtifactExpr.Variant.Foreach fe -> {
                var base = exprFromArtifact(xx, fe.base());
                if (base.hasErrors()) return base.reerr();
                var body = exprFromArtifact(xx, fe.body());
                if (body.hasErrors()) return body.reerr();
                var elementKey = exprFromArtifact(xx, fe.elementKey());
                if (elementKey.hasErrors()) return elementKey.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Foreach(
                                base.requireValue("ExprArtifacts::exprFromArtifact Foreach base"),
                                elementKey.requireValue("ExprArtifacts::exprFromArtifact Foreach elementKey"),
                                body.requireValue("ExprArtifacts::exprFromArtifact Foreach body"))));
            }
            case ArtifactExpr.Variant.Graft g -> {
                var inner = exprFromArtifact(xx, g.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Graft.Pending(inner.requireValue("ExprArtifacts::exprFromArtifact Graft inner"))));
            }
            case ArtifactExpr.Variant.If i -> {
                var conds = new TList<Expr.ConditionBranch>(i.conditions().size());
                for (var b : i.conditions()) {
                    var c = exprFromArtifact(xx, b.condition());
                    if (c.hasErrors()) return c.reerr();
                    var bo = exprFromArtifact(xx, b.body());
                    if (bo.hasErrors()) return bo.reerr();
                    conds.add(new Expr.ConditionBranch(
                            c.requireValue("ExprArtifacts::exprFromArtifact If condition"),
                            bo.requireValue("ExprArtifacts::exprFromArtifact If branch body")));
                }
                Opt<Expr> else_;
                if (i.else_().optIsSome()) {
                    var e = exprFromArtifact(xx, i.else_().optUnwrap());
                    if (e.hasErrors()) return e.reerr();
                    else_ = Opt.some(e.requireValue("ExprArtifacts::exprFromArtifact If else"));
                } else {
                    else_ = Opt.none();
                }
                return EResult.ok(new Expr(loc, new Expr.If(conds, else_)));
            }
            case ArtifactExpr.Variant.Labeled lb -> {
                var inner = exprFromArtifact(xx, lb.inner());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Labeled(
                                lb.label(), inner.requireValue("ExprArtifacts::exprFromArtifact Labeled inner"))));
            }
            case ArtifactExpr.Variant.Return rt -> {
                Opt<Expr> value;
                if (rt.value().optIsSome()) {
                    var vv = exprFromArtifact(xx, rt.value().optUnwrap());
                    if (vv.hasErrors()) return vv.reerr();
                    value = Opt.some(vv.requireValue("ExprArtifacts::exprFromArtifact Return value"));
                } else {
                    value = Opt.none();
                }
                return EResult.ok(new Expr(loc, new Expr.Return(rt.label(), value)));
            }
            case ArtifactExpr.Variant.Literal l -> {
                var errors = new TList<ExprErr>();
                var imported = ArtifactSupport.importArtifact(xx, loc, l.value());
                if (imported.hasErrors()) {
                    imported.drainInto(errors);
                    return EResult.errs(errors);
                }
                var exporterAny = imported.requireValue("ExprArtifacts::exprFromArtifact Literal imported");
                var asVal = exporterAny.exporterAnyAsValue();
                if (asVal.hasErrors()) {
                    asVal.drainInto(errors);
                    return EResult.errs(errors);
                }
                var litVal = asVal.requireValue("ExprArtifacts::exprFromArtifact Literal value");
                var expr = new Expr(loc, new Expr.Literal(Expr.LitValue.shared(litVal)));
                return EResult.ok(expr);
            }
            case ArtifactExpr.Variant.Lower lo -> {
                var inner = exprFromArtifact(xx, lo.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new Expr(
                        loc, new Expr.Lower(inner.requireValue("ExprArtifacts::exprFromArtifact Lower inner"))));
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
                    cases.add(new Expr.MatchBranch(
                            cond.requireValue("ExprArtifacts::exprFromArtifact Match cond"),
                            body.requireValue("ExprArtifacts::exprFromArtifact Match body")));
                }
                var elementKey = exprFromArtifact(xx, m.elementKey());
                if (elementKey.hasErrors()) return elementKey.reerr();
                Opt<Expr> fallback;
                if (m.fallback().optIsSome()) {
                    var f = exprFromArtifact(xx, m.fallback().optUnwrap());
                    if (f.hasErrors()) return f.reerr();
                    fallback = Opt.some(f.requireValue("ExprArtifacts::exprFromArtifact Match fallback"));
                } else {
                    fallback = Opt.none();
                }
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Match(
                                base.requireValue("ExprArtifacts::exprFromArtifact Match base"),
                                cases,
                                elementKey.requireValue("ExprArtifacts::exprFromArtifact Match elementKey"),
                                fallback)));
            }
            case ArtifactExpr.Variant.OperatorBinary oa -> {
                var base = exprFromArtifact(xx, oa.base());
                if (base.hasErrors()) return base.reerr();
                var reference = exprFromArtifact(xx, oa.reference());
                if (reference.hasErrors()) return reference.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.OperatorBinary(
                                oa.op(),
                                base.requireValue("ExprArtifacts::exprFromArtifact OperatorBinary base"),
                                reference.requireValue("ExprArtifacts::exprFromArtifact OperatorBinary reference"))));
            }
            case ArtifactExpr.Variant.OperatorUnary ou -> {
                var base = exprFromArtifact(xx, ou.base());
                if (base.hasErrors()) return base.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.OperatorUnary(
                                ou.op(), base.requireValue("ExprArtifacts::exprFromArtifact OperatorUnary base"))));
            }
            case ArtifactExpr.Variant.Record r -> {
                var entries = new TList<Expr.RecordEntry>(r.entries().size());
                for (var e : r.entries()) {
                    var k = exprFromArtifact(xx, e.key());
                    if (k.hasErrors()) return k.reerr();
                    var val = exprFromArtifact(xx, e.value());
                    if (val.hasErrors()) return val.reerr();
                    entries.add(new Expr.RecordEntry(
                            k.requireValue("ExprArtifacts::exprFromArtifact Record key"),
                            val.requireValue("ExprArtifacts::exprFromArtifact Record value")));
                }
                return EResult.ok(new Expr(loc, new Expr.Record(entries)));
            }
            case ArtifactExpr.Variant.ScopeUse su -> {
                var key = exprFromArtifact(xx, su.key());
                if (key.hasErrors()) return key.reerr();
                return EResult.ok(new Expr(
                        loc, new Expr.ScopeUse(key.requireValue("ExprArtifacts::exprFromArtifact ScopeUse key"))));
            }
            case ArtifactExpr.Variant.Seq s -> {
                var exprs = new TList<Expr>(s.exprs().size());
                for (var e : s.exprs()) {
                    var r = exprFromArtifact(xx, e);
                    if (r.hasErrors()) return r.reerr();
                    exprs.add(r.requireValue("ExprArtifacts::exprFromArtifact Seq element"));
                }
                return EResult.ok(new Expr(loc, new Expr.Seq(exprs)));
            }
            case ArtifactExpr.Variant.Set se -> {
                var base = exprFromArtifact(xx, se.base());
                if (base.hasErrors()) return base.reerr();
                var value = exprFromArtifact(xx, se.value());
                if (value.hasErrors()) return value.reerr();
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Set(
                                base.requireValue("ExprArtifacts::exprFromArtifact Set base"),
                                value.requireValue("ExprArtifacts::exprFromArtifact Set value"))));
            }
            case ArtifactExpr.Variant.Stage st -> {
                var inner = exprFromArtifact(xx, st.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new Expr(
                        loc, new Expr.Stage(inner.requireValue("ExprArtifacts::exprFromArtifact Stage inner"))));
            }
            case ArtifactExpr.Variant.Try t -> {
                var body = exprFromArtifact(xx, t.body());
                if (body.hasErrors()) return body.reerr();
                Opt<Expr.TryCatch> catchClause;
                if (t.catchClause().optIsSome()) {
                    var c = t.catchClause().optUnwrap();
                    var name = exprFromArtifact(xx, c.bindingName());
                    if (name.hasErrors()) return name.reerr();
                    var ex = exprFromArtifact(xx, c.exceptionType());
                    if (ex.hasErrors()) return ex.reerr();
                    var b = exprFromArtifact(xx, c.body());
                    if (b.hasErrors()) return b.reerr();
                    catchClause = Opt.some(new Expr.TryCatch(
                            name.requireValue("ExprArtifacts::exprFromArtifact Try catch name"),
                            ex.requireValue("ExprArtifacts::exprFromArtifact Try catch exceptionType"),
                            b.requireValue("ExprArtifacts::exprFromArtifact Try catch body")));
                } else {
                    catchClause = Opt.none();
                }
                Opt<Expr> finallyBlock;
                if (t.finallyBlock().optIsSome()) {
                    var f = exprFromArtifact(xx, t.finallyBlock().optUnwrap());
                    if (f.hasErrors()) return f.reerr();
                    finallyBlock = Opt.some(f.requireValue("ExprArtifacts::exprFromArtifact Try finally"));
                } else {
                    finallyBlock = Opt.none();
                }
                return EResult.ok(new Expr(
                        loc,
                        new Expr.Try(
                                body.requireValue("ExprArtifacts::exprFromArtifact Try body"),
                                catchClause,
                                finallyBlock)));
            }
            case ArtifactExpr.Variant.Tuple tu -> {
                var elements = new TList<Expr>(tu.elements().size());
                for (var e : tu.elements()) {
                    var r = exprFromArtifact(xx, e);
                    if (r.hasErrors()) return r.reerr();
                    elements.add(r.requireValue("ExprArtifacts::exprFromArtifact Tuple element"));
                }
                return EResult.ok(new Expr(loc, new Expr.Tuple(elements)));
            }
        }
    }

    public static EResult<ArtifactExpr> exprToArtifact(ExprContext xx, Expr expr) {
        var loc = new ArtifactExpr.ArtifactQExprId(
                expr.loc().source().value(), expr.loc().expr());
        var v = expr.variant();
        switch (v) {
            case Expr.As a -> {
                var base = exprToArtifact(xx, a.base());
                if (base.hasErrors()) return base.reerr();
                var target = exprToArtifact(xx, a.target());
                if (target.hasErrors()) return target.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.As(
                                base.requireValue("ExprArtifacts::exprToArtifact As base"),
                                target.requireValue("ExprArtifacts::exprToArtifact As target"))));
            }
            case Expr.Bind b -> {
                var base = exprToArtifact(xx, b.base());
                if (base.hasErrors()) return base.reerr();
                var value = exprToArtifact(xx, b.value());
                if (value.hasErrors()) return value.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Bind(
                                base.requireValue("ExprArtifacts::exprToArtifact Bind base"),
                                value.requireValue("ExprArtifacts::exprToArtifact Bind value"))));
            }
            case Expr.Call c -> {
                var func = exprToArtifact(xx, c.func());
                if (func.hasErrors()) return func.reerr();
                var arg = exprToArtifact(xx, c.arg());
                if (arg.hasErrors()) return arg.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Call(
                                func.requireValue("ExprArtifacts::exprToArtifact Call func"),
                                arg.requireValue("ExprArtifacts::exprToArtifact Call arg"))));
            }
            case Expr.Field f -> {
                var base = exprToArtifact(xx, f.base());
                if (base.hasErrors()) return base.reerr();
                var key = exprToArtifact(xx, f.key());
                if (key.hasErrors()) return key.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Field(
                                base.requireValue("ExprArtifacts::exprToArtifact Field base"),
                                key.requireValue("ExprArtifacts::exprToArtifact Field key"))));
            }
            case Expr.Foreach fe -> {
                var base = exprToArtifact(xx, fe.base());
                if (base.hasErrors()) return base.reerr();
                var body = exprToArtifact(xx, fe.body());
                if (body.hasErrors()) return body.reerr();
                var elementKey = exprToArtifact(xx, fe.elementKey());
                if (elementKey.hasErrors()) return elementKey.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Foreach(
                                base.requireValue("ExprArtifacts::exprToArtifact Foreach base"),
                                elementKey.requireValue("ExprArtifacts::exprToArtifact Foreach elementKey"),
                                body.requireValue("ExprArtifacts::exprToArtifact Foreach body"))));
            }
            case Expr.Graft.Pending p -> {
                var inner = exprToArtifact(xx, p.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Graft(
                                inner.requireValue("ExprArtifacts::exprToArtifact Graft inner"))));
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
                    conds.add(new ArtifactExpr.CondBranch(
                            c.requireValue("ExprArtifacts::exprToArtifact If condition"),
                            bo.requireValue("ExprArtifacts::exprToArtifact If branch body")));
                }
                Opt<ArtifactExpr> else_;
                if (i.else_().optIsSome()) {
                    var e = exprToArtifact(xx, i.else_().optUnwrap());
                    if (e.hasErrors()) return e.reerr();
                    else_ = Opt.some(e.requireValue("ExprArtifacts::exprToArtifact If else"));
                } else {
                    else_ = Opt.none();
                }
                return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.If(conds, else_)));
            }
            case Expr.Labeled lb -> {
                var inner = exprToArtifact(xx, lb.inner());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Labeled(
                                lb.label(), inner.requireValue("ExprArtifacts::exprToArtifact Labeled inner"))));
            }
            case Expr.Return rt -> {
                Opt<ArtifactExpr> value;
                if (rt.value().optIsSome()) {
                    var vv = exprToArtifact(xx, rt.value().optUnwrap());
                    if (vv.hasErrors()) return vv.reerr();
                    value = Opt.some(vv.requireValue("ExprArtifacts::exprToArtifact Return value"));
                } else {
                    value = Opt.none();
                }
                return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Return(rt.label(), value)));
            }
            case Expr.Literal l -> {
                var peeked = l.value().value;
                if (peeked.optIsNone()) {
                    return EResult.err(expr.loc(), "Cannot serialize consumed Literal");
                }
                var value = peeked.optUnwrap();
                var errors = new TList<ExprErr>();
                var type = value.valueType();
                var aspRes = GenAspectArtifact.get(xx, expr.loc(), type);
                if (aspRes.hasErrors()) {
                    aspRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var asp = aspRes.requireValue("ExprArtifacts::exprToArtifact Literal aspect");
                var artRes = asp.aspectArtifactExporter()
                        .artifactExporterExport(xx, expr.loc(), ArtifactSupport.valueAsExporterAny(value));
                if (artRes.hasErrors()) {
                    artRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var art = artRes.requireValue("ExprArtifacts::exprToArtifact Literal artifact");
                var result = new ArtifactExpr(loc, new ArtifactExpr.Variant.Literal(art));
                return EResult.ok(result);
            }
            case Expr.Lower lo -> {
                var inner = exprToArtifact(xx, lo.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Lower(
                                inner.requireValue("ExprArtifacts::exprToArtifact Lower inner"))));
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
                    cases.add(new ArtifactExpr.MatchBranchA(
                            cond.requireValue("ExprArtifacts::exprToArtifact Match cond"),
                            body.requireValue("ExprArtifacts::exprToArtifact Match body")));
                }
                var elementKey = exprToArtifact(xx, m.elementKey());
                if (elementKey.hasErrors()) return elementKey.reerr();
                Opt<ArtifactExpr> fallback;
                if (m.fallback().optIsSome()) {
                    var f = exprToArtifact(xx, m.fallback().optUnwrap());
                    if (f.hasErrors()) return f.reerr();
                    fallback = Opt.some(f.requireValue("ExprArtifacts::exprToArtifact Match fallback"));
                } else {
                    fallback = Opt.none();
                }
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Match(
                                base.requireValue("ExprArtifacts::exprToArtifact Match base"),
                                cases,
                                elementKey.requireValue("ExprArtifacts::exprToArtifact Match elementKey"),
                                fallback)));
            }
            case Expr.OperatorBinary oa -> {
                var base = exprToArtifact(xx, oa.base());
                if (base.hasErrors()) return base.reerr();
                var reference = exprToArtifact(xx, oa.reference());
                if (reference.hasErrors()) return reference.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.OperatorBinary(
                                oa.op(),
                                base.requireValue("ExprArtifacts::exprToArtifact OperatorBinary base"),
                                reference.requireValue("ExprArtifacts::exprToArtifact OperatorBinary reference"))));
            }
            case Expr.OperatorUnary ou -> {
                var base = exprToArtifact(xx, ou.base());
                if (base.hasErrors()) return base.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.OperatorUnary(
                                ou.op(), base.requireValue("ExprArtifacts::exprToArtifact OperatorUnary base"))));
            }
            case Expr.Record r -> {
                var entries = new TList<ArtifactExpr.RecordEntryA>(r.entries().size());
                for (var e : r.entries()) {
                    var k = exprToArtifact(xx, e.key());
                    if (k.hasErrors()) return k.reerr();
                    var val = exprToArtifact(xx, e.value());
                    if (val.hasErrors()) return val.reerr();
                    entries.add(new ArtifactExpr.RecordEntryA(
                            k.requireValue("ExprArtifacts::exprToArtifact Record key"),
                            val.requireValue("ExprArtifacts::exprToArtifact Record value")));
                }
                return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Record(entries)));
            }
            case Expr.ScopeUse su -> {
                var key = exprToArtifact(xx, su.key());
                if (key.hasErrors()) return key.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.ScopeUse(
                                key.requireValue("ExprArtifacts::exprToArtifact ScopeUse key"))));
            }
            case Expr.Seq s -> {
                var exprs = new TList<ArtifactExpr>(s.exprs().size());
                for (var e : s.exprs()) {
                    var r = exprToArtifact(xx, e);
                    if (r.hasErrors()) return r.reerr();
                    exprs.add(r.requireValue("ExprArtifacts::exprToArtifact Seq element"));
                }
                return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Seq(exprs)));
            }
            case Expr.Set se -> {
                var base = exprToArtifact(xx, se.base());
                if (base.hasErrors()) return base.reerr();
                var value = exprToArtifact(xx, se.value());
                if (value.hasErrors()) return value.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Set(
                                base.requireValue("ExprArtifacts::exprToArtifact Set base"),
                                value.requireValue("ExprArtifacts::exprToArtifact Set value"))));
            }
            case Expr.Stage st -> {
                var inner = exprToArtifact(xx, st.expr());
                if (inner.hasErrors()) return inner.reerr();
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Stage(
                                inner.requireValue("ExprArtifacts::exprToArtifact Stage inner"))));
            }
            case Expr.Try t -> {
                var body = exprToArtifact(xx, t.body());
                if (body.hasErrors()) return body.reerr();
                Opt<ArtifactExpr.TryCatchA> catchClause;
                if (t.catchClause().optIsSome()) {
                    var c = t.catchClause().optUnwrap();
                    var name = exprToArtifact(xx, c.bindingName());
                    if (name.hasErrors()) return name.reerr();
                    var ex = exprToArtifact(xx, c.exceptionType());
                    if (ex.hasErrors()) return ex.reerr();
                    var b = exprToArtifact(xx, c.body());
                    if (b.hasErrors()) return b.reerr();
                    catchClause = Opt.some(new ArtifactExpr.TryCatchA(
                            name.requireValue("ExprArtifacts::exprToArtifact Try catch name"),
                            ex.requireValue("ExprArtifacts::exprToArtifact Try catch exceptionType"),
                            b.requireValue("ExprArtifacts::exprToArtifact Try catch body")));
                } else {
                    catchClause = Opt.none();
                }
                Opt<ArtifactExpr> finallyBlock;
                if (t.finallyBlock().optIsSome()) {
                    var f = exprToArtifact(xx, t.finallyBlock().optUnwrap());
                    if (f.hasErrors()) return f.reerr();
                    finallyBlock = Opt.some(f.requireValue("ExprArtifacts::exprToArtifact Try finally"));
                } else {
                    finallyBlock = Opt.none();
                }
                return EResult.ok(new ArtifactExpr(
                        loc,
                        new ArtifactExpr.Variant.Try(
                                body.requireValue("ExprArtifacts::exprToArtifact Try body"),
                                catchClause,
                                finallyBlock)));
            }
            case Expr.Tuple tu -> {
                var elements = new TList<ArtifactExpr>(tu.elements().size());
                for (var e : tu.elements()) {
                    var r = exprToArtifact(xx, e);
                    if (r.hasErrors()) return r.reerr();
                    elements.add(r.requireValue("ExprArtifacts::exprToArtifact Tuple element"));
                }
                return EResult.ok(new ArtifactExpr(loc, new ArtifactExpr.Variant.Tuple(elements)));
            }
        }
    }
}
