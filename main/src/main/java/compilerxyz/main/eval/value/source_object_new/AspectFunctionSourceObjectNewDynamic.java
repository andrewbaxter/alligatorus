package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectFunction;
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
        var underlyingRes = DynUtil.materialize(xx, loc, converted);
        if (underlyingRes.hasErrors()) return underlyingRes.reerr();
        Type resultType = type.type();
        var klass = JavaInternalName.of(type.type().internalName());
        var underlyingDesc = JvmSort.descriptor(xx, underlyingType);
        var ctorDesc = JavaMethodDescriptor.fromConstructorParts(TList.of(underlyingDesc));
        var b = new JavaBytecodeBuilder(loc);
        var out = b.newInstance(
                klass, ctorDesc, b.reg(underlyingRes.requireValue("TypeSourceObjectNewDynamic::call underlying")));
        return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
