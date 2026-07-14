package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprArtifacts;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.StageLiftCell;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.util.TList;

public final class ExternalCodec {
  private ExternalCodec() {}

  public static EResult<TList<ArtifactExternal>> exportExternals(
      ExprContext xx, QExprId loc, TList<EvalContext.ExternalEntry> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<ArtifactExternal>(entries.size());
    for (var entry : entries) {
      var key = entry.key();
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
        out.add(new ArtifactExternal.StagedExpr(key, exprArt));
      } else if (host instanceof StageLiftCell liftCell) {
        var srcTypeArtRes =
            ArtifactSupport.exportArtifact(xx, loc, liftCell.sourceType());
        if (srcTypeArtRes.hasErrors()) {
          srcTypeArtRes.drainInto(errors);
          return EResult.errs(errors);
        }
        var srcTypeArt =
            srcTypeArtRes.requireValue("ExternalCodec::export StageLift source type artifact");
        out.add(new ArtifactExternal.StageLift(key, srcTypeArt));
      } else if (host instanceof Type wrappedType) {
        var typeArtRes = ArtifactSupport.exportArtifact(xx, loc, wrappedType);
        if (typeArtRes.hasErrors()) {
          typeArtRes.drainInto(errors);
          return EResult.errs(errors);
        }
        var typeArt = typeArtRes.requireValue("ExternalCodec::export Type artifact");
        out.add(new ArtifactExternal.Type(key, typeArt));
      } else if (host instanceof Unique u) {
        out.add(new ArtifactExternal.Unique(key, u));
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
          out.add(
              new EvalContext.ExternalEntry(
                  se.key(),
                  new compilerxyz.eval.value.expr.TypeExpr(),
                  new External(StagedExprCell.class, cell)));
        }
        case ArtifactExternal.StageLift sl -> {
          var typeRes =
              ArtifactSupport.independentFinalizeImportTypeArtifact(
                  xx, loc, sl.sourceTypeArtifact());
          if (typeRes.hasErrors()) {
            typeRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var sourceType =
              typeRes.requireValue("ExternalCodec::import StageLift source type");
          var liftCell =
              new StageLiftCell(
                  loc,
                  xx.vx,
                  xx.importState,
                  xx.scope,
                  sourceType,
                  xx.currentExceptionType,
                  xx.target);
          out.add(
              new EvalContext.ExternalEntry(
                  sl.key(),
                  sourceType,
                  new External(StageLiftCell.class, liftCell)));
        }
        case ArtifactExternal.Type te -> {
          var typeRes =
              ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, te.typeArtifact());
          if (typeRes.hasErrors()) {
            typeRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var type = typeRes.requireValue("ExternalCodec::import Type external");
          out.add(
              new EvalContext.ExternalEntry(te.key(), type, new External(Type.class, type)));
        }
        case ArtifactExternal.Unique uq -> {
          out.add(
              new EvalContext.ExternalEntry(
                  uq.key(),
                  new compilerxyz.eval.value.unique.TypeUnique(),
                  new External(Unique.class, uq.unique())));
        }
      }
    }
    return EResult.ok(out);
  }
}
