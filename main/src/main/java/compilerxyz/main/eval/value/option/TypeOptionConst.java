package compilerxyz.main.eval.value.option;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactOptionTypeCodec;
import compilerxyz.main.gen.codec.ArtifactOptionValueCodec;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaCache;
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

    private final MetaCache meta = new MetaCache("option_const");

    @Override
    public Meta typeMeta(ExprContext xx) {
        return meta.get(xx, this);
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
            return Opt.some(AspectArtifactOptionConst.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    static Opt<Value> payloadOf(Value value) {
        return MustCast.mustCast(ValueOptionConst.class, value).payload();
    }

    static EResult<Value> lift(ExprContext xx, QExprId loc, Type elementType, AspectLift elementLift, Opt<?> host) {
        Opt<Value> payload = Opt.none();
        if (host.optIsSome()) {
            var liftRes = elementLift.aspectLiftLift(xx, loc, host.optUnwrap());
            if (liftRes.hasErrors()) return liftRes.reerr();
            payload = Opt.some(liftRes.requireValue("TypeOptionConst::lift payload"));
        }
        return EResult.ok(new ValueOptionConst<>(new TypeOptionConst<>(elementType), payload));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var value = MustCast.mustCast(ValueOptionConst.class, self);
            var option = value.type();
            var erb = new Erb(loc);
            var elementTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, option.elementType));
            Opt<Artifact> payloadArt = Opt.none();
            var payload = payloadOf(value);
            if (payload.optIsSome()) {
                var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, payload.optUnwrap()));
                if (art.optIsSome()) payloadArt = Opt.some(art.optUnwrap());
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ArtifactOptionValue(elementTypeArt.optUnwrap(), payloadArt);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactOptionValueCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactOptionValue body;
            try {
                body = ArtifactOptionValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeOptionConst data: failed to parse ArtifactOptionValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var elementType = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.elementType()));
            Opt<Value> payload = Opt.none();
            if (body.payload().optIsSome()) {
                var imported = erb.mustErMaybeUnreachable(
                        ArtifactSupport.importArtifact(xx, loc, body.payload().optUnwrap()));
                if (imported.optIsSome()) payload = Opt.some(ArtifactSupport.importedAsValue(imported.optUnwrap()));
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            return EResult.ok(new ValueOptionConst<>(
                    new TypeOptionConst<>(MustCast.mustCast(Type.class, elementType.optUnwrap())), payload));
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var option = MustCast.mustCast(TypeOptionConst.class, self);
            return ArtifactSupport.implExportIndependentArtifact(xx, option.artifactIdCell, () -> {
                var elementArtRes = Artifactable.exportArtifact(xx, loc, option.elementType);
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactOptionType(elementArtRes.requireValue("TypeOptionConst::export element"));
                return EResult.ok(makeBody.apply(ArtifactOptionTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.OPTION_CONST_TYPE);
    }
}
