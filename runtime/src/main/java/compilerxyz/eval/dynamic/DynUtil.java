package compilerxyz.eval.dynamic;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.Eval;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.source_interface.BuiltinInterfaceKind;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.Unique;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.util.Opt;
import java.util.Iterator;
import java.util.Map;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDynamicBoxed;
import compilerxyz.gen.GenAspectRecord;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectTuple;

public final class DynUtil {
    private DynUtil() {
    }

    public record ArgSignatureSlot(Type elementType, JavaDataDescriptor jvmDesc) {}

    public static EResult<TList<ArgSignatureSlot>> argSignatureSlots(
            ExprContext xx, QExprId loc, Type argType) {
        var tupleOpt = compilerxyz.gen.GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            var elemTypes = tupleSome.value().aspectTupleElementTypes();
            var out = new TList<ArgSignatureSlot>(elemTypes.size());
            for (var et : elemTypes) {
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et,
                        descRes.requireValue("DynUtil::argSignatureSlots tuple element")));
            }
            return EResult.ok(out);
        }
        var recordOpt = compilerxyz.gen.GenAspectRecord.getMaybe(xx, argType);
        if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
            var fieldTypes = recordSome.value().aspectRecordFieldTypes();
            var out = new TList<ArgSignatureSlot>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                var et = entry.getValue();
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et,
                        descRes.requireValue("DynUtil::argSignatureSlots record field")));
            }
            return EResult.ok(out);
        }
        var descRes = jvmDescFor(xx, loc, argType);
        if (descRes.hasErrors()) return descRes.reerr();
        return EResult.ok(TList.of(new ArgSignatureSlot(argType,
                descRes.requireValue("DynUtil::argSignatureSlots scalar"))));
    }

    private static EResult<JavaDataDescriptor> jvmDescFor(ExprContext xx, QExprId loc, Type t) {
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, t);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        return EResult.ok(consumeRes.requireValue("DynUtil::jvmDescFor").aspectConsumeDynamicJvmClass(xx));
    }

    /**
     * Shared JVM foreach-loop skeleton. Callers provide the type-specific
     * pieces (iterator setup, element decode, element slot, store opcode, body)
     * and this emits the label-based loop wrapping them. The loop return value
     * is a stack null wrapped as a TypeNullDynamic ValueDynamic — no
     * AspectStackValueDynamic lookup involved.
     */
    public static CodeValue foreachSkeleton(
            ExprContext xx,
            QExprId loc,
            CodeDynamic iteratorSetup,
            CodeDynamic elementDecode,
            compilerxyz.eval.BindingId.Local elementSlot,
            int elementStoreOp,
            Code body) {
        var loopLabel = new org.objectweb.asm.tree.LabelNode();
        var doneLabel = new org.objectweb.asm.tree.LabelNode();
        var seq = new JavaBytecodeSequence();
        seq.add(iteratorSetup);
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        seq.add(
                JavaBytecodeUtils.callInterfaceMethod(
                        loc,
                        iteratorName,
                        "hasNext",
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of())));
        seq.add(
                new CodeDynamic(
                        loc,
                        new JavaBytecodeInstructionObj(
                                new org.objectweb.asm.tree.JumpInsnNode(
                                        org.objectweb.asm.Opcodes.IFEQ, doneLabel))));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
        seq.add(
                JavaBytecodeUtils.callInterfaceMethod(
                        loc,
                        iteratorName,
                        "next",
                        JavaMethodDescriptor.fromParts(
                                JavaDataDescriptor.fromClass(Object.class), TList.of())));
        seq.add(elementDecode);
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(elementStoreOp, elementSlot.key())));
        var bodyDyn = body.dynamic();
        if (bodyDyn != null) {
            seq.add(bodyDyn);
        }
        seq.add(
                new CodeDynamic(
                        loc,
                        new JavaBytecodeInstructionObj(
                                new org.objectweb.asm.tree.JumpInsnNode(
                                        org.objectweb.asm.Opcodes.GOTO, loopLabel))));
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        return new CodeValue(
            compilerxyz.eval.Code.of(loc, new CodeDynamic(loc, seq)),
            EvalOutcome.NO_VALUE);
    }

    public static EResult<CodeValue> elementUseDefaultDynamic(
            ExprContext xx, QExprId loc, Type self, ValueDynamicSource elementSource, Code carry) {
        return EResult.ok(new CodeValue(carry, EvalOutcome.ofValue(new ValueDynamic(elementSource, self))));
    }

    public static CodeDynamic renderSource(
            ExprContext xx, QExprId loc, ValueDynamicSource src) {
        if (src instanceof ValueDynamicSource.Expr) {
            throw new IllegalStateException(
                "ValueDynamicSource.Expr has no reconstructable code");
        }
        return src.realize(xx, loc);
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (!(valueOpt instanceof Opt.Some<Value> some)) {
            throw new IllegalStateException(
                    "bind_via_target called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = some.value().valueType();
        if (cv.code().isNone()) {
            Binding binding = new BindingValue(some.value());
            return EResult.ok(new AspectBind.BindResult(cv.code(), binding));
        }
        var bindDynOpt = GenAspectBindDynamic.getMaybe(xx, vtype);
        if (!(bindDynOpt instanceof Opt.Some<AspectBindDynamic> bindDynSome)) {
            return EResult.err(loc, "Type (" + vtype.typeDesc() + ") is missing aspect AspectBindDynamic");
        }
        var subRes = bindDynSome.value().aspectBindDynamicBindDynamic(xx, loc, cv);
        if (subRes.hasErrors()) {
            return subRes.reerr();
        }
        var subOpt = subRes.requireValue("DynUtil::bindViaTarget bindDynamic");
        if (!(subOpt instanceof Opt.Some<TargetBindSub> subSome)) {
            return EResult.err(
                    loc, "Type (" + vtype.typeDesc() + ") returned None from AspectBindDynamic");
        }
        var bound = xx.target.targetBind(xx, loc, subSome.value());
        Code wrappingCode = Code.of(loc, bound.code());
        Binding binding = new BindingDynamicBinding(bound.id(), vtype);
        return EResult.ok(new AspectBind.BindResult(wrappingCode, binding));
    }

    public static EResult<Opt<TargetBindSub>> bindDynamicDefault(
            ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.code().isNone()) {
            return EResult.ok(Opt.none());
        }
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
            throw new IllegalStateException(
                    "bind_dynamic_default called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = some.value().valueType();
        var dynRes = dynamicConsumeToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault dynamicConsumeToDynamic");
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    /**
     * Destructuring drop for stack-allocated composite (record/tuple-like) types.
     * The base sits on the JVM stack (source is {@code Source.Expr}). For each
     * child field whose type has {@code AspectDropDynamic} we emit
     * {@code DUP, <field-access>, <child-drop>} then finish with a single {@code POP}
     * to discard the base object reference. If no child needs a drop or the
     * source isn't stack-rooted the emission collapses to {@link #dropDefault}.
     *
     * @param children the destructured child accesses in emission order; each
     *                 entry pairs the child type (queried for AspectDropDynamic)
     *                 with the bytecode that pops the DUP'd base and pushes the
     *                 child value.
     */
    public static EResult<Code> dropDestructiveComposite(
            ExprContext xx, QExprId loc, ValueDynamicSource source, TList<DropChild> children) {
        boolean stackRooted = isStackRooted(source);
        TList<CodeDynamic> childDrops = new TList<>();
        for (var child : children) {
            var childDropOpt = compilerxyz.gen.GenAspectDropDynamic.getMaybe(xx, child.childType());
            if (!(childDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamic> childDropSome)) {
                continue;
            }
            var seq = new JavaBytecodeSequence();
            seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
            seq.add(child.access());
            var childDesc = CompositeClassEmitter.slotDescriptor(xx, child.childType()).value();
            var childWidth = childDesc.equals("J") || childDesc.equals("D") ? 2 : 1;
            var childCv =
                    new CodeValue(
                            Code.of(loc, new CodeDynamic(loc, seq)),
                            EvalOutcome.ofValue(
                                    ValueDynamic.stack(child.childType(), childWidth)));
            var childDropRes = childDropSome.value().aspectDropDynamicDrop(xx, loc, childCv);
            if (childDropRes.hasErrors()) {
                return childDropRes.reerr();
            }
            var childDropCode = childDropRes.requireValue("dropDestructiveComposite child drop");
            var childDyn = childDropCode.dynamic();
            if (childDyn != null) {
                childDrops.add(childDyn);
            }
        }
        if (childDrops.isEmpty()) {
            return EResult.ok(source.drop(loc));
        }
        var out = new JavaBytecodeSequence();
        if (!stackRooted) {
            out.add(source.realize(xx, loc));
        }
        for (var d : childDrops) out.add(d);
        out.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        return EResult.ok(Code.of(loc, new CodeDynamic(loc, out)));
    }

    /**
     * A child entry for {@link #dropDestructiveComposite}. {@code access} is
     * bytecode that consumes a base object reference from the stack and pushes
     * the corresponding child value; typical shape: {@code GETFIELD} or
     * {@code INVOKEINTERFACE} on a getter.
     */
    public record DropChild(Type childType, CodeDynamic access) {}

    public static EResult<Code> dropDefault(ExprContext xx, QExprId loc, CodeValue cv) {
        if (!(cv.value() instanceof Opt.Some<Value> some)) {
            return EResult.ok(Code.none(loc));
        }
        if (!(some.value() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        return EResult.ok(vd.source().drop(loc));
    }

    public static boolean isStackRooted(ValueDynamicSource source) {
        return switch (source) {
            case ValueDynamicSource.Expr expr -> true;
            case ValueDynamicSource.Binding binding -> false;
            case ValueDynamicSource.Field field -> isStackRooted(field.base());
        };
    }

    public static EResult<CodeDynamic> dynamicConsumeToDynamic(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().dynamic();
        if (dyn == null) {
            return EResult.err(loc, "Expression resulted in empty code");
        }
        return EResult.ok(dyn);
    }

    /**
     * If {@code type} carries AspectDynamicBoxed (i.e. is an unboxed primitive
     * dynamic type with a boxed counterpart), returns the boxed counterpart type.
     * Otherwise returns {@code type} unchanged. Used at container (list/map/tuple)
     * type-construction sites so element types are always boxed when flowing
     * through Object-shaped runtime containers.
     */
    public static Type substituteBoxed(ExprContext xx, Type type) {
        return switch (compilerxyz.gen.GenAspectDynamicBoxed.getMaybe(xx, type)) {
            case Opt.Some<compilerxyz.gen.AspectDynamicBoxed> boxedSome ->
                boxedSome.value().aspectDynamicBoxedBoxedForm();
            case Opt.None<compilerxyz.gen.AspectDynamicBoxed> n -> type;
        };
    }

    public static CodeValue dynamicStackToValue(
            ExprContext xx, QExprId loc, CodeDynamic value, Type type_, int stackSize) {
        var dynValue = ValueDynamic.stack(type_, stackSize);
        return new CodeValue(Code.of(loc, value), EvalOutcome.ofValue(dynValue));
    }

    public static CodeDynamic wrapWithCarry(QExprId loc, Code carry, CodeDynamic dynamicCode) {
        var carryDyn = carry.dynamic();
        if (carryDyn == null) {
            return dynamicCode;
        }
        var seq = new JavaBytecodeSequence();
        if (carryDyn.code() instanceof JavaBytecodeSequence carrySeq) {
            for (var child : carrySeq.children) seq.add(child);
        } else {
            seq.add(carryDyn);
        }
        seq.add(dynamicCode);
        return new CodeDynamic(loc, seq);
    }

    public static EResult<CodeValue> dynamicSet(
            ExprContext xx, QExprId loc, CodeValue self_, CodeValue value) {
        if (!(self_.value() instanceof Opt.Some<Value> selfSome)) {
            throw new IllegalStateException(
                    "dynamicSet called with absent value in self_; Rust unwraps unconditionally");
        }
        if (!(selfSome.value() instanceof ValueDynamic selfDyn)) {
            throw new IllegalStateException(
                    "dynamicSet: self_ value is not a ValueDynamic (got "
                            + selfSome.value().getClass().getName()
                            + ")");
        }
        Type targetType = selfDyn.type_();
        var convRes = Eval.typecheckConvert(xx, loc, targetType, value);
        if (convRes.hasErrors()) {
            return convRes.reerr();
        }
        var converted = convRes.requireValue("DynUtil::dynamicSet typecheckConvert");
        if (!(converted.value() instanceof Opt.Some<Value> convSome)) {
            return EResult.err(loc, "dynamicSet: converted value has no compile-time value");
        }
        Type valueType = convSome.value().valueType();
        var valueToDynRes = GenAspectConsumeDynamic.get(xx, loc, valueType);
        if (valueToDynRes.hasErrors()) {
            return valueToDynRes.reerr();
        }
        var valueToDyn = valueToDynRes.requireValue("DynUtil::dynamicSet consume aspect");
        var valueCodeRes = valueToDyn.aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (valueCodeRes.hasErrors()) {
            return valueCodeRes.reerr();
        }
        var valueCode = valueCodeRes.requireValue("DynUtil::dynamicSet consume.toDynamic");
        var selfCode = self_.code().dynamic();
        var nullStackRes = GenAspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
        if (nullStackRes.hasErrors()) {
            return nullStackRes.reerr();
        }
        var nullStack = nullStackRes.requireValue("DynUtil::dynamicSet null stack aspect");
        if (selfDyn.source() instanceof ValueDynamicSource.Binding bindingSrc) {
            var seq = new JavaBytecodeSequence();
            if (bindingSrc.key() instanceof compilerxyz.eval.BindingId.Local local) {
                // Local slot: pick the typed store opcode from the binding's type
                // so a primitive-typed slot uses DSTORE / ISTORE / etc.
                seq.add(valueCode);
                seq.add(
                    new CodeDynamic(
                        new CodeLoc.Some(loc),
                        new JavaBytecodeStoreLoad(
                            JvmSort.storeOpcode(xx, targetType), local.key())));
            } else {
                seq.add(BindingCode.bindingSet(new CodeLoc.Some(loc), xx.vx, bindingSrc.key(), valueCode));
            }
            // set expression's language type is null → push a matching Object null
            // so downstream consumers see a stack value.
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, seq);
            return EResult.ok(nullStack.aspectStackValueDynamicToValue(xx, loc, setJs));
        }
        if (selfDyn.source() instanceof ValueDynamicSource.Field fieldSrc) {
            if (isStackRooted(fieldSrc)) {
                return EResult.err(loc, "Cannot modify a temporary value");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(fieldSrc.base().realize(xx, loc));
            seq.add(valueCode);
            seq.add(
                JavaBytecodeUtils.setField(
                    loc, fieldSrc.klass(), fieldSrc.slotName(), fieldSrc.slotDesc()));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, seq);
            return EResult.ok(nullStack.aspectStackValueDynamicToValue(xx, loc, setJs));
        }
        return EResult.err(loc, "Cannot modify a temporary value");
    }

    /**
     * If {@code argType} is a tuple, destructure the compile-time value into per-element
     * CodeDynamic pushes (each element lowered via its own AspectConsumeDynamic). Otherwise
     * lowers the whole value via {@code argType}'s AspectConsumeDynamic and returns a
     * singleton list. Used at call sites whose JVM descriptor takes destructured positional args.
     */
    public static EResult<CodeValue> elementUseDefault(
            ExprContext xx,
            QExprId loc,
            compilerxyz.eval.value.ValueDynamicSource source,
            Code carry,
            Type type_) {
        var seq = new JavaBytecodeSequence();
        var carryDyn = carry.dynamic();
        if (carryDyn != null) seq.add(carryDyn);
        seq.add(renderSource(xx, loc, source));
        return EResult.ok(
                new CodeValue(
                        Code.of(loc, new CodeDynamic(loc, seq)),
                        EvalOutcome.ofValue(
                                new compilerxyz.eval.value.ValueDynamic(source, type_))));
    }

    public static boolean zeroArgSignature(ExprContext xx, Type argType) {
        var tupleOpt = compilerxyz.gen.GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            return tupleSome.value().aspectTupleElementTypes().isEmpty();
        }
        return false;
    }

    public static EResult<TList<CodeDynamic>> destructurePositional(
            ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
        var tupleOpt = compilerxyz.gen.GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var elementTypes = tupleSome.value().aspectTupleElementTypes();
            var elemCvs = tupleSome.value().aspectTupleTakeElements(xx, loc, some.value());
            if (elementTypes.size() != elemCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructurePositional: elementTypes/takeElements size mismatch");
            }
            return destructureFromElements(xx, loc, elementTypes, elemCvs);
        }
        var recordOpt = compilerxyz.gen.GenAspectRecord.getMaybe(xx, argType);
        if (recordOpt instanceof Opt.Some<compilerxyz.gen.AspectRecord> recordSome) {
            if (!(converted.value() instanceof Opt.Some<Value> some)) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var fieldTypes = recordSome.value().aspectRecordFieldTypes();
            var fieldCvs = recordSome.value().aspectRecordTakeFields(xx, loc, some.value());
            if (fieldTypes.size() != fieldCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructurePositional: record fieldTypes/takeFields size mismatch");
            }
            var elemTypes = new TList<Type>(fieldTypes.size());
            var elemCvs = new TList<EResult<CodeValue>>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                elemTypes.add(entry.getValue());
                var cvRes = fieldCvs.get(entry.getKey());
                if (cvRes == null) {
                    return EResult.err(loc,
                            "DynUtil::destructurePositional: record missing field '"
                                    + entry.getKey().toCanonical() + "'");
                }
                elemCvs.add(cvRes);
            }
            return destructureFromElements(xx, loc, elemTypes, elemCvs);
        }
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDyn =
                consumeRes
                        .requireValue("DynUtil::destructurePositional arg consume")
                        .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (argDyn.hasErrors()) return argDyn.reerr();
        return EResult.ok(TList.of(argDyn.requireValue("DynUtil::destructurePositional arg")));
    }

    private static EResult<TList<CodeDynamic>> destructureFromElements(
            ExprContext xx,
            QExprId loc,
            TList<Type> elementTypes,
            TList<EResult<CodeValue>> elemCvs) {
        var out = new TList<CodeDynamic>(elementTypes.size());
        for (int i = 0; i < elementTypes.size(); i++) {
            var elemCvRes = elemCvs.get(i);
            if (elemCvRes.hasErrors()) return elemCvRes.reerr();
            var elemCv = elemCvRes.requireValue("DynUtil::destructurePositional element");
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementTypes.get(i));
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var elemDyn =
                    consumeRes
                            .requireValue("DynUtil::destructurePositional consume")
                            .aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (elemDyn.hasErrors()) return elemDyn.reerr();
            out.add(elemDyn.requireValue("DynUtil::destructurePositional elem toDynamic"));
        }
        return EResult.ok(out);
    }

    public static Type derivedSourceType(ExprContext xx, CodeValue cv) {
        if (!(cv.value() instanceof Opt.Some<Value> someVal)) {
            throw new IllegalStateException(
                    "derivedSourceType: CodeValue has no compile-time value");
        }
        return someVal.value().valueType();
    }

}
