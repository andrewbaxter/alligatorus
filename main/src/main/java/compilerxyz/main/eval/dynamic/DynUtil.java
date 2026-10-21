package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.BindingSlotDynamic;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;

public final class DynUtil {
    private DynUtil() {}

    public record ArgSignatureSlot(Type elementType, JavaDataDescriptor jvmDesc) {}

    public static EResult<Binding> initialBind(ExprContext xx, QExprId loc, Type type, BindingId id) {
        var aspectRes = GenAspectInitialBind.get(xx, loc, type);
        if (aspectRes.hasErrors()) return aspectRes.reerr();
        return aspectRes
                .requireValue("DynUtil.initialBind aspect")
                .aspectInitialBindInitialBind(xx, loc, new BindingSlotDynamic(id));
    }

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

    public interface ElementDecode {
        JavaBytecodeReg elementDecode(JavaBytecodeBuilder b, JavaBytecodeReg raw);
    }

    public static CodeDynamicReg iteratorNext(QExprId loc, BindingId iteratorSlot, ElementDecode decode) {
        var b = new JavaBytecodeBuilder(loc);
        var it = b.load(iteratorSlot, JavaDataDescriptor.fromClass(Iterator.class));
        var raw = b.invokeInterface(
                JvmConstants.INTNAME_ITERATOR,
                "next",
                JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of()),
                it);
        return b.code(decode.elementDecode(b, raw));
    }

    public static EResult<CodeValue> foreachSkeleton(
            QExprId loc, CodeDynamicReg iteratorSetup, BindingId iteratorSlot, CodeDynamic elementBind, Code body) {
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        var b = new JavaBytecodeBuilder(loc);
        b.add(iteratorSetup.code());
        b.store(iteratorSlot, iteratorSetup.reg());
        b.land(loopKey);
        var it = b.load(iteratorSlot, JavaDataDescriptor.fromClass(Iterator.class));
        var hasNext = b.invokeInterface(
                JvmConstants.INTNAME_ITERATOR,
                "hasNext",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of()),
                it);
        b.branch(Opcodes.IFEQ, doneKey, hasNext);
        b.add(elementBind);
        var bodyRes = b.addCode(loc, body);
        if (bodyRes.optIsSome()) return EResult.errs(bodyRes.optUnwrap());
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    public static Opt<JavaBytecodeReg> invokeStaticFunction(
            ExprContext xx,
            QExprId loc,
            JavaBytecodeBuilder b,
            JavaInternalName klass,
            String methodName,
            JavaMethodDescriptor desc,
            boolean needsEvalContext,
            TList<CodeDynamicReg> args) {
        var regs = new TList<JavaBytecodeReg>();
        if (needsEvalContext) {
            regs.add(b.reg(EvalRootEmitter.loadEvalContext(loc, xx)));
        }
        for (var arg : args) regs.add(b.reg(arg));
        return b.invoke(Opcodes.INVOKESTATIC, klass, methodName, desc, false, regs);
    }

    public static EResult<TargetDynamic> jvmTarget(ExprContext xx, QExprId loc) {
        if (xx.target instanceof TargetDynamic t) return EResult.ok(t);
        return EResult.err(loc, "Value with a JVM representation is being bound under a non-JVM target");
    }

    public static EResult<AspectBind.BindResult> bindViaTarget(ExprContext xx, QExprId loc, CodeValue cv) {
        var valueOpt = cv.value();
        if (valueOpt.optIsNone()) {
            throw new IllegalStateException("bind_via_target called with absent value in CodeValue");
        }
        var vtype = valueOpt.optUnwrap().valueType();
        if (sourceOf(valueOpt.optUnwrap()).optIsNone()) {
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
        var bound = targetRes.requireValue("DynUtil::bindViaTarget jvm target").bindSlot(loc, subOpt.optUnwrap());
        Code wrappingCode = Code.target(bound.code);
        Binding binding = new BindingDynamicBinding(bound.id, vtype);
        return EResult.ok(new AspectBind.BindResult(wrappingCode, binding));
    }

    public static EResult<BindSubDynamic> bindSubWithHeldCodes(
            ExprContext xx, QExprId loc, BindSubDynamic sub, TList<Code> codes, boolean before) {
        if (codes.isEmpty()) return EResult.ok(sub);
        var parts = new TList<Code>();
        if (!before) parts.add(Code.target(sub.value().code()));
        parts.addAll(codes);
        if (before) parts.add(Code.target(sub.value().code()));
        codes.clear();
        var seqRes = xx.target.targetSeq(loc, parts);
        if (seqRes.hasErrors()) return seqRes.reerr();
        var dynRes = CodeDynamic.castRequired(
                loc, seqRes.requireValue("bindSubWithHeldCodes seq"), "bindSubWithHeldCodes: codes are not dynamic");
        if (dynRes.hasErrors()) return dynRes.reerr();
        return EResult.ok(new BindSubDynamic(
                new CodeDynamicReg(
                        dynRes.requireValue("bindSubWithHeldCodes dyn"),
                        sub.value().reg()),
                sub.type()));
    }

    public static EResult<Opt<BindSubDynamic>> bindDynamicDefault(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("bind_dynamic_default called with absent value in CodeValue");
        }
        if (sourceOf(cv.value().optUnwrap()).optIsNone()) {
            return EResult.ok(Opt.none());
        }
        var vtype = cv.value().optUnwrap().valueType();
        var dynRes = valueReg(xx, loc, cv);
        if (dynRes.hasErrors()) {
            return dynRes.reerr();
        }
        var dyn = dynRes.requireValue("DynUtil::bindDynamicDefault valueReg");
        return EResult.ok(Opt.some(new BindSubDynamic(dyn, vtype)));
    }

    public static Opt<ValueDynamicSource> sourceOf(Value value) {
        return switch (value) {
            case ValueDynamic vd -> Opt.some(vd.source());
            default -> Opt.none();
        };
    }

    public static JavaBytecodeReg sourceReg(JavaBytecodeBuilder b, ValueDynamicSource source, JavaDataDescriptor desc) {
        return switch (source) {
            case ValueDynamicSource.Reg r -> r.reg();
            case ValueDynamicSource.Slot slot -> b.load(slot.slot(), desc);
            case ValueDynamicSource.Field field ->
                b.getField(
                        field.klass(),
                        field.field(),
                        field.desc(),
                        sourceReg(b, field.base(), JavaDataDescriptor.fromInternalName(field.klass())));
        };
    }

    public static JavaBytecodeReg sourceReg(ExprContext xx, JavaBytecodeBuilder b, ValueDynamic vd) {
        return sourceReg(b, vd.source(), JvmSort.descriptor(xx, vd.type()));
    }

    public static boolean sourceIsPlace(ValueDynamicSource source) {
        return switch (source) {
            case ValueDynamicSource.Reg r -> false;
            case ValueDynamicSource.Slot slot -> true;
            case ValueDynamicSource.Field field -> sourceIsPlace(field.base());
        };
    }

    public static EResult<CodeDynamicReg> valueReg(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("DynUtil::valueReg: CodeValue has no value");
        }
        var value = cv.value().optUnwrap();
        if (!(value instanceof ValueDynamic vd)) {
            throw new IllegalStateException("DynUtil::valueReg: value has no JVM source: "
                    + value.getClass().getName());
        }
        var b = new JavaBytecodeBuilder(loc);
        var codeRes = b.addCode(loc, cv.code());
        if (codeRes.optIsSome()) return EResult.errs(codeRes.optUnwrap());
        return EResult.ok(b.code(sourceReg(xx, b, vd)));
    }

    public static EResult<CodeDynamicReg> materialize(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "expression has no value to materialize");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, vtype);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var consumed =
                consumeRes.requireValue("DynUtil::materialize consume").aspectConsumeDynamicToDynamic(xx, loc, cv);
        if (consumed.hasErrors()) return consumed.reerr();
        return valueReg(xx, loc, consumed.requireValue("DynUtil::materialize consumed"));
    }

    public static EResult<CodeDynamicReg> materialize(ExprContext xx, QExprId loc, Type as, CodeValue cv) {
        var converted = Eval.typecheckConvert(xx, loc, as, cv);
        if (converted.hasErrors()) return converted.reerr();
        return materialize(xx, loc, converted.requireValue("DynUtil::materialize convert"));
    }

    public static CodeValue methodRefOn(CodeValue receiver, Type methodType) {
        var source = sourceOf(receiver.value().optUnwrap());
        if (source.optIsNone()) {
            throw new IllegalStateException("DynUtil::methodRefOn: receiver has no JVM source");
        }
        return new CodeValue(receiver.code(), EvalOutcome.ofValue(new ValueDynamic(source.optUnwrap(), methodType)));
    }

    public static EResult<CodeValue> consumeThenRestamp(ExprContext xx, QExprId loc, CodeValue cv, Type type) {
        if (cv.value().optIsSome() && sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(dynamicCastKeepingSource(cv, type));
        }
        var consumedRes = materialize(xx, loc, cv);
        if (consumedRes.hasErrors()) return consumedRes.reerr();
        var consumed = consumedRes.requireValue("DynUtil::consumeThenRestamp consumed");
        return EResult.ok(regToValue(consumed.code(), consumed.reg(), type));
    }

    public static CodeValue dynamicCastKeepingSource(CodeValue cv, Type type) {
        var source = sourceOf(cv.value().optUnwrap());
        if (source.optIsNone()) {
            throw new IllegalStateException("DynUtil::dynamicCastKeepingSource: value has no JVM source");
        }
        return new CodeValue(cv.code(), EvalOutcome.ofValue(new ValueDynamic(source.optUnwrap(), type)));
    }

    public static boolean dynamicHasCode(CodeValue cv) {
        if (!cv.code().codeIsNone()) return true;
        return cv.value().optIsSome() && cv.value().optUnwrap() instanceof ValueDynamic;
    }

    public static EResult<CodeValue> typeConstructRuntime(
            ExprContext xx, QExprId loc, CodeValue self, CodeValue argument, JavaStaticMethodRef construct) {
        var argRes = materialize(xx, loc, argument);
        if (argRes.hasErrors()) return argRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, self.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var element = b.reg(argRes.requireValue("DynUtil::typeConstructRuntime element"));
        var xxReg = b.getStatic(
                TargetDynamic.evalRootOf(xx), EvalRootEmitter.EXPR_CONTEXT_FIELD, EvalRootEmitter.DESC_EXPR_CONTEXT);
        var out = b.invokeStatic(construct, xxReg, element);
        return EResult.ok(regToValue(b.code(), out, GenType.dynamicType()));
    }

    public static boolean valueIsRuntime(Value value) {
        return switch (value) {
            case ValueDynamic vd -> true;
            case Record r -> {
                for (var entry : r.fields()) {
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> {}
                        case RecordField.EvalRes er -> {
                            if (codeValueIsRuntime(er.result())) yield true;
                        }
                    }
                }
                yield false;
            }
            case Tuple t -> {
                for (var element : t.elements()) {
                    switch (element) {
                        case TupleElement.Dynamic d -> {}
                        case TupleElement.EvalRes er -> {
                            if (codeValueIsRuntime(er.result())) yield true;
                        }
                    }
                }
                yield false;
            }
            default -> false;
        };
    }

    private static boolean codeValueIsRuntime(EResult<CodeValue> result) {
        var cvOpt = result.valueOpt();
        if (cvOpt.optIsNone()) return false;
        var valueOpt = cvOpt.optUnwrap().value();
        return valueOpt.optIsSome() && valueIsRuntime(valueOpt.optUnwrap());
    }

    public static EResult<Type> requireBoxedTypeArgument(ExprContext xx, QExprId loc, String what, Type type) {
        if (GenAspectDynamicBoxed.getMaybe(xx, type).optIsSome()) {
            return EResult.err(
                    loc,
                    what + " cannot be declared with the unboxed primitive " + type.typeDesc()
                            + "; use its boxed type (e.g. types.int_box)");
        }
        return EResult.ok(type);
    }

    public static Type substituteBoxed(ExprContext xx, Type type) {
        return switch (GenAspectDynamicBoxed.getMaybe(xx, type)) {
            case Opt.Some<AspectDynamicBoxed> boxedSome -> boxedSome.value().aspectDynamicBoxedBoxedForm();
            case Opt.None<AspectDynamicBoxed> n -> type;
        };
    }

    public static CodeValue regToValue(CodeDynamic code, JavaBytecodeReg reg, Type type) {
        return new CodeValue(
                Code.target(code), EvalOutcome.ofValue(new ValueDynamic(new ValueDynamicSource.Reg(reg), type)));
    }

    public static CodeValue regToValue(CodeDynamicReg value, Type type) {
        return regToValue(value.code(), value.reg(), type);
    }

    public static EResult<CodeValue> dynamicSet(ExprContext xx, QExprId loc, CodeValue self, CodeValue value) {
        if (self.value().optIsNone()) {
            throw new IllegalStateException("dynamicSet called with absent value in self_");
        }
        if (!(self.value().optUnwrap() instanceof ValueDynamic selfDyn)) {
            throw new IllegalStateException("dynamicSet: self_ value is not a ValueDynamic (got "
                    + self.value().optUnwrap().getClass().getName()
                    + ")");
        }
        var valueRes = materialize(xx, loc, selfDyn.type(), value);
        if (valueRes.hasErrors()) return valueRes.reerr();
        var valueDyn = valueRes.requireValue("DynUtil::dynamicSet value");
        var b = new JavaBytecodeBuilder(loc);
        var selfCodeRes = b.addCode(loc, self.code());
        if (selfCodeRes.optIsSome()) return EResult.errs(selfCodeRes.optUnwrap());
        switch (selfDyn.source()) {
            case ValueDynamicSource.Reg r -> {
                return EResult.err(loc, "Cannot modify a temporary value");
            }
            case ValueDynamicSource.Slot slot -> {
                var newValue = b.reg(valueDyn);
                b.store(slot.slot(), newValue);
            }
            case ValueDynamicSource.Field field -> {
                if (!sourceIsPlace(field)) {
                    return EResult.err(loc, "Cannot modify a temporary value");
                }
                var base = sourceReg(b, field.base(), JavaDataDescriptor.fromInternalName(field.klass()));
                var newValue = b.reg(valueDyn);
                b.putField(field.klass(), field.field(), field.desc(), base, newValue);
            }
        }
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    public static boolean zeroArgSignature(ExprContext xx, Type argType) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (tupleOpt.optIsSome()) {
            return tupleOpt.optUnwrap().aspectTupleElementTypes().isEmpty();
        }
        return false;
    }

    public record Positional(Code carry, TList<CodeDynamicReg> args) {}

    public static EResult<Positional> destructurePositional(
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
            var tupleArgs = destructureFromElements(xx, loc, elementTypes, elemCvs);
            if (tupleArgs.hasErrors()) return tupleArgs.reerr();
            return EResult.ok(new Positional(converted.code(), tupleArgs.requireValue("tuple args")));
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
                var cvResOpt = fieldCvs.getOpt(entry.getKey());
                if (cvResOpt.optIsNone()) {
                    return EResult.err(
                            loc,
                            "DynUtil::destructurePositional: record missing field '"
                                    + entry.getKey().constKeyToCanonical() + "'");
                }
                var cvRes = cvResOpt.optUnwrap();
                elemCvs.add(cvRes);
            }
            var recordArgs = destructureFromElements(xx, loc, elemTypes, elemCvs);
            if (recordArgs.hasErrors()) return recordArgs.reerr();
            return EResult.ok(new Positional(converted.code(), recordArgs.requireValue("record args")));
        }
        var argDyn = materialize(xx, loc, converted);
        if (argDyn.hasErrors()) return argDyn.reerr();
        return EResult.ok(
                new Positional(Code.none(loc), TList.of(argDyn.requireValue("DynUtil::destructurePositional arg"))));
    }

    public record MethodRefArg(CodeValue receiver, CodeValue methodArgs) {}

    public static EResult<MethodRefArg> methodRefSplitArgument(
            ExprContext xx, QExprId loc, Type argType, CodeValue argument, String desc) {
        if (zeroArgSignature(xx, argType)) {
            return EResult.ok(new MethodRefArg(
                    argument, new CodeValue(Code.none(loc), EvalOutcome.ofValue(new Tuple(IList.of())))));
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
        var receiver = elemCvs.get(0).requireValue("DynUtil::methodRefSplitArgument receiver");
        var receiverCode = Eval.codeConcat(xx, loc, argument.code(), receiver.code());
        if (receiverCode.hasErrors()) return receiverCode.reerr();
        return EResult.ok(new MethodRefArg(
                new CodeValue(
                        receiverCode.requireValue("DynUtil::methodRefSplitArgument receiver code"), receiver.outcome()),
                elemCvs.get(1).requireValue("DynUtil::methodRefSplitArgument args")));
    }

    private static EResult<TList<CodeDynamicReg>> destructureFromElements(
            ExprContext xx, QExprId loc, TList<Type> elementTypes, TList<EResult<CodeValue>> elemCvs) {
        var out = new TList<CodeDynamicReg>(elementTypes.size());
        for (int i = 0; i < elementTypes.size(); i++) {
            var elemCvRes = elemCvs.get(i);
            if (elemCvRes.hasErrors()) return elemCvRes.reerr();
            var elemCv = elemCvRes.requireValue("DynUtil::destructurePositional element");
            var elemDyn = materialize(xx, loc, elementTypes.get(i), elemCv);
            if (elemDyn.hasErrors()) return elemDyn.reerr();
            out.add(elemDyn.requireValue("DynUtil::destructurePositional elem"));
        }
        return EResult.ok(out);
    }
}
