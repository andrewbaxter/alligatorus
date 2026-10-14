package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectArtifactObjectConst;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectTypeObjectConstData;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactDataSourceUnionTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6141L)
public record TypeTypeSourceUnionConst(TypeSourceUnionDynamic type) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "union";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(AspectTypeObjectConstData.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactObjectConst.INSTANCE);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_CONST_IDENTITY)
                || key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx,
                other,
                GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_CONST_IDENTITY,
                TypeTypeSourceUnionConst.class);
        return oOpt.optIsSome() && type.typeIsEqual(xx, oOpt.optUnwrap().type);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.SOURCE_UNION_TYPE_CONST);
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var uc = MustCast.mustCast(TypeTypeSourceUnionConst.class, self);
            var uRes = Exporters.exportType(xx, loc, uc.type);
            if (uRes.hasErrors()) return uRes.reerr();
            var body = new ArtifactDataSourceUnionType(uRes.requireValue("TypeTypeSourceUnionConst::export union"));
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactDataSourceUnionTypeCodec.toValue(body))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDataSourceUnionType body;
            try {
                body = ArtifactDataSourceUnionTypeCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeTypeSourceUnionConst data: failed to parse ArtifactDataSourceUnionType: "
                                + e.getMessage());
            }
            var uRes = ArtifactSupport.importArtifact(xx, loc, body.union());
            if (uRes.hasErrors()) return uRes.reerr();
            return EResult.ok(new TypeTypeSourceUnionConst(MustCast.mustCast(
                    TypeSourceUnionDynamic.class, uRes.requireValue("TypeTypeSourceUnionConst::import union"))));
        }
    };
}
