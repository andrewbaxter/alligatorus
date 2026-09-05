package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

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
        xx.vx.definitionSets.putIfAbsent(type.type().set.defSetId, type.type().set);
        var variantBinaryName =
                SourceUnionEmitter.variantBinaryNameFor(type.type().baseBinaryName, type.variantIndex());
        var erb = new Erb(loc);
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
        if (convertedOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var converted = convertedOpt.optUnwrap();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var underlyingJsRes = consumeRes
                .requireValue("TypeSourceUnionNewDynamic::call consume")
                .aspectConsumeDynamicToDynamic(xx, loc, converted);
        if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
        var underlyingJs = underlyingJsRes.requireValue("TypeSourceUnionNewDynamic::call toDynamic");
        Type resultType = type.type().withActiveVariants(TList.of(type.variantIndex()));
        var variantInternal = JavaInternalName.of(variantBinaryName.replace('.', '/'));
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, underlyingType);
        var ctorDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(payloadDesc));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.newInstance(loc, variantInternal));
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(underlyingJs);
        seq.add(JavaBytecodeUtils.invokeSpecial(loc, variantInternal, "<init>", ctorDesc));
        var newObject = new CodeDynamic(loc, seq);
        return EResult.ok(DynUtil.dynamicStackToValue(xx, newObject, resultType));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
