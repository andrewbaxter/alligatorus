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
      typeArtRes.drainInto(errors);
      var typeArt = typeArtRes.valueOrNull();
      if (typeArt == null) {
        return EResult.errs(errors);
      }
      var valueRes = externalToValue(loc, entry.external());
      valueRes.drainInto(errors);
      var valueVal = valueRes.valueOrNull();
      if (valueVal == null) {
        return EResult.errs(errors);
      }
      var valueArtRes = ArtifactSupport.exportArtifact(xx, loc, valueVal);
      valueArtRes.drainInto(errors);
      var valueArt = valueArtRes.valueOrNull();
      if (valueArt == null) {
        return EResult.errs(errors);
      }
      out.add(new ArtifactExternal(entry.key(), typeArt, valueArt));
    }
    return errors.isEmpty() ? EResult.ok(out) : EResult.mixed(out, errors);
  }

  public static EResult<TList<EvalContext.ExternalEntry>> importExternals(
      ExprContext xx, QExprId loc, TList<ArtifactExternal> entries) {
    var errors = new TList<ExprErr>();
    var out = new TList<EvalContext.ExternalEntry>(entries.size());
    for (var entry : entries) {
      var typeRes =
          ArtifactSupport.independentFinalizeImportTypeArtifact(xx, loc, entry.typeArtifact());
      typeRes.drainInto(errors);
      var type = typeRes.valueOrNull();
      if (type == null) {
        return EResult.errs(errors);
      }
      var valueAnyRes = ArtifactSupport.importArtifact(xx, loc, entry.valueArtifact());
      valueAnyRes.drainInto(errors);
      var valueAny = valueAnyRes.valueOrNull();
      if (valueAny == null) {
        return EResult.errs(errors);
      }
      var valueRes = valueAny.exporterAnyAsValue();
      valueRes.drainInto(errors);
      var value = valueRes.valueOrNull();
      if (value == null) {
        return EResult.errs(errors);
      }
      var externalRes = valueToExternal(loc, type, value);
      externalRes.drainInto(errors);
      var external = externalRes.valueOrNull();
      if (external == null) {
        return EResult.errs(errors);
      }
      out.add(new EvalContext.ExternalEntry(entry.key(), type, external));
    }
    return errors.isEmpty() ? EResult.ok(out) : EResult.mixed(out, errors);
  }

  private static EResult<Value> externalToValue(QExprId loc, External ext) {
    return switch (ext) {
      case External.TypeExternal te -> {
        if (!(te.type() instanceof Value tv)) {
          yield EResult.err(loc, "ExternalCodec: TypeExternal.type is not a Value");
        }
        yield EResult.ok(tv);
      }
      case External.UniqueExternal ue -> EResult.ok(new ValueUnique(ue.unique()));
      case External.StagedExprCellExternal se -> {
        var stored = se.cell().peek();
        if (stored == null) {
          yield EResult.err(loc, "ExternalCodec: captured StagedExprCell already consumed");
        }
        if (!(stored instanceof Value sv)) {
          yield EResult.err(
              loc,
              "ExternalCodec: captured StagedExprCell holds non-Value: "
                  + stored.getClass().getSimpleName());
        }
        yield EResult.ok(sv);
      }
      default ->
          EResult.err(
              loc,
              "ExternalCodec: unsupported External variant for artifact round-trip: "
                  + ext.getClass().getSimpleName());
    };
  }

  private static EResult<External> valueToExternal(QExprId loc, Type type, Value value) {
    if (type instanceof TypeExpr) {
      return EResult.ok(new External.StagedExprCellExternal(new StagedExprCell(value)));
    }
    if (type instanceof TypeUnique) {
      if (!(value instanceof ValueUnique vu)) {
        return EResult.err(
            loc,
            "ExternalCodec: imported value for TypeUnique is not ValueUnique: "
                + value.getClass().getSimpleName());
      }
      return EResult.ok(new External.UniqueExternal(vu.unique()));
    }
    if (value instanceof Type wrappedType) {
      return EResult.ok(new External.TypeExternal(wrappedType));
    }
    return EResult.err(
        loc,
        "ExternalCodec: cannot reconstruct External from type "
            + type.getClass().getSimpleName()
            + " and value "
            + value.getClass().getSimpleName());
  }
}
