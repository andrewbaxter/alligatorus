package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.TList;

public record AspectUnionObjectDynamic(TypeObjectDynamic type) implements AspectUnion {

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : type.unionVariants()) out.add(new AspectUnion.Variant(v.key(), v.type()));
        return out;
    }

    @Override
    public EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var v = type.unionVariants().get(variantIndex);
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionObjectDynamic::test base"));
        return EResult.ok(DynUtil.regToValue(b.code(), b.instanceOf(v.klass(), baseReg), new TypeBoolDynamic()));
    }

    @Override
    public EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var v = type.unionVariants().get(variantIndex);
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionObjectDynamic::extract base"));
        return EResult.ok(DynUtil.regToValue(b.code(), b.checkcast(v.klassDesc(), baseReg), v.type()));
    }
}
