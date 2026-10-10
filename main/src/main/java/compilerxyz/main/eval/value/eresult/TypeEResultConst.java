package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactEResultTypeCodec;
import compilerxyz.main.gen.codec.ArtifactEResultValueCodec;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6464L)
public final class TypeEResultConst<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeEResultConst(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public String typeDesc() {
        return "const eresult(" + elementType.typeDesc() + ")";
    }

    @Override
    public Meta typeMeta(ExprContext xx) {
        var key = new ArrayList<Object>(List.of("eresult_const", xx.vx.ix.metaLog.metaOf(xx, elementType)));
        return xx.vx.ix.metaLog.metaShared(key, () -> MetaSupport.derive(xx, this, "eresult_const"));
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectUnion.AspectId)) {
            return Opt.some(new AspectUnionEResultDynamic<>(new TypeEResultDynamic<>(elementType)));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldEResultConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicEResultConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindEResultConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactEResultConst.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    static Result<Value, TList<Value>> resultOf(Value value) {
        return MustCast.mustCast(ValueEResultConst.class, value).result();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var value = MustCast.mustCast(ValueEResultConst.class, self);
            var eresult = value.type();
            var erb = new Erb(loc);
            var elementTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, eresult.elementType));
            Opt<Artifact> okArt = Opt.none();
            var errArts = new TList<Artifact>();
            switch (resultOf(value)) {
                case Result.Ok<Value, TList<Value>> ok -> {
                    var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, ok.value()));
                    if (art.optIsSome()) okArt = Opt.some(art.optUnwrap());
                }
                case Result.Err<Value, TList<Value>> err -> {
                    for (var e : err.error()) {
                        var art = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, e));
                        if (art.optIsSome()) errArts.add(art.optUnwrap());
                    }
                }
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ArtifactEResultValue(elementTypeArt.optUnwrap(), okArt, errArts);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactEResultValueCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactEResultValue body;
            try {
                body = ArtifactEResultValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(
                        loc, "TypeEResultConst data: failed to parse ArtifactEResultValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var elementType = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.elementType()));
            Result<Value, TList<Value>> result;
            if (body.ok().optIsSome()) {
                var imported = erb.mustErMaybeUnreachable(
                        ArtifactSupport.importArtifact(xx, loc, body.ok().optUnwrap()));
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                result = new Result.Ok<>(ArtifactSupport.importedAsValue(imported.optUnwrap()));
            } else {
                var errs = new TList<Value>(body.errs().size());
                for (var errArt : body.errs()) {
                    var imported = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, errArt));
                    if (imported.optIsSome()) errs.add(ArtifactSupport.importedAsValue(imported.optUnwrap()));
                }
                result = new Result.Err<>(errs);
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            return EResult.ok(new ValueEResultConst<>(
                    new TypeEResultConst<>(MustCast.mustCast(Type.class, elementType.optUnwrap())), result));
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var eresult = MustCast.mustCast(TypeEResultConst.class, self);
            return ArtifactSupport.implExportIndependentArtifact(xx, eresult.artifactIdCell, () -> {
                var elementArtRes = Artifactable.exportArtifact(xx, loc, eresult.elementType);
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactEResultType(elementArtRes.requireValue("TypeEResultConst::export element"));
                return EResult.ok(makeBody.apply(ArtifactEResultTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeEResultConst requires an artifact id to deserialize");
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
                                    "TypeEResultConst data: failed to parse ArtifactEResultType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var out = new TypeEResultConst<>(
                                    elementRes.requireValue("TypeEResultConst::finalizer element type"));
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_E_RESULT_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeEResultConst<?> o
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
        return new TypeEResultDynamic<>(elementType).typeIsEqual(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return new AspectConsumeDynamicEResultConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.ERESULT_CONST_TYPE);
    }
}
