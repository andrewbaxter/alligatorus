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
import compilerxyz.eval.BindingId.Local;
import compilerxyz.gen.AspectDynamicBoxed;
import compilerxyz.gen.AspectRecord;
import compilerxyz.gen.AspectTuple;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public final class DynUtil {
    private DynUtil() {
    }

    public record ArgSignatureSlot(Type elementType, JavaDataDescriptor jvmDesc) {}

    public static EResult<TList<ArgSignatureSlot>> argSignatureSlots(
            ExprContext xx, QExprId loc, Type argType) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsSome()) {
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var out = new TList<ArgSignatureSlot>(elemTypes.size());
            for (var et : elemTypes) {
                var descRes = jvmDescFor(xx, loc, et);
                if (descRes.hasErrors()) return descRes.reerr();
                out.add(new ArgSignatureSlot(et,
                        descRes.requireValue("DynUtil::argSignatureSlots tuple element")));
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
            Local elementSlot,
            int elementStoreOp,
            Code body) {
        var loopLabel = new LabelNode();
        var doneLabel = new LabelNode();
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
                                new JumpInsnNode(
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
        var bodyDyn = body.codeDynamic();
        if (bodyDyn != null) {
            seq.add(bodyDyn);
        }
        seq.add(
                new CodeDynamic(
                        loc,
                        new JavaBytecodeInstructionObj(
                                new JumpInsnNode(
                                        org.objectweb.asm.Opcodes.GOTO, loopLabel))));
        seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.POP));
        return new CodeValue(
            Code.of(loc, new CodeDynamic(loc, seq)),
            EvalOutcome.NO_VALUE);
    }

    public static CodeDynamic renderSource(
            ExprContext xx, QExprId loc, ValueDynamicSource src) {
        return switch (src) {
            case ValueDynamicSource.Expr e -> throw new IllegalStateException(
                "ValueDynamicSource.Expr has no reconstructable code");
            case ValueDynamicSource.Binding b -> b.valueDynamicSourceRealize(xx, loc);
            case ValueDynamicSource.Field f -> f.valueDynamicSourceRealize(xx, loc);
        };
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(
            ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (valueOpt.optIsNone()) {
            throw new IllegalStateException(
                    "bind_via_target called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = valueOpt.optUnwrap().valueType();
        if (cv.code().codeIsNone()) {
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
            return EResult.err(
                    loc, "Type (" + vtype.typeDesc() + ") returned None from AspectBindDynamic");
        }
        var bound = xx.target.targetBind(xx, loc, subOpt.optUnwrap());
        Code wrappingCode = Code.of(loc, bound.code());
        Binding binding = new BindingDynamicBinding(bound.id(), vtype);
        return EResult.ok(new AspectBind.BindResult(wrappingCode, binding));
    }

    public static EResult<Opt<TargetBindSub>> bindDynamicDefault(
            ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.code().codeIsNone()) {
            return EResult.ok(Opt.none());
        }
        if (cv.value().optIsNone()) {
            throw new IllegalStateException(
                    "bind_dynamic_default called with absent value in CodeValue; Rust unwraps unconditionally");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var dynRes = dynamicConsumeToDynamic(xx, loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault dynamicConsumeToDynamic");
        return EResult.ok(Opt.some(new TargetBindSub(dyn, vtype)));
    }

    public record DropChild(Type childType, CodeDynamic access) {}

    public static EResult<Code> dropDefault(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.ok(Code.none(loc));
        }
        if (!(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        return EResult.ok(vd.source().valueDynamicSourceDrop(loc));
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
        var dyn = cv.code().codeDynamic();
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
        return switch (GenAspectDynamicBoxed.getMaybe(xx, type)) {
            case Opt.Some<AspectDynamicBoxed> boxedSome ->
                boxedSome.value().aspectDynamicBoxedBoxedForm();
            case Opt.None<AspectDynamicBoxed> n -> type;
        };
    }

    public static CodeValue dynamicStackToValue(
            ExprContext xx, QExprId loc, CodeDynamic value, Type type_, int stackSize) {
        var dynValue = ValueDynamic.stack(type_, stackSize);
        return new CodeValue(Code.of(loc, value), EvalOutcome.ofValue(dynValue));
    }

    public static CodeDynamic wrapWithCarry(QExprId loc, Code carry, CodeDynamic dynamicCode) {
        var carryDyn = carry.codeDynamic();
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
        if (self_.value().optIsNone()) {
            throw new IllegalStateException(
                    "dynamicSet called with absent value in self_; Rust unwraps unconditionally");
        }
        if (!(self_.value().optUnwrap() instanceof ValueDynamic selfDyn)) {
            throw new IllegalStateException(
                    "dynamicSet: self_ value is not a ValueDynamic (got "
                            + self_.value().optUnwrap().getClass().getName()
                            + ")");
        }
        Type targetType = selfDyn.type_();
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
        var selfCode = self_.code().codeDynamic();
        var nullStackRes = GenAspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
        if (nullStackRes.hasErrors()) {
            return nullStackRes.reerr();
        }
        var nullStack = nullStackRes.requireValue("DynUtil::dynamicSet null stack aspect");
        return switch (selfDyn.source()) {
          case ValueDynamicSource.Binding bindingSrc -> {
            var seq = new JavaBytecodeSequence();
            switch (bindingSrc.key()) {
              case BindingId.Local local -> {
                seq.add(valueCode);
                seq.add(
                    new CodeDynamic(
                        new CodeLoc.Some(loc),
                        new JavaBytecodeStoreLoad(
                            JvmSort.storeOpcode(xx, targetType), local.key())));
              }
            }
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, seq);
            yield EResult.ok(nullStack.aspectStackValueDynamicToValue(xx, loc, setJs));
          }
          case ValueDynamicSource.Field fieldSrc -> {
            if (isStackRooted(fieldSrc)) {
                yield EResult.err(loc, "Cannot modify a temporary value");
            }
            var seq = new JavaBytecodeSequence();
            seq.add(fieldSrc.base().valueDynamicSourceRealize(xx, loc));
            seq.add(valueCode);
            seq.add(
                JavaBytecodeUtils.setField(
                    new CodeLoc.Some(loc), fieldSrc.klass(), fieldSrc.slotName(), fieldSrc.slotDesc()));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
            var setJs = new CodeDynamic(loc, seq);
            yield EResult.ok(nullStack.aspectStackValueDynamicToValue(xx, loc, setJs));
          }
          case ValueDynamicSource.Expr e -> EResult.err(loc, "Cannot modify a temporary value");
        };
    }

    /**
     * If {@code argType} is a tuple, destructure the compile-time value into per-element
     * CodeDynamic pushes (each element lowered via its own AspectConsumeDynamic). Otherwise
     * lowers the whole value via {@code argType}'s AspectConsumeDynamic and returns a
     * singleton list. Used at call sites whose JVM descriptor takes destructured positional args.
     */
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
            var elemCvs = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, converted.value().optUnwrap());
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
            var fieldCvs = recordOpt.optUnwrap().aspectRecordTakeFields(xx, loc, converted.value().optUnwrap());
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
                                    + entry.getKey().constKeyToCanonical() + "'");
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

}
