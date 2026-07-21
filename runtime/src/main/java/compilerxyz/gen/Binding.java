package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTypeId;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.util.TList;

@Builtin(uniqueId = 2005L, scopeId = 3005)
@BuiltinPath("eval.binding")
@BuiltinTypeId(7025L)
public interface Binding {
    @SourceExposed
    CodeValue bindingUse(ExprContext xx, QExprId loc, Code code);

    default EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        var useCv = bindingUse(xx, loc, Code.none(loc));
        return xx.target.targetDrop(xx, loc, useCv);
    }

    Binding bindingFork();

    EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others);
}
