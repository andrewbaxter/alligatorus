package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.argrecord.ArgumentEntry;
import compilerxyz.main.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.main.eval.value.argrecord.ArgumentTuple;
import compilerxyz.main.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.record.RecordField.Dynamic;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
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
        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = Opt.some(dtData.internalName());
        var innerTarget = new TargetDynamic();
        var selfId = new BindingId();
        innerBindingId = Opt.some(selfId);
        var outerId = new BindingId();
        outerBindingId = Opt.some(outerId);

        var bodyScope = new Scope(innerVx);
        bodyScope.pushAll(Eval.bodyBuiltinScope(xx));
        TypeSourceObjectDynamic outerType = dtData;
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
        var methodArgType = ifaceMethod.argType;
        var dataFieldNames = new TList<String>();
        for (var n : ifaceMethod.dataFieldNames) {
            dataFieldNames.add(n);
        }
        var tupleOpt = GenAspectTuple.getMaybe(xx, methodArgType);
        TList<Type> paramTypes;
        var argParamIds = new TList<BindingId>();
        if (methodArgType instanceof Record rec) {
            if (dataFieldNames.size() != rec.fields().size()) {
                throw new AssertionError("Record method '"
                        + ifaceMethod.name
                        + "' data_field_names ("
                        + dataFieldNames.size()
                        + ") does not match record field count ("
                        + rec.fields().size()
                        + ")");
            }
            paramTypes = new TList<>(dataFieldNames.size());
            var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
            for (var fieldName : dataFieldNames) {
                var fOpt = rec.fields().getOpt(new ConstKey.Str(fieldName));
                if (fOpt.optIsNone()) {
                    throw new AssertionError("Record method arg_type missing field '" + fieldName + "'");
                }
                var f = fOpt.optUnwrap();
                Dynamic fd;
                switch (f) {
                    case Dynamic d -> fd = d;
                    case RecordField.EvalRes ignored ->
                        throw new AssertionError("Record method arg_type field '" + fieldName + "' is not Dynamic");
                }
                paramTypes.add(fd.type());
                var pid = new BindingId();
                argParamIds.add(pid);
                var ibResult = GenAspectInitialBindDynamic.get(xx, bodyLoc, fd.type());
                if (ibResult.hasErrors()) {
                    return ibResult.errorsOpt();
                }
                var ibAspect = ibResult.requireValue("SourceObjectInterfaceMethodImplData::compile param InitialBind");
                fields.put(
                        new ConstKey.Str(fieldName),
                        new ArgumentEntry(ibAspect.aspectInitialBindDynamicInitialBindDynamic(pid), fd.type()));
            }
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(fields));
        } else if (tupleOpt.optIsSome()) {
            if (!dataFieldNames.isEmpty()) {
                throw new AssertionError("Tuple method '"
                        + ifaceMethod.name
                        + "' must have empty data_field_names, got: "
                        + dataFieldNames);
            }
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            paramTypes = new TList<>(elemTypes.size());
            var elemBindings = new TList<Binding>(elemTypes.size());
            for (var elemType : elemTypes) {
                paramTypes.add(elemType);
                var pid = new BindingId();
                argParamIds.add(pid);
                var ibResult = GenAspectInitialBindDynamic.get(xx, bodyLoc, elemType);
                if (ibResult.hasErrors()) {
                    return ibResult.errorsOpt();
                }
                var ibAspect =
                        ibResult.requireValue("SourceObjectInterfaceMethodImplData::compile tuple elem InitialBind");
                elemBindings.add(ibAspect.aspectInitialBindDynamicInitialBindDynamic(pid));
            }
            var argTuple = new ArgumentTuple(elemBindings, elemTypes);
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
        } else {
            if (!dataFieldNames.isEmpty()) {
                throw new AssertionError("Non-record method '"
                        + ifaceMethod.name
                        + "' must have empty data_field_names, got: "
                        + dataFieldNames);
            }
            paramTypes = TList.of(methodArgType);
            var argId = new BindingId();
            argParamIds.add(argId);
            var argIbMaybe = GenAspectInitialBindDynamic.getMaybe(xx, methodArgType);
            if (argIbMaybe.optIsSome()) {
                bodyScope.pushNamed(
                        new ConstKey.Str("arg"),
                        argIbMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(argId));
            }
        }
        compiledArgParamTypes = Opt.some(paramTypes);
        var methodXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, Opt.none());

        xx.vx.ix.metaLog.scopeBegin(implExprAst.loc());
        var implResult = ConstEval.constEval(methodXx, implExprAst);
        xx.vx.ix.metaLog.scopeEnd(implExprAst.loc());
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
