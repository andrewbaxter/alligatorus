package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.ArtifactExternal;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.expr.TypeExpr;
import compilerxyz.eval.value.unique.TypeUnique;
import compilerxyz.eval.value.unique.ValueUnique;
import compilerxyz.util.TList;

public final class ExternalCodec {
  private ExternalCodec() {}

  public static EResult<TList<ArtifactExternal>> exportExternals(
      ExprContext xx, QExprId loc, TList<EvalContext.ExternalEntry> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<ArtifactExternal>(entries.size());
    for (var entry : entries) {
      var typeArtRes = ArtifactSupport.exportArtifact(xx, loc, entry.type());
      if (typeArtRes.hasErrors()) {
        typeArtRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var typeArt = typeArtRes.requireValue("ExternalCodec::exportExternals type artifact");
      var valueRes = externalToValue(loc, entry.external());
      if (valueRes.hasErrors()) {
        valueRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var valueVal = valueRes.requireValue("ExternalCodec::exportExternals value");
      var valueArtRes = ArtifactSupport.exportArtifact(xx, loc, valueVal);
      if (valueArtRes.hasErrors()) {
        valueArtRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var valueArt = valueArtRes.requireValue("ExternalCodec::exportExternals value artifact");
      out.add(new ArtifactExternal(entry.key(), typeArt, valueArt));
    }
    return EResult.ok(out);
  }

  public static EResult<TList<EvalContext.ExternalEntry>> importExternals(
      ExprContext xx, QExprId loc, TList<ArtifactExternal> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<EvalContext.ExternalEntry>(entries.size());
    for (var entry : entries) {
      var typeRes =
          ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, entry.typeArtifact());
      if (typeRes.hasErrors()) {
        typeRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var type = typeRes.requireValue("ExternalCodec::importExternals type");
      var valueAnyRes = ArtifactSupport.importArtifact(xx, loc, entry.valueArtifact());
      if (valueAnyRes.hasErrors()) {
        valueAnyRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var valueAny = valueAnyRes.requireValue("ExternalCodec::importExternals valueAny");
      var valueRes = valueAny.exporterAnyAsValue();
      if (valueRes.hasErrors()) {
        valueRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var value = valueRes.requireValue("ExternalCodec::importExternals value");
      var externalRes = valueToExternal(loc, type, value);
      if (externalRes.hasErrors()) {
        externalRes.drainInto(errors);
        return EResult.errs(errors);
      }
      var external = externalRes.requireValue("ExternalCodec::importExternals external");
      out.add(new EvalContext.ExternalEntry(entry.key(), type, external));
    }
    return EResult.ok(out);
  }

  private static EResult<Value> externalToValue(QExprId loc, External ext) {
    var host = ext.hostValue();
    if (host instanceof StagedExprCell cell) {
      var stored = cell.peek();
      if (stored == null) {
        return EResult.err(loc, "ExternalCodec: captured StagedExprCell already consumed");
      }
      if (!(stored instanceof Value sv)) {
        return EResult.err(
            loc,
            "ExternalCodec: captured StagedExprCell holds non-Value: "
                + stored.getClass().getSimpleName());
      }
      return EResult.ok(sv);
    }
    if (host instanceof Unique u) {
      return EResult.ok(new ValueUnique(u));
    }
    if (host instanceof Value tv) {
      return EResult.ok(tv);
    }
    return EResult.err(
        loc,
        "ExternalCodec: unsupported External host for artifact round-trip: "
            + (host == null ? "null" : host.getClass().getSimpleName()));
  }

  private static EResult<External> valueToExternal(QExprId loc, Type type, Value value) {
    if (type instanceof TypeExpr) {
      return EResult.ok(new External(StagedExprCell.class, new StagedExprCell(value)));
    }
    if (type instanceof TypeUnique) {
      if (!(value instanceof ValueUnique vu)) {
        return EResult.err(
            loc,
            "ExternalCodec: imported value for TypeUnique is not ValueUnique: "
                + value.getClass().getSimpleName());
      }
      return EResult.ok(new External(Unique.class, vu.unique()));
    }
    if (value instanceof Type wrappedType) {
      return EResult.ok(new External(Type.class, wrappedType));
    }
    return EResult.err(
        loc,
        "ExternalCodec: cannot reconstruct External from type "
            + type.getClass().getSimpleName()
            + " and value "
            + value.getClass().getSimpleName());
  }
}
