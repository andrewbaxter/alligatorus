package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionSourceUnionNewDynamic(TypeSourceUnionNewDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
        if (type.variantIndex() < 0
                || type.variantIndex() >= type.type().variants.size()) {
            return EResult.err(
                    loc, "TypeSourceUnionNewDynamic::call: variant index " + type.variantIndex() + " out of range");
        }
        var variant = type.type().variants.get(type.variantIndex());
        Type underlyingType = variant.type;
        if (underlyingType == null) {
            return EResult.err(loc, "TypeSourceUnionNewDynamic::call: variant payload type not finalised");
        }
        var variantBinaryName =
                SourceUnionEmitter.variantBinaryNameFor(type.type().baseBinaryName, type.variantIndex());
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var underlyingRes = DynUtil.materialize(xx, loc, converted);
        if (underlyingRes.hasErrors()) return underlyingRes.reerr();
        Type resultType = type.type().withActiveVariants(TList.of(type.variantIndex()));
        var variantInternal = JavaInternalName.of(variantBinaryName.replace('.', '/'));
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, underlyingType);
        var ctorDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(payloadDesc));
        var b = new JavaBytecodeBuilder(loc);
        b.defSet(type.type().set);
        var out = b.newInstance(
                variantInternal,
                ctorDesc,
                b.reg(underlyingRes.requireValue("TypeSourceUnionNewDynamic::call underlying")));
        return EResult.ok(DynUtil.regToValue(b.code(), out, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
