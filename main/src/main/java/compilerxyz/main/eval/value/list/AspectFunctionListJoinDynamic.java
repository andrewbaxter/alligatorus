package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
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
        var argRes = DynUtil.materialize(xx, loc, converted);
        if (argRes.hasErrors()) return argRes.reerr();
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var stringName = JavaInternalName.fromClass(String.class);
        var charSeqDesc = JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(CharSequence.class));
        var iterableDesc = JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Iterable.class));
        var stringDesc = JavaDataDescriptor.fromClass(String.class);
        var joinDesc = JavaMethodDescriptor.fromParts(stringDesc, TList.of(charSeqDesc, iterableDesc));
        var b = new JavaBytecodeBuilder(loc);
        var sep = b.reg(argRes.requireValue("toDynamic"));
        var list = b.reg(selfRes.requireValue("selfDyn"));
        var out = b.invokeStatic(stringName, "join", joinDesc, sep, list);
        return EResult.ok(DynUtil.regToValue(b.code(), out, sepType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
