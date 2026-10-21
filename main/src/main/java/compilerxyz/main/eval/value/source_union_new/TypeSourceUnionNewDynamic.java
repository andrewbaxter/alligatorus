package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactDataSourceUnionNewCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6142L)
public record TypeSourceUnionNewDynamic(TypeSourceUnionDynamic type, int variantIndex) implements Aspect, Type {

    @Override
    public String typeDesc() {
        if (variantIndex < 0 || variantIndex >= type.variants.size()) {
            return "union_new(?)";
        }
        return "union_new(" + type.variants.get(variantIndex).tag.constKeyToCanonical() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)) {
            return Opt.some(new AspectFunctionSourceUnionNewDynamic(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(AspectBindSourceUnionNewDynamic.INSTANCE);
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var fn = MustCast.mustCast(TypeSourceUnionNewDynamic.class, self);
            var tu = fn.type;
            var variantIndex = fn.variantIndex;
            if (variantIndex < 0 || variantIndex >= tu.variants.size()) {
                return EResult.err(
                        loc,
                        "TypeSourceUnionNewDynamic::exporterExport: variant index " + variantIndex + " out of range");
            }
            var variant = tu.variants.get(variantIndex);
            return ArtifactSupport.implExportIndependentArtifact(xx, variant.newArtifactIdCell, () -> {
                var tuArtRes = Exporters.exportType(xx, loc, tu);
                if (tuArtRes.hasErrors()) return tuArtRes.reerr();
                var body = new ArtifactDataSourceUnionNew(
                        tuArtRes.requireValue("TypeSourceUnionNewDynamic::export union"), variantIndex);
                return EResult.ok(makeBody.apply(ArtifactDataSourceUnionNewCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeSourceUnionNewDynamic requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactDataSourceUnionNew body;
                        try {
                            body = ArtifactDataSourceUnionNewCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("TypeSourceUnionNewDynamic data: failed to parse "
                                    + "ArtifactDataSourceUnionNew: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var tuArt = body.union();
                        var variantIndex = body.variantIndex();
                        var deps = new TList<Artifact>();
                        deps.add(tuArt);
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var parentAny = ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, tuArt);
                            var parentVal = parentAny.requireValue("TypeSourceUnionNewDynamic::finalizer parent union");
                            var tuData = MustCast.mustCast(TypeSourceUnionDynamic.class, parentVal);
                            if (variantIndex >= 0 && variantIndex < tuData.variants.size()) {
                                tuData.variants
                                        .get(variantIndex)
                                        .newArtifactIdCell
                                        .set(idValue);
                            }
                            return new TypeSourceUnionNewDynamic(tuData, variantIndex);
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.SOURCE_UNION_NEW);
    }

    record SourceUnionNewConstBinding(Value value) implements Binding {
        @Override
        public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
            return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
        }

        @Override
        public Binding bindingFork() {
            return this;
        }

        @Override
        public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
            return EResult.ok(this);
        }

        @Override
        public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
            return Binding.bindingDrop(xx, loc, this);
        }
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
                GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY,
                TypeSourceUnionNewDynamic.class);
        return oOpt.optIsSome()
                && type.typeIsEqual(xx, oOpt.optUnwrap().type)
                && variantIndex == oOpt.optUnwrap().variantIndex;
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
