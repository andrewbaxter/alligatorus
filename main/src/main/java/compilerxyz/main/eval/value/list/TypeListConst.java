package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter.Root;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.AspectListConst;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectListConst;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactListTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;

@BuiltinLight(uniqueId = 6152L)
public final class TypeListConst<T extends Type>
        implements Type,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectIterable,
                AspectListConst,
                ExporterAny {

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
        if (key.equals(GenAspectListConst.AspectId)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @SuppressWarnings("unchecked")
    private static TList<Value> elementsOf(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof TList<?> els) {
            return (TList<Value>) els;
        }
        throw new IllegalArgumentException("TypeListConst: expected list ValueObjectConst, got " + value.getClass());
    }

    @Override
    public TList<Value> aspectListConstElements(Value value) {
        return elementsOf(value);
    }

    @Override
    public Type aspectListConstElementType(Value value) {
        return elementType;
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.LIST_CONST;
    }

    private static Artifact nullArtifact() {
        return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new Root()));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, ExporterAny self, Function<JsonValue, ArtifactBody> makeBody) {
            TypeListConst list;
            if (self instanceof TypeListConst<?> t) {
                list = t;
            } else if (self instanceof ValueObjectConst v && v.type() instanceof TypeListConst<?> t) {
                list = t;
            } else {
                return EResult.err(
                        loc,
                        "TypeListConst::exporterRootDelegateSelfExport: expected TypeListConst, got "
                                + self.getClass());
            }
            var cell = list.artifactIdCell;
            var errors = new TList<ExprErr>();
            var artifact = ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, cell, () -> {
                var elementArtRes = list.elementType().artifactableExportArtifact(xx, loc);
                elementArtRes.drainInto(errors);
                var elementArt = elementArtRes.valueOrNull() != null
                        ? elementArtRes.requireValue("TypeListConst::exporterExport element")
                        : nullArtifact();
                var body = new ArtifactListType(elementArt);
                return makeBody.apply(ArtifactListTypeCodec.toValue(body));
            });
            if (errors.isEmpty()) return EResult.ok(artifact);
            return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
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
                        var holder = new ExporterAny[] {null};
                        ExporterAny stub = new ExporterAny() {
                            @Override
                            public EResult<Type> exporterAnyAsType() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException("TypeListConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsType();
                            }

                            @Override
                            public EResult<Value> exporterAnyAsValue() {
                                if (holder[0] == null) {
                                    throw new IllegalStateException("TypeListConst import stub accessed pre-finalize");
                                }
                                return holder[0].exporterAnyAsValue();
                            }
                        };
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var elementT = elementRes.requireValue("TypeListConst::finalizer element type");
                            var out = GenLists.listDynamic(xx2, elementT);
                            out.listDynamicArtifactIdCell().set(idValue);
                            holder[0] = out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
                    });
        }
    };

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(
                    new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
        }
        return EResult.err(loc, "TypeListConst::bind: CodeValue has no compile-time value");
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return switch (GenLists.listDynamic(xx, elementType)) {
            case TypeListSpecializedDynamic<?> spec -> spec.aspectConsumeDynamicJvmClass(xx);
            case TypeListBoxedDynamic<?> boxed ->
                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class));
        };
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeListConst::toDynamic: CodeValue has no compile-time value");
        }
        var elements = elementsOf(cv.value().optUnwrap());
        return switch (GenLists.listDynamic(xx, elementType)) {
            case TypeListSpecializedDynamic<?> spec -> {
                var dynamicElements = new TList<CodeDynamic>();
                for (var element : elements) {
                    var convRes = Eval.typecheckConvert(
                            xx, loc, elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (convRes.hasErrors()) yield convRes.reerr();
                    var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
                    if (elemConsumeRes.hasErrors()) yield elemConsumeRes.reerr();
                    var elemDynRes = elemConsumeRes
                            .requireValue("TypeListConst::toDynamic element AspectConsumeDynamic")
                            .aspectConsumeDynamicToDynamic(
                                    xx, loc, convRes.requireValue("TypeListConst::toDynamic element convert"));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    dynamicElements.add(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic"));
                }
                yield EResult.ok(spec.emitNew(xx, loc, dynamicElements));
            }
            case TypeListBoxedDynamic<?> boxed -> {
                var listName = JavaInternalName.fromClass(TList.class);
                var seq = new JavaBytecodeSequence();
                seq.add(JavaBytecodeUtils.instantiate(
                        loc,
                        listName,
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of()),
                        new CodeDynamic(loc, new JavaBytecodeSequence())));
                var addDesc = JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JvmConstants.DESC_OBJECT));
                for (var element : elements) {
                    var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, element.valueType());
                    if (elemConsumeRes.hasErrors()) yield elemConsumeRes.reerr();
                    var elemDynRes = elemConsumeRes
                            .requireValue("TypeListConst::toDynamic element AspectConsumeDynamic")
                            .aspectConsumeDynamicToDynamic(
                                    xx, loc, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
                    seq.add(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic"));
                    seq.add(JavaBytecodeUtils.callMethod(loc, listName, "add", addDesc));
                    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
                }
                yield EResult.ok(new CodeDynamic(loc, seq));
            }
        };
    }

    private EResult<CodeValue> dynamicCv(ExprContext xx, QExprId loc, CodeValue self) {
        var dynRes = aspectConsumeDynamicToDynamic(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        var dynType = GenLists.listDynamic(xx, elementType);
        return EResult.ok(new CodeValue(
                Code.of(dynRes.requireValue("TypeListConst::dynamicCv")),
                EvalOutcome.ofValue(ValueDynamic.stack(dynType, 1))));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dynRes = dynamicCv(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        var fieldRes = GenAspectField.get(xx, loc, GenLists.listDynamic(xx, elementType));
        if (fieldRes.hasErrors()) return fieldRes.reerr();
        return fieldRes.requireValue("TypeListConst::field AspectField")
                .aspectFieldField(xx, loc, dynRes.requireValue("TypeListConst::field self"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, GenLists.listDynamic(xx, elementType))
                .optUnwrap()
                .aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, GenLists.listDynamic(xx, elementType))
                .optUnwrap()
                .aspectFieldTypeOpt(xx, key);
    }

    @Override
    public Type aspectIterableElementType() {
        return elementType;
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return GenAspectIterable.getMaybe(xx, GenLists.listDynamic(xx, elementType))
                .optUnwrap()
                .aspectIterableLoopBinding(xx);
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
        var dynRes = dynamicCv(xx, loc, base);
        if (dynRes.hasErrors()) return dynRes.reerr();
        var iterRes = GenAspectIterable.get(xx, loc, GenLists.listDynamic(xx, elementType));
        if (iterRes.hasErrors()) return iterRes.reerr();
        return iterRes.requireValue("TypeListConst::foreach AspectIterable")
                .aspectIterableForeach(
                        xx, loc, dynRes.requireValue("TypeListConst::foreach self"), elementScopeId, body);
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(xx, loc, cv);
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
                .optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return dynamicCv(xx, loc, cv);
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
