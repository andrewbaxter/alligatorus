package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1014L, identityId = 4014L)
@BuiltinPath("eval.aspect_union")
public interface AspectUnion extends Aspect {
    @BuiltinFull(uniqueId = 6412L)
    @BuiltinPath("types.aspect_union_variant")
    record Variant(ConstKey key, Type type) {}

    TList<Variant> aspectUnionVariants();

    EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex);

    EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex);
}
