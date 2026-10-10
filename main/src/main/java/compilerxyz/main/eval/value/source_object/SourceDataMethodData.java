package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.DynUtil.ArgSignatureSlot;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.argrecord.ArgumentEntry;
import compilerxyz.main.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.main.eval.value.argrecord.ArgumentTuple;
import compilerxyz.main.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
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
    public Opt<JavaBytecodeReg> compiledBodyResult = Opt.none();

    public Opt<TList<ArgSignatureSlot>> compiledArgSlots = Opt.none();
    public Opt<TList<BindingId>> compiledArgSlotIds = Opt.none();

    public SourceDataMethodData(String name, String jvmMethodName) {
        this.name = name;
        this.jvmMethodName = jvmMethodName;
        this.capturedExternals = new TList<>();
    }

    public Opt<String> incomplete(String dtName) {
        if (compiledBodyCode.optIsSome() || implExpr.optIsSome()) {
            return Opt.none();
        }
        return Opt.some("Object '" + dtName + "' method '" + name + "' has no implementation (impl was not called)");
    }

    public Opt<TList<ExprErr>> finish(
            ExprContext xx,
            String dtName,
            TypeSourceObjectDynamic outerType,
            Type underlyingType,
            String objectInternalName) {
        if (compiledBodyCode.optIsSome()) {
            return Opt.none();
        }
        if (implExpr.optIsNone()) {
            throw new AssertionError("Object '" + dtName + "' method '" + name + "' finished while incomplete");
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError(
                    "Data type '" + dtName + "' method '" + name + "' impl is not an Expr: " + implExprValue);
        }
        var bodyLoc = implExprAst.loc();
        var retT = retType;
        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = Opt.some(objectInternalName);
        var innerTarget = new TargetDynamic();
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
        Opt<TList<ArgSignatureSlot>> argSlotsOut = Opt.none();
        Opt<TList<BindingId>> argSlotIdsOut = Opt.none();
        if (argType.optIsSome()) {
            var argT = argType.optUnwrap();
            var slotsRes = DynUtil.argSignatureSlots(xx, bodyLoc, argT);
            if (slotsRes.hasErrors()) return slotsRes.errorsOpt();
            var argSlots = slotsRes.requireValue("SourceDataMethodData::finish argSignatureSlots");
            var argSlotIds = new TList<BindingId>(argSlots.size());
            argSlotsOut = Opt.some(argSlots);
            argSlotIdsOut = Opt.some(argSlotIds);
            var tupleOpt = GenAspectTuple.getMaybe(xx, argT);
            var recordOpt = GenAspectRecord.getMaybe(xx, argT);
            if (tupleOpt.optIsSome()) {
                var elemBindings = new TList<Binding>(argSlots.size());
                for (var slot : argSlots) {
                    var slotId = new BindingId();
                    argSlotIds.add(slotId);
                    var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, slot.elementType());
                    if (ibMaybe.optIsNone()) {
                        throw new AssertionError("SourceDataMethodData::finish tuple element type "
                                + slot.elementType().typeDesc() + " missing AspectInitialBindDynamic"
                                + " (checked at impl)");
                    }
                    elemBindings.add(ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
                }
                var argTuple =
                        new ArgumentTuple(elemBindings, tupleOpt.optUnwrap().aspectTupleElementTypes());
                bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
            } else if (recordOpt.optIsSome()) {
                var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
                for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                    var slotId = new BindingId();
                    argSlotIds.add(slotId);
                    var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, entry.getValue());
                    if (ibMaybe.optIsNone()) {
                        throw new AssertionError("SourceDataMethodData::finish record field type "
                                + entry.getValue().typeDesc() + " missing AspectInitialBindDynamic"
                                + " (checked at impl)");
                    }
                    fields.put(
                            entry.getKey(),
                            new ArgumentEntry(
                                    ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId),
                                    entry.getValue()));
                }
                bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(fields));
            } else {
                var slotId = new BindingId();
                argSlotIds.add(slotId);
                var argIbMaybe = GenAspectInitialBindDynamic.getMaybe(xx, argT);
                if (argIbMaybe.optIsNone()) {
                    throw new AssertionError("SourceDataMethodData::finish arg type " + argT.typeDesc()
                            + " missing AspectInitialBindDynamic (checked at impl)");
                }
                bodyScope.pushNamed(
                        new ConstKey.Str("arg"),
                        argIbMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
            }
        }
        var methodXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, Opt.none());

        xx.vx.ix.metaLog.scopeBegin(implExprAst.loc());
        var implResult = ConstEval.constEval(methodXx, implExprAst);
        xx.vx.ix.metaLog.scopeEnd(implExprAst.loc());
        if (implResult.hasErrors()) {
            return implResult.errorsOpt();
        }
        var implEv = implResult.requireValue("SourceDataMethodData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = retT.optIsSome()
                ? Eval.typecheckConvert(methodXx, bodyLoc, retT.optUnwrap(), implCv)
                : EResult.ok(implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.errorsOpt();
        }
        var convertedCv = convertedResult.requireValue("SourceDataMethodData::compile typecheckConvert");
        CodeDynamic bodyCode;
        Opt<JavaBytecodeReg> bodyResult;
        if (convertedCv.value().optIsSome()) {
            var dynResult = DynUtil.materialize(methodXx, bodyLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.errorsOpt();
            }
            var dyn = dynResult.requireValue("SourceDataMethodData::compile materialize");
            bodyCode = dyn.code();
            bodyResult = Opt.some(dyn.reg());
        } else {
            var bodyCodeRes = CodeDynamic.castRequired(
                    bodyLoc,
                    convertedCv.code(),
                    "Data type '" + dtName + "' method '" + name + "' body produced no compile-time value");
            if (bodyCodeRes.hasErrors()) return bodyCodeRes.errorsOpt();
            bodyCode = bodyCodeRes.requireValue("bodyCode");
            if (retT.optIsSome()) {
                return Opt.some(TList.of(ExprErr.opaque(
                        bodyLoc,
                        "Data type '" + dtName + "' method '" + name
                                + "' body produces no value but declares a return type")));
            }
            bodyResult = Opt.none();
        }

        compiledBodyCode = Opt.some(bodyCode);
        compiledBodyResult = bodyResult;
        compiledArgSlots = argSlotsOut;
        compiledArgSlotIds = argSlotIdsOut;
        return Opt.none();
    }
}
