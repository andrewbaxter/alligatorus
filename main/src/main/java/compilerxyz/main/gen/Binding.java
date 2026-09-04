package compilerxyz.main.gen;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 2005L, scopeId = 3005)
@BuiltinPath("eval.binding")
public interface Binding {
    CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code);

    EResult<Code> bindingDrop(ExprContext xx, QExprId loc);

    static EResult<Code> bindingDrop(ExprContext xx, QExprId loc, Binding self) {
        var useCv = self.bindingUse(xx, loc, new BindingSetterConsumed(), Code.none(loc));
        return Eval.dropValue(xx, loc, useCv);
    }

    Binding bindingFork();

    EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others);
}
