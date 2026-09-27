package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public interface MatchDynamic {

    Opt<TList<ExprErr>> matchDynamicPrepare(ExprContext xx);

    EResult<CodeDynamicReg> matchDynamicTest(QExprId loc, JavaBytecodeReg base, int variantIndex);

    EResult<CodeDynamicReg> matchDynamicExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType);
}
