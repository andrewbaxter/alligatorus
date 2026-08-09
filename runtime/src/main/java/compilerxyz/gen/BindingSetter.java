package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

/**
 * Upward channel a value uses to communicate a mutation back to the scope
 * entry it came from. A value receives a setter at {@code use} and, when its
 * {@code set} runs, hands the setter the replacement binding plus whether the
 * mutation changed drop behavior; the setter installs it and advances the
 * entry's state ids.
 *
 * <p>Composable: a type that wraps another binding creates its own setter
 * around the parent's — on set it rebuilds its composite around the new inner
 * binding and forwards to the parent, so mutations of a nested binding reach
 * the real scope entry without the inner ever naming it.
 */
@BuiltinFull(uniqueId = 2008L, scopeId = 3008)
@BuiltinPath("eval.binding_setter")
public interface BindingSetter {
    /**
     * Install {@code newBinding} as the entry's binding and advance its state
     * ({@code dropModified} also advances the drop state). Returns false when
     * no live entry backs this setter.
     */
    boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified);
}
