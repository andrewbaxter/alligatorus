package compilerxyz.main.eval.value.list;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
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
import compilerxyz.main.eval.binding.BindingIteratorStack;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicConst;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectFieldUseDynamic;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public final class TypeListSpecializedDynamic<T extends Type>
        implements ListDynamic,
                AspectFieldUseDynamic,
                AspectDynamicConst,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectIterable,
                AspectStackValueDynamic {

    public final T elementType;
    public final JavaInternalName storeName;
    public final Unique identity;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeListSpecializedDynamic(T elementType, JavaInternalName storeName, Unique identity) {
        this.elementType = elementType;
        this.storeName = storeName;
        this.identity = identity;
    }

    @Override
    public ArtifactSupport.ArtifactIdHolder listDynamicArtifactIdCell() {
        return artifactIdCell;
    }

    public JavaDataDescriptor elementDescriptor(ExprContext xx) {
        return GenAspectConsumeDynamic.getMaybe(xx, elementType).optUnwrap().aspectConsumeDynamicJvmClass(xx);
    }

    public JavaMethodDescriptor pushDescriptor(ExprContext xx) {
        return JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(elementDescriptor(xx)));
    }

    public CodeDynamic emitNew(ExprContext xx, QExprId loc, TList<CodeDynamic> elements) {
        var pushDesc = pushDescriptor(xx);
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.instantiate(
                loc,
                storeName,
                JavaMethodDescriptor.fromConstructorParts(TList.of()),
                new CodeDynamic(loc, new JavaBytecodeSequence())));
        for (var element : elements) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            seq.add(element);
            seq.add(JavaBytecodeUtils.callMethod(loc, storeName, "push", pushDesc));
        }
        return new CodeDynamic(loc, seq);
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(storeName);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn == null) {
            throw new IllegalStateException(
                    "TypeListSpecializedDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
        }
        return EResult.ok(dyn);
    }

    @Override
    public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
            ExprContext xx, QExprId loc, ValueDynamicSource source) {
        return EResult.ok(new CodeValue(
                Code.of(DynUtil.renderSource(xx, loc, source)), EvalOutcome.ofValue(new ValueDynamic(source, this))));
    }

    @Override
    public String typeDesc() {
        return "list(" + elementType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(identity)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)
                || key.equals(GenAspectDynamicConst.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
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

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListSpecializedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (ck.equals(new ConstKey.Str("new"))) {
            return EResult.ok(new CodeValue(
                    self.code(),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            ValueObjectConst.plainTypeType(), new TypeListSpecializedNewDynamic(this)))));
        }
        if (ck.equals(new ConstKey.Str("push"))) {
            return EResult.ok(new CodeValue(
                    self.code(),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, new TypeListSpecializedPushDynamic(this)))));
        }
        return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(new ConstKey.Str("new"), new ConstKey.Str("push"));
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(new ConstKey.Str("new"))) {
            return Opt.some(new TypeListSpecializedNewDynamic(this));
        }
        if (key.equals(new ConstKey.Str("push"))) {
            return Opt.some(new TypeListSpecializedPushDynamic(this));
        }
        return Opt.none();
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
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
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
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
        var baseDynRes = aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDynRes.hasErrors()) return baseDynRes.reerr();
        var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");
        Code bodyCode;
        if (body.value().optIsSome()) {
            var dropRes = xx.target.targetDrop(xx, loc, body);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var dropCode = dropRes.requireValue("TypeListSpecializedDynamic::foreach body drop");
            var bodyDyn = body.code().codeDynamic();
            var dropDyn = dropCode.codeDynamic();
            if (bodyDyn == null && dropDyn == null) {
                bodyCode = Code.none(loc);
            } else {
                var seq = new JavaBytecodeSequence();
                if (bodyDyn != null) seq.add(bodyDyn);
                if (dropDyn != null) seq.add(dropDyn);
                bodyCode = Code.of(new CodeDynamic(loc, seq));
            }
        } else {
            bodyCode = body.code();
        }
        var localBinding = (elementScopeId);
        var storeOp = JvmSort.storeOpcode(xx, elementType);
        var getDesc = JavaMethodDescriptor.fromParts(elementDescriptor(xx), TList.of(JavaDataDescriptor.DESC_INT));
        var sizeDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());
        var indexSlot = new BindingId();
        var loopLabel = new LabelNode();
        var doneLabel = new LabelNode();
        var loopSeq = new JavaBytecodeSequence();
        loopSeq.add(baseDyn);
        loopSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ISTORE, indexSlot)));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        loopSeq.add(JavaBytecodeUtils.callMethod(loc, storeName, "size", sizeDesc));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.SWAP));
        loopSeq.add(
                new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IF_ICMPGE, doneLabel))));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        loopSeq.add(JavaBytecodeUtils.callMethod(loc, storeName, "get", getDesc));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(storeOp, localBinding)));
        var bodyDyn = bodyCode.codeDynamic();
        if (bodyDyn != null) {
            loopSeq.add(bodyDyn);
        }
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        loopSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, 1));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.IADD));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ISTORE, indexSlot)));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, loopLabel))));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(new CodeValue(Code.of(new CodeDynamic(loc, loopSeq)), EvalOutcome.NO_VALUE));
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
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, identity).optIsSome();
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
