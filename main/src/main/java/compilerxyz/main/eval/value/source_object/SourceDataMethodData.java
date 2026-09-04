package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.DynUtil.ArgSignatureSlot;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.argrecord.ArgumentEntry;
import compilerxyz.main.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.main.eval.value.argrecord.ArgumentTuple;
import compilerxyz.main.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectNull;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class SourceDataMethodData {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;

    public final String jvmMethodName;

    public Opt<Type> argType = Opt.none();
    public Opt<Type> retType = Opt.none();
    public Opt<Value> implExpr = Opt.none();
    public Opt<BindingId> innerBindingId = Opt.none();
    public Opt<BindingId> outerBindingId = Opt.none();
    public final TList<EvalContext.ExternalEntry> capturedExternals;
    public Opt<CodeDynamic> compiledBodyCode = Opt.none();

    public Opt<TList<ArgSignatureSlot>> compiledArgSlots = Opt.none();
    public Opt<TList<BindingId>> compiledArgSlotIds = Opt.none();

    public SourceDataMethodData(String name, String jvmMethodName) {
        this.name = name;
        this.jvmMethodName = jvmMethodName;
        this.capturedExternals = new TList<>();
    }

    public EResult<Void> finish(
            ExprContext xx,
            QExprId rootLoc,
            String dtName,
            TypeSourceObjectDynamic outerType,
            Type underlyingType,
            String objectInternalName) {
        if (compiledBodyCode.optIsSome()) {
            return EResult.ok(null);
        }
        if (implExpr.optIsNone()) {
            return EResult.ok(null);
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError(
                    "Data type '" + dtName + "' method '" + name + "' impl is not an Expr: " + implExprValue);
        }
        var retT = retType;
        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = objectInternalName;
        var innerTarget = new TargetDynamic(innerVx.nextLocalId);
        var selfId = new BindingId();
        innerBindingId = Opt.some(selfId);

        var outerId = new BindingId();
        outerBindingId = Opt.some(outerId);
        var bodyScope = new Scope(innerVx);
        bodyScope.pushAll(Eval.bodyBuiltinScope(xx));
        var outerIb = GenAspectInitialBindDynamic.getMaybe(xx, outerType);
        if (outerIb.optIsSome()) {
            bodyScope.pushNamed(
                    new ConstKey.Str("outer"), outerIb.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(outerId));
        }
        var selfIb = GenAspectInitialBindDynamic.getMaybe(xx, underlyingType);
        if (selfIb.optIsSome()) {
            bodyScope.pushNamed(
                    new ConstKey.Str("inner"), selfIb.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(selfId));
        }
        TList<ArgSignatureSlot> argSlots = null;
        TList<BindingId> argSlotIds = null;
        if (argType.optIsSome()) {
            var argT = argType.optUnwrap();
            var slotsRes = DynUtil.argSignatureSlots(xx, rootLoc, argT);
            if (slotsRes.hasErrors()) return slotsRes.reerr();
            argSlots = slotsRes.requireValue("SourceDataMethodData::finish argSignatureSlots");
            argSlotIds = new TList<>(argSlots.size());
            var tupleOpt = GenAspectTuple.getMaybe(xx, argT);
            var recordOpt = GenAspectRecord.getMaybe(xx, argT);
            if (tupleOpt.optIsSome()) {
                var elemBindings = new TList<Binding>(argSlots.size());
                for (var slot : argSlots) {
                    var slotId = new BindingId();
                    argSlotIds.add(slotId);
                    var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, slot.elementType());
                    if (ibMaybe.optIsNone()) {
                        return EResult.err(
                                rootLoc,
                                "SourceDataMethodData::finish tuple element type "
                                        + slot.elementType().typeDesc() + " missing AspectInitialBindDynamic");
                    }
                    elemBindings.add(ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
                }
                var argTuple =
                        new ArgumentTuple(elemBindings, tupleOpt.optUnwrap().aspectTupleElementTypes());
                bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
            } else if (recordOpt.optIsSome()) {
                var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
                int i = 0;
                for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                    var slotId = new BindingId();
                    argSlotIds.add(slotId);
                    var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, entry.getValue());
                    if (ibMaybe.optIsNone()) {
                        return EResult.err(
                                rootLoc,
                                "SourceDataMethodData::finish record field type "
                                        + entry.getValue().typeDesc() + " missing AspectInitialBindDynamic");
                    }
                    fields.put(
                            entry.getKey(),
                            new ArgumentEntry(
                                    ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId),
                                    entry.getValue()));
                    i++;
                }
                bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(fields));
            } else {
                var slotId = new BindingId();
                argSlotIds.add(slotId);
                var argIbMaybe = GenAspectInitialBindDynamic.getMaybe(xx, argT);
                if (argIbMaybe.optIsSome()) {
                    bodyScope.pushNamed(
                            new ConstKey.Str("arg"),
                            argIbMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
                }
            }
        }
        var methodXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, Opt.none());

        var implResult = ConstEval.constEval(methodXx, implExprAst);
        if (implResult.hasErrors()) {
            return implResult.reerr();
        }
        var implEv = implResult.requireValue("SourceDataMethodData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = retT.optIsSome()
                ? Eval.typecheckConvert(methodXx, rootLoc, retT.optUnwrap(), implCv)
                : EResult.ok(implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.reerr();
        }
        var convertedCv = convertedResult.requireValue("SourceDataMethodData::compile typecheckConvert");
        CodeDynamic bodyCode;
        if (convertedCv.value().optIsSome()) {
            var valueType = convertedCv.value().optUnwrap().valueType();
            var consumeResult = GenAspectConsumeDynamic.get(methodXx, rootLoc, valueType);
            if (consumeResult.hasErrors()) {
                return consumeResult.reerr();
            }
            var consume = consumeResult.requireValue("SourceDataMethodData::compile consume aspect");
            var dynResult = consume.aspectConsumeDynamicToDynamic(methodXx, rootLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.reerr();
            }
            bodyCode = dynResult.requireValue("SourceDataMethodData::compile consume.toDynamic");
        } else {
            var bodyCodeRes = CodeDynamic.castRequired(
                    rootLoc,
                    convertedCv.code(),
                    "Data type '" + dtName + "' method '" + name + "' body produced no compile-time value");
            if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
            bodyCode = bodyCodeRes.requireValue("bodyCode");
            if (retT.optIsSome()) {
                if (GenAspectNull.getMaybe(methodXx, retT.optUnwrap()).optIsNone()) {
                    return EResult.err(
                            rootLoc,
                            "Data type '" + dtName + "' method '" + name
                                    + "' body produces no value but declares a non-null return type");
                }
                var padded = new JavaBytecodeSequence();
                padded.add(bodyCode);
                padded.add(JavaBytecodeUtils.literalNull(rootLoc));
                bodyCode = new CodeDynamic(new CodeLoc.Some(rootLoc), padded);
            }
        }

        outerType.set.absorbBodyContext(xx, innerVx);

        var captured = innerVx.externals;
        capturedExternals.addAll(captured);

        compiledBodyCode = Opt.some(bodyCode);
        compiledArgSlots = Opt.ofNullable(argSlots);
        compiledArgSlotIds = Opt.ofNullable(argSlotIds);
        return EResult.ok(null);
    }
}
