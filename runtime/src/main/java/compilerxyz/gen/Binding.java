package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.TList;

@Builtin(uniqueId = 2005L, scopeId = 3005)
@BuiltinPath("eval.binding")
public interface Binding extends AspectElementUseDynamicBind {
    CodeValue bindingUse(ExprContext xx, QExprId loc, Code code);

    default EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        var useCv = bindingUse(xx, loc, Code.none(loc));
        return xx.target.targetDrop(xx, loc, useCv);
    }

    Binding bindingFork();

    EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others);

    /**
     * True when this binding and {@code other} (a fork of the same binding)
     * produce identical drop behavior. Used to decide whether two control-flow
     * forks over the same scope entry can share one drop-cleanup region.
     */
    boolean bindingEquals(ExprContext xx, QExprId loc, Binding other);

    @Override
    default Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, GenBinding.dynamicType());
    }
}
