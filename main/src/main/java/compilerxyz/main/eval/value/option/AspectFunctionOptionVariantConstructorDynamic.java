package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenOptions;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionOptionVariantConstructorDynamic(TypeOptionVariantConstructorDynamic type)
        implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        Type underlyingType = type.isSome() ? type.elementType() : new Tuple(Opt.none(), IList.of());
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var underlyingRes = DynUtil.materialize(xx, loc, converted);
        if (underlyingRes.hasErrors()) return underlyingRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var underlying = b.reg(underlyingRes.requireValue("TypeOptionVariantConstructorDynamic::call underlying"));
        var resultType = GenOptions.optionDynamic(xx, type.elementType());
        var newOpt = b.reg(resultType.optionDynamicConstruct(
                xx, loc, type.isSome() ? Opt.some(underlying) : Opt.<JavaBytecodeReg>none()));
        return EResult.ok(DynUtil.regToValue(b.code(), newOpt, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
