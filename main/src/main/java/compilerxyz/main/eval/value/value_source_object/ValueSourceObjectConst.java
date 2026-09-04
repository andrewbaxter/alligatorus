package compilerxyz.main.eval.value.value_source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectConst;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.lang.reflect.Constructor;
import java.util.function.Function;

public final class ValueSourceObjectConst {
    private ValueSourceObjectConst() {}

    public record ArtifactValueSourceObject(Artifact typeArtifact, Artifact underlying)
            implements SafeJsonSerializable {}

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof ValueObjectConst inst)) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected ValueObjectConst, got "
                                + self.getClass());
            }
            if (!(inst.type() instanceof TypeSourceObjectConst tsoc)) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: expected TypeSourceObjectConst type, got "
                                + inst.type().getClass());
            }
            var tsod = tsoc.dynamic();
            Object underlyingHost;
            try {
                var field = inst.data().getClass().getField(SourceObjectEmitter.DATA_INSTANCE_FIELD);
                underlyingHost = field.get(inst.data());
            } catch (NoSuchFieldException | IllegalAccessException e) {
                return EResult.err(
                        loc,
                        "ValueSourceObjectConst::exporterRootDelegateSelfExport: source-object instance "
                                + inst.data().getClass().getName() + " missing "
                                + SourceObjectEmitter.DATA_INSTANCE_FIELD + " field");
            }
            if (tsod.underlyingType.optIsNone()) {
                return EResult.err(
                        loc,
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
            var typeArtRes = Artifactable.exportArtifact(xx, loc, tsod);
            if (typeArtRes.hasErrors()) {
                typeArtRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var typeArt = typeArtRes.requireValue("ValueSourceObjectConst::exporterExport type artifact");
            var underArtRes = Artifactable.exportArtifact(xx, loc, underlying);
            if (underArtRes.hasErrors()) {
                underArtRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var underArt = underArtRes.requireValue("ValueSourceObjectConst::exporterExport underlying artifact");
            var body = new ArtifactValueSourceObject(typeArt, underArt);
            var artifact = new Artifact.Body(
                    makeBody.apply(ValueSourceObjectConstArtifactValueSourceObjectCodec.toValue(body)));
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
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
            var typeAny = typeRes.requireValue("ValueSourceObjectConst::exporterFromArtifactData type any");
            var dt = MustCast.mustCast(TypeSourceObjectDynamic.class, typeAny);
            var underRes = ArtifactSupport.importArtifact(xx, loc, body.underlying());
            if (underRes.hasErrors()) {
                underRes.drainInto(errors);
                return EResult.errs(errors);
            }
            var underAny = underRes.requireValue("ValueSourceObjectConst::exporterFromArtifactData underlying any");
            var underVal = ArtifactSupport.importedAsValue(underAny);
            var underlying = underVal;
            var installRes = dt.set.install(xx, loc);
            if (installRes.hasErrors()) {
                installRes.drainInto(errors);
                return EResult.errs(errors);
            }
            if (xx.vx.ix.classLoader.optIsNone()) {
                return EResult.err(
                        loc, "ValueSourceObjectConst::exporterFromArtifactData: classLoader missing after install");
            }
            var loader = xx.vx.ix.classLoader.optUnwrap();
            var binaryName = dt.binaryName;
            Class<?> cls;
            try {
                cls = Class.forName(binaryName, true, loader);
            } catch (ClassNotFoundException e) {
                return EResult.err(
                        loc, "ValueSourceObjectConst::exporterFromArtifactData: class " + binaryName + " not loadable");
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
                        loc, "ValueSourceObjectConst::finalizer: " + binaryName + " has no single-arg constructor");
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
            Artifactable out = new ValueObjectConst(new TypeSourceObjectConst(dt), instance);
            if (errors.isEmpty()) return EResult.ok(out);
            return EResult.mixed(out, errors);
        }
    };
}
