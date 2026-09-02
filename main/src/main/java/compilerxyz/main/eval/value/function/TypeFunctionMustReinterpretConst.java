package compilerxyz.main.eval.value.function;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectFunction;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinPath("must_reinterpret")
@BuiltinFull(uniqueId = 6111L)
public record TypeFunctionMustReinterpretConst() implements Type, AspectFunction, AspectInitialBind {

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
        return EResult.ok(new CodeValue(Code.of(out), EvalOutcome.ofValue(ValueDynamic.stack(xx, targetType))));
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return EResult.err(loc, typeDesc() + " is not exportable");
    }
}
