package compilerxyz.eval.binding;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.gen.Binding;
import compilerxyz.gen.BindingSetter;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;

public record BindingIteratorStack() implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
        throw new IllegalStateException(
                "BindingIteratorStack: iterator is a stack-only scope entry, never resolved by name");
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return EResult.ok(Code.of(JavaBytecodeUtils.inst(loc, Opcodes.POP)));
    }

    @Override
    public Binding bindingFork() {
        return this;
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        for (var other : others) {
            if (!this.equals(other)) {
                return EResult.err(
                        loc,
                        "cannot merge " + getClass().getSimpleName()
                                + " with a differently-typed binding across branches");
            }
        }
        return EResult.ok(this);
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return Binding.elementUseDynamicBind(dynamicKey);
    }
}
