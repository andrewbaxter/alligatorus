package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.binding.BindingIteratorStack;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6117L)
public final class TypeListBoxedDynamic<T extends Type>
        implements ListDynamic,
                AspectInitialBindDynamic,
                AspectFieldUseDynamic,
                AspectDynamicConst,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectIterable,
                AspectStackValueDynamic {

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
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class));
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.target(DynUtil.renderSource(xx, loc, source)),
                EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_BOXED_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, elementType).optIsSome() ? Opt.some(this) : Opt.none();
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDynamicConst.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectIterable.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public Type aspectDynamicConstConstForm() {
        return new TypeListConst(elementType);
    }

    private sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Factory(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }

        record Method(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    private TMap<ConstKey, FieldSpec> fieldSpecs(ExprContext xx) {
        var m = new TMap<ConstKey, FieldSpec>();
        m.put(new ConstKey.Str("new"), new FieldSpec.Factory(new TypeListNewDynamic(elementType)));
        m.put(new ConstKey.Str("push"), new FieldSpec.Method(new TypeListPushDynamic(elementType)));
        if (GenAspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
            m.put(new ConstKey.Str("join"), new FieldSpec.Method(new TypeListJoinDynamic(elementType)));
        }
        return m;
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListBoxedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var spec = fieldSpecs(xx).getOrNull(ck);
        if (spec == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
        }
        return switch (spec) {
            case FieldSpec.Factory f ->
                EResult.ok(new CodeValue(
                        self.code(),
                        EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
            case FieldSpec.Method m ->
                EResult.ok(new CodeValue(self.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, m.type()))));
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(fieldSpecs(xx).keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var spec = fieldSpecs(xx).getOrNull(key);
        return spec == null ? Opt.none() : Opt.some(spec.fieldSpecFieldType());
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsNone()) {
            throw new IllegalStateException(
                    "TypeListBoxedDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
        }
        var dyn = dynCast.requireValue("dyn").optUnwrap();
        return EResult.ok(dyn);
    }

    @Override
    public Type aspectIterableElementType() {
        return elementType;
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorStack();
    }

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return EResult.ok(
                DynUtil.dynamicStackToValue(xx, DynUtil.iteratorNext(loc, elementDecodeCast(xx, loc)), elementType));
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseConsumeRes = GenAspectConsumeDynamic.get(xx, loc, this);
        if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
        var baseDynRes =
                baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDynRes.hasErrors()) return baseDynRes.reerr();
        var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

        var iterableName = JavaInternalName.fromClass(Iterable.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(baseDyn);
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }

        Code bodyCode;
        if (body.value().optIsSome()) {
            var dropRes = xx.target.targetDrop(xx, loc, body);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var dropCode = dropRes.requireValue("TypeListBoxedDynamic::foreach body drop");
            var bodyDynCast = CodeDynamic.castOpt(loc, body.code());
            if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
            var bodyDyn = bodyDynCast.requireValue("bodyDyn");
            var dropDynCast = CodeDynamic.castOpt(loc, dropCode);
            if (dropDynCast.hasErrors()) return dropDynCast.reerr();
            var dropDyn = dropDynCast.requireValue("dropDyn");
            if (bodyDyn.optIsNone() && dropDyn.optIsNone()) {
                bodyCode = Code.none(loc);
            } else {
                var seq = new JavaBytecodeSequence();
                if (bodyDyn.optIsSome()) seq.add(bodyDyn.optUnwrap());
                if (dropDyn.optIsSome()) seq.add(dropDyn.optUnwrap());
                bodyCode = Code.target(new CodeDynamic(loc, seq));
            }
        } else {
            bodyCode = body.code();
        }
        return DynUtil.foreachSkeleton(loc, iteratorSetup, elementBindDyn.optUnwrap(), bodyCode);
    }

    private CodeDynamic elementDecodeCast(ExprContext xx, QExprId loc) {
        var decodeSeq = new JavaBytecodeSequence();
        var elemDesc = JvmSort.descriptor(xx, elementType);
        if (elemDesc.isReference() && !elemDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            decodeSeq.add(JavaBytecodeUtils.cast(loc, elemDesc));
        }
        return new CodeDynamic(loc, decodeSeq);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var elemDropRes = GenAspectDropDynamic.get(xx, loc, elementType);
        if (elemDropRes.hasErrors()) return elemDropRes.reerr();
        var elemDrop = elemDropRes.requireValue("TypeListBoxedDynamic::drop element AspectDropDynamic");
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var elementSlot = new BindingId();
        var loadElement = new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, elementSlot));
        var elementCv =
                new CodeValue(Code.target(loadElement), EvalOutcome.ofValue(ValueDynamic.stack(xx, elementType)));
        var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
        if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
        var elemDropCode = elemDropCodeRes.requireValue("TypeListBoxedDynamic::drop element drop");

        var iterableName = JavaInternalName.fromClass(Iterable.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(vd.source().valueDynamicSourceRealize(xx, loc));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var elementStoreSeq = new JavaBytecodeSequence();
        elementStoreSeq.add(DynUtil.iteratorNext(loc, elementDecodeCast(xx, loc)));
        elementStoreSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, elementSlot)));
        var loopRes = DynUtil.foreachSkeleton(loc, iteratorSetup, new CodeDynamic(loc, elementStoreSeq), elemDropCode);
        if (loopRes.hasErrors()) return loopRes.reerr();
        var loopCodeCast = CodeDynamic.castOpt(loc, loopRes.requireValue("loop").code());
        if (loopCodeCast.hasErrors()) return loopCodeCast.reerr();
        var loopCode = loopCodeCast.requireValue("loopCode");
        var finalSeq = new JavaBytecodeSequence();
        if (loopCode.optIsSome()) finalSeq.add(loopCode.optUnwrap());
        finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, finalSeq)));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_LIST_BOXED_DYNAMIC_IDENTITY, TypeListBoxedDynamic.class);
        return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return typeIsEqual(xx, sourceType);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        return EResult.ok(cv);
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return aspectDynamicConstConstForm().artifactableExporter(xx, loc);
    }
}
