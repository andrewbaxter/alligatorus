package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.eval.value.array.TypeArrayConst;
import compilerxyz.main.eval.value.array.TypeArrayDynamic;
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
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.codec.ArtifactListTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinLight;
import compilerxyz.prelude.util.ListStore;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.function.Function;
import org.objectweb.asm.Opcodes;

@BuiltinLight(uniqueId = 6499L)
public final class TypeListDynamic<T extends Type> implements Aspect, Type {

    public final T elementType;
    public final JavaInternalName storeName;
    public final Unique identity;
    public final boolean boxedStore;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeListDynamic(T elementType, JavaInternalName storeName, Unique identity, boolean boxedStore) {
        this.elementType = elementType;
        this.storeName = storeName;
        this.identity = identity;
        this.boxedStore = boxedStore;
    }

    public static <T extends Type> TypeListDynamic<T> ofObject(T elementType) {
        return new TypeListDynamic<>(
                elementType,
                JavaInternalName.fromClass(TList.class),
                GenTrivialIdentityAspects.TYPE_LIST_DYNAMIC_IDENTITY,
                true);
    }

    public JavaDataDescriptor elementDescriptor(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, elementType).optUnwrap().aspectConsumeDynamicJvmClass(xx);
    }

    public JavaDataDescriptor slotDescriptor(ExprContext xx) {
        return boxedStore ? JvmConstants.DESC_OBJECT : elementDescriptor(xx);
    }

    public JavaBytecodeReg elementDecode(ExprContext xx, JavaBytecodeBuilder b, JavaBytecodeReg raw) {
        if (!boxedStore) return raw;
        var elemDesc = JvmSort.descriptor(xx, elementType);
        if (elemDesc.isReference() && !elemDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            return b.checkcast(elemDesc, raw);
        }
        return raw;
    }

    public JavaMethodDescriptor addDescriptor(ExprContext xx) {
        return JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(slotDescriptor(xx)));
    }

    public JavaMethodDescriptor getDescriptor(ExprContext xx) {
        return JavaMethodDescriptor.fromParts(slotDescriptor(xx), TList.of(JavaDataDescriptor.DESC_INT));
    }

    public static final JavaMethodDescriptor SIZE_DESCRIPTOR =
            JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());

    public ListStore newStore() {
        try {
            return MustCast.mustCast(
                    ListStore.class,
                    Class.forName(storeName.value.replace('/', '.'))
                            .getDeclaredConstructor()
                            .newInstance());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("list store " + storeName.value + " cannot be constructed", e);
        }
    }

    public JavaDataDescriptor storeDescriptor() {
        return JavaDataDescriptor.fromInternalName(storeName);
    }

    public CodeDynamicReg emitNew(ExprContext xx, QExprId loc, TList<CodeDynamicReg> elements) {
        var addDesc = addDescriptor(xx);
        var b = new JavaBytecodeBuilder(loc);
        var regs = new TList<JavaBytecodeReg>(elements.size());
        for (var element : elements) regs.add(b.reg(element));
        var list = b.newInstance(storeName, JavaMethodDescriptor.fromConstructorParts(TList.of()));
        for (var reg : regs) b.invokeVirtualVoid(storeName, "add", addDesc, list, reg);
        return b.code(list);
    }

    TMap<ConstKey, Type> methodTypes(ExprContext xx) {
        var m = new TMap<ConstKey, Type>();
        m.put(new ConstKey.Str("push"), new TypeListPushDynamic(this));
        m.put(new ConstKey.Str("get"), new TypeListGetDynamic(this));
        m.put(new ConstKey.Str("set"), new TypeListSetDynamic(this));
        if (GenAspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
            m.put(new ConstKey.Str("join"), new TypeListJoinDynamic(elementType));
        }
        return m;
    }

    static final ConstKey LEN_KEY = new ConstKey.Str("len");

    static Type lenType() {
        return new TypeIntDynamic();
    }

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectLift.AspectId)) {
            return switch (GenAspectLift.getMaybe(xx, elementType)) {
                case Opt.Some<AspectLift> elementLift -> Opt.some(new AspectLiftListDynamic<>(this));
                case Opt.None<AspectLift> none -> Opt.none();
            };
        }
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindListDynamic<>(this));
        }
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, elementType).optIsSome()
                    ? Opt.some(new AspectDropDynamicListDynamic<>(this))
                    : Opt.none();
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableListDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldListDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicListDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicListDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindListDynamic<>(this));
        }
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstListDynamic<>(this));
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicListDynamic<>(this));
        }
        return Opt.none();
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(xx, other, identity, TypeListDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        if (typeIsEqual(xx, sourceType)) return true;
        var tupleOpt = GenAspectTuple.getMaybe(xx, sourceType);
        if (tupleOpt.optIsSome()) {
            for (var sourceElement : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                if (!canConvert(xx, loc, sourceElement, elementType)) return false;
            }
            return true;
        }
        var arrayElement = arrayElementType(xx, sourceType);
        return arrayElement.optIsSome() && canConvert(xx, loc, arrayElement.optUnwrap(), elementType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && typeIsEqual(xx, cv.value().optUnwrap().valueType())) {
            return EResult.ok(cv);
        }
        var addDesc = addDescriptor(xx);
        if (cv.value().optIsNone()) {
            return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
        }
        var value = cv.value().optUnwrap();
        var sourceType = value.valueType();
        var storeCtor = JavaMethodDescriptor.fromConstructorParts(TList.of());
        var tupleOpt = GenAspectTuple.getMaybe(xx, sourceType);
        if (tupleOpt.optIsSome()) {
            var elements = new TList<CodeDynamicReg>();
            for (var elementRes : tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, value)) {
                if (elementRes.hasErrors()) return elementRes.reerr();
                var dynRes = DynUtil.materialize(
                        xx, loc, elementType, elementRes.requireValue("TypeListDynamic::castFrom tuple element"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                elements.add(dynRes.requireValue("TypeListDynamic::castFrom tuple element dynamic"));
            }
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var regs = new TList<JavaBytecodeReg>(elements.size());
            for (var element : elements) regs.add(b.reg(element));
            var list = b.newInstance(storeName, storeCtor);
            for (var reg : regs) b.invokeVirtualVoid(storeName, "add", addDesc, list, reg);
            return EResult.ok(DynUtil.regToValue(b.code(), list, this));
        }
        var sourceElementType = arrayElementType(xx, sourceType).optUnwrap();
        var sourceElementDesc = JvmSort.descriptor(xx, sourceElementType);
        var arrayDesc = sourceElementDesc.arrayOf();
        var storeDesc = storeDescriptor();
        var baseRes = DynUtil.materialize(xx, loc, cv);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var baseSlot = new BindingId();
        var indexSlot = new BindingId();
        var listSlot = new BindingId();
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        var b = new JavaBytecodeBuilder(loc);
        b.store(baseSlot, b.reg(baseRes.requireValue("TypeListDynamic::castFrom array")));
        b.store(indexSlot, b.constInt(0));
        b.store(listSlot, b.newInstance(storeName, storeCtor));
        b.land(loopKey);
        var index = b.load(indexSlot, JavaDataDescriptor.DESC_INT);
        var size = b.op(Opcodes.ARRAYLENGTH, JavaDataDescriptor.DESC_INT, b.load(baseSlot, arrayDesc));
        b.branch(Opcodes.IF_ICMPGE, doneKey, index, size);
        var eb = new JavaBytecodeBuilder(loc);
        var sourceElement = eb.op(
                sourceElementDesc.arrayLoadOpcode(),
                sourceElementDesc,
                eb.load(baseSlot, arrayDesc),
                eb.load(indexSlot, JavaDataDescriptor.DESC_INT));
        var elementRes = DynUtil.materialize(
                xx, loc, elementType, DynUtil.regToValue(eb.code(), sourceElement, sourceElementType));
        if (elementRes.hasErrors()) return elementRes.reerr();
        var list = b.load(listSlot, storeDesc);
        b.invokeVirtualVoid(
                storeName,
                "add",
                addDesc,
                list,
                b.reg(elementRes.requireValue("TypeListDynamic::castFrom array element")));
        b.store(
                indexSlot,
                b.op(
                        Opcodes.IADD,
                        JavaDataDescriptor.DESC_INT,
                        b.load(indexSlot, JavaDataDescriptor.DESC_INT),
                        b.constInt(1)));
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(DynUtil.regToValue(b.code(), b.load(listSlot, storeDesc), this));
    }

    private static boolean canConvert(ExprContext xx, QExprId loc, Type source, Type dest) {
        return source.typeCanCastTo(xx, loc, dest) || dest.typeCanCastFrom(xx, loc, source);
    }

    private static Opt<Type> arrayElementType(ExprContext xx, Type sourceType) {
        var dynamicOpt = AspectLookup.getMaybe(
                xx, sourceType, GenTrivialIdentityAspects.TYPE_ARRAY_DYNAMIC_IDENTITY, TypeArrayDynamic.class);
        if (dynamicOpt.optIsSome()) return Opt.some(dynamicOpt.optUnwrap().elementType);
        var constOpt = AspectLookup.getMaybe(
                xx, sourceType, GenTrivialIdentityAspects.TYPE_ARRAY_CONST_IDENTITY, TypeArrayConst.class);
        if (constOpt.optIsSome()) return Opt.some(TypeForms.dynamicForm(xx, constOpt.optUnwrap().elementType));
        return Opt.none();
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
    public Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx) {
        return Opt.some(ExporterRoot.LIST_DYNAMIC);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof TypeListDynamic<?> list)) {
                return EResult.err(
                        loc,
                        "TypeListDynamic::exporterRootDelegateSelfExport: expected a dynamic list type, got "
                                + self.getClass());
            }
            return ArtifactSupport.implExportIndependentArtifact(xx, list.artifactIdCell, () -> {
                var elementArtRes = Exporters.exportType(xx, loc, list.elementType);
                if (elementArtRes.hasErrors()) return elementArtRes.reerr();
                var body = new ArtifactListType(elementArtRes.requireValue("TypeListDynamic::export element"));
                return EResult.ok(makeBody.apply(ArtifactListTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "TypeListDynamic requires an artifact id to deserialize");
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
                                    "TypeListDynamic data: failed to parse ArtifactListType: " + e.getMessage());
                        }
                        return new Result.Ok<>(body);
                    },
                    (body) -> {
                        var deps = new TList<Artifact>();
                        deps.add(body.elementType());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var elementRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                    xx2, loc2, body.elementType());
                            var elementT = elementRes.requireValue("TypeListDynamic::finalizer element type");
                            var out = GenLists.listDynamic(xx2, elementT);
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };
}
