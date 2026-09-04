package compilerxyz.main.eval.binding;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record BindingIteratorStack() implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        throw new IllegalStateException(
                "BindingIteratorStack: iterator is a stack-only scope entry, never resolved by name");
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return EResult.ok(Code.target(JavaBytecodeUtils.inst(loc, Opcodes.POP)));
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
}
