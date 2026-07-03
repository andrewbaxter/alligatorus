package compilerxyz.eval.value;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record ValueDynamic(Source source, Type type_) implements Value {
  @Override
  public Type valueTypeOf() {
    return type_;
  }

  public sealed interface Source {

    record Binding(BindingId key) implements Source {}

    record Expr() implements Source {}

    record Field(Source base, String key) implements Source {}
  }
}
