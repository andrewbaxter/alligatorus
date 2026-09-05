package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenLists;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionListConstructorConst implements AspectFunction {
    public static final AspectFunctionListConstructorConst INSTANCE = new AspectFunctionListConstructorConst();

    private AspectFunctionListConstructorConst() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeListConstructorConst::call: argument must have a compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var argMeta = argValue.valueType();
        var aspectTypeRes = GenAspectType.get(xx, loc, argMeta);
        if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
        var elementType = aspectTypeRes
                .requireValue("TypeListConstructorConst::call AspectType")
                .aspectTypeType(argValue);
        Type listType = GenLists.listDynamic(xx, elementType);
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), listType))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
