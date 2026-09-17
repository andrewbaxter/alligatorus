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
    public Opt<JavaBytecodeReg> compiledBodyResult = Opt.none();

    public Opt<TList<BindingId>> compiledArgParamIds = Opt.none();

    public Opt<TList<Type>> compiledArgParamTypes = Opt.none();

    public SourceObjectInterfaceMethodImplData(InterfaceMethodData interfaceMethod, String jvmMethodName) {
        this.interfaceMethod = interfaceMethod;
        this.jvmMethodName = jvmMethodName;
        this.capturedExternals = new TList<>();
    }

    public EResult<Void> finish(ExprContext xx, QExprId rootLoc, TypeSourceObjectDynamic dtData, Type underlyingType) {
        if (compiledBodyCode.optIsSome()) {
            return EResult.ok(null);
        }
        if (implExpr.optIsNone()) {
            return EResult.ok(null);
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError("Interface method impl is not an Expr: " + implExprValue);
        }
        var ifaceMethod = interfaceMethod;
        if (ifaceMethod.retType.optIsNone()) {
            throw new AssertionError("Interface method impl for a no-value return is unsupported");
        }
        var retT = ifaceMethod.retType.optUnwrap();
        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = dtData.internalName();
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
                var f = rec.fields().getOrNull(new ConstKey.Str(fieldName));
                if (f == null) {
                    throw new AssertionError("Record method arg_type missing field '" + fieldName + "'");
                }
                Dynamic fd;
                switch (f) {
                    case Dynamic d -> fd = d;
                    case RecordField.EvalRes ignored ->
                        throw new AssertionError("Record method arg_type field '" + fieldName + "' is not Dynamic");
                }
                paramTypes.add(fd.type());
                var pid = new BindingId();
                argParamIds.add(pid);
                var ibResult = GenAspectInitialBindDynamic.get(xx, rootLoc, fd.type());
                if (ibResult.hasErrors()) {
                    return ibResult.reerr();
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
                var ibResult = GenAspectInitialBindDynamic.get(xx, rootLoc, elemType);
                if (ibResult.hasErrors()) {
                    return ibResult.reerr();
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

        var implResult = ConstEval.constEval(methodXx, implExprAst);
        if (implResult.hasErrors()) {
            return implResult.reerr();
        }
        var implEv = implResult.requireValue("SourceObjectInterfaceMethodImplData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = Eval.typecheckConvert(methodXx, rootLoc, retT, implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.reerr();
        }
        var convertedCv = convertedResult.requireValue("SourceObjectInterfaceMethodImplData::compile typecheckConvert");
        CodeDynamic bodyCode;
        Opt<JavaBytecodeReg> bodyResult;
        if (convertedCv.value().optIsSome()) {
            var dynResult = DynUtil.materialize(methodXx, rootLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.reerr();
            }
            var dyn = dynResult.requireValue("SourceObjectInterfaceMethodImplData::compile materialize");
            bodyCode = dyn.code();
            bodyResult = Opt.some(dyn.reg());
        } else {
            var bodyCodeRes = CodeDynamic.castRequired(
                    rootLoc, convertedCv.code(), "Interface method impl body produced no compile-time value");
            if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
            bodyCode = bodyCodeRes.requireValue("bodyCode");
            bodyResult = Opt.none();
        }

        dtData.set.absorbBodyContext(xx, innerVx);

        var captured = innerVx.externals;
        capturedExternals.addAll(captured);

        compiledBodyCode = Opt.some(bodyCode);
        compiledBodyResult = bodyResult;
        compiledArgParamIds = Opt.some(argParamIds);
        return EResult.ok(null);
    }
}
