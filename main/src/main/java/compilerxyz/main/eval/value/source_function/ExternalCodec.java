package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.ArtifactExpr;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprArtifacts;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.StageLiftCell;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.unique.TypeUniqueDynamic;
import compilerxyz.main.gen.GenQExprId;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public final class ExternalCodec {
    private ExternalCodec() {}

    public static EResult<TList<ArtifactExternal>> exportExternals(
            ExprContext xx, QExprId loc, TList<EvalContext.ExternalEntry> entries) {
        var errors = new TList<ExprErr>();
        var out = new TList<ArtifactExternal>(entries.size());
        for (var entry : entries) {
            var host = entry.external().hostValue();
            if (host instanceof StagedExprCell cell && cell.value() instanceof Expr expr) {
                var exprArtRes = ExprArtifacts.exprToArtifact(xx, expr);
                if (exprArtRes.hasErrors()) {
                    exprArtRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var exprArt = exprArtRes.requireValue("ExternalCodec::export StagedExpr artifact");
                out.add(new ArtifactExternal.StagedExpr(exprArt));
            } else if (host instanceof StagedExprCell cell && cell.value() instanceof Value lowered) {
                var artRes = Artifactable.exportValue(xx, loc, lowered);
                if (artRes.hasErrors()) {
                    artRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                out.add(new ArtifactExternal.Exported(
                        artRes.requireValue("ExternalCodec::export lowered value artifact"), true));
            } else if (host instanceof StagedExprCell cell && cell.value() instanceof Artifactable lowered) {
                var artRes = Artifactable.exportArtifact(xx, loc, lowered);
                if (artRes.hasErrors()) {
                    artRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                out.add(new ArtifactExternal.Exported(
                        artRes.requireValue("ExternalCodec::export lowered artifact"), true));
            } else if (host instanceof StagedExprCell cell) {
                return EResult.err(
                        loc,
                        "ExternalCodec: lowered value is not exportable: "
                                + cell.value().getClass().getSimpleName());
            } else if (host instanceof StageLiftCell liftCell) {
                var srcTypeArtRes = Artifactable.exportArtifact(xx, loc, liftCell.sourceType());
                if (srcTypeArtRes.hasErrors()) {
                    srcTypeArtRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var srcTypeArt = srcTypeArtRes.requireValue("ExternalCodec::export StageLift source type artifact");
                out.add(new ArtifactExternal.StageLift(srcTypeArt));
            } else if (host instanceof Artifactable artifactable) {
                var artRes = Artifactable.exportArtifact(xx, loc, artifactable);
                if (artRes.hasErrors()) {
                    artRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                out.add(new ArtifactExternal.Exported(
                        artRes.requireValue("ExternalCodec::export host artifact"), false));
            } else if (host instanceof Unique u) {
                out.add(new ArtifactExternal.UniqueRef(u));
            } else if (host instanceof QExprId q) {
                out.add(new ArtifactExternal.Loc(
                        new ArtifactExpr.ArtifactQExprId(q.source().value(), q.expr())));
            } else {
                return EResult.err(
                        loc,
                        "ExternalCodec: unsupported External host for artifact round-trip: "
                                + (host == null ? "null" : host.getClass().getSimpleName()));
            }
        }
        return EResult.ok(out);
    }

    public static EResult<TList<EvalContext.ExternalEntry>> importExternals(
            ExprContext xx, QExprId loc, TList<ArtifactExternal> entries) {
        var errors = new TList<ExprErr>();
        var out = new TList<EvalContext.ExternalEntry>(entries.size());
        for (var entry : entries) {
            switch (entry) {
                case ArtifactExternal.StagedExpr se -> {
                    var exprRes = ExprArtifacts.exprFromArtifact(xx, se.expr());
                    if (exprRes.hasErrors()) {
                        exprRes.drainInto(errors);
                        return EResult.errs(errors);
                    }
                    var expr = exprRes.requireValue("ExternalCodec::import StagedExpr expr");
                    var cell = new StagedExprCell(expr);
                    out.add(new EvalContext.ExternalEntry(
                            new BindingId(), new TypeExpr(), new External(StagedExprCell.class, cell)));
                }
                case ArtifactExternal.StageLift sl -> {
                    var typeRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, sl.sourceTypeArtifact());
                    if (typeRes.hasErrors()) {
                        typeRes.drainInto(errors);
                        return EResult.errs(errors);
                    }
                    var sourceType = typeRes.requireValue("ExternalCodec::import StageLift source type");
                    var liftCell = new StageLiftCell(
                            loc, xx.vx, xx.importState, xx.scope, sourceType, xx.currentExceptionType, xx.target);
                    out.add(new EvalContext.ExternalEntry(
                            new BindingId(), sourceType, new External(StageLiftCell.class, liftCell)));
                }
                case ArtifactExternal.Exported ex -> {
                    var importedRes = ArtifactSupport.independentFinalizeImportArtifact(xx, loc, ex.artifact());
                    if (importedRes.hasErrors()) {
                        importedRes.drainInto(errors);
                        return EResult.errs(errors);
                    }
                    var imported = importedRes.requireValue("ExternalCodec::import exported external");
                    if (ex.lowered()) {
                        var cell =
                                new StagedExprCell(imported.type().optIsSome() ? imported.asValue() : imported.data());
                        out.add(new EvalContext.ExternalEntry(
                                new BindingId(), new TypeExpr(), new External(StagedExprCell.class, cell)));
                    } else {
                        var data = imported.data();
                        var entryType =
                                switch (data) {
                                    case Type t -> t;
                                    case Value v -> v.valueType();
                                    default -> imported.type().optUnwrap();
                                };
                        out.add(new EvalContext.ExternalEntry(
                                new BindingId(), entryType, new External(data.getClass(), data)));
                    }
                }
                case ArtifactExternal.UniqueRef uq -> {
                    out.add(new EvalContext.ExternalEntry(
                            new BindingId(), new TypeUniqueDynamic(), new External(Unique.class, uq.unique())));
                }
                case ArtifactExternal.Loc l -> {
                    var q = new QExprId(new SourceId(l.loc().source()), l.loc().expr());
                    out.add(new EvalContext.ExternalEntry(
                            new BindingId(), GenQExprId.dynamicType(), new External(QExprId.class, q)));
                }
            }
        }
        return EResult.ok(out);
    }
}
