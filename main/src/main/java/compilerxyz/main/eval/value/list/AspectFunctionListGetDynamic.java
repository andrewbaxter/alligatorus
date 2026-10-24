package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionListGetDynamic(TypeListGetDynamic type) implements AspectFunction {
    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var indexConvertedRes = Eval.typecheckConvert(xx, loc, new TypeIntDynamic(), argument);
        if (indexConvertedRes.hasErrors()) return indexConvertedRes.reerr();
        var indexRes = DynUtil.materialize(xx, loc, indexConvertedRes.requireValue("get index converted"));
        if (indexRes.hasErrors()) return indexRes.reerr();
        var selfRes = DynUtil.valueReg(xx, loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var listType = type.listType();
        var b = new JavaBytecodeBuilder(loc);
        var list = b.reg(selfRes.requireValue("selfDyn"));
        var index = b.reg(indexRes.requireValue("toDynamic index"));
        var raw = b.invokeVirtual(listType.storeName, "get", listType.getDescriptor(xx), list, index);
        return EResult.ok(DynUtil.regToValue(b.code(), listType.elementDecode(xx, b, raw), listType.elementType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
