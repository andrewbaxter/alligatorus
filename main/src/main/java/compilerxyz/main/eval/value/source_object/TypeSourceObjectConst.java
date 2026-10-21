package compilerxyz.main.eval.value.source_object;

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
import compilerxyz.main.eval.aspect.AspectBindViaTarget;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.AspectConstDynamicOf;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConstDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactSourceObjectConstTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6137L)
public record TypeSourceObjectConst(TypeSourceObjectDynamic dynamic) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return dynamic.typeDesc();
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectConstDynamic.AspectId)) {
            return Opt.some(new AspectConstDynamicOf(dynamic));
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindSourceObjectConst(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldSourceObjectConst(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicSourceObjectConst(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindViaTarget.INSTANCE);
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(new AspectArtifactSourceObjectConst(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY) || key.equals(dynamic.identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            for (var impl : dynamic.interfaceImpls) {
                if (impl.interface_.identity.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)) {
                    return Opt.some(AspectTypeSourceObjectConst.INSTANCE);
                }
            }
            return Opt.none();
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return dynamic.typeAspect(xx, key);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_CONST_IDENTITY, TypeSourceObjectConst.class);
        return oOpt.optIsSome() && dynamic.typeIsEqual(xx, oOpt.optUnwrap().dynamic);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return dynamic.typeCanCastFrom(xx, loc, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return dynamic.typeCastFrom(xx, loc, cv);
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return dynamic.typeCanCastTo(xx, loc, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var dynRes = new AspectConsumeDynamicSourceObjectConst(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return dynamic.typeCastTo(xx, loc, dynRes.requireValue(typeDesc() + "::typeCastTo materialize"), destType);
    }

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.SOURCE_OBJECT_CONST_TYPE);
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var type = MustCast.mustCast(TypeSourceObjectConst.class, self);
            var dynamicArtRes = Exporters.exportType(xx, loc, type.dynamic);
            if (dynamicArtRes.hasErrors()) return dynamicArtRes.reerr();
            var body = new ArtifactSourceObjectConstType(
                    dynamicArtRes.requireValue("TypeSourceObjectConst::export dynamic"));
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactSourceObjectConstTypeCodec.toValue(body))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactSourceObjectConstType body;
            try {
                body = ArtifactSourceObjectConstTypeCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc,
                        "TypeSourceObjectConst data: failed to parse ArtifactSourceObjectConstType: " + e.getMessage());
            }
            var dynamicRes = ArtifactSupport.importArtifact(xx, loc, body.dynamic());
            if (dynamicRes.hasErrors()) return dynamicRes.reerr();
            return EResult.ok(new TypeSourceObjectConst(MustCast.mustCast(
                    TypeSourceObjectDynamic.class, dynamicRes.requireValue("TypeSourceObjectConst::import dynamic"))));
        }
    };
}
