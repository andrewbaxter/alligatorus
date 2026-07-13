package compilerxyz.eval.value.object_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueObjectConst(Type type, Type inner) implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return type;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException(
        "ValueObjectConst::exporterAnyAsType: a wrapped compile-time value is not itself a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
