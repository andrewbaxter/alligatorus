package compilerxyz.main.gen;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

@BuiltinFull(uniqueId = 2005L, scopeId = 3005)
@BuiltinPath("eval.binding")
public interface Binding extends AspectElementUseDynamicBind {
    CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code);

    EResult<Code> bindingDrop(ExprContext xx, QExprId loc);

    static EResult<Code> bindingDrop(ExprContext xx, QExprId loc, Binding self) {
        var useCv = self.bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc));
        return Eval.dropValue(xx, loc, useCv);
    }

    Binding bindingFork();

    EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others);

    static Binding elementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, GenBinding.dynamicType());
    }
}
