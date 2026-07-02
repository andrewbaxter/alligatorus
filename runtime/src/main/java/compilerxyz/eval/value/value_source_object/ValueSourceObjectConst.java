package compilerxyz.eval.value.value_source_object;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.source_object.SourceObjectData;
import compilerxyz.eval.value.source_object.TypeSourceObjectConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;

public record ValueSourceObjectConst(SourceObjectData data, Value underlying)
    implements Value, ExporterAny {
  @Override
  public Type valueTypeOf() {
    return new TypeSourceObjectConst(data);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException(
        "ValueSourceObjectConst::exporterAnyAsType: a source-object instance is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  public record ArtifactValueSourceObject(Artifact typeArtifact, Artifact underlying)
      implements SafeJsonSerializable {}

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof ValueSourceObjectConst inst)) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected ValueSourceObjectConst, got "
                    + self.getClass());
          }
          var errors = new TList<ExprErr>();
          var typeValue = new TypeSourceObjectConst(inst.data);
          var typeArtRes = ArtifactSupport.exportArtifact(xx, loc, typeValue);
          typeArtRes.drainInto(errors);
          var typeArt = typeArtRes.valueOrNull();
          if (typeArt == null) return EResult.errs(errors);
          var underArtRes = ArtifactSupport.exportArtifact(xx, loc, inst.underlying);
          underArtRes.drainInto(errors);
          var underArt = underArtRes.valueOrNull();
          if (underArt == null) return EResult.errs(errors);
          var body = new ArtifactValueSourceObject(typeArt, underArt);
          var artifact = new Artifact.Body(makeBody.apply(JsonValues.toValue(body)));
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          ArtifactValueSourceObject body;
          try {
            body = JsonValues.fromValue(ArtifactValueSourceObject.class, data);
          } catch (RuntimeException e) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfFromArtifactData: failed to parse "
                    + "ArtifactValueSourceObject: "
                    + e.getMessage());
          }
          var errors = new TList<ExprErr>();
          var typeRes = ArtifactSupport.importArtifact(xx, loc, body.typeArtifact());
          typeRes.drainInto(errors);
          var typeAny = typeRes.valueOrNull();
          SourceObjectData dt;
          if (typeAny instanceof TypeSourceObjectConst tc) {
            dt = tc.data();
          } else if (typeAny
              instanceof TypeSourceObjectConst.ImportedSourceObjectConst imported) {
            dt = imported.data();
          } else {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfFromArtifactData: type artifact resolved to "
                    + (typeAny == null ? "null" : typeAny.getClass()));
          }
          var underRes = ArtifactSupport.importArtifact(xx, loc, body.underlying());
          underRes.drainInto(errors);
          var underAny = underRes.valueOrNull();
          if (underAny == null) return EResult.errs(errors);
          var underVal = underAny.exporterAnyAsValue();
          underVal.drainInto(errors);
          var underlying = underVal.valueOrNull();
          if (underlying == null) return EResult.errs(errors);
          ExporterAny out = new ValueSourceObjectConst(dt, underlying);
          if (errors.isEmpty()) return EResult.ok(out);
          return EResult.mixed(out, errors);
        }
      };
}
