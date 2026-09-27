package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
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
            var b = new JavaBytecodeBuilder(loc);
            var selfRes = b.addCode(loc, self.code());
            if (selfRes.optIsSome()) return EResult.errs(selfRes.optUnwrap());
            var map = b.newInstance(
                    JavaInternalName.fromClass(TOrderedMap.class),
                    JavaMethodDescriptor.fromConstructorParts(TList.of()));
            return EResult.ok(DynUtil.regToTypedValue(xx, loc, b.code(), map, instanceType));
        }
        return Eval.typecheckConvert(xx, loc, instanceType, argument);
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
