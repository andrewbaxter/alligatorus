package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionArrayConstructorConst implements AspectFunction {
    public static final AspectFunctionArrayConstructorConst INSTANCE = new AspectFunctionArrayConstructorConst();

    private AspectFunctionArrayConstructorConst() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "TypeArrayConstructorConst::call: argument must have a compile-time value");
        }
        var argValue = argument.value().optUnwrap();
        var aspectTypeRes = GenAspectType.get(xx, loc, argValue.valueType());
        if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
        var elementType = aspectTypeRes
                .requireValue("TypeArrayConstructorConst::call AspectType")
                .aspectTypeType(argValue);
        if (GenAspectConsumeDynamic.getMaybe(xx, elementType).optIsNone()) {
            return EResult.err(loc, "array element type " + elementType.typeDesc() + " has no runtime representation");
        }
        Type arrayType = new TypeArrayDynamic<>(elementType);
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), arrayType))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
