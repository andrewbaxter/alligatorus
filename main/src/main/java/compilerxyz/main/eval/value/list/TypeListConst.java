package compilerxyz.main.eval.value.list;

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
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectListConst;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactListTypeCodec;
import compilerxyz.main.gen.codec.ArtifactListValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6152L)
public final class TypeListConst<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeListConst(T elementType) {
        this.elementType = elementType;
    }

    public Type elementType() {
        return elementType;
    }

    @Override
    public String typeDesc() {
        return "const list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectListConst.AspectId)) {
            return Opt.some(new AspectListConstListConst<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableListConst<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldListConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicListConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindListConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactListConst.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    static TList<Object> hostElements(Object host) {
        if (host instanceof ListStore store) {
            var out = new TList<Object>(store.listStoreSize());
            for (int i = 0; i < store.listStoreSize(); i++) out.add(store.listStoreBoxed(i));
            return out;
        }
        var out = new TList<Object>();
        for (var element : MustCast.mustCast(TList.class, host)) out.add(element);
        return out;
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var value = MustCast.mustCast(ValueObjectConst.class, self);
            var list = MustCast.mustCast(TypeListConst.class, value.type());
            var erb = new Erb(loc);
            var elementTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, list.elementType()));
            var elementArts = new TList<Artifact>();
            for (var host : hostElements(value.data())) {
                var element = erb.mustErMaybeUnreachable(ConstHost.lift(xx, loc, list.elementType(), host));
                if (element.optIsNone()) continue;
                var elementArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, element.optUnwrap()));
                if (elementArt.optIsSome()) elementArts.add(elementArt.optUnwrap());
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ArtifactListValue(elementTypeArt.optUnwrap(), elementArts);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactListValueCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactListValue body;
            try {
                body = ArtifactListValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeListConst data: failed to parse ArtifactListValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var elementTypeOpt =
                    erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.elementType()));
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var elementType = MustCast.mustCast(Type.class, elementTypeOpt.optUnwrap());
            var hosts = new TList<Object>(body.elements().size());
            for (var elementArt : body.elements()) {
                var element = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, elementArt));
                if (element.optIsNone()) continue;
                var host = erb.mustErMaybeUnreachable(
                        ConstHost.unlift(loc, ArtifactSupport.importedAsValue(element.optUnwrap())));
                if (host.optIsSome()) hosts.add(host.optUnwrap());
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            Object host =
                    switch (GenLists.listDynamic(xx, elementType)) {
                        case TypeListSpecializedDynamic<?> spec -> {
                            var store = spec.newStore();
                            for (var element : hosts) store.listStorePushBoxed(element);
                            yield store;
                        }
                        case TypeListBoxedDynamic<?> boxed -> hosts;
                    };
            return EResult.ok(new ValueObjectConst(new TypeListConst<>(elementType), host));
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var selfDc = GenAspectDynamicConst.getMaybe(xx, MustCast.mustCast(Type.class, self));
            if (selfDc.optIsSome()) self = selfDc.optUnwrap().aspectDynamicConstConstForm();
            var list = MustCast.mustCast(TypeListConst.class, self);
            var cell = list.artifactIdCell;
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var elementArtRes = Artifactable.exportArtifact(xx, loc, list.elementType());
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactListType(elementArtRes.requireValue("TypeListConst::export element"));
                return EResult.ok(makeBody.apply(ArtifactListTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeListConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactListType body;
                        try {
                            body = ArtifactListTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeListConst data: failed to parse ArtifactListType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var elementT = elementRes.requireValue("TypeListConst::finalizer element type");
                            var out = GenLists.listDynamic(xx2, elementT);
                            out.listDynamicArtifactIdCell().set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    EResult<CodeValue> dynamicCv(ExprContext xx, QExprId loc, CodeValue self) {
        return new AspectConsumeDynamicListConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, self);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_LIST_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeListConst<?> o
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
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_LIST_BOXED_DYNAMIC_IDENTITY)
                        .optIsSome()
                || GenLists.listDynamic(xx, elementType).typeIsEqual(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return dynamicCv(xx, loc, cv);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.LIST_CONST_TYPE);
    }
}
