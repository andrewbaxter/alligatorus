package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
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
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactListTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6117L)
public final class TypeListBoxedDynamic<T extends Type> implements Aspect, ListDynamic {

    public final T elementType;
    public final ArtifactIdHolder artifactIdCell = new ArtifactIdHolder();

    public TypeListBoxedDynamic(T elementType) {
        this.elementType = elementType;
    }

    @Override
    public ArtifactIdHolder listDynamicArtifactIdCell() {
        return artifactIdCell;
    }

    @Override
    public Type listDynamicElementType() {
        return elementType;
    }

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectLift.AspectId)) {
            return switch (GenAspectLift.getMaybe(xx, elementType)) {
                case Opt.Some<AspectLift> elementLift -> Opt.some(new AspectLiftListBoxedDynamic<>(this));
                case Opt.None<AspectLift> none -> Opt.none();
            };
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindListBoxedDynamic<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_BOXED_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, elementType).optIsSome()
                    ? Opt.some(new AspectDropDynamicListBoxedDynamic<>(this))
                    : Opt.none();
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindListBoxedDynamic<>(this));
        }
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstListBoxedDynamic<>(this));
        }
        return Opt.none();
    }

    sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Method(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }

        record Length() implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return new TypeIntDynamic();
            }
        }
    }

    TMap<ConstKey, FieldSpec> fieldSpecs(ExprContext xx) {
        var m = new TMap<ConstKey, FieldSpec>();
        m.put(new ConstKey.Str("push"), new FieldSpec.Method(new TypeListPushDynamic(elementType)));
        m.put(new ConstKey.Str("len"), new FieldSpec.Length());
        m.put(new ConstKey.Str("get"), new FieldSpec.Method(new TypeListGetDynamic(this)));
        m.put(new ConstKey.Str("set"), new FieldSpec.Method(new TypeListSetDynamic(this)));
        if (GenAspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
            m.put(new ConstKey.Str("join"), new FieldSpec.Method(new TypeListJoinDynamic(elementType)));
        }
        return m;
    }

    DynUtil.ElementDecode elementDecodeCast(ExprContext xx) {
        var elemDesc = JvmSort.descriptor(xx, elementType);
        return (b, raw) -> {
            if (elemDesc.isReference() && !elemDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
                return b.checkcast(elemDesc, raw);
            }
            return raw;
        };
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_LIST_BOXED_DYNAMIC_IDENTITY, TypeListBoxedDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType) || ListCastFrom.canCast(xx, loc, elementType, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && typeIsEqual(xx, cv.value().optUnwrap().valueType())) {
            return EResult.ok(cv);
        }
        return ListCastFrom.cast(
                xx,
                loc,
                this,
                elementType,
                new ListCastFrom.Store(
                        JavaInternalName.fromClass(TList.class),
                        "add",
                        JavaMethodDescriptor.fromParts(
                                JavaDataDescriptor.DESC_VOID, TList.of(JavaDataDescriptor.fromClass(Object.class)))),
                cv);
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
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.LIST_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof ListDynamic list)) {
                return EResult.err(
                        loc,
                        "TypeListBoxedDynamic::exporterRootDelegateSelfExport: expected a dynamic list type, got "
                                + self.getClass());
            }
            return ArtifactSupport.implExportIndependentArtifact(xx, list.listDynamicArtifactIdCell(), () -> {
                var elementArtRes = Exporters.exportType(xx, loc, list.listDynamicElementType());
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactListType(elementArtRes.requireValue("TypeListBoxedDynamic::export element"));
                return EResult.ok(makeBody.apply(ArtifactListTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeListBoxedDynamic requires an artifact id to deserialize");
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
                                    "TypeListBoxedDynamic data: failed to parse ArtifactListType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var elementT = elementRes.requireValue("TypeListBoxedDynamic::finalizer element type");
                            var out = GenLists.listDynamic(xx2, elementT);
                            out.listDynamicArtifactIdCell().set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };
}
