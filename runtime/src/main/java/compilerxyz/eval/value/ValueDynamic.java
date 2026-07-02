package compilerxyz.eval.value;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.dynamic.CodeDynamic;

public record ValueDynamic(Source source, Type type_) implements Value {
  @Override
  public Type valueTypeOf() {
    return type_;
  }

  public sealed interface Source {

    record Binding(BindingId key) implements Source {}

    record Expr() implements Source {}

    record Field(CodeDynamic base, String key) implements Source {}

    record MethodField(CodeDynamic base, String key) implements Source {}
  }
}
