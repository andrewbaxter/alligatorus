package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectFunctionMapNewDynamic(TypeMapNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var keyRes = DynUtil.requireBoxedTypeArgument(xx, loc, "map key type", type.keyType());
        if (keyRes.hasErrors()) return keyRes.reerr();
        var valueRes = DynUtil.requireBoxedTypeArgument(xx, loc, "map value type", type.valueType());
        if (valueRes.hasErrors()) return valueRes.reerr();
        var instanceType = new TypeMapDynamic(type.keyType(), type.valueType());
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeMapNewDynamic::call: argument has no compile-time value");
        }
        var argType = argument.value().optUnwrap().valueType();
        var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
        if (argTupleOpt.optIsSome()
                && argTupleOpt.optUnwrap().aspectTupleElementTypes().isEmpty()) {
            var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
            if (retStackRes.hasErrors()) return retStackRes.reerr();

            var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
            var instantiateMap = JavaBytecodeUtils.instantiate(
                    loc,
                    torderedMapIntname,
                    JavaMethodDescriptor.fromConstructorParts(TList.of()),
                    new CodeDynamic(loc, new JavaBytecodeSequence()));
            var selfDynCast = CodeDynamic.castOpt(loc, self.code());
            if (selfDynCast.hasErrors()) return selfDynCast.reerr();
            var selfDyn = selfDynCast.requireValue("selfDyn");
            CodeDynamic newMap;
            if (selfDyn.optIsNone()) {
                newMap = instantiateMap;
            } else {
                var seq = new JavaBytecodeSequence();
                seq.add(selfDyn.optUnwrap());
                seq.add(instantiateMap);
                newMap = new CodeDynamic(loc, seq);
            }
            return EResult.ok(retStackRes
                    .requireValue("AspectStackValueDynamic.get")
                    .aspectStackValueDynamicToValue(xx, loc, newMap));
        }
        return Eval.typecheckConvert(xx, loc, instanceType, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
