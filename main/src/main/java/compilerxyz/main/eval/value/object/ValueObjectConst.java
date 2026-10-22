package compilerxyz.main.eval.value.object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
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

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof ValueObjectConst voc)) {
                return EResult.err(
                        loc,
                        "ValueObjectConst::exporterRootDelegateSelfExport: expected ValueObjectConst, got "
                                + self.getClass().getName());
            }
            var errors = new TList<ExprErr>();
            var typeRes = Exporters.exportType(xx, loc, voc.type());
            typeRes.drainInto(errors);
            var dataRes = Exporters.exportData(xx, loc, voc.type(), voc.data());
            dataRes.drainInto(errors);
            if (!errors.isEmpty()) return EResult.errs(errors);
            var body = new ArtifactValueObjectConst(
                    typeRes.requireValue("ValueObjectConst::export type"),
                    dataRes.requireValue("ValueObjectConst::export data"));
            return EResult.ok(new Artifact.Inline(makeBody.apply(ArtifactValueObjectConstCodec.toValue(body))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
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

    @Override
    public Opt<ArtifactExporter> valueArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.OBJECT_CONST);
    }
}
