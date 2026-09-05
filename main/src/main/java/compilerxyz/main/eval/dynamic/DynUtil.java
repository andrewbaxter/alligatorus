package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public final class DynUtil {
    private DynUtil() {}

    public record ArgSignatureSlot(Type elementType, JavaDataDescriptor jvmDesc) {}

    public static EResult<TList<ArgSignatureSlot>> argSignatureSlots(ExprContext xx, QExprId loc, Type argType) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsSome()) {
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var out = new TList<ArgSignatureSlot>(elemTypes.size());
            for (var et : elemTypes) {
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et, descRes.requireValue("DynUtil::argSignatureSlots tuple element")));
            }
            return EResult.ok(out);
        }
        var recordOpt = GenAspectRecord.getMaybe(xx, argType);
        if (recordOpt.optIsSome()) {
            var fieldTypes = recordOpt.optUnwrap().aspectRecordFieldTypes();
            var out = new TList<ArgSignatureSlot>(fieldTypes.size());
            for (var entry : fieldTypes.entrySet()) {
                var et = entry.getValue();
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et, descRes.requireValue("DynUtil::argSignatureSlots record field")));
            }
            return EResult.ok(out);
        }
        var descRes = jvmDescFor(xx, loc, argType);
        if (descRes.hasErrors()) return descRes.reerr();
        return EResult.ok(
                TList.of(new ArgSignatureSlot(argType, descRes.requireValue("DynUtil::argSignatureSlots scalar"))));
    }

    private static EResult<JavaDataDescriptor> jvmDescFor(ExprContext xx, QExprId loc, Type t) {
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, t);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        return EResult.ok(consumeRes.requireValue("DynUtil::jvmDescFor").aspectConsumeDynamicJvmClass(xx));
    }

    public static CodeDynamic iteratorNext(QExprId loc, CodeDynamic elementDecode) {
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(JavaBytecodeUtils.callInterfaceMethod(
                loc,
                JavaInternalName.fromClass(Iterator.class),
                "next",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
        seq.add(elementDecode);
        return new CodeDynamic(loc, seq);
    }

    public static EResult<CodeValue> foreachSkeleton(
            QExprId loc, CodeDynamic iteratorSetup, CodeDynamic elementBind, Code body) {
        var loopLabel = new LabelNode();
        var doneLabel = new LabelNode();
        var seq = new JavaBytecodeSequence();
        seq.add(iteratorSetup);
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(
                loc,
                iteratorName,
                "hasNext",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of())));
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IFEQ, doneLabel))));
        seq.add(elementBind);
        var bodyDynCast = CodeDynamic.castOpt(loc, body);
        if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
        var bodyDyn = bodyDynCast.requireValue("bodyDyn");
        if (bodyDyn.optIsSome()) {
            seq.add(bodyDyn.optUnwrap());
        }
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, loopLabel))));
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
    }

    public static EResult<TargetDynamic> jvmTarget(ExprContext xx, QExprId loc) {
        if (xx.target instanceof TargetDynamic t) return EResult.ok(t);
        return EResult.err(loc, "Value with a JVM representation is being bound under a non-JVM target");
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (valueOpt.optIsNone()) {
            throw new IllegalStateException(
                    "bind_via_target called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = valueOpt.optUnwrap().valueType();
        if (!dynamicHasCode(cv)) {
            Binding binding = new BindingValue(valueOpt.optUnwrap());
            return EResult.ok(new AspectBind.BindResult(cv.code(), binding));
        }
        var bindDynOpt = GenAspectBindDynamic.getMaybe(xx, vtype);
        if (bindDynOpt.optIsNone()) {
            return EResult.err(loc, "Type (" + vtype.typeDesc() + ") is missing aspect AspectBindDynamic");
        }
        var subRes = bindDynOpt.optUnwrap().aspectBindDynamicBindDynamic(xx, loc, cv);
        if (subRes.hasErrors()) {
            return subRes.reerr();
        }
        var subOpt = subRes.requireValue("DynUtil::bindViaTarget bindDynamic");
        if (subOpt.optIsNone()) {
            return EResult.err(loc, "Type (" + vtype.typeDesc() + ") returned None from AspectBindDynamic");
        }
        var targetRes = jvmTarget(xx, loc);
        if (targetRes.hasErrors()) return targetRes.reerr();
        var bound = targetRes.requireValue("DynUtil::bindViaTarget jvm target").bindSlot(xx, loc, subOpt.optUnwrap());
        Code wrappingCode = Code.target(bound.code);
        Binding binding = new BindingDynamicBinding(bound.id, vtype);
        return EResult.ok(new AspectBind.BindResult(wrappingCode, binding));
    }

    public static EResult<Opt<TargetBindSub>> bindDynamicDefault(QExprId loc, CodeValue cv) {
        if (!dynamicHasCode(cv)) {
            return EResult.ok(Opt.none());
        }
        if (cv.value().optIsNone()) {
            throw new IllegalStateException(
                    "bind_dynamic_default called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var dynRes = dynamicConsumeToDynamic(loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault dynamicConsumeToDynamic");
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    public record DropChild(Type childType, CodeDynamic access) {}

    public static EResult<Code> dropDefault(QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.ok(Code.none(loc));
        }
        if (!(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        return EResult.ok(vd.source().valueDynamicSourceDrop(loc));
    }

    public static EResult<CodeDynamic> dynamicConsumeToDynamic(QExprId loc, CodeValue cv) {
        var carryRes = CodeDynamic.castOpt(loc, cv.code());
        if (carryRes.hasErrors()) return carryRes.reerr();
        var carry = carryRes.requireValue("DynUtil::dynamicConsumeToDynamic carry");
        Opt<CodeDynamic> realized = cv.value().optIsSome() && cv.value().optUnwrap() instanceof ValueDynamic vd
                ? Opt.some(vd.source().valueDynamicSourceRealize(loc))
                : Opt.none();
        if (realized.optIsNone()) {
            if (carry.optIsNone()) return EResult.err(loc, "Expression resulted in empty code");
            return EResult.ok(carry.optUnwrap());
        }
        if (carry.optIsNone()) return EResult.ok(realized.optUnwrap());
        var seq = new JavaBytecodeSequence();
        seq.add(carry.optUnwrap());
        seq.add(realized.optUnwrap());
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    public static CodeValue methodRefOn(CodeValue receiver, Type methodType) {
        var vd = MustCast.mustCast(ValueDynamic.class, receiver.value().optUnwrap());
        return new CodeValue(receiver.code(), EvalOutcome.ofValue(new ValueDynamic(vd.source(), methodType)));
    }

    public static CodeValue dynamicCastKeepingSource(CodeValue cv, Type type) {
        var vd = MustCast.mustCast(ValueDynamic.class, cv.value().optUnwrap());
        return new CodeValue(cv.code(), EvalOutcome.ofValue(new ValueDynamic(vd.source(), type)));
    }

    public static boolean dynamicHasCode(CodeValue cv) {
        if (!cv.code().codeIsNone()) return true;
        return cv.value().optIsSome() && cv.value().optUnwrap() instanceof ValueDynamic;
    }

    public static Type substituteBoxed(ExprContext xx, Type type) {
        return switch (GenAspectDynamicBoxed.getMaybe(xx, type)) {
            case Opt.Some<AspectDynamicBoxed> boxedSome -> boxedSome.value().aspectDynamicBoxedBoxedForm();
            case Opt.None<AspectDynamicBoxed> n -> type;
        };
    }

    public static CodeValue dynamicStackToValue(ExprContext xx, CodeDynamic value, Type type) {
        var dynValue = ValueDynamic.stack(xx, type);
        return new CodeValue(Code.target(value), EvalOutcome.ofValue(dynValue));
    }

    public static EResult<CodeDynamic> wrapWithCarry(QExprId loc, Code carry, CodeDynamic dynamicCode) {
        var carryDynCast = CodeDynamic.castOpt(loc, carry);
        if (carryDynCast.hasErrors()) return carryDynCast.reerr();
        if (carryDynCast.requireValue("carryDyn").optIsNone()) {
            return EResult.ok(dynamicCode);
        }
        var carryDyn = carryDynCast.requireValue("carryDyn").optUnwrap();
        var seq = new JavaBytecodeSequence();
        if (carryDyn.code() instanceof JavaBytecodeSequence carrySeq) {
            for (var child : carrySeq.children) seq.add(child);
        } else {
            seq.add(carryDyn);
        }
        seq.add(dynamicCode);
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    public static EResult<CodeValue> dynamicSet(ExprContext xx, QExprId loc, CodeValue self, CodeValue value) {
        if (self.value().optIsNone()) {
            throw new IllegalStateException(
                    "dynamicSet called with absent value in self_; Rust unwraps unconditionally");
        }
        if (!(self.value().optUnwrap() instanceof ValueDynamic selfDyn)) {
            throw new IllegalStateException("dynamicSet: self_ value is not a ValueDynamic (got "
                    + self.value().optUnwrap().getClass().getName()
                    + ")");
        }
        Type targetType = selfDyn.type();
        var convRes = Eval.typecheckConvert(xx, loc, targetType, value);
        if (convRes.hasErrors()) {
            return convRes.reerr();
        }
        var converted = convRes.requireValue("DynUtil::dynamicSet typecheckConvert");
        if (converted.value().optIsNone()) {
            return EResult.err(loc, "dynamicSet: converted value has no compile-time value");
        }
        Type valueType = converted.value().optUnwrap().valueType();
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
        var nullStackRes = GenAspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
        if (nullStackRes.hasErrors()) {
            return nullStackRes.reerr();
        }
        var nullStack = nullStackRes.requireValue("DynUtil::dynamicSet null stack aspect");
        var storeRes = selfDyn.source().valueDynamicSourceStore(loc, valueCode);
        if (storeRes.hasErrors()) return storeRes.reerr();
        var seq = new JavaBytecodeSequence();
        seq.add(storeRes.requireValue("DynUtil::dynamicSet store"));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
        return EResult.ok(nullStack.aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    public static boolean zeroArgSignature(ExprContext xx, Type argType) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsSome()) {
            return tupleOpt.optUnwrap().aspectTupleElementTypes().isEmpty();
        }
        return false;
    }

    public static EResult<TList<CodeDynamic>> destructurePositional(
            ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsSome()) {
            if (converted.value().optIsNone()) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var elementTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var elemCvs = tupleOpt.optUnwrap()
                    .aspectTupleTakeElements(xx, loc, converted.value().optUnwrap());
            if (elementTypes.size() != elemCvs.size()) {
                throw new IllegalStateException(
                        "DynUtil::destructurePositional: elementTypes/takeElements size mismatch");
            }
            return destructureFromElements(xx, loc, elementTypes, elemCvs);
        }
        var recordOpt = GenAspectRecord.getMaybe(xx, argType);
        if (recordOpt.optIsSome()) {
            if (converted.value().optIsNone()) {
                return EResult.err(loc, "DynUtil::destructurePositional: arg has no compile-time value");
            }
            var fieldTypes = recordOpt.optUnwrap().aspectRecordFieldTypes();
            var fieldCvs = recordOpt
                    .optUnwrap()
                    .aspectRecordTakeFields(xx, loc, converted.value().optUnwrap());
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
                    return EResult.err(
                            loc,
                            "DynUtil::destructurePositional: record missing field '"
                                    + entry.getKey().constKeyToCanonical() + "'");
                }
                elemCvs.add(cvRes);
            }
            return destructureFromElements(xx, loc, elemTypes, elemCvs);
        }
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDyn = consumeRes
                .requireValue("DynUtil::destructurePositional arg consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (argDyn.hasErrors()) return argDyn.reerr();
        return EResult.ok(TList.of(argDyn.requireValue("DynUtil::destructurePositional arg")));
    }

    public record MethodRefArg(CodeValue receiver, CodeValue methodArgs) {}

    public static EResult<MethodRefArg> methodRefSplitArgument(
            ExprContext xx, QExprId loc, Type argType, CodeValue argument, String desc) {
        if (zeroArgSignature(xx, argType)) {
            return EResult.ok(new MethodRefArg(
                    argument,
                    new CodeValue(
                            Code.none(loc), EvalOutcome.ofValue(new Tuple(Opt.none(), new TList<TupleElement>())))));
        }
        if (argument.value().optIsNone()) {
            return EResult.err(loc, desc + ": argument has no compile-time value");
        }
        var argVal = argument.value().optUnwrap();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argVal.valueType());
        if (tupleOpt.optIsNone()) {
            return EResult.err(loc, desc + ": expected a (receiver, args) 2-tuple argument");
        }
        var elemCvs = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argVal);
        if (elemCvs.size() != 2) {
            return EResult.err(
                    loc, desc + ": expected a (receiver, args) 2-tuple argument, got " + elemCvs.size() + " elements");
        }
        if (elemCvs.get(0).hasErrors()) return elemCvs.get(0).reerr();
        if (elemCvs.get(1).hasErrors()) return elemCvs.get(1).reerr();
        return EResult.ok(new MethodRefArg(
                elemCvs.get(0).requireValue("DynUtil::methodRefSplitArgument receiver"),
                elemCvs.get(1).requireValue("DynUtil::methodRefSplitArgument args")));
    }

    private static EResult<TList<CodeDynamic>> destructureFromElements(
            ExprContext xx, QExprId loc, TList<Type> elementTypes, TList<EResult<CodeValue>> elemCvs) {
        var out = new TList<CodeDynamic>(elementTypes.size());
        for (int i = 0; i < elementTypes.size(); i++) {
            var elemCvRes = elemCvs.get(i);
            if (elemCvRes.hasErrors()) return elemCvRes.reerr();
            var elemCv = elemCvRes.requireValue("DynUtil::destructurePositional element");
            var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementTypes.get(i));
            if (consumeRes.hasErrors()) return consumeRes.reerr();
            var elemDyn = consumeRes
                    .requireValue("DynUtil::destructurePositional consume")
                    .aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (elemDyn.hasErrors()) return elemDyn.reerr();
            out.add(elemDyn.requireValue("DynUtil::destructurePositional elem toDynamic"));
        }
        return EResult.ok(out);
    }
}
