package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactExporterHostType;
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
import compilerxyz.main.eval.aspect.AspectBindConstValue;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectTypeObjectConstData;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.ArtifactTypeOfType;
import compilerxyz.main.eval.value.TypeOfTypeCast;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactTypeOfTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6479L)
public record TypeTypeEResult<T extends Type>(TypeEResultDynamic<T> type) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "type(" + type.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectType.AspectId)) {
            return Opt.some(AspectTypeObjectConstData.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(new AspectArtifactObjectConst(ArtifactExporterHostType.INSTANCE));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldTypeTypeEResult<>(type));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_E_RESULT_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindConstValue.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.TYPE_TYPE_ERESULT);
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var tt = MustCast.mustCast(TypeTypeEResult.class, self);
            var heldRes = Exporters.exportType(xx, loc, tt.type);
            if (heldRes.hasErrors()) return heldRes.reerr();
            var body = new ArtifactTypeOfType(heldRes.requireValue("TypeTypeEResult::export held type"));
            return EResult.ok(new Artifact.Inline(makeBody.apply(ArtifactTypeOfTypeCodec.toValue(body))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactTypeOfType body;
            try {
                body = ArtifactTypeOfTypeCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeTypeEResult data: failed to parse ArtifactTypeOfType: " + e.getMessage());
            }
            var heldRes = ArtifactSupport.importArtifact(xx, loc, body.type());
            if (heldRes.hasErrors()) return heldRes.reerr();
            TypeEResultDynamic<Type> held = MustCast.mustCast(
                    TypeEResultDynamic.class, heldRes.requireValue("TypeTypeEResult::import held type"));
            return EResult.ok(new TypeTypeEResult<>(held));
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_TYPE_E_RESULT_IDENTITY, TypeTypeEResult.class);
        return oOpt.optIsSome() && type.typeIsEqual(xx, oOpt.optUnwrap().type);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return TypeOfTypeCast.canCastTo(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return TypeOfTypeCast.castTo(xx, loc, cv, destType, type);
    }
}
