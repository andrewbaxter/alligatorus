package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;

public interface MatchDynamic {

    EResult<Void> matchDynamicPrepare(ExprContext xx);

    EResult<CodeDynamicReg> matchDynamicTest(QExprId loc, JavaBytecodeReg base, int variantIndex);

    EResult<CodeDynamicReg> matchDynamicExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType);
}
