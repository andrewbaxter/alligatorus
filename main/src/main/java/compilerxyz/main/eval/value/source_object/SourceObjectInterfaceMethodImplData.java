package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalContextInternal;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.argrecord.ArgumentEntry;
import compilerxyz.main.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.main.eval.value.argrecord.ArgumentTuple;
import compilerxyz.main.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class SourceObjectInterfaceMethodImplData {
    public final InterfaceMethodData interfaceMethod;

    public final String jvmMethodName;

    public Opt<Value> implExpr = Opt.none();
    public Opt<BindingId> innerBindingId = Opt.none();
    public Opt<BindingId> outerBindingId = Opt.none();
    public final TList<EvalContext.ExternalEntry> capturedExternals;
    public Opt<CodeDynamic> compiledBodyCode = Opt.none();
    public Opt<QExprId> compiledBodyLoc = Opt.none();
    public Opt<JavaBytecodeReg> compiledBodyResult = Opt.none();

    public Opt<TList<BindingId>> compiledArgParamIds = Opt.none();

    public Opt<TList<Type>> compiledArgParamTypes = Opt.none();

    public SourceObjectInterfaceMethodImplData(InterfaceMethodData interfaceMethod, String jvmMethodName) {
        this.interfaceMethod = interfaceMethod;
        this.jvmMethodName = jvmMethodName;
        this.capturedExternals = new TList<>();
    }

    public Opt<TList<ExprErr>> finish(ExprContext xx, TypeSourceObjectDynamic dtData, Type underlyingType) {
        if (compiledBodyCode.optIsSome()) {
            return Opt.none();
        }
        if (implExpr.optIsNone()) {
            return Opt.none();
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError("Interface method impl is not an Expr: " + implExprValue);
        }
        var bodyLoc = implExprAst.loc();
        var ifaceMethod = interfaceMethod;
        if (ifaceMethod.retType.optIsNone()) {
            throw new AssertionError("Interface method impl for a no-value return is unsupported");
        }
        var retT = ifaceMethod.retType.optUnwrap();
        var innerVx = new EvalContext(EvalContextInternal.ix(xx.vx));
        var innerTarget = new TargetDynamic(Opt.some(JavaInternalName.of(dtData.internalName())));
        var selfId = new BindingId();
        innerBindingId = Opt.some(selfId);
        var outerId = new BindingId();
        outerBindingId = Opt.some(outerId);

        var bodyScope = new Scope(innerVx);
        bodyScope.pushAll(Eval.bodyBuiltinScope(xx));
        TypeSourceObjectDynamic outerType = dtData;
        if (GenAspectInitialBind.getMaybe(xx, outerType).optIsSome()) {
            var outerRes = DynUtil.initialBind(xx, bodyLoc, outerType, outerId);
            if (outerRes.hasErrors()) return outerRes.errorsOpt();
            bodyScope.pushNamed(new ConstKey.Str("outer"), outerRes.requireValue("outer binding"));
        }

        if (GenAspectInitialBind.getMaybe(xx, underlyingType).optIsSome()) {
            var selfRes = DynUtil.initialBind(xx, bodyLoc, underlyingType, selfId);
            if (selfRes.hasErrors()) return selfRes.errorsOpt();
            bodyScope.pushNamed(new ConstKey.Str("inner"), selfRes.requireValue("inner binding"));
        }
        var methodArgType = ifaceMethod.argType;
        var slotsRes = DynUtil.argSignatureSlots(xx, bodyLoc, methodArgType);
        if (slotsRes.hasErrors()) return slotsRes.errorsOpt();
        var argSlots = slotsRes.requireValue("SourceObjectInterfaceMethodImplData::finish argSignatureSlots");
        var paramTypes = new TList<Type>(argSlots.size());
        var argParamIds = new TList<BindingId>(argSlots.size());
        for (var slot : argSlots) {
            paramTypes.add(slot.elementType());
            argParamIds.add(new BindingId());
        }
        var tupleOpt = GenAspectTuple.getMaybe(xx, methodArgType);
        var recordOpt = GenAspectRecord.getMaybe(xx, methodArgType);
        if (tupleOpt.optIsSome()) {
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var elemBindings = new TList<Binding>(elemTypes.size());
            for (int i = 0; i < elemTypes.size(); i++) {
                var bRes = DynUtil.initialBind(xx, bodyLoc, elemTypes.get(i), argParamIds.get(i));
                if (bRes.hasErrors()) return bRes.errorsOpt();
                elemBindings.add(bRes.requireValue("SourceObjectInterfaceMethodImplData::finish tuple binding"));
            }
            bodyScope.pushNamed(
                    new ConstKey.Str("arg"), new ArgumentTupleBinding(new ArgumentTuple(elemBindings, elemTypes)));
        } else if (recordOpt.optIsSome()) {
            var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
            int i = 0;
            for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                var bRes = DynUtil.initialBind(xx, bodyLoc, entry.getValue(), argParamIds.get(i));
                if (bRes.hasErrors()) return bRes.errorsOpt();
                fields.put(
                        entry.getKey(),
                        new ArgumentEntry(
                                bRes.requireValue("SourceObjectInterfaceMethodImplData::finish record binding"),
                                entry.getValue()));
                i++;
            }
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(fields));
        } else {
            if (GenAspectInitialBind.getMaybe(xx, methodArgType).optIsSome()) {
                var bRes = DynUtil.initialBind(xx, bodyLoc, methodArgType, argParamIds.get(0));
                if (bRes.hasErrors()) return bRes.errorsOpt();
                bodyScope.pushNamed(
                        new ConstKey.Str("arg"),
                        bRes.requireValue("SourceObjectInterfaceMethodImplData::finish arg binding"));
            }
        }
        compiledArgParamTypes = Opt.some(paramTypes);
        var methodXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, Opt.none());

        var implResult = ConstEval.constEval(methodXx, implExprAst);
        if (implResult.hasErrors()) {
            return implResult.errorsOpt();
        }
        var implEv = implResult.requireValue("SourceObjectInterfaceMethodImplData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = Eval.typecheckConvert(methodXx, bodyLoc, retT, implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.errorsOpt();
        }
        var convertedCv = convertedResult.requireValue("SourceObjectInterfaceMethodImplData::compile typecheckConvert");
        CodeDynamic bodyCode;
        Opt<JavaBytecodeReg> bodyResult;
        if (convertedCv.value().optIsSome()) {
            var dynResult = DynUtil.materialize(methodXx, bodyLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.errorsOpt();
            }
            var dyn = dynResult.requireValue("SourceObjectInterfaceMethodImplData::compile materialize");
            bodyCode = dyn.code();
            bodyResult = Opt.some(dyn.reg());
        } else {
            var bodyCodeRes = CodeDynamic.castRequired(
                    bodyLoc, convertedCv.code(), "Interface method impl body produced no compile-time value");
            if (bodyCodeRes.hasErrors()) return bodyCodeRes.errorsOpt();
            bodyCode = bodyCodeRes.requireValue("bodyCode");
            bodyResult = Opt.none();
        }

        compiledBodyCode = Opt.some(bodyCode);
        compiledBodyLoc = Opt.some(bodyLoc);
        compiledBodyResult = bodyResult;
        compiledArgParamIds = Opt.some(argParamIds);
        return Opt.none();
    }
}
