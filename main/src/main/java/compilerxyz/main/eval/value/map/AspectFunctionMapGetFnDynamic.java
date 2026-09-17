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
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Map;

public record AspectFunctionMapGetFnDynamic(TypeMapGetFnDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var keyConvertedRes = Eval.typecheckConvert(xx, loc, type.keyType(), argument);
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("get key converted");
        var keyRes = DynUtil.materialize(xx, loc, keyConverted);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var selfRes = DynUtil.valueReg(loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var mapName = JavaInternalName.fromClass(Map.class);
        var getDesc = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of(JvmConstants.DESC_OBJECT));
        var b = new JavaBytecodeBuilder(loc);
        var selfReg = b.reg(selfRes.requireValue("selfDyn"));
        var k = b.reg(keyRes.requireValue("toDynamic key"));
        var out = b.invokeInterface(mapName, "get", getDesc, selfReg, k);
        var valueConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.valueType());
        if (valueConsumeRes.hasErrors()) return valueConsumeRes.reerr();
        var valueDesc =
                valueConsumeRes.requireValue("AspectConsumeDynamic.get value").aspectConsumeDynamicJvmClass(xx);
        if (valueDesc.value().startsWith("L") && !valueDesc.value().equals("Ljava/lang/Object;")) {
            out = b.checkcast(valueDesc, out);
        }
        return EResult.ok(DynUtil.regToTypedValue(xx, loc, b.code(), out, type.valueType()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
