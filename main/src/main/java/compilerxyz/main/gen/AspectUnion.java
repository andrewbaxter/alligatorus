package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 1014L, identityId = 4014L)
public interface AspectUnion extends Aspect {
    @BuiltinFull(uniqueId = 6412L)
    record Variant(ConstKey key, Type type) {}

    TList<Variant> aspectUnionVariants();

    Opt<TList<ExprErr>> aspectUnionPrepare(ExprContext xx);

    EResult<CodeDynamicReg> aspectUnionTest(QExprId loc, JavaBytecodeReg base, int variantIndex);

    EResult<CodeDynamicReg> aspectUnionExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType);
}
