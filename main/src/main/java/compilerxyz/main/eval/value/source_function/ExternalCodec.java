package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.ArtifactExternal;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprArtifacts;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.main.eval.dynamic.StageLiftCell;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.unique.TypeUniqueDynamic;
import compilerxyz.prelude.util.TList;

public final class ExternalCodec {
    private ExternalCodec() {}

    public static EResult<TList<ArtifactExternal>> exportExternals(
            ExprContext xx, QExprId loc, TList<EvalContext.ExternalEntry> entries) {
        var errors = new TList<ExprErr>();
        var out = new TList<ArtifactExternal>(entries.size());
        for (var entry : entries) {
            var host = entry.external().hostValue();
            if (host instanceof StagedExprCell cell) {
                var stored = cell.value();
                if (!(stored instanceof Expr expr)) {
                    return EResult.err(
                            loc,
                            "ExternalCodec: captured StagedExprCell holds non-Expr: "
                                    + stored.getClass().getSimpleName());
                }
                var exprArtRes = ExprArtifacts.exprToArtifact(xx, expr);
                if (exprArtRes.hasErrors()) {
                    exprArtRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var exprArt = exprArtRes.requireValue("ExternalCodec::export StagedExpr artifact");
                out.add(new ArtifactExternal.StagedExpr(exprArt));
            } else if (host instanceof StageLiftCell liftCell) {
                var srcTypeArtRes = liftCell.sourceType().artifactableExportArtifact(xx, loc);
                if (srcTypeArtRes.hasErrors()) {
                    srcTypeArtRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var srcTypeArt = srcTypeArtRes.requireValue("ExternalCodec::export StageLift source type artifact");
                out.add(new ArtifactExternal.StageLift(srcTypeArt));
            } else if (host instanceof Type wrappedType) {
                var typeArtRes = wrappedType.artifactableExportArtifact(xx, loc);
                if (typeArtRes.hasErrors()) {
                    typeArtRes.drainInto(errors);
                    return EResult.errs(errors);
                }
                var typeArt = typeArtRes.requireValue("ExternalCodec::export Type artifact");
                out.add(new ArtifactExternal.Type(typeArt));
            } else if (host instanceof Unique u) {
                out.add(new ArtifactExternal.UniqueRef(u));
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
                            new JavaBytecodeBindingKey(), new TypeExpr(), new External(StagedExprCell.class, cell)));
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
                            new JavaBytecodeBindingKey(), sourceType, new External(StageLiftCell.class, liftCell)));
                }
                case ArtifactExternal.Type te -> {
                    var typeRes = ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, te.typeArtifact());
                    if (typeRes.hasErrors()) {
                        typeRes.drainInto(errors);
                        return EResult.errs(errors);
                    }
                    var type = typeRes.requireValue("ExternalCodec::import Type external");
                    out.add(new EvalContext.ExternalEntry(
                            new JavaBytecodeBindingKey(), type, new External(Type.class, type)));
                }
                case ArtifactExternal.UniqueRef uq -> {
                    out.add(new EvalContext.ExternalEntry(
                            new JavaBytecodeBindingKey(),
                            new TypeUniqueDynamic(),
                            new External(Unique.class, uq.unique())));
                }
            }
        }
        return EResult.ok(out);
    }
}
