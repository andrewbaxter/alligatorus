package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionSourceObjectNewDynamic(TypeSourceObjectNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        var set = type.type().set;
        if (set == null) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::call: object has no definition set reference");
        }
        xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
        if (type.type().underlyingType.optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::call: object underlying type not finalised");
        }
        Type underlyingType = type.type().underlyingType.optUnwrap();
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var underlyingJsRes = consumeRes
                .requireValue("TypeSourceObjectNewDynamic::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
        var underlyingJs = underlyingJsRes.requireValue("TypeSourceObjectNewDynamic::call toDynamic");
        Type resultType = type.type();

        var objInternalName = type.type().internalName();
        var klass = JavaInternalName.of(objInternalName);
        var underlyingDesc = consumeRes
                .requireValue("TypeSourceObjectNewDynamic::call underlying desc")
                .aspectConsumeDynamicJvmClass(xx);
        var ctorDesc = JavaMethodDescriptor.fromConstructorParts(TList.of(underlyingDesc));
        var newInstance = JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
        return EResult.ok(DynUtil.dynamicStackToValue(xx, newInstance, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
