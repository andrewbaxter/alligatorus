package compilerxyz.eval.value.number;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record ValueNumberConst(double value) implements Value, ExporterAny {
  @Override
  public Type valueTypeOf() {
    return new TypeNumberConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ValueNumberConst::exporterAnyAsType — value is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
