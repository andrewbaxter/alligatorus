package compilerxyz.eval.value.record;

import compilerxyz.gen.Binding;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.Type;
import compilerxyz.util.TList;

public sealed interface RecordScopeField {
  record Dynamic(Type type) implements RecordScopeField {}

  record Error(TList<ExprErr> errors) implements RecordScopeField {}

  record Scope(Binding binding) implements RecordScopeField {}
}
