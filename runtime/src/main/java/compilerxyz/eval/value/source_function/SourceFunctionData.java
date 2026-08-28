package compilerxyz.eval.value.source_function;

import compilerxyz.artifact.ArtifactSupport.ArtifactIdHolder;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DefSetFunctionsEmitter;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.TargetDynamic;
import compilerxyz.eval.value.argrecord.ArgumentEntry;
import compilerxyz.eval.value.argrecord.ArgumentRecord;
import compilerxyz.eval.value.argrecord.ArgumentRecordBinding;
import compilerxyz.eval.value.argrecord.ArgumentTuple;
import compilerxyz.eval.value.argrecord.ArgumentTupleBinding;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectRecord;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

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
    public int funcIdx;
    public Opt<String> compiledClassName = Opt.none();
    public Opt<byte[]> compiledBytecode = Opt.none();

    public Opt<DefSetFunctionsEmitter.FunctionSpec> functionSpec = Opt.none();

    public Opt<TOrderedMap<String, Object>> compiledStaticFieldValues = Opt.none();
    public final TList<EvalContext.ExternalEntry> capturedExternals;

    public SourceFunctionData(String name, DefinitionSet set, Unique identity) {
        this.set = set;
        this.name = Opt.some(name);
        this.identity = identity;
        this.capturedExternals = new TList<>();
    }

    @SuppressWarnings("NullAway")
    public SourceFunctionData(Opt<String> name, Unique identity) {
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

    public EResult<Void> finish(ExprContext xx, QExprId rootLoc) {
        if (functionSpec.optIsSome() || compiledClassName.optIsSome()) {
            return EResult.ok(null);
        }
        var nameStr = name.optUnwrapOr("(unnamed)");
        if (implExpr.optIsNone()) {
            return EResult.err(rootLoc, "Definition set function '" + nameStr + "' is missing an implementation");
        }
        var implExprValue = implExpr.optUnwrap();
        implExpr = Opt.none();
        if (!(implExprValue instanceof Expr implExprAst)) {
            throw new AssertionError("Definition set function '" + nameStr + "' impl is not an Expr: " + implExprValue);
        }
        if (argType.optIsNone()) {
            return EResult.err(rootLoc, "Definition set function '" + nameStr + "' is missing an arg type");
        }
        var argT = argType.optUnwrap();
        var retT = retType;
        var funcSet = set;
        if (funcSet == null) {
            throw new AssertionError("Definition set function '" + nameStr + "' is not attached to a definition set");
        }

        var innerVx = new EvalContext(xx.vx.ix);
        innerVx.evalRootInternalName = DefSetFunctionsEmitter.functionsInternalName(funcSet.defSetId);
        var innerTarget = new TargetDynamic(innerVx.nextLocalId);

        var bodyScope = new Scope(innerVx);
        bodyScope.pushAll(Eval.bodyBuiltinScope(xx));

        var argSlots = new TList<DefSetFunctionsEmitter.ArgSlot>();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argT);
        var recordOpt = GenAspectRecord.getMaybe(xx, argT);
        if (tupleOpt.optIsSome()) {
            var elemTypes = tupleOpt.optUnwrap().aspectTupleElementTypes();
            var elemBindings = new TList<Binding>();
            for (var et : elemTypes) {
                var slotId = new BindingId.Local(new JavaBytecodeBindingKey());
                argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, et)));
                var ibMaybe = GenAspectInitialBind.getMaybe(xx, et);
                if (ibMaybe.optIsNone()) {
                    return EResult.err(
                            rootLoc, "arg tuple element type " + et.typeDesc() + " missing AspectInitialBind");
                }
                elemBindings.add(ibMaybe.optUnwrap().aspectInitialBindInitialBind(slotId));
            }
            var argTuple = new ArgumentTuple(elemBindings, elemTypes);
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentTupleBinding(argTuple));
        } else if (recordOpt.optIsSome()) {
            var fieldTypes = recordOpt.optUnwrap().aspectRecordFieldTypes();
            var fields = new TOrderedMap<ConstKey, ArgumentEntry>();
            for (var entry : fieldTypes.entrySet()) {
                var et = entry.getValue();
                var slotId = new BindingId.Local(new JavaBytecodeBindingKey());
                argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, et)));
                var ibMaybe = GenAspectInitialBind.getMaybe(xx, et);
                if (ibMaybe.optIsNone()) {
                    return EResult.err(
                            rootLoc, "arg record field type " + et.typeDesc() + " missing AspectInitialBind");
                }
                fields.put(
                        entry.getKey(),
                        new ArgumentEntry(ibMaybe.optUnwrap().aspectInitialBindInitialBind(slotId), et));
            }
            var argRec = new ArgumentRecord(fields);
            bodyScope.pushNamed(new ConstKey.Str("arg"), new ArgumentRecordBinding(argRec));
        } else {
            var slotId = new BindingId.Local(new JavaBytecodeBindingKey());
            argSlots.add(new DefSetFunctionsEmitter.ArgSlot(slotId, jvmDescFor(xx, rootLoc, argT)));
            var ibMaybe = GenAspectInitialBind.getMaybe(xx, argT);
            if (ibMaybe.optIsSome()) {
                bodyScope.pushNamed(new ConstKey.Str("arg"), ibMaybe.optUnwrap().aspectInitialBindInitialBind(slotId));
            }
        }
        var funcXx = new ExprContext(bodyScope, innerTarget, innerVx, xx.importState, exceptionType);

        var implResult = ConstEval.constEval(funcXx, implExprAst);
        if (implResult.hasErrors()) {
            return implResult.reerr();
        }
        var implEv = implResult.requireValue("SourceFunctionData::compile constEval");
        var implCv = implEv.cv();
        var convertedResult = retT.optIsSome()
                ? Eval.typecheckConvert(funcXx, rootLoc, retT.optUnwrap(), implCv)
                : EResult.ok(implCv);
        if (convertedResult.hasErrors()) {
            return convertedResult.reerr();
        }
        var convertedCv = convertedResult.requireValue("SourceFunctionData::compile typecheckConvert");
        CodeDynamic bodyCode;
        if (convertedCv.value().optIsSome()) {
            var valueType = convertedCv.value().optUnwrap().valueType();
            var consumeResult = GenAspectConsumeDynamic.get(funcXx, rootLoc, valueType);
            if (consumeResult.hasErrors()) {
                return consumeResult.reerr();
            }
            var consume = consumeResult.requireValue("SourceFunctionData::compile consume aspect");
            var dynResult = consume.aspectConsumeDynamicToDynamic(funcXx, rootLoc, convertedCv);
            if (dynResult.hasErrors()) {
                return dynResult.reerr();
            }
            bodyCode = dynResult.requireValue("SourceFunctionData::compile consume.toDynamic");
        } else {
            bodyCode = convertedCv.code().codeDynamic();
            if (bodyCode == null) {
                return EResult.err(
                        rootLoc, "Definition set function '" + nameStr + "' body produced no compile-time value");
            }
        }

        for (var entry : innerVx.definitionSets.entrySet()) {
            xx.vx.definitionSets.putIfAbsent(entry.getKey(), entry.getValue());
        }

        var owningSet = set;
        for (var entry : innerVx.definitionSets.entrySet()) {
            if (entry.getKey().equals(owningSet.defSetId)) {
                continue;
            }
            owningSet.dependencies.putIfAbsent(entry.getKey(), entry.getValue());
        }

        var captured = innerVx.externals;
        capturedExternals.addAll(captured);

        JavaDataDescriptor retDesc;
        if (retT.optIsNone()) {
            retDesc = JavaDataDescriptor.DESC_VOID;
        } else {
            var _rdConsume = GenAspectConsumeDynamic.getMaybe(xx, retT.optUnwrap());
            if (_rdConsume.optIsSome()) {
                retDesc = _rdConsume.optUnwrap().aspectConsumeDynamicJvmClass(xx);
            } else {
                retDesc = JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
            }
        }
        functionSpec = Opt.some(
                new DefSetFunctionsEmitter.FunctionSpec(funcIdx, argSlots, retDesc, bodyCode, TList.from(captured)));
        return EResult.ok(null);
    }

    private static JavaDataDescriptor jvmDescFor(ExprContext xx, QExprId loc, Type t) {
        var consumeOpt = GenAspectConsumeDynamic.getMaybe(xx, t);
        if (consumeOpt.optIsSome()) {
            return consumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        }
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }
}
