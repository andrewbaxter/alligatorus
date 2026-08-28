package compilerxyz.eval.dynamic;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;

public interface MatchDynamic {

    EResult<Void> matchDynamicPrepare(ExprContext xx, QExprId loc);

    EResult<CodeDynamic> matchDynamicTest(ExprContext xx, QExprId loc, int variantIndex);

    EResult<CodeDynamic> matchDynamicExtract(
            ExprContext xx, QExprId loc, int variantIndex, Type variantType);
}
