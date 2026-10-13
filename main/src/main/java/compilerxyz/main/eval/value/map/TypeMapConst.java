package compilerxyz.main.eval.value.map;

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
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactMapTypeCodec;
import compilerxyz.main.gen.codec.ArtifactMapValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6162L)
public final class TypeMapConst<K extends Type, V extends Type> implements Type, Aspect {

    public final K keyType;
    public final V valueType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeMapConst(K keyType, V valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String typeDesc() {
        return "const map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldMapConst<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableMapConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicMapConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindMapConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactMapConst.INSTANCE);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var value = MustCast.mustCast(ValueObjectConst.class, self);
            var map = MustCast.mustCast(TypeMapConst.class, value.type());
            var host = MustCast.mustCast(TOrderedMap.class, value.data());
            var erb = new Erb(loc);
            var keyTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, map.keyType));
            var valueTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, map.valueType));
            var entryArts = new TList<ArtifactMapEntry>();
            for (var raw : host.entrySet()) {
                var entry = MustCast.mustCast(Map.Entry.class, raw);
                var key = erb.mustErMaybeUnreachable(ConstHost.lift(xx, loc, map.keyType, entry.getKey()));
                var entryValue = erb.mustErMaybeUnreachable(ConstHost.lift(xx, loc, map.valueType, entry.getValue()));
                if (key.optIsNone() || entryValue.optIsNone()) continue;
                var keyArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, key.optUnwrap()));
                var valueArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, entryValue.optUnwrap()));
                if (keyArt.optIsSome() && valueArt.optIsSome()) {
                    entryArts.add(new ArtifactMapEntry(keyArt.optUnwrap(), valueArt.optUnwrap()));
                }
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ArtifactMapValue(keyTypeArt.optUnwrap(), valueTypeArt.optUnwrap(), entryArts);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactMapValueCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactMapValue body;
            try {
                body = ArtifactMapValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeMapConst data: failed to parse ArtifactMapValue: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var keyType = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.keyType()));
            var valueType = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.valueType()));
            var keys = new TList<Object>(body.entries().size());
            var values = new TList<Object>(body.entries().size());
            for (var entryArt : body.entries()) {
                var key = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, entryArt.key()));
                var value = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, entryArt.value()));
                if (key.optIsNone() || value.optIsNone()) continue;
                var keyHost = erb.mustErMaybeUnreachable(
                        ConstHost.unlift(loc, ArtifactSupport.importedAsValue(key.optUnwrap())));
                var valueHost = erb.mustErMaybeUnreachable(
                        ConstHost.unlift(loc, ArtifactSupport.importedAsValue(value.optUnwrap())));
                if (keyHost.optIsNone() || valueHost.optIsNone()) continue;
                keys.add(keyHost.optUnwrap());
                values.add(valueHost.optUnwrap());
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            return EResult.ok(new ValueObjectConst(
                    new TypeMapConst<>(
                            MustCast.mustCast(Type.class, keyType.optUnwrap()),
                            MustCast.mustCast(Type.class, valueType.optUnwrap())),
                    ConstHost.hostMap(keys, values)));
        }
    };

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var selfDc = GenAspectDynamicConst.getMaybe(xx, MustCast.mustCast(Type.class, self));
            if (selfDc.optIsSome()) self = selfDc.optUnwrap().aspectDynamicConstConstForm();
            var map = MustCast.mustCast(TypeMapConst.class, self);
            var cell = map.artifactIdCell;
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var erb = new Erb(loc);
                var keyArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, map.keyType));
                var valueArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, map.valueType));
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactMapType(keyArt.optUnwrap(), valueArt.optUnwrap());
                return EResult.ok(makeBody.apply(ArtifactMapTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeMapConst requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        ArtifactMapType body;
                        try {
                            body = ArtifactMapTypeCodec.fromValue(raw);
                        } catch (RuntimeException e) {
                            return new Result.Err<>(
                                    "TypeMapConst data: failed to parse ArtifactMapType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.keyType());
                        deps.add(body.valueType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var keyRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.keyType());
                            var keyT = keyRes.requireValue("TypeMapConst::finalizer key type");
                            var valueRes =
                                    ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.valueType());
                            var valueT = valueRes.requireValue("TypeMapConst::finalizer value type");
                            var out = new TypeMapDynamic(keyT, valueT);
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_MAP_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeMapConst<?, ?> o
                && keyType.typeIsEqual(xx, o.keyType)
                && valueType.typeIsEqual(xx, o.valueType);
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
        return new TypeMapDynamic<>(keyType, valueType).typeIsEqual(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return new AspectConsumeDynamicMapConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.MAP_CONST_TYPE);
    }
}
