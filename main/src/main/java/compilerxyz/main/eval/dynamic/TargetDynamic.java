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
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.expr.TypeExprStagedPlaceholder;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleBaseDynamic;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.IfBranch;
import compilerxyz.main.gen.Target;
import compilerxyz.main.gen.TargetCase;
import compilerxyz.main.gen.TargetMatchFallback;
import compilerxyz.main.gen.TryCatchClause;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
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
        var aspectRes = GenAspectInitialBindDynamic.get(xx, loc, type);
        if (aspectRes.hasErrors()) return aspectRes.reerr();
        return EResult.ok(aspectRes
                .requireValue("targetInitialBind AspectInitialBindDynamic")
                .aspectInitialBindDynamicInitialBindDynamic(new BindingId()));
    }

    @Override
    public boolean targetValueIsRuntime(ExprContext xx, Value value) {
        return DynUtil.sourceOf(value).optIsSome();
    }

    @Override
    public EResult<Code> targetExit(ExprContext xx, QExprId loc, JumpKey dest, Opt<Type> argument, CodeValue value) {
        var b = new JavaBytecodeBuilder(loc);
        if (argument.optIsSome()) {
            var valueRes = DynUtil.materialize(xx, loc, argument.optUnwrap(), value);
            if (valueRes.hasErrors()) return valueRes.reerr();
            b.jump(dest, b.reg(valueRes.requireValue("targetExit value")));
        } else {
            var codeRes = b.addCode(loc, value.code());
            if (codeRes.hasErrors()) return codeRes.reerr();
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
        return EResult.ok(DynUtil.regToTypedValue(xx, loc, b.code(), param, type));
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
        if (armRes.hasErrors()) return armRes.reerr();
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
            if (catchArmRes.hasErrors()) return catchArmRes.reerr();
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
            out = DynUtil.regToTypedValue(xx, loc, b.code(), resultParam.optUnwrap(), resultType.optUnwrap());
        } else {
            out = new CodeValue(Code.target(b.code()), outcome);
        }
        return EResult.ok(out);
    }

    private static EResult<Void> emitArm(
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
                if (codeRes.hasErrors()) return codeRes;
                b.jump(joinKey, b.constDefault(JvmSort.descriptor(xx, resultType.optUnwrap())));
                return EResult.ok(null);
            }
            var valueRes = DynUtil.materialize(xx, loc, resultType.optUnwrap(), arm);
            if (valueRes.hasErrors()) return valueRes.reerr();
            b.jump(joinKey, b.reg(valueRes.requireValue(what + " value")));
            return EResult.ok(null);
        }
        if (arm.value().optIsSome()) {
            return EResult.err(loc, what + " yields a value while the other arm yields none");
        }
        var codeRes = b.addCode(loc, arm.code());
        if (codeRes.hasErrors()) return codeRes;
        b.jump(joinKey);
        return EResult.ok(null);
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

    private record ArmsShape(boolean anyValue, boolean allControlEnded, Opt<Type> resultType) {}

    private static EResult<ArmsShape> armsShape(QExprId loc, TList<CodeValue> arms, boolean elseMissing, String what) {
        boolean anyValue = false;
        boolean anyNoValue = false;
        boolean allControlEnded = true;
        Type resultType = null;
        for (var arm : arms) {
            switch (arm.outcome()) {
                case EvalOutcome.HasValue h -> {
                    anyValue = true;
                    allControlEnded = false;
                    if (resultType == null) resultType = h.value().valueType();
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
        return EResult.ok(
                new ArmsShape(anyValue, allControlEnded, resultType == null ? Opt.<Type>none() : Opt.some(resultType)));
    }

    private static EResult<Void> emitBranchBody(
            ExprContext xx, QExprId loc, JavaBytecodeBuilder b, CodeValue body, ArmsShape shape, JumpKey endKey) {
        if (body.controlEnded()) {
            return b.addCode(loc, body.code());
        }
        if (shape.anyValue()) {
            var valueRes = DynUtil.materialize(xx, loc, shape.resultType().optUnwrap(), body);
            if (valueRes.hasErrors()) return valueRes.reerr();
            b.jump(endKey, b.reg(valueRes.requireValue("branch value")));
            return EResult.ok(null);
        }
        var codeRes = b.addCode(loc, body.code());
        if (codeRes.hasErrors()) return codeRes;
        b.jump(endKey);
        return EResult.ok(null);
    }

    private static CodeValue finishArms(
            ExprContext xx, QExprId loc, JavaBytecodeBuilder b, ArmsShape shape, JumpKey endKey) {
        if (shape.allControlEnded()) {
            return new CodeValue(Code.target(b.code()), EvalOutcome.CONTROL_ENDED);
        }
        if (!shape.anyValue()) {
            b.land(endKey);
            return new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE);
        }
        var type = shape.resultType().optUnwrap();
        var param = new JavaBytecodeReg(JvmSort.descriptor(xx, type));
        b.land(endKey, param);
        return DynUtil.regToTypedValue(xx, loc, b.code(), param, type);
    }

    @Override
    public EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<CodeValue> else_) {
        var arms = new TList<CodeValue>();
        for (var branch : conditions) arms.add(branch.body);
        if (else_.optIsSome()) arms.add(else_.optUnwrap());
        var shapeRes = armsShape(loc, arms, else_.optIsNone(), "If");
        if (shapeRes.hasErrors()) return shapeRes.reerr();
        var shape = shapeRes.requireValue("targetIf shape");
        var b = new JavaBytecodeBuilder(loc);
        var endKey = new JumpKey();
        for (var branch : conditions) {
            var condRes = DynUtil.materialize(xx, loc, branch.condition);
            if (condRes.hasErrors()) return condRes.reerr();
            var cond = b.reg(condRes.requireValue("targetIf condition"));
            var nextKey = new JumpKey();
            b.branch(Opcodes.IFEQ, nextKey, cond);
            var bodyRes = emitBranchBody(xx, loc, b, branch.body, shape, endKey);
            if (bodyRes.hasErrors()) return bodyRes.reerr();
            b.land(nextKey);
        }
        if (else_.optIsSome()) {
            var elseRes = emitBranchBody(xx, loc, b, else_.optUnwrap(), shape, endKey);
            if (elseRes.hasErrors()) return elseRes.reerr();
        }
        return EResult.ok(finishArms(xx, loc, b, shape, endKey));
    }

    @Override
    public EResult<CodeValue> targetMatch(
            ExprContext xx, QExprId loc, CodeValue base, TList<TargetCase> cases, Opt<TargetMatchFallback> fallback) {
        var baseType = base.value().optUnwrap().valueType();
        MatchDynamic md;
        if (baseType instanceof MatchDynamic direct) {
            md = direct;
        } else {
            return EResult.err(loc, "Match: base type is not matchable: " + baseType.typeDesc());
        }
        var arms = new TList<CodeValue>();
        for (var branch : cases) arms.add(branch.body);
        if (fallback.optIsSome()) arms.add(fallback.optUnwrap().body);
        var shapeRes = armsShape(loc, arms, false, "Match");
        if (shapeRes.hasErrors()) return shapeRes.reerr();
        var shape = shapeRes.requireValue("targetMatch shape");
        var prepRes = md.matchDynamicPrepare(xx);
        if (prepRes.hasErrors()) return prepRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var baseReg = b.reg(baseRes.requireValue("targetMatch base"));
        var endKey = new JumpKey();
        for (var branch : cases) {
            var nextCaseKey = new JumpKey();
            var testRes = md.matchDynamicTest(loc, baseReg, branch.variantIndex);
            if (testRes.hasErrors()) return testRes.reerr();
            var test = b.reg(testRes.requireValue("targetMatch variant test"));
            b.branch(Opcodes.IFEQ, nextCaseKey, test);
            var extractRes = md.matchDynamicExtract(xx, loc, baseReg, branch.variantIndex, branch.variantType);
            if (extractRes.hasErrors()) return extractRes.reerr();
            var payload = b.reg(extractRes.requireValue("targetMatch variant extract"));
            b.store(initialBindSlot(branch.payloadBinding), payload);
            var bodyRes = emitBranchBody(xx, loc, b, branch.body, shape, endKey);
            if (bodyRes.hasErrors()) return bodyRes.reerr();
            b.land(nextCaseKey);
        }
        if (fallback.optIsSome()) {
            b.store(initialBindSlot(fallback.optUnwrap().payloadBinding), baseReg);
            var fbRes = emitBranchBody(xx, loc, b, fallback.optUnwrap().body, shape, endKey);
            if (fbRes.hasErrors()) return fbRes.reerr();
        } else {
            var err = b.newInstance(
                    JavaInternalName.of("java/lang/AssertionError"),
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID));
            b.athrow(err);
        }
        return EResult.ok(finishArms(xx, loc, b, shape, endKey));
    }

    @Override
    public EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues) {
        var treeCell = new StagedExprCell(tree);
        var treeEntry = new EvalContext.ExternalEntry(
                new BindingId(), new TypeExpr(), new External(StagedExprCell.class, treeCell));
        xx.vx.externals.add(treeEntry);
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
                xx.vx.externals.add(entry);
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
                xx.vx.externals.add(liftEntry);
                var innerRes = DynUtil.materialize(xx, loc, cv);
                if (innerRes.hasErrors()) return innerRes.reerr();
                restElements.add(b.external(liftEntry));
                restElements.add(b.reg(innerRes.requireValue("stage lower value")));
            }
        }
        for (var cv : graftValues) {
            if (!DynUtil.dynamicHasCode(cv)) {
                var graftCv = new CodeValue(Code.none(loc), cv.outcome());
                var graftType = cv.value().optIsSome() ? cv.value().optUnwrap().valueType() : new TypeNullDynamic();
                var entry = new EvalContext.ExternalEntry(
                        new BindingId(), graftType, new External(CodeValue.class, graftCv));
                xx.vx.externals.add(entry);
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

    public void emit(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
        emitInner(seq, ctx, code);
    }

    private void emitInner(JavaBytecodeSequence seq, EmitCtx ctx, CodeDynamic code) {
        switch (code.code()) {
            case JavaBytecodeSequence v -> {
                for (var child : v.children) emitInner(seq, ctx, child);
            }
            case CodeDynamic nested -> emitInner(seq, ctx, nested);
            case JavaBytecode jbc -> seq.add(new CodeDynamic(code.loc(), jbc));
        }
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
        Type recordType = new Record(Opt.none(), dynamicFields);
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
        Type tupleType = new Tuple(Opt.<TupleBaseDynamic>none(), dynamicElements);
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
        var out = new TList<BindSubDynamic>(entries.size());
        for (int i = 0; i < entries.size(); i++) out.add(null);
        for (var e : entries.entrySet()) {
            out.set(sort.slotOfKey().get(e.getKey()), e.getValue());
        }
        return out;
    }

    private static EResult<CodeDynamicReg> emitCompositeInstantiation(
            ExprContext xx, QExprId loc, Code carry, TList<BindSubDynamic> sortedSubs) {
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, carry);
        if (carryRes.hasErrors()) return carryRes.reerr();
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
