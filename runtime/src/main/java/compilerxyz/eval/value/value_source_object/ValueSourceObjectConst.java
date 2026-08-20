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
import compilerxyz.eval.ImportInstall;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_object.TypeSourceObjectConst;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.SafeJsonSerializable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.lang.reflect.Constructor;
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
          if (!(self instanceof ValueObjectConst inst)) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected ValueObjectConst, got "
                    + self.getClass());
          }
          if (!(inst.type() instanceof TypeSourceObjectConst tsoc)) {
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
                SourceObjectEmitter.DATA_INSTANCE_FIELD);
            underlyingHost = field.get(inst.data());
          } catch (NoSuchFieldException | IllegalAccessException e) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: source-object instance "
                    + inst.data().getClass().getName() + " missing "
                    + SourceObjectEmitter.DATA_INSTANCE_FIELD + " field");
          }
          // Lift the underlying JVM value to a compile-time Value via the underlying type's AspectLift.
          if (tsod.underlyingType.optIsNone()) {
            return EResult.err(loc,
                "ValueSourceObjectConst::exporterRootDelegateSelfExport: object underlying type not finalised");
          }
          var ut = tsod.underlyingType.optUnwrap();
          var underlyingLiftRes = GenAspectLift.get(xx, loc, ut);
          if (underlyingLiftRes.hasErrors()) return underlyingLiftRes.reerr();
          var underlyingLiftRes2 = underlyingLiftRes
              .requireValue("ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift aspect")
              .aspectLiftLift(xx, loc, Opt.ofNullable(underlyingHost));
          if (underlyingLiftRes2.hasErrors()) return underlyingLiftRes2.reerr();
          Value underlying = underlyingLiftRes2.requireValue(
              "ValueSourceObjectConst::exporterRootDelegateSelfExport underlying lift");
          var errors = new TList<ExprErr>();
          // Export instType directly by invoking its AspectArtifact — bypass the artifactableExportArtifact
          // wrap, which would go through ofType and recurse back into this delegate.
          var typeArtRes = instType.aspectArtifactExporter().artifactExporterExport(xx, loc, instType);
          if (typeArtRes.hasErrors()) {
            typeArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var typeArt =
              typeArtRes.requireValue("ValueSourceObjectConst::exporterExport type artifact");
          var underArtRes = underlying.artifactableExportArtifact(xx, loc);
          if (underArtRes.hasErrors()) {
            underArtRes.drainInto(errors);
            return EResult.errs(errors);
          }
          var underArt =
              underArtRes.requireValue(
                  "ValueSourceObjectConst::exporterExport underlying artifact");
          var body = new ArtifactValueSourceObject(typeArt, underArt);
          var artifact = new Artifact.Body(makeBody.apply(ValueSourceObjectConstArtifactValueSourceObjectCodec.toValue(body)));
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          ArtifactValueSourceObject body;
          try {
            body = ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(data);
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
          var installRes =
              ImportInstall.installDefSetToDynamic(xx, loc, dt.set);
          if (installRes.hasErrors()) {
            installRes.drainInto(errors);
            return EResult.errs(errors);
          }
          if (xx.vx.ix.classLoader.optIsNone()) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterFromArtifactData: classLoader missing after install");
          }
          var loader = xx.vx.ix.classLoader.optUnwrap();
          if (dt.compiledBinaryName.optIsNone()) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterFromArtifactData: source object '"
                    + dt.name.optUnwrapOr("(unnamed)")
                    + "' has not been compiled");
          }
          var binaryName = dt.compiledBinaryName.optUnwrap();
          Class<?> cls;
          try {
            cls = Class.forName(binaryName, true, loader);
          } catch (ClassNotFoundException e) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::exporterFromArtifactData: class " + binaryName + " not loadable");
          }
          Constructor<?> ctor = null;
          for (var c : cls.getDeclaredConstructors()) {
            if (c.getParameterCount() == 1) {
              ctor = c;
              break;
            }
          }
          if (ctor == null) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::finalizer: " + binaryName + " has no single-arg constructor");
          }
          Object underlyingHost;
          if (underlying instanceof ValueObjectConst uvoc) {
            underlyingHost = uvoc.data();
          } else {
            underlyingHost = underlying;
          }
          Object instance;
          try {
            instance = ctor.newInstance(underlyingHost);
          } catch (ReflectiveOperationException | IllegalArgumentException e) {
            return EResult.err(
                loc,
                "ValueSourceObjectConst::finalizer: cannot instantiate " + binaryName + ": " + e.getMessage());
          }
          ExporterAny out = new ValueObjectConst(
              new TypeTypeSourceObjectConst(dt), instance);
          if (errors.isEmpty()) return EResult.ok(out);
          return EResult.mixed(out, errors);
        }
      };
}
