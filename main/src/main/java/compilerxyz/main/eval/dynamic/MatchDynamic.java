package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;

public interface MatchDynamic {

    EResult<Void> matchDynamicPrepare(ExprContext xx);

    EResult<CodeDynamic> matchDynamicTest(QExprId loc, int variantIndex);

    EResult<CodeDynamic> matchDynamicExtract(ExprContext xx, QExprId loc, int variantIndex, Type variantType);
}
