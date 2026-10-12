package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.BindingSlot;
import compilerxyz.main.eval.BindingSlotDynamic;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.Binding;

public final class AspectInitialBindUniqueDynamic implements AspectInitialBind {
    public static final AspectInitialBindUniqueDynamic INSTANCE = new AspectInitialBindUniqueDynamic();

    private AspectInitialBindUniqueDynamic() {}

    @Override
    public EResult<Binding> aspectInitialBindInitialBind(ExprContext xx, QExprId loc, BindingSlot slot) {
        var dynamicKey = BindingSlotDynamic.idOf(slot);
        return EResult.ok(new BindingDynamicBinding(dynamicKey, new TypeUniqueDynamic()));
    }
}
