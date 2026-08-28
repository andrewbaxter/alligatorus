package compilerxyz.eval.value.function;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectTuple;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

@BuiltinPath("must_reinterpret")
@BuiltinLight(uniqueId = 6111L)
public record TypeFunctionMustReinterpretConst() implements Type, AspectFunction, AspectInitialBind, ExporterAny {

    @Override
    public String typeDesc() {
        return "fn-must-reinterpret";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_FUNCTION_MUST_REINTERPRET_CONST_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        return Opt.none();
    }

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var erb = new Erb(loc);
        Opt.Some<Value> argSome;
        switch (argument.value()) {
            case Opt.Some<Value> s -> argSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(
                        loc, "must_reinterpret: argument must have a compile-time value (a (value, type) tuple)");
            }
        }
        var argValue = argSome.value();
        var argMeta = argValue.valueType();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        Opt.Some<AspectTuple> tupleSome;
        switch (tupleOpt) {
            case Opt.Some<AspectTuple> s -> tupleSome = s;
            case Opt.None<AspectTuple> ignored -> {
                return EResult.err(loc, "must_reinterpret: argument must be a (value, type) tuple");
            }
        }
        var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(
                    loc, "must_reinterpret: expected exactly 2 elements (value, type), got " + elements.size());
        }
        var valRes = elements.get(0);
        if (valRes.hasErrors()) return valRes.reerr();
        var valueCv = valRes.requireValue("must_reinterpret value element");
        var typeRes = elements.get(1);
        if (typeRes.hasErrors()) return typeRes.reerr();
        var typeCv = typeRes.requireValue("must_reinterpret type element");
        Opt.Some<Value> typeSome;
        switch (typeCv.value()) {
            case Opt.Some<Value> s -> typeSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, "must_reinterpret: second argument (type) must be constant");
            }
        }
        var typeValue = typeSome.value();
        var typeAtOpt = erb.mustEr(GenAspectType.get(xx, loc, typeValue.valueType()));
        if (typeAtOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetType = typeAtOpt.optUnwrap().aspectTypeType(typeValue);
        targetType = DynUtil.substituteBoxed(xx, targetType);

        Opt.Some<Value> valueSome;
        switch (valueCv.value()) {
            case Opt.Some<Value> s -> valueSome = s;
            case Opt.None<Value> ignored -> {
                return EResult.err(loc, "must_reinterpret: first argument (value) has no compile-time value");
            }
        }
        var sourceType = valueSome.value().valueType();
        var sourceConsumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, sourceType));
        if (sourceConsumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var sourceCodeOpt = erb.mustEr(sourceConsumeOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, valueCv));
        if (sourceCodeOpt.optIsNone()) return EResult.errs(erb.errors());

        var targetConsumeOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, targetType));
        if (targetConsumeOpt.optIsNone()) return EResult.errs(erb.errors());
        var targetJvmClass = targetConsumeOpt.optUnwrap().aspectConsumeDynamicJvmClass(xx);
        if (!targetJvmClass.value.startsWith("L") || !targetJvmClass.value.endsWith(";")) {
            return EResult.err(
                    loc,
                    "must_reinterpret: target type "
                            + targetType.typeDesc()
                            + " has non-object runtime representation "
                            + targetJvmClass.value
                            + "; only object-shaped targets are supported");
        }
        var seq = new JavaBytecodeSequence();
        seq.add(sourceCodeOpt.optUnwrap());
        seq.add(JavaBytecodeUtils.cast(loc, targetJvmClass));
        var out = new CodeDynamic(loc, seq);
        return EResult.ok(new CodeValue(Code.of(loc, out), EvalOutcome.ofValue(ValueDynamic.stack(targetType, 1))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Type> exporterAnyAsType() {
        return EResult.ok(this);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
        return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_FUNCTION_MUST_REINTERPRET_CONST_IDENTITY)
                .optIsSome();
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
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Type.exportArtifact(xx, loc, this);
    }
}
