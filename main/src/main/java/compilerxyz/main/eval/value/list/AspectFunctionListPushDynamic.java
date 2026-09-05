package compilerxyz.main.eval.value.list;

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
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.List;
import org.objectweb.asm.Opcodes;

public record AspectFunctionListPushDynamic(TypeListPushDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var convertedRes = Eval.typecheckConvert(xx, loc, type.elementType(), argument);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("typecheckConvert");
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDynRes =
                consumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (argDynRes.hasErrors()) return argDynRes.reerr();
        var argDyn = argDynRes.requireValue("toDynamic");

        var selfDynRes = DynUtil.dynamicConsumeToDynamic(loc, self);
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");

        var retType = new TypeNullDynamic();
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var listName = JavaInternalName.fromClass(List.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var addDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(argDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, listName, "add", addDesc));
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
