package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprArtifacts;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.util.TList;

public final class ExternalCodec {
  private ExternalCodec() {}

  public static EResult<TList<ArtifactExternal.CapturedEntry>> exportExternals(
      ExprContext xx, QExprId loc, TList<EvalContext.ExternalEntry> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<ArtifactExternal.CapturedEntry>(entries.size());
    for (var entry : entries) {
      var localIdx = entry.key();
      var ext = entry.external();
      ArtifactExternal artExt;
      switch (ext) {
        case External.TypeExternal te -> {
          var artRes = ArtifactSupport.exportArtifact(xx, loc, te.type());
          artRes.drainInto(errors);
          var v = artRes.valueOrNull();
          if (v == null) {
            return EResult.errs(errors);
          }
          artExt = new ArtifactExternal.Type(localIdx, v);
        }
        case External.UniqueExternal ue -> artExt = new ArtifactExternal.Unique(localIdx, ue.unique());
        case External.StagedExprCellExternal se -> {
          var stored = se.cell().peek();
          if (stored == null) {
            return EResult.err(
                loc,
                "ExternalCodec.exportExternals: captured StagedExprCell already consumed");
          }
          if (!(stored instanceof Expr expr)) {
            return EResult.err(
                loc,
                "ExternalCodec.exportExternals: captured StagedExprCell holds non-Expr value: "
                    + stored.getClass().getSimpleName());
          }
          var artRes = ExprArtifacts.exprToArtifact(xx, expr);
          artRes.drainInto(errors);
          var v = artRes.valueOrNull();
          if (v == null) {
            return EResult.errs(errors);
          }
          artExt = new ArtifactExternal.StagedExpr(localIdx, v);
        }
        default -> {
          return EResult.err(
              loc,
              "ExternalCodec.exportExternals: unsupported External variant for artifact round-trip: "
                  + ext.getClass().getSimpleName());
        }
      }
      var typeArtRes = ArtifactSupport.exportArtifact(xx, loc, entry.type());
      typeArtRes.drainInto(errors);
      var typeArt = typeArtRes.valueOrNull();
      if (typeArt == null) {
        return EResult.errs(errors);
      }
      out.add(new ArtifactExternal.CapturedEntry(entry.key(), typeArt, artExt));
    }
    return errors.isEmpty() ? EResult.ok(out) : EResult.mixed(out, errors);
  }

  public static EResult<TList<EvalContext.ExternalEntry>> importExternals(
      ExprContext xx, QExprId loc, TList<ArtifactExternal.CapturedEntry> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<EvalContext.ExternalEntry>(entries.size());
    for (var entry : entries) {
      External external;
      var artExt = entry.external();
      switch (artExt) {
        case ArtifactExternal.Type t -> {
          var typeRes =
              ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, t.typeArtifact());
          typeRes.drainInto(errors);
          var v = typeRes.valueOrNull();
          if (v == null) {
            return EResult.errs(errors);
          }
          external = new External.TypeExternal(v);
        }
        case ArtifactExternal.Unique u -> external = new External.UniqueExternal(u.unique());
        case ArtifactExternal.StagedExpr s -> {
          var exprRes = ExprArtifacts.exprFromArtifact(xx, s.expr());
          exprRes.drainInto(errors);
          var v = exprRes.valueOrNull();
          if (v == null) {
            return EResult.errs(errors);
          }
          external = new External.StagedExprCellExternal(new StagedExprCell(v));
        }
        default -> {
          return EResult.err(
              loc,
              "ExternalCodec.importExternals: unsupported ArtifactExternal variant: "
                  + artExt.getClass().getSimpleName());
        }
      }
      var typeRes =
          ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, entry.typeArtifact());
      typeRes.drainInto(errors);
      var t = typeRes.valueOrNull();
      if (t == null) {
        return EResult.errs(errors);
      }
      out.add(new EvalContext.ExternalEntry(entry.key(), t, external));
    }
    return errors.isEmpty() ? EResult.ok(out) : EResult.mixed(out, errors);
  }
}
