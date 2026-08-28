package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;

public sealed interface TupleScopeField {
    record Dynamic(Type type) implements TupleScopeField {}

    record Error(TList<ExprErr> errors) implements TupleScopeField {}

    record Scope(Binding binding) implements TupleScopeField {}
}
