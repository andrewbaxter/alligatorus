package compilerxyz.eval.value.tuple;

import compilerxyz.eval.ExprErr;
import compilerxyz.eval.Type;
import compilerxyz.gen.Binding;
import compilerxyz.util.TList;

public sealed interface TupleScopeField {
    record Dynamic(Type type) implements TupleScopeField {}

    record Error(TList<ExprErr> errors) implements TupleScopeField {}

    record Scope(Binding binding) implements TupleScopeField {}
}
