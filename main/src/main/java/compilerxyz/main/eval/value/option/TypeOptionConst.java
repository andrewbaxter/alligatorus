package compilerxyz.main.eval.value.option;

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
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactOptionTypeCodec;
import compilerxyz.main.gen.codec.ArtifactOptionValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6463L)
public final class TypeOptionConst<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeOptionConst(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public String typeDesc() {
        return "const option(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_OPTION_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(GenAspectUnion.getMaybe(xx, GenOptions.optionDynamic(xx, elementType))
                    .optUnwrap());
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldOptionConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicOptionConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindOptionConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(new AspectArtifactOptionConst<>(this));
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(loc, "TypeOptionConst data is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactOptionValue body;
            try {
                body = ArtifactOptionValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeOptionConst data: failed to parse ArtifactOptionValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var elementTypeOpt =
                    erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.elementType()));
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var elementType = MustCast.mustCast(Type.class, elementTypeOpt.optUnwrap());
            Opt<Object> payload = Opt.none();
            if (body.payload().optIsSome()) {
                var imported = erb.mustErMaybeUnreachable(
                        ArtifactSupport.importArtifact(xx, loc, body.payload().optUnwrap()));
                if (imported.optIsSome()) payload = Opt.some(ConstHost.hostOf(imported.optUnwrap()));
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            Object host =
                    switch (GenOptions.optionDynamic(xx, elementType)) {
                        case TypeOptionSpecializedDynamic<?> spec -> spec.storeFromBoxed(payload);
                        case TypeOptionDynamic<?> boxed -> payload;
                    };
            return EResult.ok(host);
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var option = MustCast.mustCast(TypeOptionConst.class, self);
            return ArtifactSupport.implExportIndependentArtifact(xx, option.artifactIdCell, () -> {
                var elementArtRes = Exporters.exportType(xx, loc, option.elementType);
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactOptionType(elementArtRes.requireValue("TypeOptionConst::export element"));
                return EResult.ok(makeBody.apply(ArtifactOptionTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeOptionConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactOptionType body;
                        try {
                            body = ArtifactOptionTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeOptionConst data: failed to parse ArtifactOptionType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var out = new TypeOptionConst<>(
                                    elementRes.requireValue("TypeOptionConst::finalizer element type"));
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_OPTION_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeOptionConst<?> o
                && elementType.typeIsEqual(xx, o.elementType);
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
        var dynamic = GenOptions.optionDynamic(xx, elementType);
        return dynamic.typeIsEqual(xx, destType) || dynamic.typeCanCastTo(xx, loc, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var dynamicRes = new AspectConsumeDynamicOptionConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (dynamicRes.hasErrors()) return dynamicRes.reerr();
        var dynamic = GenOptions.optionDynamic(xx, elementType);
        if (dynamic.typeIsEqual(xx, destType)) return dynamicRes;
        return dynamic.typeCastTo(xx, loc, dynamicRes.requireValue("TypeOptionConst::castTo dynamic"), destType);
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.OPTION_CONST_TYPE);
    }
}
