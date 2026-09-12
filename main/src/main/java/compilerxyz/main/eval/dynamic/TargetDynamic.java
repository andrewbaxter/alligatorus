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
import compilerxyz.main.eval.binding.BindingOperandParked;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.expr.TypeExpr;
import compilerxyz.main.eval.value.expr.TypeExprStagedPlaceholder;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleBaseDynamic;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.IfBranch;
import compilerxyz.main.gen.Target;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.TargetCase;
import compilerxyz.main.gen.TargetMatchFallback;
import compilerxyz.main.gen.TargetOperandParked;
import compilerxyz.main.gen.TryCatchClause;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Comparator;
import java.util.IdentityHashMap;
import org.objectweb.asm.Label;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TypeInsnNode;

public final class TargetDynamic implements Target {
    private static final JavaInternalName INTNAME_DYN_EXCEPTION = JavaInternalName.fromClass(DynException.class);

    private final IdentityHashMap<Binding, BindingId> initialBindSlots = new IdentityHashMap<>();

    public TargetDynamic() {}

    @Override
    public EResult<Code> targetDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.ok(Code.none(loc));
        }
        var value = cv.value().optUnwrap();
        var vtype = value.valueType();
        var dropAspect = GenAspectDropDynamic.getMaybe(xx, vtype);
        if (dropAspect.optIsSome()) {
            return dropAspect.optUnwrap().aspectDropDynamicDrop(xx, loc, cv);
        }
        var lightAspect = GenAspectDropDynamicLight.getMaybe(xx, vtype);
        if (lightAspect.optIsSome()) {
            return lightAspect.optUnwrap().aspectDropDynamicLightDropDynamicLight(xx, loc, cv);
        }
        return EResult.ok(Code.none(loc));
    }

    @Override
    public CodeValue targetReturn(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return cv;
        }
        var consumeOpt =
                GenAspectConsumeDynamic.getMaybe(xx, cv.value().optUnwrap().valueType());
        if (consumeOpt.optIsNone()) {
            return cv;
        }
        var valueOpt = cv.value();
        if (valueOpt.optIsNone() || !(valueOpt.optUnwrap() instanceof ValueDynamic vd)) {
            return cv;
        }
        return switch (vd.source()) {
            case ValueDynamicSource.Stack ignored -> cv;
            case ValueDynamicSource.Slot ignored -> returnRealize(xx, loc, cv, vd);
            case ValueDynamicSource.Field ignored -> returnRealize(xx, loc, cv, vd);
        };
    }

    private static CodeValue returnRealize(ExprContext xx, QExprId loc, CodeValue cv, ValueDynamic vd) {
        var seq = new JavaBytecodeSequence();
        switch (cv.code()) {
            case Code.None n -> {}
            case Code.Target t -> seq.add(MustCast.mustCast(CodeDynamic.class, t.impl()));
        }
        seq.add(vd.source().valueDynamicSourceRealize(loc));
        return new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, vd.type())));
    }

    @Override
    public EResult<Binding> targetInitialBind(ExprContext xx, QExprId loc, Type type) {
        var aspectRes = GenAspectInitialBindDynamic.get(xx, loc, type);
        if (aspectRes.hasErrors()) return aspectRes.reerr();
        var key = new BindingId();
        var binding = aspectRes
                .requireValue("targetInitialBind AspectInitialBindDynamic")
                .aspectInitialBindDynamicInitialBindDynamic(key);
        initialBindSlots.put(binding, key);
        return EResult.ok(binding);
    }

    @Override
    public EResult<Opt<TargetOperandParked>> targetOperandPark(ExprContext xx, QExprId loc, CodeValue operand) {
        var slots = new TList<BindingOperandParked.Slot>();
        var parkedRes = operandParkValue(xx, loc, operand, slots);
        if (parkedRes.hasErrors()) return parkedRes.reerr();
        if (slots.isEmpty()) return EResult.ok(Opt.none());
        return EResult.ok(Opt.some(new TargetOperandParked(
                parkedRes.requireValue("targetOperandPark value"), new BindingOperandParked(slots))));
    }

    private EResult<CodeValue> operandParkValue(
            ExprContext xx, QExprId loc, CodeValue cv, TList<BindingOperandParked.Slot> slots) {
        if (cv.value().optIsNone()) return EResult.ok(cv);
        var value = cv.value().optUnwrap();
        switch (value) {
            case ValueDynamic vd -> {
                return operandParkStack(xx, loc, cv, vd.type(), vd.source(), slots);
            }
            case Tuple tuple -> {
                if (tuple.base.optIsSome()) {
                    return operandParkStack(
                            xx, loc, cv, tuple, tuple.base.optUnwrap().source(), slots);
                }
                var elements = new TList<TupleElement>(tuple.elements.size());
                for (var element : tuple.elements) {
                    switch (element) {
                        case TupleElement.Dynamic d -> elements.add(d);
                        case TupleElement.EvalRes er -> {
                            if (er.result().valueOpt().optIsNone()) {
                                elements.add(er);
                                continue;
                            }
                            var innerRes = operandParkValue(
                                    xx, loc, er.result().valueOpt().optUnwrap(), slots);
                            if (innerRes.hasErrors()) return innerRes.reerr();
                            var errors = new TList<ExprErr>();
                            er.result().drainInto(errors);
                            var inner = innerRes.requireValue("operandParkValue tuple element");
                            elements.add(new TupleElement.EvalRes(
                                    errors.isEmpty() ? EResult.ok(inner) : EResult.mixed(inner, errors)));
                        }
                    }
                }
                return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(new Tuple(Opt.none(), elements))));
            }
            case Record record -> {
                if (record.base.optIsSome()) {
                    return operandParkStack(
                            xx, loc, cv, record, record.base.optUnwrap().source(), slots);
                }
                var fields = new TOrderedMap<ConstKey, RecordField>();
                for (var fe : record.fields.entrySet()) {
                    switch (fe.getValue()) {
                        case RecordField.Dynamic d -> fields.put(fe.getKey(), d);
                        case RecordField.EvalRes er -> {
                            if (er.result().valueOpt().optIsNone()) {
                                fields.put(fe.getKey(), er);
                                continue;
                            }
                            var innerRes = operandParkValue(
                                    xx, loc, er.result().valueOpt().optUnwrap(), slots);
                            if (innerRes.hasErrors()) return innerRes.reerr();
                            var errors = new TList<ExprErr>();
                            er.result().drainInto(errors);
                            var inner = innerRes.requireValue("operandParkValue record field");
                            fields.put(
                                    fe.getKey(),
                                    new RecordField.EvalRes(
                                            errors.isEmpty() ? EResult.ok(inner) : EResult.mixed(inner, errors)));
                        }
                    }
                }
                return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), fields))));
            }
            default -> {
                return EResult.ok(cv);
            }
        }
    }

    private EResult<CodeValue> operandParkStack(
            ExprContext xx,
            QExprId loc,
            CodeValue cv,
            Type type,
            ValueDynamicSource source,
            TList<BindingOperandParked.Slot> slots) {
        if (!source.valueDynamicSourceIsStackRooted()) return EResult.ok(cv);
        var bindAspectOpt = GenAspectBind.getMaybe(xx, type);
        if (bindAspectOpt.optIsNone() || GenAspectBindDynamic.getMaybe(xx, type).optIsNone()) return EResult.ok(cv);
        var dup = JavaBytecodeUtils.inst(loc, JvmSort.descriptor(xx, type).isWide() ? Opcodes.DUP2 : Opcodes.DUP);
        var bindRes = bindAspectOpt.optUnwrap().aspectBindBind(xx, loc, new CodeValue(Code.target(dup), cv.outcome()));
        if (bindRes.hasErrors()) return bindRes.reerr();
        var bound = bindRes.requireValue("operandParkStack bind");
        var seq = new JavaBytecodeSequence();
        var origCast = CodeDynamic.castOpt(loc, cv.code());
        if (origCast.hasErrors()) return origCast.reerr();
        var orig = origCast.requireValue("operandParkStack orig");
        if (orig.optIsSome()) seq.add(orig.optUnwrap());
        var storeCast = CodeDynamic.castOpt(loc, bound.code());
        if (storeCast.hasErrors()) return storeCast.reerr();
        var store = storeCast.requireValue("operandParkStack store");
        if (store.optIsSome()) seq.add(store.optUnwrap());
        slots.add(new BindingOperandParked.Slot(type, bound.binding()));
        return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), cv.outcome()));
    }

    @Override
    public EResult<Code> targetLabeledStart(QExprId loc, JumpKey end) {
        return EResult.ok(Code.target(new CodeDynamic(loc, new JavaBytecodeStackMark(end))));
    }

    @Override
    public EResult<Code> targetExit(ExprContext xx, QExprId loc, JumpKey dest, JumpKey labeled, CodeValue value) {
        var seq = new JavaBytecodeSequence();
        Opt<JavaDataDescriptor> carry = Opt.none();
        if (value.value().optIsSome()) {
            switch (value.value().optUnwrap()) {
                case ValueDynamic vd -> {
                    if (vd.source().valueDynamicSourceIsStackRooted())
                        carry = Opt.some(JvmSort.descriptor(xx, vd.type()));
                }
                case Tuple tuple -> {
                    if (tuple.base.optIsSome()
                            && tuple.base.optUnwrap().source().valueDynamicSourceIsStackRooted()) {
                        carry = Opt.some(JvmSort.descriptor(xx, tuple));
                    }
                }
                case Record record -> {
                    if (record.base.optIsSome()
                            && record.base.optUnwrap().source().valueDynamicSourceIsStackRooted()) {
                        carry = Opt.some(JvmSort.descriptor(xx, record));
                    }
                }
                default -> {}
            }
        }
        seq.add(new CodeDynamic(loc, new JavaBytecodeStackUnwind(labeled, carry)));
        seq.add(new CodeDynamic(loc, new JavaBytecodeJump(dest)));
        return EResult.ok(Code.target(new CodeDynamic(loc, seq)));
    }

    private BindingId initialBindSlot(Binding binding) {
        var key = initialBindSlots.get(binding);
        if (key == null) {
            throw new IllegalStateException("binding was not made by targetInitialBind: " + binding.getClass());
        }
        return key;
    }

    public BindResultSlot bindSlot(ExprContext xx, QExprId loc, TargetBindSub sub) {
        var key = new BindingId();
        var local = key;
        var seq = new JavaBytecodeSequence();
        seq.add(sub.data);
        seq.add(new CodeDynamic(
                new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(JvmSort.storeOpcode(xx, sub.type), key)));
        return new BindResultSlot(new CodeDynamic(loc, seq), local);
    }

    @Override
    public EResult<Code> targetTryCatchFinally(
            ExprContext xx,
            QExprId loc,
            CodeValue body,
            Opt<TryCatchClause> catch_,
            Opt<JumpKey> catchHandler,
            Opt<Code> finallyNormal) {
        var erb = new Erb(loc);
        var seq = new JavaBytecodeSequence();
        boolean bodyHasValue = body.value().optIsSome();
        if (bodyHasValue) {
            var bodyDynOpt = cvToDynamic(erb, xx, loc, body);
            if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
            seq.add(bodyDynOpt.optUnwrap());
        } else {
            var bodyDynCast = CodeDynamic.castOpt(loc, body.code());
            if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
            var bodyDyn = bodyDynCast.requireValue("bodyDyn");
            if (bodyDyn.optIsSome()) seq.add(bodyDyn.optUnwrap());
        }
        if (catch_.optIsSome()) {
            var clause = catch_.optUnwrap();
            var handler = catchHandler.optUnwrap();
            var joinKey = new JumpKey();
            if (!body.controlEnded()) {
                seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(joinKey)));
            }
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(handler)));
            var isExc = new LabelNode(new Label());
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            seq.add(new CodeDynamic(
                    new CodeLoc.Some(loc),
                    new JavaBytecodeInstructionObj(
                            new TypeInsnNode(Opcodes.INSTANCEOF, INTNAME_DYN_EXCEPTION.value()))));
            seq.add(new CodeDynamic(
                    new CodeLoc.Some(loc), new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IFNE, isExc))));
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionObj(isExc)));
            seq.add(JavaBytecodeUtils.cast(loc, new JavaDataDescriptor("L" + INTNAME_DYN_EXCEPTION.value() + ";")));
            seq.add(JavaBytecodeUtils.accessField(loc, INTNAME_DYN_EXCEPTION, "payload", JvmConstants.DESC_OBJECT));
            var payloadConsumeRes = GenAspectConsumeDynamic.get(xx, loc, clause.excType);
            if (payloadConsumeRes.hasErrors()) return payloadConsumeRes.reerr();
            var payloadDesc = payloadConsumeRes
                    .requireValue("try/catch payload AspectConsumeDynamic")
                    .aspectConsumeDynamicJvmClass(xx);
            if (payloadDesc.value().startsWith("L") && !payloadDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
                seq.add(JavaBytecodeUtils.cast(loc, payloadDesc));
            }
            seq.add(new CodeDynamic(
                    new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, initialBindSlot(clause.binding))));
            if (clause.body.value().optIsSome()) {
                var catchDynOpt = cvToDynamic(erb, xx, loc, clause.body);
                if (catchDynOpt.optIsNone()) return EResult.errs(erb.errors());
                seq.add(catchDynOpt.optUnwrap());
            } else {
                var catchDynCast = CodeDynamic.castOpt(loc, clause.body.code());
                if (catchDynCast.hasErrors()) return catchDynCast.reerr();
                var catchDyn = catchDynCast.requireValue("catchDyn");
                if (catchDyn.optIsSome()) seq.add(catchDyn.optUnwrap());
            }
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(joinKey)));
        }
        if (finallyNormal.optIsSome()) {
            var finDynCast = CodeDynamic.castOpt(loc, finallyNormal.optUnwrap());
            if (finDynCast.hasErrors()) return finDynCast.reerr();
            var finDyn = finDynCast.requireValue("finDyn");
            if (finDyn.optIsSome()) {
                seq.add(JavaBytecodeUtils.freshen(finDyn.optUnwrap()));
            }
        }
        return EResult.ok(Code.target(new CodeDynamic(loc, seq)));
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
    public EResult<Code> targetLand(QExprId loc, JumpKey dest) {
        return EResult.ok(Code.target(new CodeDynamic(loc, new JavaBytecodeLand(dest))));
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
        var seq = new JavaBytecodeSequence();
        seq.add(new CodeDynamic(loc, new JavaBytecodeLand(self)));
        var excKey = new BindingId();
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, excKey)));
        var cleanupDynCast = CodeDynamic.castOpt(loc, cleanup);
        if (cleanupDynCast.hasErrors()) return cleanupDynCast.reerr();
        var cleanupDyn = cleanupDynCast.requireValue("cleanupDyn");
        if (cleanupDyn.optIsSome()) {
            seq.add(JavaBytecodeUtils.freshen(cleanupDyn.optUnwrap()));
        }
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, excKey)));
        if (parent.optIsSome()) {
            seq.add(new CodeDynamic(loc, new JavaBytecodeJump(parent.optUnwrap())));
        } else {
            seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
        }
        return EResult.ok(Code.target(new CodeDynamic(loc, seq)));
    }

    @Override
    public EResult<Code> targetExitTrampoline(
            ExprContext xx, QExprId loc, JumpKey self, Opt<Type> carryType, Code cleanup, JumpKey next) {
        var seq = new JavaBytecodeSequence();
        seq.add(new CodeDynamic(loc, new JavaBytecodeLand(self)));
        var cleanupDynCast = CodeDynamic.castOpt(loc, cleanup);
        if (cleanupDynCast.hasErrors()) return cleanupDynCast.reerr();
        var cleanupDyn = cleanupDynCast.requireValue("cleanupDyn");
        BindingId parkKey = null;
        Type parkType = null;
        if (carryType.optIsSome() && cleanupDyn.optIsSome()) {
            parkType = carryType.optUnwrap();
            parkKey = new BindingId();
            seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(JvmSort.storeOpcode(xx, parkType), parkKey)));
        }
        if (cleanupDyn.optIsSome()) {
            seq.add(cleanupDyn.optUnwrap());
        }
        if (parkKey != null) {
            seq.add(new CodeDynamic(
                    loc,
                    new JavaBytecodeStoreLoad(JvmSort.descriptor(xx, parkType).loadOpcode(), parkKey)));
        }
        seq.add(new CodeDynamic(loc, new JavaBytecodeJump(next)));
        return EResult.ok(Code.target(new CodeDynamic(loc, seq)));
    }

    @Override
    public EResult<CodeValue> targetIf(ExprContext xx, QExprId loc, TList<IfBranch> conditions, Opt<CodeValue> else_) {
        var erb = new Erb(loc);
        boolean anyValue = false;
        boolean anyNoValue = false;
        boolean allControlEnded = true;
        Type resultType = null;
        for (var branch : conditions) {
            switch (branch.body.outcome()) {
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
        if (else_.optIsSome()) {
            switch (else_.optUnwrap().outcome()) {
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
        } else {
            allControlEnded = false;
        }
        if (anyValue && anyNoValue) {
            return EResult.err(loc, "If: some branches yield a value and some don't");
        }
        var seq = new JavaBytecodeSequence();
        var endLabel = new LabelNode();
        for (var branch : conditions) {
            var condDynOpt = cvToDynamic(erb, xx, loc, branch.condition);
            if (condDynOpt.optIsNone()) return EResult.errs(erb.errors());
            var bodyCv = branch.body;
            CodeDynamic bodyDyn;
            if (bodyCv.value().optIsSome()) {
                var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
                if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
                bodyDyn = bodyDynOpt.optUnwrap();
            } else {
                bodyDyn = switch (bodyCv.code()) {
                    case Code.Target ct when ct.impl() instanceof CodeDynamic cd -> cd;
                    case Code.None n -> new CodeDynamic(loc, new JavaBytecodeSequence());
                    case Code.Target ct -> new CodeDynamic(loc, new JavaBytecodeSequence());
                };
            }
            var nextLabel = new LabelNode();
            seq.add(condDynOpt.optUnwrap());
            seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextLabel)));
            seq.add(bodyDyn);
            if (!bodyCv.controlEnded()) {
                seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
            }
            seq.add(JavaBytecodeUtils.wrapObj(loc, nextLabel));
        }
        if (else_.optIsSome()) {
            var elseCv = else_.optUnwrap();
            if (elseCv.value().optIsSome()) {
                var elseDynOpt = cvToDynamic(erb, xx, loc, elseCv);
                if (elseDynOpt.optIsNone()) return EResult.errs(erb.errors());
                seq.add(elseDynOpt.optUnwrap());
            } else {
                switch (elseCv.code()) {
                    case Code.Target ct when ct.impl() instanceof CodeDynamic ecd -> seq.add(ecd);
                    case Code.None n -> {}
                    case Code.Target ct -> {}
                }
            }
        } else if (anyValue) {
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
        }
        if (allControlEnded) {
            return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.CONTROL_ENDED));
        }
        seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
        if (!anyValue) {
            return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
        }
        var finalType = resultType == null ? (Type) new TypeNullDynamic() : resultType;
        var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
        if (stackOpt.optIsNone()) return EResult.errs(erb.errors());
        return EResult.ok(stackOpt.optUnwrap().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public EResult<CodeValue> targetMatch(
            ExprContext xx, QExprId loc, CodeValue base, TList<TargetCase> cases, Opt<TargetMatchFallback> fallback) {
        var erb = new Erb(loc);
        var baseDynOpt = cvToDynamic(erb, xx, loc, base);
        if (baseDynOpt.optIsNone()) return EResult.errs(erb.errors());
        var baseKey = new BindingId();

        var seq = new JavaBytecodeSequence();
        seq.add(baseDynOpt.optUnwrap());
        seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, baseKey)));

        var endLabel = new LabelNode();
        Type resultType = null;
        boolean anyValue = false;
        boolean anyNoValue = false;
        boolean allControlEnded = true;
        for (var branch : cases) {
            switch (branch.body.outcome()) {
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
        if (fallback.optIsSome()) {
            switch (fallback.optUnwrap().body.outcome()) {
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
        if (anyValue && anyNoValue) {
            return EResult.err(loc, "Match: some branches yield a value and some don't");
        }
        var baseType = ((Opt.Some<Value>) base.value()).value().valueType();
        MatchDynamic md;
        if (baseType instanceof MatchDynamic direct) {
            md = direct;
        } else {
            return EResult.err(loc, "Match: base type is not matchable: " + baseType.typeDesc());
        }
        var prepRes = md.matchDynamicPrepare(xx);
        if (prepRes.hasErrors()) return prepRes.reerr();
        for (var branch : cases) {
            var bodyCv = branch.body;
            CodeDynamic bodyDyn;
            if (bodyCv.value().optIsSome()) {
                var bodyDynOpt = cvToDynamic(erb, xx, loc, bodyCv);
                if (bodyDynOpt.optIsNone()) return EResult.errs(erb.errors());
                bodyDyn = bodyDynOpt.optUnwrap();
            } else {
                bodyDyn = switch (bodyCv.code()) {
                    case Code.Target ct when ct.impl() instanceof CodeDynamic cd -> cd;
                    case Code.None n -> new CodeDynamic(loc, new JavaBytecodeSequence());
                    case Code.Target ct -> new CodeDynamic(loc, new JavaBytecodeSequence());
                };
            }
            var nextCaseLabel = new LabelNode();
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
            var testRes = md.matchDynamicTest(loc, branch.variantIndex);
            if (testRes.hasErrors()) return testRes.reerr();
            seq.add(testRes.requireValue("targetMatch variant test"));
            seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.IFEQ, nextCaseLabel)));
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
            var extractRes = md.matchDynamicExtract(xx, loc, branch.variantIndex, branch.variantType);
            if (extractRes.hasErrors()) return extractRes.reerr();
            seq.add(extractRes.requireValue("targetMatch variant extract"));
            seq.add(new CodeDynamic(
                    new CodeLoc.Some(loc),
                    new JavaBytecodeStoreLoad(
                            JvmSort.storeOpcode(xx, branch.variantType), initialBindSlot(branch.payloadBinding))));
            seq.add(bodyDyn);
            if (!bodyCv.controlEnded()) {
                seq.add(JavaBytecodeUtils.wrapObj(loc, new JumpInsnNode(Opcodes.GOTO, endLabel)));
            }
            seq.add(JavaBytecodeUtils.wrapObj(loc, nextCaseLabel));
        }
        if (fallback.optIsSome()) {
            var fallbackCv = fallback.optUnwrap().body;
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ALOAD, baseKey)));
            seq.add(new CodeDynamic(
                    new CodeLoc.Some(loc),
                    new JavaBytecodeStoreLoad(Opcodes.ASTORE, initialBindSlot(fallback.optUnwrap().payloadBinding))));
            if (fallbackCv.value().optIsSome()) {
                var fallbackDynOpt = cvToDynamic(erb, xx, loc, fallbackCv);
                if (fallbackDynOpt.optIsNone()) return EResult.errs(erb.errors());
                seq.add(fallbackDynOpt.optUnwrap());
            } else {
                switch (fallbackCv.code()) {
                    case Code.Target ct when ct.impl() instanceof CodeDynamic fcd -> seq.add(fcd);
                    case Code.None n -> {}
                    case Code.Target ct -> {}
                }
            }
        } else {
            seq.add(JavaBytecodeUtils.instantiate(
                    loc,
                    JavaInternalName.of("java/lang/AssertionError"),
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID),
                    new CodeDynamic(loc, new JavaBytecodeSequence())));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
        }
        seq.add(JavaBytecodeUtils.wrapObj(loc, endLabel));
        if (allControlEnded) {
            return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.CONTROL_ENDED));
        }
        if (!anyValue) {
            return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
        }
        var finalType = resultType == null ? (Type) new TypeNullDynamic() : resultType;
        var stackOpt = erb.mustEr(GenAspectStackValueDynamic.get(xx, loc, finalType));
        if (stackOpt.optIsNone()) return EResult.errs(erb.errors());
        return EResult.ok(stackOpt.optUnwrap().aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    private static Opt<CodeDynamic> cvToDynamic(Erb erb, ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            erb.errPush("TargetDynamic::if_: CodeValue has no value");
            return Opt.none();
        }
        var vtype = cv.value().optUnwrap().valueType();
        var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
        if (consumeOpt.optIsNone()) return Opt.none();
        return erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
    }

    @Override
    public EResult<CodeValue> targetStage(
            ExprContext xx, QExprId loc, Expr tree, TList<CodeValue> lowerValues, TList<CodeValue> graftValues) {
        var erb = new Erb(loc);
        var treeCell = new StagedExprCell(tree);
        var treeKey = new BindingId();
        var treeId = treeKey;
        var treeExprType = new TypeExpr();
        xx.vx.externals.add(
                new EvalContext.ExternalEntry(treeKey, treeExprType, new External(StagedExprCell.class, treeCell)));
        var restElements = new TList<CodeDynamic>();
        for (var cv : lowerValues) {
            if (!DynUtil.dynamicHasCode(cv)) {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: lower CodeValue has no value");
                }
                var cell = new StagedExprCell(cv.value().optUnwrap());
                var key = new BindingId();
                var id = key;
                xx.vx.externals.add(
                        new EvalContext.ExternalEntry(key, new TypeExpr(), new External(StagedExprCell.class, cell)));
                restElements.add(JavaBytecodeUtils.literalNull(loc));
                restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
            } else {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: dynamic lower CodeValue has no value");
                }
                var vtype = cv.value().optUnwrap().valueType();
                var liftCell =
                        new StageLiftCell(loc, xx.vx, xx.importState, xx.scope, vtype, xx.currentExceptionType, this);
                var liftKey = new BindingId();
                var liftId = liftKey;
                xx.vx.externals.add(
                        new EvalContext.ExternalEntry(liftKey, vtype, new External(StageLiftCell.class, liftCell)));
                var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
                if (consumeOpt.optIsNone()) return EResult.errs(erb.errors());
                var innerJsOpt = erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
                if (innerJsOpt.optIsNone()) return EResult.errs(erb.errors());
                restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), liftId));
                restElements.add(innerJsOpt.optUnwrap());
            }
        }
        for (var cv : graftValues) {
            if (!DynUtil.dynamicHasCode(cv)) {
                var graftCv = new CodeValue(Code.none(loc), cv.outcome());
                var key = new BindingId();
                var id = key;
                var graftType = cv.value().optIsSome() ? cv.value().optUnwrap().valueType() : new TypeNullDynamic();
                xx.vx.externals.add(
                        new EvalContext.ExternalEntry(key, graftType, new External(CodeValue.class, graftCv)));
                restElements.add(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
            } else {
                if (cv.value().optIsNone()) {
                    return EResult.err(loc, "TargetDynamic::stage: dynamic graft CodeValue has no value");
                }
                var vtype = cv.value().optUnwrap().valueType();
                var consumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, vtype));
                if (consumeOpt.optIsNone()) return EResult.errs(erb.errors());
                var innerJsOpt = erb.mustEr(consumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, cv));
                if (innerJsOpt.optIsNone()) return EResult.errs(erb.errors());
                restElements.add(innerJsOpt.optUnwrap());
            }
        }
        var stageSeq = new JavaBytecodeSequence();
        stageSeq.add(BindingCode.bindingUse(new CodeLoc.Some(loc), treeId));
        stageSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, lowerValues.size()));
        stageSeq.add(JavaBytecodeUtils.buildObjectArray(loc, restElements));
        stageSeq.add(JavaBytecodeUtils.callStaticMethod(loc, GenCodegenDescriptors.DYN_RUNTIME_STAGE));
        var stageCall = new CodeDynamic(loc, stageSeq);
        var resultValue = ValueDynamic.stack(xx, new TypeExprStagedPlaceholder());
        return EResult.ok(new CodeValue(Code.target(stageCall), EvalOutcome.ofValue(resultValue)));
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

    public EResult<Opt<TargetBindSub>> maybeBindSub(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException(
                    "Target::maybeBindSub: CodeValue has no value; Rust unwraps unconditionally");
        }
        var vtype = cv.value().optUnwrap().valueType();
        var bindDynOpt = GenAspectBindDynamic.getMaybe(xx, vtype);
        if (bindDynOpt.optIsNone()) {
            return EResult.ok(Opt.none());
        }
        return bindDynOpt.optUnwrap().aspectBindDynamicBindDynamic(xx, loc, cv);
    }

    public EResult<TargetBindSub> bindSubRecord(
            ExprContext xx, QExprId loc, Code carry, TList<RecordBindField> fields) {
        var entries = new TOrderedMap<ConstKey, TargetBindSub>();
        for (var f : fields) entries.put(f.key, f.sub);
        var sortedSubs = sortBindSubs(xx, entries, ConstKey.CANONICAL_ORDER);
        var dynamicFields = new TOrderedMap<ConstKey, RecordField>();
        for (var f : fields) {
            dynamicFields.put(f.key, new RecordField.Dynamic(f.sub.type));
        }
        Type recordType = new Record(Opt.none(), dynamicFields);
        var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
        if (dynamicCode.hasErrors()) return dynamicCode.reerr();
        return EResult.ok(new TargetBindSub(dynamicCode.requireValue("record composite"), recordType));
    }

    public EResult<TargetBindSub> bindSubTuple(ExprContext xx, QExprId loc, Code carry, TList<TupleBindSlot> slots) {
        var entries = new TOrderedMap<Integer, TargetBindSub>();
        for (int i = 0; i < slots.size(); i++) entries.put(i, slots.get(i).sub);
        var sortedSubs = sortBindSubs(xx, entries, Integer::compare);
        var dynamicElements = new TList<TupleElement>(slots.size());
        for (var slot : slots) {
            dynamicElements.add(new TupleElement.Dynamic(slot.sub.type));
        }
        Type tupleType = new Tuple(Opt.<TupleBaseDynamic>none(), dynamicElements);
        var dynamicCode = emitCompositeInstantiation(xx, loc, carry, sortedSubs);
        if (dynamicCode.hasErrors()) return dynamicCode.reerr();
        return EResult.ok(new TargetBindSub(dynamicCode.requireValue("tuple composite"), tupleType));
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
        if (!DynUtil.dynamicHasCode(castCv)) {
            return EResult.ok(Opt.<Code>none());
        }
        var seq = new JavaBytecodeSequence();
        seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeLand(self)));
        seq.add(new CodeDynamic(
                new CodeLoc.Some(loc), new JavaBytecodeStoreLoad(Opcodes.ASTORE, initialBindSlot(binding))));
        var castDynOpt = cvToDynamic(erb, xx, loc, castCv);
        if (castDynOpt.optIsNone()) return EResult.errs(erb.errors());
        seq.add(castDynOpt.optUnwrap());
        if (next.optIsSome()) {
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeJump(next.optUnwrap())));
        } else {
            seq.add(new CodeDynamic(new CodeLoc.Some(loc), new JavaBytecodeInstructionInt(Opcodes.ATHROW)));
        }
        return EResult.ok(Opt.some(Code.target(new CodeDynamic(loc, seq))));
    }

    @Override
    public EResult<Code> targetFresh(QExprId loc, Code code) {
        var dynCast = CodeDynamic.castOpt(loc, code);
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsNone()) return EResult.ok(code);
        var dyn = dynCast.requireValue("dyn").optUnwrap();
        return EResult.ok(Code.target(JavaBytecodeUtils.freshen(dyn)));
    }

    private static <K> TList<TargetBindSub> sortBindSubs(
            ExprContext xx, TOrderedMap<K, TargetBindSub> entries, Comparator<K> tiebreaker) {
        var typeEntries = new TOrderedMap<K, Type>();
        for (var e : entries.entrySet()) typeEntries.put(e.getKey(), e.getValue().type);
        var sort = CompositeSlotSort.sort(xx, typeEntries, tiebreaker);
        var out = new TList<TargetBindSub>(entries.size());
        for (int i = 0; i < entries.size(); i++) out.add(null);
        for (var e : entries.entrySet()) {
            out.set(sort.slotOfKey().get(e.getKey()), e.getValue());
        }
        return out;
    }

    private static EResult<CodeDynamic> emitCompositeInstantiation(
            ExprContext xx, QExprId loc, Code carry, TList<TargetBindSub> sortedSubs) {
        var sortedTypes = new TList<Type>(sortedSubs.size());
        var argSeq = new JavaBytecodeSequence();
        for (var sub : sortedSubs) {
            sortedTypes.add(sub.type);
            argSeq.add(sub.data);
        }
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        var wrapped = DynUtil.wrapWithCarry(
                loc, carry, JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq)));
        if (wrapped.hasErrors()) return wrapped.reerr();
        return EResult.ok(wrapped.requireValue("bindSubComposite"));
    }
}
