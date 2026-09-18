package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.util.List;

public record AspectFunctionListPushDynamic(TypeListPushDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var convertedRes = Eval.typecheckConvert(xx, loc, type.elementType(), argument);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var converted = convertedRes.requireValue("typecheckConvert");
        var argRes = DynUtil.materialize(xx, loc, converted);
        if (argRes.hasErrors()) return argRes.reerr();
        var selfRes = DynUtil.valueReg(loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var listName = JavaInternalName.fromClass(List.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var addDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));
        var b = new JavaBytecodeBuilder(loc);
        var selfReg = b.reg(selfRes.requireValue("selfDyn"));
        var argReg = b.reg(argRes.requireValue("toDynamic"));
        b.invokeInterface(listName, "add", addDesc, selfReg, argReg);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
