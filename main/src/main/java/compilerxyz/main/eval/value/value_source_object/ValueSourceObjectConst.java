package compilerxyz.main.eval.value.value_source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

public final class ValueSourceObjectConst {
    private ValueSourceObjectConst() {}

    public record ArtifactValueSourceObject(Artifact typeArtifact, Artifact underlying)
            implements SafeJsonSerializable {}

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(loc, "a source object instance is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return importInstance(xx, loc, data);
        }
    };

    private static EResult<Object> importInstance(ExprContext xx, QExprId loc, JsonValue data) {
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
        var erb = new Erb(loc);
        var typeAny = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.typeArtifact()));
        var underlying = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.underlying()));
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var dt = MustCast.mustCast(TypeSourceObjectDynamic.class, typeAny.optUnwrap());
        var loaderRes = dt.set.install(xx, loc);
        if (loaderRes.hasErrors()) return loaderRes.reerr();
        Class<?> klass;
        try {
            klass = Class.forName(dt.binaryName, true, loaderRes.requireValue("ValueSourceObjectConst::import loader"));
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("ValueSourceObjectConst::import: " + dt.binaryName + " not loadable", e);
        }
        for (var ctor : klass.getConstructors()) {
            if (ctor.getParameterCount() != 1) continue;
            try {
                return EResult.ok(ctor.newInstance(underlying.optUnwrap()));
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException(
                        "ValueSourceObjectConst::import: " + dt.binaryName + " construction failed", e);
            }
        }
        throw new IllegalStateException(
                "ValueSourceObjectConst::import: " + dt.binaryName + " has no underlying constructor");
    }
}
