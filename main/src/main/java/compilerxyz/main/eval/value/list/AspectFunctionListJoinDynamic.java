package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFunctionListJoinDynamic implements AspectFunction {
    public static final AspectFunctionListJoinDynamic INSTANCE = new AspectFunctionListJoinDynamic();

    private AspectFunctionListJoinDynamic() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var sepType = new TypeStringDynamic();
        var convertedRes = Eval.typecheckConvert(xx, loc, sepType, argument);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("typecheckConvert");
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, sepType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var argDynRes =
                consumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (argDynRes.hasErrors()) return argDynRes.reerr();
        var argDyn = argDynRes.requireValue("toDynamic");

        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeListJoinDynamic::call: self has no dynamic code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");

        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, sepType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var stringName = JavaInternalName.fromClass(String.class);
        var charSeqDesc = JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(CharSequence.class));
        var iterableDesc = JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Iterable.class));
        var stringDesc = JavaDataDescriptor.fromClass(String.class);
        var joinDesc = JavaMethodDescriptor.fromParts(stringDesc, TList.of(charSeqDesc, iterableDesc));

        var seq = new JavaBytecodeSequence();
        seq.add(argDyn);
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callStaticMethod(loc, stringName, "join", joinDesc));
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
