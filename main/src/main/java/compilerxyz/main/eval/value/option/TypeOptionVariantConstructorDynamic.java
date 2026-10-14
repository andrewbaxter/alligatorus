package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6132L)
public record TypeOptionVariantConstructorDynamic(Type elementType, boolean isSome) implements Aspect, Type {

    @Override
    public String typeDesc() {
        return "option(" + elementType.typeDesc() + ")." + (isSome ? "some" : "none");
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionOptionVariantConstructorDynamic(this));
        }
        return Opt.none();
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.OPTION_VARIANT_CONSTRUCTOR_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(
                    loc,
                    "TypeOptionVariantConstructorDynamic::exporterRootDelegateSelfExport: recursive export_artifact"
                            + " (eval/artifact.rs) port deferred");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return EResult.err(
                    loc,
                    "TypeOptionVariantConstructorDynamic::exporterRootDelegateSelfFromArtifactData: recursive"
                            + " import_type_artifact (eval/artifact.rs) port deferred");
        }
    };

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
                GenTrivialIdentityAspects.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY,
                TypeOptionVariantConstructorDynamic.class);
        return oOpt.optIsSome()
                && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType)
                && isSome == oOpt.optUnwrap().isSome;
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
}
