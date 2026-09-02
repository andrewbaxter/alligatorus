package compilerxyz.main.eval.value.function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.EvalRootEmitter;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 6110L)
public record TypeFunctionDynamic(
        TList<FunctionParam> params, Opt<Type> ret, JavaInternalName klass, String methodName, boolean needsEvalContext)
        implements Type,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamicLight,
                AspectFunction,
                AspectElementUseDynamicBind,
                AspectInitialBind {

    public Type arg() {
        return FunctionParam.argType(params);
    }

    public static EResult<JavaMethodDescriptor> descriptorFor(
            ExprContext xx, QExprId loc, Type arg, Opt<Type> ret, boolean needsEvalContext) {
        var argDescs = new TList<JavaDataDescriptor>();
        if (needsEvalContext) argDescs.add(JavaDataDescriptor.fromClass(EvalContext.class));
        var tupleOpt = GenAspectTuple.getMaybe(xx, arg);
        if (tupleOpt.optIsSome()) {
            for (var elem : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                var elemDescRes = jvmDescOf(xx, loc, elem);
                if (elemDescRes.hasErrors()) return elemDescRes.reerr();
                argDescs.add(elemDescRes.requireValue("TypeFunctionDynamic::descriptor tuple elem"));
            }
        } else {
            var recordOpt = GenAspectRecord.getMaybe(xx, arg);
            if (recordOpt.optIsSome()) {
                for (var entry : recordOpt.optUnwrap().aspectRecordFieldTypes().entrySet()) {
                    var fieldDescRes = jvmDescOf(xx, loc, entry.getValue());
                    if (fieldDescRes.hasErrors()) return fieldDescRes.reerr();
                    argDescs.add(fieldDescRes.requireValue("TypeFunctionDynamic::descriptor record field"));
                }
            } else {
                var argDescRes = jvmDescOf(xx, loc, arg);
                if (argDescRes.hasErrors()) return argDescRes.reerr();
                argDescs.add(argDescRes.requireValue("TypeFunctionDynamic::descriptor single arg"));
            }
        }
        JavaDataDescriptor retDesc;
        if (ret.optIsSome()) {
            var retDescRes = jvmDescOf(xx, loc, ret.optUnwrap());
            if (retDescRes.hasErrors()) return retDescRes.reerr();
            retDesc = retDescRes.requireValue("TypeFunctionDynamic::descriptor ret");
        } else {
            retDesc = JavaDataDescriptor.DESC_VOID;
        }
        return EResult.ok(JavaMethodDescriptor.fromParts(retDesc, argDescs));
    }

    public static EResult<CodeDynamic> emitStaticInvocation(
            ExprContext xx,
            QExprId loc,
            JavaInternalName klass,
            String methodName,
            JavaMethodDescriptor desc,
            boolean needsEvalContext,
            TList<CodeDynamic> args) {
        var seq = new JavaBytecodeSequence();
        if (needsEvalContext) {
            seq.add(EvalRootEmitter.loadEvalContext(loc, xx));
        }
        for (var argCode : args) {
            seq.add(argCode);
        }
        seq.add(JavaBytecodeUtils.callStaticMethod(loc, klass, methodName, desc));
        return EResult.ok(new CodeDynamic(loc, seq));
    }

    private static EResult<JavaDataDescriptor> jvmDescOf(ExprContext xx, QExprId loc, Type t) {
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, t);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        return EResult.ok(consumeRes
                .requireValue("TypeFunctionDynamic::jvmDescOf consume")
                .aspectConsumeDynamicJvmClass(xx));
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public String typeDesc() {
        return "dynamic func";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argType = arg();
        var argList = new TList<CodeDynamic>();
        EResult<TList<CodeDynamic>> argsDynRes;
        if (DynUtil.zeroArgSignature(xx, argType)) {
            argsDynRes = EResult.ok(new TList<>());
        } else {
            var convertedRes = Eval.typecheckConvert(xx, loc, argType, argument);
            if (convertedRes.hasErrors()) return convertedRes.reerr();
            var converted = convertedRes.requireValue("TypeFunctionDynamic::call converted arg");
            argsDynRes = DynUtil.destructurePositional(xx, loc, argType, converted);
            if (argsDynRes.hasErrors()) return argsDynRes.reerr();
        }
        for (var c : argsDynRes.requireValue("TypeFunctionDynamic::call positional args")) {
            argList.add(c);
        }
        var descRes = descriptorFor(xx, loc, arg(), ret, needsEvalContext);
        if (descRes.hasErrors()) return descRes.reerr();
        var invocationRes = emitStaticInvocation(
                xx,
                loc,
                klass,
                methodName,
                descRes.requireValue("TypeFunctionDynamic::emit desc"),
                needsEvalContext,
                argList);
        if (invocationRes.hasErrors()) return invocationRes.reerr();
        var invocation = invocationRes.requireValue("TypeFunctionDynamic::call invocation");
        if (ret.optIsNone()) {
            return EResult.ok(new CodeValue(Code.of(invocation), EvalOutcome.NO_VALUE));
        }
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, ret.optUnwrap());
        if (retStackRes.hasErrors()) return retStackRes.reerr();
        return EResult.ok(retStackRes
                .requireValue("TypeFunctionDynamic::call ret stack")
                .aspectStackValueDynamicToValue(xx, loc, invocation));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dyn = cv.code().codeDynamic();
        if (dyn != null) return EResult.ok(dyn);
        return EResult.err(loc, "TypeFunctionDynamic::toDynamic: CodeValue has no dynamic code");
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingFunctionDynamic(dynamicKey, this);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_FUNCTION_DYNAMIC_IDENTITY, TypeFunctionDynamic.class);
        return oOpt.optIsSome()
                && paramsEqual(xx, params, oOpt.optUnwrap().params)
                && retEqual(xx, ret, oOpt.optUnwrap().ret)
                && klass.equals(oOpt.optUnwrap().klass)
                && methodName.equals(oOpt.optUnwrap().methodName)
                && needsEvalContext == oOpt.optUnwrap().needsEvalContext;
    }

    static boolean paramsEqual(ExprContext xx, TList<FunctionParam> a, TList<FunctionParam> b) {
        if (a.size() != b.size()) return false;
        for (int i = 0; i < a.size(); i++) {
            if (!a.get(i).name().equals(b.get(i).name())) return false;
            if (!a.get(i).type().typeIsEqual(xx, b.get(i).type())) return false;
        }
        return true;
    }

    public static boolean retEqual(ExprContext xx, Opt<Type> a, Opt<Type> b) {
        if (a.optIsSome() && b.optIsSome()) {
            return a.optUnwrap().typeIsEqual(xx, b.optUnwrap());
        }
        return a.optIsNone() && b.optIsNone();
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        return false;
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingFunctionDynamic(dynamicKey, this);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
