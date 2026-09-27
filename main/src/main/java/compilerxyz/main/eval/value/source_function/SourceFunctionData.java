package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaClassCompiled;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.TargetDynamic;
import compilerxyz.main.eval.value.argrecord.ArgumentEntry;
import compilerxyz.main.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.main.eval.value.argrecord.ArgumentTuple;
import compilerxyz.main.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class SourceFunctionData {
    public final ArtifactIdHolder artifactIdCell = new ArtifactIdHolder();
    public final Unique identity;

    @SuppressWarnings("NullAway.Init")
    public final DefinitionSet set;

    public final Opt<String> name;
    public Opt<String> description = Opt.none();
    public Opt<Type> exceptionType = Opt.none();
    public Opt<Value> implExpr = Opt.none();
    public Opt<Type> argType = Opt.none();
    public Opt<Type> retType = Opt.none();
    public final String jvmMethodName;

    public Opt<String> compiledClassName = Opt.none();
    public Opt<JavaClassCompiled> compiledClass = Opt.none();

    public Opt<DefSetFunctionsEmitter.FunctionSpec> functionSpec = Opt.none();

    public Opt<TOrderedMap<String, Object>> compiledStaticFieldValues = Opt.none();
    public final TList<EvalContext.ExternalEntry> capturedExternals;

    public SourceFunctionData(String name, DefinitionSet set, Unique identity, String jvmMethodName) {
        this.jvmMethodName = jvmMethodName;
        this.set = set;
        this.name = Opt.some(name);
        this.identity = identity;
        this.capturedExternals = new TList<>();
    }

    @SuppressWarnings("NullAway")
    public SourceFunctionData(Opt<String> name, Unique identity, String jvmMethodName) {
        this.jvmMethodName = jvmMethodName;
        this.identity = identity;
        this.set = (DefinitionSet) null;
        this.name = name;
        this.capturedExternals = new TList<>();
    }

    public void initSetReflectively(DefinitionSet set) {
        try {
            var f = SourceFunctionData.class.getDeclaredField("set");
            f.setAccessible(true);
            f.set(this, set);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SourceFunctionData.initSetReflectively: failed to write final 'set' field", e);
        }
    }

    public Opt<String> incomplete() {
        if (functionSpec.optIsSome() || compiledClassName.optIsSome() || implExpr.optIsSome()) {
            return Opt.none();
        }
        return Opt.some("Definition set function '" + name.optUnwrapOr("(unnamed)")
                + "' has no implementation (impl was not called)");
    }

    public Opt<TList<ExprErr>> finish(ExprContext xx) {
        if (functionSpec.optIsSome() || compiledClassName.optIsSome()) {
            return Opt.none();
        }
        var nameStr = name.optUnwrapOr("(unnamed)");
        if (implExpr.optIsNone() || argType.optIsNone()) {
            throw new AssertionError("Definition set function '" + nameStr + "' finished while incomplete");
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError("Definition set function '" + nameStr + "' impl is not an Expr: " + implExprValue);
        }
        var bodyLoc = implExprAst.loc();
        var argT = argType.optUnwrap();
        var retT = retType;
        var funcSet = set;
        if (funcSet == null) {
            throw new AssertionError("Definition set function '" + nameStr + "' is not attached to a definition set");
        }

        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = Opt.some(DefSetFunctionsEmitter.functionsInternalName(funcSet.defSetId));
        var innerTarget = new TargetDynamic();

        var bodyScope = new Scope(innerVx);
        bodyScope.pushAll(Eval.bodyBuiltinScope(xx));

        var argSlots = new TList<DefSetFunctionsEmitter.ArgSlot>();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argT);
        var recordOpt = GenAspectRecord.getMaybe(xx, argT);
        if (tupleOpt.optIsSome()) {
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var elemBindings = new TList<Binding>();
            for (var et : elemTypes) {
                var slotId = new BindingId();
                argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, et)));
                var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, et);
                if (ibMaybe.optIsNone()) {
                    throw new AssertionError("arg tuple element type " + et.typeDesc()
                            + " missing AspectInitialBindDynamic" + " (checked at impl)");
                }
                elemBindings.add(ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
            }
            var argTuple = new ArgumentTuple(elemBindings, elemTypes);
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
        } else if (recordOpt.optIsSome()) {
            var fieldTypes = recordOpt.optUnwrap().aspectRecordFieldTypes();
            var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
            for (var entry : fieldTypes.entrySet()) {
                var et = entry.getValue();
                var slotId = new BindingId();
                argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, et)));
                var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, et);
                if (ibMaybe.optIsNone()) {
                    throw new AssertionError("arg record field type " + et.typeDesc()
                            + " missing AspectInitialBindDynamic" + " (checked at impl)");
                }
                fields.put(
                        entry.getKey(),
                        new ArgumentEntry(ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId), et));
            }
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(fields));
        } else {
            var slotId = new BindingId();
            argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, argT)));
            var ibMaybe = GenAspectInitialBindDynamic.getMaybe(xx, argT);
            if (ibMaybe.optIsNone()) {
                throw new AssertionError(
                        "arg type " + argT.typeDesc() + " missing AspectInitialBindDynamic" + " (checked at impl)");
            }
            bodyScope.pushNamed(
                    new ConstKey.Str("arg"), ibMaybe.optUnwrap().aspectInitialBindDynamicInitialBindDynamic(slotId));
        }
        var funcXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, exceptionType);

        var implResult = ConstEval.constEval(funcXx, implExprAst);
        if (implResult.hasErrors()) {
            return implResult.errorsOpt();
        }
        var implEv = implResult.requireValue("SourceFunctionData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = retT.optIsSome()
                ? Eval.typecheckConvert(funcXx, bodyLoc, retT.optUnwrap(), implCv)
                : EResult.ok(implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.errorsOpt();
        }
        var convertedCv = convertedResult.requireValue("SourceFunctionData::compile typecheckConvert");
        CodeDynamic bodyCode;
        Opt<JavaBytecodeReg> bodyResult;
        if (convertedCv.value().optIsSome()) {
            var dynResult = DynUtil.materialize(funcXx, bodyLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.errorsOpt();
            }
            var dyn = dynResult.requireValue("SourceFunctionData::compile materialize");
            bodyCode = dyn.code();
            bodyResult = Opt.some(dyn.reg());
        } else {
            var bodyCodeRes = CodeDynamic.castRequired(
                    bodyLoc,
                    convertedCv.code(),
                    "Definition set function '" + nameStr + "' body produced no compile-time value");
            if (bodyCodeRes.hasErrors()) return bodyCodeRes.errorsOpt();
            bodyCode = bodyCodeRes.requireValue("bodyCode");
            if (retT.optIsSome()) {
                return Opt.some(TList.of(ExprErr.opaque(
                        bodyLoc,
                        "Definition set function '" + nameStr
                                + "' body produces no value but declares a return type")));
            }
            bodyResult = Opt.none();
        }

        set.absorbBodyContext(xx, innerVx);

        var captured = innerVx.externals;
        capturedExternals.addAll(captured);

        JavaDataDescriptor retDesc;
        if (retT.optIsNone()) {
            retDesc = JavaDataDescriptor.DESC_VOID;
        } else {
            var rdConsume = GenAspectConsumeDynamic.getMaybe(xx, retT.optUnwrap());
            if (rdConsume.optIsSome()) {
                retDesc = rdConsume.optUnwrap().aspectConsumeDynamicJvmClass(xx);
            } else {
                retDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
            }
        }
        functionSpec = Opt.some(new DefSetFunctionsEmitter.FunctionSpec(
                jvmMethodName, argSlots, retDesc, bodyCode, bodyResult, TList.from(captured)));
        return Opt.none();
    }

    private static JavaDataDescriptor jvmDescFor(ExprContext xx, Type t) {
        var consumeOpt = GenAspectConsumeDynamic.getMaybe(xx, t);
        if (consumeOpt.optIsSome()) {
            return consumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        }
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }
}
