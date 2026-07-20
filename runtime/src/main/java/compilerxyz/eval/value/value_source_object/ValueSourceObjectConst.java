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
import compilerxyz.gen.Value;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValues;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;

public final class ValueSourceObjectConst {
  private ValueSourceObjectConst() {}

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
          if (!(self instanceof compilerxyz.eval.value.object_.ValueObjectConst inst)) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected ValueObjectConst, got "
                    + self.getClass());
          }
          if (!(inst.type() instanceof compilerxyz.eval.value.source_object.TypeSourceObjectConst tsoc)) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected TypeSourceObjectConst type, got "
                    + inst.type().getClass());
          }
          var tsod = tsoc.dynamic();
          var instType = new TypeTypeSourceObjectConst(tsod);
          // Extract the underlying JVM value from the source-object instance via known field name.
          Object underlyingHost;
          try {
            var field = inst.data().getClass().getField(
                compilerxyz.eval.dynamic.SourceObjectEmitter.DATA_INSTANCE_FIELD);
            underlyingHost = field.get(inst.data());
          } catch (NoSuchFieldException | IllegalAccessException e) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: source-object instance "
                    + inst.data().getClass().getName() + " missing "
                    + compilerxyz.eval.dynamic.SourceObjectEmitter.DATA_INSTANCE_FIELD + " field");
          }
          // Lift the underlying JVM value to a compile-time Value via the underlying type's AspectLift.
          var ut = tsod.underlyingType;
          if (ut == null) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: object underlying type not finalised");
          }
          var underlyingLiftRes = compilerxyz.gen.GenAspectLift.get(xx, loc, ut);
          if (underlyingLiftRes.hasErrors()) return underlyingLiftRes.reerr();
          var underlyingLiftRes2 = underlyingLiftRes
              .requireValue("ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift aspect")
              .aspectLiftLift(xx, loc, underlyingHost);
          if (underlyingLiftRes2.hasErrors()) return underlyingLiftRes2.reerr();
          Value underlying = underlyingLiftRes2.requireValue(
              "ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift");
          var errors = new TList<ExprErr>();
          // Export instType directly by invoking its AspectArtifact — bypass the exportArtifact
          // wrap, which would go through ofType and recurse back into this delegate.
          var typeArtRes = instType.aspectArtifactExporter().artifactExporterExport(xx, loc, instType);
          if (typeArtRes.hasErrors()) {
            typeArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var typeArt =
              typeArtRes.requireValue("ValueSourceObjectConst::exporterExport type artifact");
          var underArtRes = underlying.exportArtifact(xx, loc);
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
          ExporterAny out = new compilerxyz.eval.value.object_.ValueObjectConst(
              new TypeTypeSourceObjectConst(dt), underlying);
          if (errors.isEmpty()) return EResult.ok(out);
          return EResult.mixed(out, errors);
        }
      };
}
