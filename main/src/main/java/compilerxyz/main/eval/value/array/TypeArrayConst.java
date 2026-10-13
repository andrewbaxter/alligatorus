package compilerxyz.main.eval.value.array;

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
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ConstHost;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactArrayTypeCodec;
import compilerxyz.main.gen.codec.ArtifactArrayValueCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.lang.reflect.Array;
import java.util.function.BiFunction;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6465L)
public final class TypeArrayConst<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeArrayConst(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public String typeDesc() {
        return "const array(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_ARRAY_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldArrayConst<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableArrayConst<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicArrayConst<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindArrayConst<>(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactArrayConst.INSTANCE);
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
            var array = MustCast.mustCast(TypeArrayConst.class, value.type());
            var host = value.data();
            var erb = new Erb(loc);
            var elementTypeArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, array.elementType));
            var elementArts = new TList<Artifact>();
            for (int i = 0; i < Array.getLength(host); i++) {
                var element =
                        erb.mustErMaybeUnreachable(ConstHost.lift(xx, loc, array.elementType, Array.get(host, i)));
                if (element.optIsNone()) continue;
                var elementArt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, element.optUnwrap()));
                if (elementArt.optIsSome()) elementArts.add(elementArt.optUnwrap());
            }
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ArtifactArrayValue(elementTypeArt.optUnwrap(), elementArts);
            return EResult.ok(new Artifact.Body(makeBody.apply(ArtifactArrayValueCodec.toValue(body))));
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactArrayValue body;
            try {
                body = ArtifactArrayValueCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "TypeArrayConst data: failed to parse ArtifactArrayValue: " + e.getMessage());
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
            var array = Array.newInstance(ConstHost.hostClass(xx, JvmSort.descriptor(xx, elementType)), hosts.size());
            for (int i = 0; i < hosts.size(); i++) Array.set(array, i, hosts.get(i));
            return EResult.ok(new ValueObjectConst(new TypeArrayConst<>(elementType), array));
        }
    };

    static EResult<Artifact> exportType(
            ExprContext xx,
            QExprId loc,
            ArtifactSupport.ArtifactIdHolder cell,
            Type elementType,
            Function<JsonValue, ArtifactBody> makeBody) {
        return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
            var elementArtRes = Artifactable.exportArtifact(xx, loc, elementType);
            if (elementArtRes.hasErrors()) return elementArtRes.reerr();
            var body = new ArtifactArrayType(elementArtRes.requireValue("array type::export element"));
            return EResult.ok(makeBody.apply(ArtifactArrayTypeCodec.toValue(body)));
        });
    }

    static EResult<Artifactable> importType(
            ExprContext xx,
            QExprId loc,
            Opt<ArtifactIdSource> id,
            JsonValue data,
            BiFunction<Type, ArtifactIdSource, Type> make) {
        if (id.optIsNone()) {
            return EResult.err(loc, "array type requires an artifact id to deserialize");
        }
        var idValue = id.optUnwrap();
        return ArtifactSupport.implImportIndependentArtifact(
                xx,
                loc,
                idValue,
                data,
                (raw) -> {
                    ArtifactArrayType body;
                    try {
                        body = ArtifactArrayTypeCodec.fromValue(raw);
                    } catch (RuntimeException e) {
                        return new Result.Err<>(
                                "array type data: failed to parse ArtifactArrayType: " + e.getMessage());
                    }
                    return new Result.Ok<>(body);
                },
                (body) -> {
                    var deps = new TList<Artifact>();
                    deps.add(body.elementType());
                    ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> make.apply(
                            ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, body.elementType())
                                    .requireValue("array type::finalizer element type"),
                            idValue);
                    return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                });
    }

    public static final ExporterRootDelegate TYPE_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            var array = MustCast.mustCast(TypeArrayConst.class, self);
            return exportType(xx, loc, array.artifactIdCell, array.elementType, makeBody);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            return importType(xx, loc, id, data, (elementType, idValue) -> {
                var out = new TypeArrayConst<>(elementType);
                out.artifactIdCell.set(idValue);
                return out;
            });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var someOpt = other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_ARRAY_CONST_IDENTITY);
        return someOpt.optIsSome()
                && someOpt.optUnwrap() instanceof TypeArrayConst<?> o
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
        return new TypeArrayDynamic<>(elementType).typeIsEqual(xx, destType);
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return new AspectConsumeDynamicArrayConst<>(this).aspectConsumeDynamicToDynamic(xx, loc, cv);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.ok(ExporterRoot.ARRAY_CONST_TYPE);
    }
}
