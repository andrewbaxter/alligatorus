package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.value.AspectDynamicConstOf;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactEResultTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6017L)
public final class TypeEResultDynamic<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeEResultDynamic(T elementType) {
        this.elementType = elementType;
    }

    public T elementType() {
        return elementType;
    }

    public ArtifactSupport.ArtifactIdHolder artifactIdCell() {
        return artifactIdCell;
    }

    @Override
    public String typeDesc() {
        return "eresult(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstOf(new TypeEResultConst<>(elementType())));
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindEResultDynamic<>(this));
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(new AspectUnionEResultDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldEResultDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindEResultDynamic<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicEResultDynamic<>(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            var elementLift = GenAspectLift.getMaybe(xx, elementType);
            var errLift = GenAspectLift.getMaybe(xx, GenExprErr.dynamicType());
            if (elementLift.optIsNone() || errLift.optIsNone()) return Opt.none();
            return Opt.some(new AspectLiftEResultDynamic<>(this));
        }
        return Opt.none();
    }

    sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Factory(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    TMap<ConstKey, FieldSpec> fieldSpecs() {
        return TMap.ofEntries(
                new Pair<>(
                        new ConstKey.Str("ok"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.OK))),
                new Pair<>(
                        new ConstKey.Str("err"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.ERR))),
                new Pair<>(
                        new ConstKey.Str("errs"),
                        new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(
                                elementType, TypeEResultVariantConstructorDynamic.Kind.ERRS))));
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY, TypeEResultDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType);
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
    public EResult<ArtifactExporter> typeExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.ERESULT_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeEResultDynamic<?> eresult)) {
                return EResult.err(
                        loc,
                        "TypeEResultDynamic::exporterRootDelegateSelfExport: expected TypeEResultDynamic, got "
                                + self.getClass());
            }
            var cell = eresult.artifactIdCell();
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var elementArtRes = Exporters.exportType(xx, loc, eresult.elementType());
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactEResultType(elementArtRes.requireValue("TypeEResultDynamic::export element"));
                return EResult.ok(makeBody.apply(ArtifactEResultTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeEResultDynamic requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactEResultType body;
                        try {
                            body = ArtifactEResultTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeEResultDynamic data: failed to parse ArtifactEResultType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var elementT = elementRes.requireValue("TypeEResultDynamic::finalizer element type");
                            var out = new TypeEResultDynamic<>(elementT);
                            out.artifactIdCell().set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };
}
