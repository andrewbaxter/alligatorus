package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.expr.TypeExprStagedPlaceholder;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectDynamicBoxed;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDynamicBoxed;
import compilerxyz.main.gen.GenAspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.IfBranch;
import compilerxyz.main.gen.Target;
import compilerxyz.main.gen.TargetCase;
import compilerxyz.main.gen.TargetMatchFallback;
import compilerxyz.main.gen.TryCatchClause;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Comparator;
import org.objectweb.asm.Opcodes;

public final class TargetDynamic implements Target {
    private static final JavaInternalName INTNAME_DYN_EXCEPTION = JavaInternalName.fromClass(DynException.class);
    private static final JavaDataDescriptor DESC_THROWABLE = JavaDataDescriptor.fromClass(Throwable.class);

    public TargetDynamic() {}

    @Override
    public EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.ok(Code.none(loc));
        }
        var value = cv.value().optUnwrap();
        var dropAspect = GenAspectDropDynamic.getMaybe(xx, value.valueType());
        if (dropAspect.optIsSome()) {
            return dropAspect.optUnwrap().aspectDropDynamicDrop(xx, loc, cv);
        }
        return EResult.ok(Code.none(loc));
    }

    @Override
    public CodeValue targetReturn(ExprContext xx, QExprId loc, CodeValue cv) {
        return cv;
    }

    @Override
    public EResult<Binding> targetInitialBind(ExprContext xx, QExprId loc, Type type) {
        return DynUtil.initialBind(xx, loc, type, new BindingId());
    }

    @Override
    public boolean targetValueIsRuntime(ExprContext xx, Value value) {
        return DynUtil.sourceOf(value).optIsSome();
    }

    @Override
    public EResult<CodeValue> targetSourceValue(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) return EResult.ok(cv);
        return switch (cv.value().optUnwrap()) {
            case Record record -> {
                var b = new JavaBytecodeBuilder(loc);
                var carryRes = b.addCode(loc, cv.code());
                if (carryRes.optIsSome()) yield EResult.errs(carryRes.optUnwrap());
                var fields = new TOrderedMap<ConstKey, RecordField>();
                var dynamicTypes = new TOrderedMap<ConstKey, Type>();
                var regs = new TMap<ConstKey, JavaBytecodeReg>();
                for (var entry : record.fields()) {
                    var held =
                            switch (entry.getValue()) {
                                case RecordField.Dynamic d ->
                                    throw new IllegalStateException(
                                            "targetSourceValue: record value has a dynamic-only field");
                                case RecordField.EvalRes er -> er;
                            };
                    if (held.result().hasErrors()) yield held.result().reerr();
                    var partRes = sourcePart(xx, loc, b, held.result().requireValue("targetSourceValue field"));
                    if (partRes.hasErrors()) yield partRes.reerr();
                    var part = partRes.requireValue("targetSourceValue field part");
                    if (part.reg().optIsSome()) {
                        var type = part.cv().value().optUnwrap().valueType();
                        dynamicTypes.put(entry.getKey(), type);
                        regs.put(entry.getKey(), part.reg().optUnwrap());
                        fields.put(entry.getKey(), new RecordField.Dynamic(type));
                    } else {
                        fields.put(entry.getKey(), new RecordField.EvalRes(EResult.ok(part.cv())));
                    }
                }
                if (dynamicTypes.isEmpty()) yield EResult.ok(cv);
                var slotInfo = CompositeSlotSort.sort(xx, dynamicTypes, ConstKey.CANONICAL_ORDER);
                var ctorRegs = new JavaBytecodeReg[slotInfo.sortedTypes().size()];
                for (var entry : regs.entrySet()) {
                    ctorRegs[slotInfo.slotOfKey().getOpt(entry.getKey()).optUnwrap()] = entry.getValue();
                }
                var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
                var obj = b.newInstance(
                        klass, CompositeClassEmitter.constructorDesc(xx, slotInfo.sortedTypes()), ctorRegs);
                Value result = new ValueDynamic(new ValueDynamicSource.Reg(obj), new Record(IOrderedMap.from(fields)));
                yield EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.ofValue(result)));
            }
            case Tuple tuple -> {
                var b = new JavaBytecodeBuilder(loc);
                var carryRes = b.addCode(loc, cv.code());
                if (carryRes.optIsSome()) yield EResult.errs(carryRes.optUnwrap());
                var elements = new TList<TupleElement>(tuple.elements().size());
                var dynamicTypes = new TOrderedMap<Integer, Type>();
                var regs = new TMap<Integer, JavaBytecodeReg>();
                for (int i = 0; i < tuple.elements().size(); i++) {
                    var held =
                            switch (tuple.elements().get(i)) {
                                case TupleElement.Dynamic d ->
                                    throw new IllegalStateException(
                                            "targetSourceValue: tuple value has a dynamic-only element");
                                case TupleElement.EvalRes er -> er;
                            };
                    if (held.result().hasErrors()) yield held.result().reerr();
                    var partRes = sourcePart(xx, loc, b, held.result().requireValue("targetSourceValue element"));
                    if (partRes.hasErrors()) yield partRes.reerr();
                    var part = partRes.requireValue("targetSourceValue element part");
                    if (part.reg().optIsSome()) {
                        var type = part.cv().value().optUnwrap().valueType();
                        dynamicTypes.put(dynamicTypes.size(), type);
                        regs.put(regs.size(), part.reg().optUnwrap());
                        elements.add(new TupleElement.Dynamic(type));
                    } else {
                        elements.add(new TupleElement.EvalRes(EResult.ok(part.cv())));
                    }
                }
                if (dynamicTypes.isEmpty()) yield EResult.ok(cv);
                var slotInfo = CompositeSlotSort.sort(xx, dynamicTypes, Integer::compare);
                var ctorRegs = new JavaBytecodeReg[slotInfo.sortedTypes().size()];
                for (var entry : regs.entrySet()) {
                    ctorRegs[slotInfo.slotOfKey().getOpt(entry.getKey()).optUnwrap()] = entry.getValue();
                }
                var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
                var obj = b.newInstance(
                        klass, CompositeClassEmitter.constructorDesc(xx, slotInfo.sortedTypes()), ctorRegs);
                Value result = new ValueDynamic(new ValueDynamicSource.Reg(obj), new Tuple(IList.from(elements)));
                yield EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.ofValue(result)));
            }
            default -> EResult.ok(cv);
        };
    }

    private record SourcedPart(Opt<JavaBytecodeReg> reg, CodeValue cv) {}

    private EResult<SourcedPart> sourcePart(ExprContext xx, QExprId loc, JavaBytecodeBuilder b, CodeValue part) {
        var sourcedRes = targetSourceValue(xx, loc, part);
        if (sourcedRes.hasErrors()) return sourcedRes.reerr();
        var sourced = sourcedRes.requireValue("targetSourceValue part");
        if (sourced.value().optIsNone()
                || DynUtil.sourceOf(sourced.value().optUnwrap()).optIsNone()) {
            return EResult.ok(new SourcedPart(Opt.none(), sourced));
        }
        var regRes = DynUtil.valueReg(loc, sourced);
        if (regRes.hasErrors()) return regRes.reerr();
        return EResult.ok(new SourcedPart(Opt.some(b.reg(regRes.requireValue("targetSourceValue part reg"))), sourced));
    }

    @Override
    public EResult<ExitValue> targetExitValue(ExprContext xx, QExprId loc, CodeValue cv) {
        var sourcedRes = targetSourceValue(xx, loc, cv);
        if (sourcedRes.hasErrors()) return sourcedRes.reerr();
        var sourced = sourcedRes.requireValue("targetExitValue sourced");
        var carries = sourced.value().optIsSome()
                && DynUtil.sourceOf(sourced.value().optUnwrap()).optIsSome();
        return EResult.ok(new ExitValue(carries, sourced));
    }

    @Override
    public EResult<Code> targetExit(ExprContext xx, QExprId loc, JumpKey dest, Opt<Type> argument, CodeValue value) {
        var b = new JavaBytecodeBuilder(loc);
        if (argument.optIsSome()) {
            var valueRes = DynUtil.valueReg(loc, value);
            if (valueRes.hasErrors()) return valueRes.reerr();
            b.jump(dest, b.reg(valueRes.requireValue("targetExit value")));
        } else {
            var codeRes = b.addCode(loc, value.code());
            if (codeRes.optIsSome()) return EResult.errs(codeRes.optUnwrap());
            b.jump(dest);
        }
        return EResult.ok(Code.target(b.code()));
    }

    @Override
    public EResult<CodeValue> targetLand(ExprContext xx, QExprId loc, JumpKey dest, Opt<Type> argument) {
        var b = new JavaBytecodeBuilder(loc);
        if (argument.optIsNone()) {
            b.land(dest);
            return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
        }
        var type = argument.optUnwrap();
        var param = new JavaBytecodeReg(JvmSort.descriptor(xx, type));
        b.land(dest, param);
        return EResult.ok(DynUtil.regToValue(b.code(), param, type));
    }

    private static BindingId initialBindSlot(Binding binding) {
        if (!(binding instanceof BindingDynamicSlot slot)) {
            throw new IllegalStateException("binding was not made by targetInitialBind: " + binding.getClass());
        }
        return slot.bindingDynamicSlotKey();
    }

    public BindResultSlot bindSlot(QExprId loc, BindSubDynamic sub) {
        var key = new BindingId();
        var b = new JavaBytecodeBuilder(loc);
        b.add(sub.value().code());
        b.store(key, sub.value().reg());
        return new BindResultSlot(b.code(), key);
    }

    @Override
    public EResult<CodeValue> targetTryCatchFinally(
            ExprContext xx,
            QExprId loc,
            CodeValue body,
            Opt<TryCatchClause> catch_,
            Opt<JumpKey> catchHandler,
            Opt<Code> finallyNormal) {
        var b = new JavaBytecodeBuilder(loc);
        Opt<Type> resultType = body.value().optIsSome()
                ? Opt.some(body.value().optUnwrap().valueType())
                : catch_.optIsSome() && catch_.optUnwrap().body.value().optIsSome()
                        ? Opt.some(catch_.optUnwrap().body.value().optUnwrap().valueType())
                        : Opt.<Type>none();
        var joinKey = new JumpKey();
        boolean bodyEnds = body.controlEnded();
        var armRes = emitArm(xx, loc, b, body, resultType, joinKey, "try body");
        if (armRes.optIsSome()) return EResult.errs(armRes.optUnwrap());
        boolean allEnd = bodyEnds;
        if (catch_.optIsSome()) {
            var clause = catch_.optUnwrap();
            var handler = catchHandler.optUnwrap();
            var exc = new JavaBytecodeReg(DESC_THROWABLE);
            b.land(handler, exc);
            var isExc = b.instanceOf(INTNAME_DYN_EXCEPTION, exc);
            var isExcKey = new JumpKey();
            b.branch(Opcodes.IFNE, isExcKey, isExc);
            b.athrow(exc);
            b.land(isExcKey);
            var dynExc = b.checkcast(JavaDataDescriptor.fromInternalName(INTNAME_DYN_EXCEPTION), exc);
            var payload = b.getField(INTNAME_DYN_EXCEPTION, "payload", JvmConstants.DESC_OBJECT, dynExc);
            var payloadConsumeRes = GenAspectConsumeDynamic.get(xx, loc, clause.excType);
            if (payloadConsumeRes.hasErrors()) return payloadConsumeRes.reerr();
            var payloadDesc = payloadConsumeRes
                    .requireValue("try/catch payload AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (payloadDesc.value().startsWith("L") && !payloadDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
                payload = b.checkcast(payloadDesc, payload);
            }
            b.store(initialBindSlot(clause.binding), payload);
            var catchArmRes = emitArm(xx, loc, b, clause.body, resultType, joinKey, "catch body");
            if (catchArmRes.optIsSome()) return EResult.errs(catchArmRes.optUnwrap());
            allEnd = allEnd && clause.body.controlEnded();
        }
        EvalOutcome outcome;
        Opt<JavaBytecodeReg> resultParam = Opt.none();
        if (allEnd) {
            outcome = EvalOutcome.CONTROL_ENDED;
        } else if (resultType.optIsSome()) {
            var param = new JavaBytecodeReg(JvmSort.descriptor(xx, resultType.optUnwrap()));
            b.land(joinKey, param);
            resultParam = Opt.some(param);
            outcome = EvalOutcome.NO_VALUE;
        } else {
            b.land(joinKey);
            outcome = EvalOutcome.NO_VALUE;
        }
        if (finallyNormal.optIsSome()) {
            var finCast = CodeDynamic.castOpt(loc, finallyNormal.optUnwrap());
            if (finCast.hasErrors()) return finCast.reerr();
            var finDyn = finCast.requireValue("finDyn");
            if (finDyn.optIsSome()) b.add(finDyn.optUnwrap());
        }
        CodeValue out;
        if (resultParam.optIsSome()) {
            out = DynUtil.regToValue(b.code(), resultParam.optUnwrap(), resultType.optUnwrap());
        } else {
            out = new CodeValue(Code.target(b.code()), outcome);
        }
        return EResult.ok(out);
    }

    private static Opt<TList<ExprErr>> emitArm(
            ExprContext xx,
            QExprId loc,
            JavaBytecodeBuilder b,
            CodeValue arm,
            Opt<Type> resultType,
            JumpKey joinKey,
            String what) {
        if (arm.controlEnded()) {
            return b.addCode(loc, arm.code());
        }
        if (resultType.optIsSome()) {
            if (arm.value().optIsNone()) {
                var codeRes = b.addCode(loc, arm.code());
                if (codeRes.optIsSome()) return codeRes;
                b.jump(joinKey, b.constDefault(JvmSort.descriptor(xx, resultType.optUnwrap())));
                return Opt.none();
            }
            var valueRes = DynUtil.materialize(xx, loc, resultType.optUnwrap(), arm);
            if (valueRes.hasErrors()) return valueRes.errorsOpt();
            b.jump(joinKey, b.reg(valueRes.requireValue(what + " value")));
            return Opt.none();
        }
        if (arm.value().optIsSome()) {
            return Opt.some(TList.of(ExprErr.opaque(loc, what + " yields a value while the other arm yields none")));
        }
        var codeRes = b.addCode(loc, arm.code());
        if (codeRes.optIsSome()) return codeRes;
        b.jump(joinKey);
        return Opt.none();
    }

    @Override
    public EResult<Code> targetSeq(QExprId loc, TList<Code> entries) {
        var seq = new JavaBytecodeSequence();
        boolean any = false;
        for (var entry : entries) {
            var dynCast = CodeDynamic.castOpt(loc, entry);
            if (dynCast.hasErrors()) return dynCast.reerr();
            if (dynCast.requireValue("dyn").optIsNone()) continue;
            var dyn = dynCast.requireValue("dyn").optUnwrap();
            seq.add(dyn);
            any = true;
        }
        if (!any) {
            return EResult.ok(Code.none(loc));
        }
        return EResult.ok(Code.target(new CodeDynamic(loc, seq)));
    }

    @Override
    public EResult<Code> targetJump(QExprId loc, JumpKey dest) {
        return EResult.ok(Code.target(new CodeDynamic(loc, new JavaBytecodeJump(dest))));
    }

    @Override
    public EResult<Code> targetGuarded(QExprId loc, JumpKey dest, Code inner) {
        var innerDynCast = CodeDynamic.castOpt(loc, inner);
        if (innerDynCast.hasErrors()) return innerDynCast.reerr();
        if (innerDynCast.requireValue("innerDyn").optIsNone()) {
            return EResult.ok(inner);
        }
        var innerDyn = innerDynCast.requireValue("innerDyn").optUnwrap();
        return EResult.ok(Code.target(new CodeDynamic(loc, new JavaBytecodeGuarded(dest, innerDyn))));
    }

    @Override
    public EResult<Code> targetExcCleanup(
            ExprContext xx, QExprId loc, JumpKey self, Code cleanup, Opt<JumpKey> parent) {
        var b = new JavaBytecodeBuilder(loc);
        var exc = new JavaBytecodeReg(DESC_THROWABLE);
        b.land(self, exc);
        var cleanupDynCast = CodeDynamic.castOpt(loc, cleanup);
        if (cleanupDynCast.hasErrors()) return cleanupDynCast.reerr();
        var cleanupDyn = cleanupDynCast.requireValue("cleanupDyn");
        if (cleanupDyn.optIsSome()) {
            b.add(cleanupDyn.optUnwrap());
        }
        if (parent.optIsSome()) {
            b.jump(parent.optUnwrap(), exc);
        } else {
            b.athrow(exc);
        }
        return EResult.ok(Code.target(b.code()));
    }

    @Override
    public EResult<Code> targetExitTrampoline(
            ExprContext xx, QExprId loc, JumpKey self, Opt<Type> argument, Code cleanup, JumpKey next) {
        var b = new JavaBytecodeBuilder(loc);
        Opt<JavaBytecodeReg> carry = Opt.none();
        if (argument.optIsSome()) {
            var param = new JavaBytecodeReg(JvmSort.descriptor(xx, argument.optUnwrap()));
            carry = Opt.some(param);
            b.land(self, param);
        } else {
            b.land(self);
        }
        var cleanupDynCast = CodeDynamic.castOpt(loc, cleanup);
        if (cleanupDynCast.hasErrors()) return cleanupDynCast.reerr();
        var cleanupDyn = cleanupDynCast.requireValue("cleanupDyn");
        if (cleanupDyn.optIsSome()) {
            b.add(cleanupDyn.optUnwrap());
        }
        if (carry.optIsSome()) {
            b.jump(next, carry.optUnwrap());
        } else {
            b.jump(next);
        }
        return EResult.ok(Code.target(b.code()));
    }

    private record ArmsShape(
            boolean anyValue, boolean allControlEnded, boolean carries, Opt<Value> structural, Opt<Type> resultType) {}

    private EResult<ArmsShape> armsShape(
            ExprContext xx, QExprId loc, TList<CodeValue> arms, boolean elseMissing, String what) {
        boolean anyValue = false;
        boolean anyNoValue = false;
        boolean allControlEnded = true;
        Type resultType = null;
        Opt<Value> structural = Opt.none();
        int sourcedCount = 0;
        int valuedCount = 0;
        for (var arm : arms) {
            switch (arm.outcome()) {
                case EvalOutcome.HasValue h -> {
                    anyValue = true;
                    allControlEnded = false;
                    valuedCount += 1;
                    var sourced = DynUtil.sourceOf(h.value()).optIsSome();
                    if (sourced) sourcedCount += 1;
                    if (resultType == null) {
                        resultType = h.value().valueType();
                        structural = Opt.some(h.value());
                    } else if (sourced && !resultType.typeIsEqual(xx, h.value().valueType())) {
                        return EResult.err(
                                loc,
                                what + ": branch values have different types after sourcing (" + resultType.typeDesc()
                                        + " vs " + h.value().valueType().typeDesc() + ")");
                    }
                }
                case EvalOutcome.NoValue n -> {
                    anyNoValue = true;
                    allControlEnded = false;
                }
                case EvalOutcome.ControlEnded c -> {}
            }
        }
        if (elseMissing) allControlEnded = false;
        if (elseMissing && anyValue) {
            return EResult.err(loc, what + ": branches yield a value but there is no else");
        }
        if (anyValue && anyNoValue) {
            return EResult.err(loc, what + ": some branches yield a value and some don't");
        }
        if (sourcedCount != 0 && sourcedCount != valuedCount) {
            return EResult.err(
                    loc,
                    what + ": some branches yield runtime values and some compile-time ones; cast the compile-time"
                            + " ones to a dynamic type");
        }
        return EResult.ok(new ArmsShape(
                anyValue,
                allControlEnded,
                sourcedCount != 0,
                structural,
                resultType == null ? Opt.<Type>none() : Opt.some(resultType)));
    }

    private static Opt<TList<ExprErr>> emitBranchBody(
            QExprId loc, JavaBytecodeBuilder b, CodeValue body, ArmsShape shape, JumpKey endKey) {
        if (body.controlEnded()) {
            return b.addCode(loc, body.code());
        }
        if (shape.carries()) {
            var valueRes = DynUtil.valueReg(loc, body);
            if (valueRes.hasErrors()) return valueRes.errorsOpt();
            b.jump(endKey, b.reg(valueRes.requireValue("branch value")));
            return Opt.none();
        }
        var codeRes = b.addCode(loc, body.code());
        if (codeRes.optIsSome()) return codeRes;
        b.jump(endKey);
        return Opt.none();
    }

    private static CodeValue finishArms(ExprContext xx, JavaBytecodeBuilder b, ArmsShape shape, JumpKey endKey) {
        if (shape.allControlEnded()) {
            return new CodeValue(Code.target(b.code()), EvalOutcome.CONTROL_ENDED);
        }
        if (!shape.anyValue()) {
            b.land(endKey);
            return new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE);
        }
        if (!shape.carries()) {
            b.land(endKey);
            return new CodeValue(
                    Code.target(b.code()),
                    EvalOutcome.ofValue(shape.structural().optUnwrap()));
        }
        var type = shape.resultType().optUnwrap();
        var param = new JavaBytecodeReg(JvmSort.descriptor(xx, type));
        b.land(endKey, param);
        return DynUtil.regToValue(b.code(), param, type);
    }

    private EResult<TList<CodeValue>> sourceArms(ExprContext xx, QExprId loc, TList<CodeValue> arms) {
        var out = new TList<CodeValue>(arms.size());
        for (var arm : arms) {
            var sourcedRes = targetSourceValue(xx, loc, arm);
            if (sourcedRes.hasErrors()) return sourcedRes.reerr();
            out.add(sourcedRes.requireValue("sourceArms arm"));
        }
        return EResult.ok(out);
    }

    @Override
    public EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<CodeValue> else_) {
        var rawArms = new TList<CodeValue>();
        for (var branch : conditions) rawArms.add(branch.body);
        if (else_.optIsSome()) rawArms.add(else_.optUnwrap());
        var armsRes = sourceArms(xx, loc, rawArms);
        if (armsRes.hasErrors()) return armsRes.reerr();
        var arms = armsRes.requireValue("targetIf arms");
        var shapeRes = armsShape(xx, loc, arms, else_.optIsNone(), "If");
        if (shapeRes.hasErrors()) return shapeRes.reerr();
        var shape = shapeRes.requireValue("targetIf shape");
        var b = new JavaBytecodeBuilder(loc);
        var endKey = new JumpKey();
        for (int i = 0; i < conditions.size(); i++) {
            var branch = conditions.get(i);
            var condRes = DynUtil.materialize(xx, loc, branch.condition);
            if (condRes.hasErrors()) return condRes.reerr();
            var cond = b.reg(condRes.requireValue("targetIf condition"));
            var nextKey = new JumpKey();
            b.branch(Opcodes.IFEQ, nextKey, cond);
            var bodyRes = emitBranchBody(loc, b, arms.get(i), shape, endKey);
            if (bodyRes.optIsSome()) return EResult.errs(bodyRes.optUnwrap());
            b.land(nextKey);
        }
        if (else_.optIsSome()) {
            var elseRes = emitBranchBody(loc, b, arms.get(conditions.size()), shape, endKey);
            if (elseRes.optIsSome()) return EResult.errs(elseRes.optUnwrap());
        }
        return EResult.ok(finishArms(xx, b, shape, endKey));
    }

    @Override
    public EResult<CodeValue> targetMatch(
            ExprContext xx, QExprId loc, CodeValue base, TList<TargetCase> cases, Opt<TargetMatchFallback> fallback) {
        var baseType = base.value().optUnwrap().valueType();
        var unionRes = GenAspectUnion.get(xx, loc, baseType);
        if (unionRes.hasErrors()) return unionRes.reerr();
        var union = unionRes.requireValue("targetMatch union");
        var rawArms = new TList<CodeValue>();
        for (var branch : cases) rawArms.add(branch.body);
        if (fallback.optIsSome()) rawArms.add(fallback.optUnwrap().body);
        var armsRes = sourceArms(xx, loc, rawArms);
        if (armsRes.hasErrors()) return armsRes.reerr();
        var arms = armsRes.requireValue("targetMatch arms");
        var shapeRes = armsShape(xx, loc, arms, false, "Match");
        if (shapeRes.hasErrors()) return shapeRes.reerr();
        var shape = shapeRes.requireValue("targetMatch shape");
        var prepRes = union.aspectUnionPrepare(xx);
        if (prepRes.optIsSome()) return EResult.errs(prepRes.optUnwrap());
        var b = new JavaBytecodeBuilder(loc);
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var baseReg = b.reg(baseRes.requireValue("targetMatch base"));
        var endKey = new JumpKey();
        for (int ci = 0; ci < cases.size(); ci++) {
            var branch = cases.get(ci);
            var nextCaseKey = new JumpKey();
            var testRes = union.aspectUnionTest(loc, baseReg, branch.variantIndex);
            if (testRes.hasErrors()) return testRes.reerr();
            var test = b.reg(testRes.requireValue("targetMatch variant test"));
            b.branch(Opcodes.IFEQ, nextCaseKey, test);
            var extractRes = union.aspectUnionExtract(xx, loc, baseReg, branch.variantIndex, branch.variantType);
            if (extractRes.hasErrors()) return extractRes.reerr();
            var payload = b.reg(extractRes.requireValue("targetMatch variant extract"));
            b.store(initialBindSlot(branch.payloadBinding), payload);
            var bodyRes = emitBranchBody(loc, b, arms.get(ci), shape, endKey);
            if (bodyRes.optIsSome()) return EResult.errs(bodyRes.optUnwrap());
            b.land(nextCaseKey);
        }
        if (fallback.optIsSome()) {
            b.store(initialBindSlot(fallback.optUnwrap().payloadBinding), baseReg);
            var fbRes = emitBranchBody(loc, b, arms.get(cases.size()), shape, endKey);
            if (fbRes.optIsSome()) return EResult.errs(fbRes.optUnwrap());
        } else {
            var err = b.newInstance(
                    JavaInternalName.of("java/lang/AssertionError"),
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID));
            b.athrow(err);
        }
        return EResult.ok(finishArms(xx, b, shape, endKey));
    }

    @Override
    public EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues) {
        var treeCell = new StagedExprCell(tree);
        var treeEntry = new EvalContext.ExternalEntry(
                new BindingId(), new TypeExpr(), new External(StagedExprCell.class, treeCell));
        var b = new JavaBytecodeBuilder(loc);
        var treeReg = b.external(treeEntry);
        var restElements = new TList<JavaBytecodeReg>();
        for (var cv : lowerValues) {
            if (!DynUtil.dynamicHasCode(cv)) {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
                }
                var cell = new StagedExprCell(cv.value().optUnwrap());
                var entry = new EvalContext.ExternalEntry(
                        new BindingId(), new TypeExpr(), new External(StagedExprCell.class, cell));
                restElements.add(b.constNull());
                restElements.add(b.external(entry));
            } else {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: dynamic lower CodeValue has no value");
                }
                var vtype = cv.value().optUnwrap().valueType();
                var liftCell =
                        new StageLiftCell(loc, xx.vx, xx.importState, xx.scope, vtype, xx.currentExceptionType, this);
                var liftEntry = new EvalContext.ExternalEntry(
                        new BindingId(), vtype, new External(StageLiftCell.class, liftCell));
                CodeValue hostCv;
                switch (GenAspectDynamicBoxed.getMaybe(xx, vtype)) {
                    case Opt.Some<AspectDynamicBoxed> boxed -> {
                        var boxRes =
                                Eval.typecheckConvert(xx, loc, boxed.value().aspectDynamicBoxedBoxedForm(), cv);
                        if (boxRes.hasErrors()) return boxRes.reerr();
                        hostCv = boxRes.requireValue("TargetDynamic::stage boxed lower value");
                    }
                    case Opt.None<AspectDynamicBoxed> unboxedNone -> hostCv = cv;
                }
                var innerRes = DynUtil.materialize(xx, loc, hostCv);
                if (innerRes.hasErrors()) return innerRes.reerr();
                restElements.add(b.external(liftEntry));
                restElements.add(b.reg(innerRes.requireValue("stage lower value")));
            }
        }
        for (var cv : graftValues) {
            if (!DynUtil.dynamicHasCode(cv)) {
                var graftCv = new CodeValue(Code.none(loc), cv.outcome());
                var graftType = cv.value().optUnwrap().valueType();
                var entry = new EvalContext.ExternalEntry(
                        new BindingId(), graftType, new External(CodeValue.class, graftCv));
                restElements.add(b.external(entry));
            } else {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
                }
                var innerRes = DynUtil.materialize(xx, loc, cv);
                if (innerRes.hasErrors()) return innerRes.reerr();
                restElements.add(b.reg(innerRes.requireValue("stage graft value")));
            }
        }
        var countReg = b.constInt(lowerValues.size());
        var arrayReg = b.newObjectArray(restElements);
        var staged = b.invokeStatic(GenCodegenDescriptors.DYN_RUNTIME_STAGE, treeReg, countReg, arrayReg);
        return EResult.ok(DynUtil.regToValue(b.code(), staged, new TypeExprStagedPlaceholder()));
    }

    public EResult<Opt<BindSubDynamic>> maybeBindSub(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Target::maybeBindSub: CodeValue has no value");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var bindDynOpt = GenAspectBindDynamic.getMaybe(xx, vtype);
        if (bindDynOpt.optIsNone()) {
            return EResult.ok(Opt.none());
        }
        return bindDynOpt.optUnwrap().aspectBindDynamicBindDynamic(xx, loc, cv);
    }

    public EResult<BindSubDynamic> bindSubRecord(
            ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields) {
        var entries = new TOrderedMap<ConstKey, BindSubDynamic>();
        for (var f : fields) entries.put(f.key, f.sub);
        var sortedSubs = sortBindSubs(xx, entries, ConstKey.CANONICAL_ORDER);
        var dynamicFields = new TOrderedMap<ConstKey, RecordField>();
        for (var f : fields) {
            dynamicFields.put(f.key, new RecordField.Dynamic(f.sub.type()));
        }
        Type recordType = new Record(IOrderedMap.from(dynamicFields));
        var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
        if (dynamicCode.hasErrors()) return dynamicCode.reerr();
        var composite = dynamicCode.requireValue("record composite");
        return EResult.ok(new BindSubDynamic(composite, recordType));
    }

    public EResult<BindSubDynamic> bindSubTuple(ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots) {
        var entries = new TOrderedMap<Integer, BindSubDynamic>();
        for (int i = 0; i < slots.size(); i++) entries.put(i, slots.get(i).sub);
        var sortedSubs = sortBindSubs(xx, entries, Integer::compare);
        var dynamicElements = new TList<TupleElement>(slots.size());
        for (var slot : slots) {
            dynamicElements.add(new TupleElement.Dynamic(slot.sub.type()));
        }
        Type tupleType = new Tuple(IList.from(dynamicElements));
        var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
        if (dynamicCode.hasErrors()) return dynamicCode.reerr();
        var composite = dynamicCode.requireValue("tuple composite");
        return EResult.ok(new BindSubDynamic(composite, tupleType));
    }

    @Override
    public EResult<Opt<Code>> targetCastException(
            ExprContext xx, QExprId loc, JumpKey self, Type calleeExc, Type callerExc, Opt<JumpKey> next) {
        var erb = new Erb(loc);
        var bindingOpt = erb.mustEr(targetInitialBind(xx, loc, calleeExc));
        if (bindingOpt.optIsNone()) return EResult.errs(erb.errors());
        var binding = bindingOpt.optUnwrap();
        var boundCv = binding.bindingUse(xx, loc, new BindingSetterConsumed(), Code.none(loc));
        var castCvOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, callerExc, boundCv));
        if (castCvOpt.optIsNone()) return EResult.errs(erb.errors());
        var castCv = castCvOpt.optUnwrap();
        if (castCv.code().codeIsNone()) {
            return EResult.ok(Opt.<Code>none());
        }
        var b = new JavaBytecodeBuilder(loc);
        var exc = new JavaBytecodeReg(DESC_THROWABLE);
        b.land(self, exc);
        b.store(initialBindSlot(binding), exc);
        var castRes = DynUtil.materialize(xx, loc, castCv);
        if (castRes.hasErrors()) return castRes.reerr();
        var casted = b.reg(castRes.requireValue("cast exception value"));
        if (next.optIsSome()) {
            b.jump(next.optUnwrap(), casted);
        } else {
            b.athrow(casted);
        }
        return EResult.ok(Opt.some(Code.target(b.code())));
    }

    private static <K> TList<BindSubDynamic> sortBindSubs(
            ExprContext xx, TOrderedMap<K, BindSubDynamic> entries, Comparator<K> tiebreaker) {
        var typeEntries = new TOrderedMap<K, Type>();
        for (var e : entries.entrySet())
            typeEntries.put(e.getKey(), e.getValue().type());
        var sort = CompositeSlotSort.sort(xx, typeEntries, tiebreaker);
        var bySlot = new TMap<Integer, BindSubDynamic>();
        for (var e : entries.entrySet()) {
            bySlot.put(sort.slotOfKey().getOpt(e.getKey()).optUnwrap(), e.getValue());
        }
        var out = new TList<BindSubDynamic>(entries.size());
        for (int i = 0; i < entries.size(); i++) out.add(bySlot.getOpt(i).optUnwrap());
        return out;
    }

    private static EResult<CodeDynamicReg> emitCompositeInstantiation(
            ExprContext xx, QExprId loc, Code carry, TList<BindSubDynamic> sortedSubs) {
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, carry);
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var sortedTypes = new TList<Type>(sortedSubs.size());
        var regs = new TList<JavaBytecodeReg>(sortedSubs.size());
        for (var sub : sortedSubs) {
            sortedTypes.add(sub.type());
            b.add(sub.value().code());
            regs.add(sub.value().reg());
        }
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        var obj = b.newInstance(klass, ctorDesc, regs);
        return EResult.ok(b.code(obj));
    }
}
