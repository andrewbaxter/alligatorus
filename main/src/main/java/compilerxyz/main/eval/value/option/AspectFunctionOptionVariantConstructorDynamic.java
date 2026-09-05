package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc.Some;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.null_.TypeNullDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionOptionVariantConstructorDynamic(TypeOptionVariantConstructorDynamic type)
        implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        Type underlyingType = type.isSome() ? type.elementType() : new TypeNullDynamic();
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var underlyingJsRes = consumeRes
                .requireValue("TypeOptionVariantConstructorDynamic::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
        var underlyingJs = underlyingJsRes.requireValue("TypeOptionVariantConstructorDynamic::call toDynamic");

        Type resultType = new TypeOptionDynamic(DynUtil.substituteBoxed(xx, type.elementType()));
        CodeDynamic newOpt;
        if (type.isSome()) {
            newOpt = JavaBytecodeUtils.instantiate(
                    loc,
                    JavaInternalName.fromClass(Opt.Some.class),
                    JavaMethodDescriptor.fromConstructorParts(TList.of(JvmConstants.DESC_OBJECT)),
                    underlyingJs);
        } else {
            newOpt = JavaBytecodeUtils.callStaticMethod(new Some(loc), GenCodegenDescriptors.OPT_NONE);
        }
        return EResult.ok(DynUtil.dynamicStackToValue(xx, newOpt, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
