package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionMapHasFnDynamic(TypeMapHasFnDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var keyConvertedRes = Eval.typecheckConvert(xx, loc, type.keyType(), argument);
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("has key converted");
        var keyRes = DynUtil.materialize(xx, loc, keyConverted);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var selfReg = b.reg(selfRes.requireValue("selfDyn"));
        var k = b.reg(keyRes.requireValue("toDynamic key"));
        var out = b.invokeStatic(GenCodegenDescriptors.DYN_RUNTIME_MAP_HAS, selfReg, k);
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
