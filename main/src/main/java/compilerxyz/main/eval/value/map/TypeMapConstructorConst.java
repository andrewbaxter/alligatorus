package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.Binding;
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

@BuiltinPath("types.map")
@BuiltinFull(uniqueId = 6121L)
public record TypeMapConstructorConst() implements Type, AspectFunction, AspectInitialBind {

    @Override
    public String typeDesc() {
        return "map constructor";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_CONSTRUCTOR_CONST_IDENTITY)) {
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
        Opt.Some<Value> argSome;
        switch (argument.value()) {
            case Opt.Some<Value> s -> argSome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: argument must have a compile-time value");
            }
        }
        var argValue = argSome.value();
        var argMeta = argValue.valueType();
        var aspectTupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        Opt.Some<AspectTuple> tupleSome;
        switch (aspectTupleOpt) {
            case Opt.Some<AspectTuple> s -> tupleSome = s;
            case Opt.None<AspectTuple> n -> {
                return EResult.err(loc, "map() requires a tuple of (key_type, value_type)");
            }
        }
        var elements = tupleSome.value().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(loc, "map() requires exactly 2 type arguments (key, value), got " + elements.size());
        }
        var keyRes = elements.get(0);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var keyCv = keyRes.requireValue("TypeMapConstructorConst::call key element");
        var valueRes = elements.get(1);
        if (valueRes.hasErrors()) return valueRes.reerr();
        var valueCv = valueRes.requireValue("TypeMapConstructorConst::call value element");
        Opt.Some<Value> keySome;
        switch (keyCv.value()) {
            case Opt.Some<Value> s -> keySome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: key slot has no compile-time value");
            }
        }
        Opt.Some<Value> valueSome;
        switch (valueCv.value()) {
            case Opt.Some<Value> s -> valueSome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "TypeMapConstructorConst::call: value slot has no compile-time value");
            }
        }
        var keyValue = keySome.value();
        var valueValue = valueSome.value();
        var keyAtRes = GenAspectType.get(xx, loc, keyValue.valueType());
        if (keyAtRes.hasErrors()) return keyAtRes.reerr();
        var keyType = keyAtRes.requireValue("TypeMapConstructorConst::call key AspectType")
                .aspectTypeType(keyValue);
        var valueAtRes = GenAspectType.get(xx, loc, valueValue.valueType());
        if (valueAtRes.hasErrors()) return valueAtRes.reerr();
        var valueType = valueAtRes
                .requireValue("TypeMapConstructorConst::call value AspectType")
                .aspectTypeType(valueValue);
        keyType = DynUtil.substituteBoxed(xx, keyType);
        valueType = DynUtil.substituteBoxed(xx, valueType);
        Type mapType = new TypeMapDynamic(keyType, valueType);
        return EResult.ok(new CodeValue(
                Code.concat(loc, self.code(), argument.code()),
                EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), mapType))));
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
        return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_MAP_CONSTRUCTOR_CONST_IDENTITY)
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
