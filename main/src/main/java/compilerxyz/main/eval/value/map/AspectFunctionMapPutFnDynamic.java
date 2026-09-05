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
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.Map;
import org.objectweb.asm.Opcodes;

public record AspectFunctionMapPutFnDynamic(TypeMapPutFnDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeMapPutFnDynamic::call: argument has no compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var argMeta = argValue.valueType();
        var tupleOpt = GenAspectTuple.getMaybe(xx, argMeta);
        if (tupleOpt.optIsNone()) {
            return EResult.err(loc, "TypeMapPutFnDynamic::call: argument must be a (key, value) tuple");
        }
        var elements = tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
        if (elements.size() != 2) {
            return EResult.err(
                    loc, "TypeMapPutFnDynamic::call: expected exactly 2 elements (key, value), got " + elements.size());
        }
        var keyRes = elements.get(0);
        if (keyRes.hasErrors()) return keyRes.reerr();
        var valRes = elements.get(1);
        if (valRes.hasErrors()) return valRes.reerr();

        var keyConvertedRes = Eval.typecheckConvert(xx, loc, type.keyType(), keyRes.requireValue("put key"));
        if (keyConvertedRes.hasErrors()) return keyConvertedRes.reerr();
        var keyConverted = keyConvertedRes.requireValue("put key converted");
        var keyConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.keyType());
        if (keyConsumeRes.hasErrors()) return keyConsumeRes.reerr();
        var keyDynRes = keyConsumeRes
                .requireValue("AspectConsumeDynamic.get key")
                .aspectConsumeDynamicToDynamic(xx, loc, keyConverted);
        if (keyDynRes.hasErrors()) return keyDynRes.reerr();
        var keyDyn = keyDynRes.requireValue("toDynamic key");

        var valConvertedRes = Eval.typecheckConvert(xx, loc, type.valueType(), valRes.requireValue("put value"));
        if (valConvertedRes.hasErrors()) return valConvertedRes.reerr();
        var valConverted = valConvertedRes.requireValue("put value converted");
        var valConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.valueType());
        if (valConsumeRes.hasErrors()) return valConsumeRes.reerr();
        var valDynRes = valConsumeRes
                .requireValue("AspectConsumeDynamic.get value")
                .aspectConsumeDynamicToDynamic(xx, loc, valConverted);
        if (valDynRes.hasErrors()) return valDynRes.reerr();
        var valDyn = valDynRes.requireValue("toDynamic value");

        var selfDynRes = DynUtil.dynamicConsumeToDynamic(loc, self);
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");

        var retType = new TypeNullDynamic();
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var mapName = JavaInternalName.fromClass(Map.class);
        var putDesc = JavaMethodDescriptor.fromParts(
                JvmConstants.DESC_OBJECT, TList.of(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT));

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(keyDyn);
        seq.add(valDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "put", putDesc));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ACONST_NULL));
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
