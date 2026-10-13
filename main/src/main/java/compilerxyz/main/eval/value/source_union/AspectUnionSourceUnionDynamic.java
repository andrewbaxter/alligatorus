package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.TList;

public record AspectUnionSourceUnionDynamic(TypeSourceUnionDynamic type) implements AspectUnion {

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : type.variants) {
            out.add(new AspectUnion.Variant(v.tag, v.type));
        }
        return out;
    }

    @Override
    public EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionSourceUnionDynamic::test base"));
        b.defSet(type.set);
        var test = b.instanceOf(type.variantInternal(variantIndex), baseReg);
        return EResult.ok(DynUtil.regToValue(b.code(), test, new TypeBoolDynamic()));
    }

    @Override
    public EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var variantType = type.variants.get(variantIndex).type;
        var internal = type.variantInternal(variantIndex);
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, variantType);
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionSourceUnionDynamic::extract base"));
        var variant = b.checkcast(JavaDataDescriptor.fromInternalName(internal), baseReg);
        var payload = b.getField(internal, SourceUnionEmitter.PAYLOAD_FIELD, payloadDesc, variant);
        return EResult.ok(DynUtil.regToValue(b.code(), payload, variantType));
    }
}
