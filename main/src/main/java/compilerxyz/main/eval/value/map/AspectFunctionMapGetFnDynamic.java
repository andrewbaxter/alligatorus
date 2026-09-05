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
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Map;

public record AspectFunctionMapGetFnDynamic(TypeMapGetFnDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var keyConvertedRes = Eval.typecheckConvert(xx, loc, type.keyType(), argument);
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("get key converted");
        var keyConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.keyType());
        if (keyConsumeRes.hasErrors()) return keyConsumeRes.reerr();
        var keyDynRes = keyConsumeRes
                .requireValue("AspectConsumeDynamic.get key")
                .aspectConsumeDynamicToDynamic(xx, loc, keyConverted);
        if (keyDynRes.hasErrors()) return keyDynRes.reerr();
        var keyDyn = keyDynRes.requireValue("toDynamic key");

        var selfDynRes = DynUtil.dynamicConsumeToDynamic(loc, self);
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");

        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, type.valueType());
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var mapName = JavaInternalName.fromClass(Map.class);
        var getDesc = JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of(JvmConstants.DESC_OBJECT));

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(keyDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "get", getDesc));
        var valueConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.valueType());
        if (valueConsumeRes.hasErrors()) return valueConsumeRes.reerr();
        var valueDesc =
                valueConsumeRes.requireValue("AspectConsumeDynamic.get value").aspectConsumeDynamicJvmClass(xx);
        if (valueDesc.value().startsWith("L") && !valueDesc.value().equals("Ljava/lang/Object;")) {
            seq.add(JavaBytecodeUtils.cast(loc, valueDesc));
        }
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
