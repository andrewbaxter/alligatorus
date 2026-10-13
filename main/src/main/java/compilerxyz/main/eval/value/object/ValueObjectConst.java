package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactValueObjectConstCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6213L, lift = false)
@BuiltinPath("types.value_object_const")
public record ValueObjectConst(Type type, Object data) implements Value {
    @Override
    public Type valueType() {
        return type;
    }

    public static Type plainTypeType() {
        return ((TypeObjectDynamic) GenType.dynamicType()).pairedConst();
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var voc = MustCast.mustCast(ValueObjectConst.class, self);
            if (!(voc.data() instanceof Artifactable data)) {
                return EResult.err(loc, "value of type " + voc.type().typeDesc() + " is not exportable");
            }
            var errors = new TList<ExprErr>();
            var typeRes = Artifactable.exportArtifact(xx, loc, voc.type());
            typeRes.drainInto(errors);
            var dataRes = Artifactable.exportArtifact(xx, loc, data);
            dataRes.drainInto(errors);
            if (!errors.isEmpty()) return EResult.errs(errors);
            var body = new ArtifactValueObjectConst(
                    typeRes.requireValue("ValueObjectConst::export type"),
                    dataRes.requireValue("ValueObjectConst::export data"));
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactValueObjectConstCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactValueObjectConst body;
            try {
                body = ArtifactValueObjectConstCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc, "ValueObjectConst data: failed to parse ArtifactValueObjectConst: " + e.getMessage());
            }
            var errors = new TList<ExprErr>();
            var typeRes = ArtifactSupport.importArtifact(xx, loc, body.type());
            typeRes.drainInto(errors);
            var dataRes = ArtifactSupport.importArtifact(xx, loc, body.data());
            dataRes.drainInto(errors);
            if (!errors.isEmpty()) return EResult.errs(errors);
            return EResult.ok(new ValueObjectConst(
                    MustCast.mustCast(Type.class, typeRes.requireValue("ValueObjectConst::import type")),
                    dataRes.requireValue("ValueObjectConst::import data")));
        }
    };

    public static final ExporterRootDelegate PAYLOAD_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            return DELEGATE.exporterRootDelegateSelfExport(xx, loc, self, makeBody);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            var res = DELEGATE.exporterRootDelegateSelfFromArtifactData(xx, loc, id, data);
            if (res.hasErrors()) return res.reerr();
            var voc = MustCast.mustCast(ValueObjectConst.class, res.requireValue("ValueObjectConst::import payload"));
            if (!(voc.data() instanceof ValueObjectConst payload)) {
                return EResult.err(
                        loc,
                        "ValueObjectConst payload data: expected a const value, got "
                                + voc.data().getClass().getName());
            }
            return EResult.ok(new ValueObjectConst(voc.type(), payload.data()));
        }
    };
}
