package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;

public sealed interface RecordScopeField {
    record Dynamic(Type type) implements RecordScopeField {}

    record Error(TList<ExprErr> errors) implements RecordScopeField {}

    record Scope(Binding binding) implements RecordScopeField {}
}
