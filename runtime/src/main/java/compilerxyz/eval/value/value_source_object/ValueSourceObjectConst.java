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
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;

public record ValueSourceObjectConst(TypeSourceObjectDynamic data, Value underlying)
    implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeTypeSourceObjectConst(data);
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
          var typeValue = new TypeTypeSourceObjectConst(inst.data);
          var typeArtRes = ArtifactSupport.exportArtifact(xx, loc, typeValue);
          if (typeArtRes.hasErrors()) {
            typeArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var typeArt =
              typeArtRes.requireValue("ValueSourceObjectConst::exporterExport type artifact");
          var underArtRes = ArtifactSupport.exportArtifact(xx, loc, inst.underlying);
          if (underArtRes.hasErrors()) {
            underArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var underArt =
              underArtRes.requireValue(
                  "ValueSourceObjectConst::exporterExport underlying artifact");
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
          if (typeRes.hasErrors()) {
            typeRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var typeAny =
              typeRes.requireValue("ValueSourceObjectConst::exporterFromArtifactData type any");
          TypeSourceObjectDynamic dt;
          if (typeAny instanceof TypeTypeSourceObjectConst tc) {
            dt = tc.type_();
          } else if (typeAny
              instanceof TypeTypeSourceObjectConst.ImportedSourceObjectConst imported) {
            dt = imported.type_();
          } else {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfFromArtifactData: type artifact resolved to "
                    + typeAny.getClass());
          }
          var underRes = ArtifactSupport.importArtifact(xx, loc, body.underlying());
          if (underRes.hasErrors()) {
            underRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var underAny =
              underRes.requireValue(
                  "ValueSourceObjectConst::exporterFromArtifactData underlying any");
          var underVal = underAny.exporterAnyAsValue();
          if (underVal.hasErrors()) {
            underVal.drainInto(errors);
            return EResult.errs(errors);
          }
          var underlying =
              underVal.requireValue(
                  "ValueSourceObjectConst::exporterFromArtifactData underlying value");
          ExporterAny out = new ValueSourceObjectConst(dt, underlying);
          if (errors.isEmpty()) return EResult.ok(out);
          return EResult.mixed(out, errors);
        }
      };
}
