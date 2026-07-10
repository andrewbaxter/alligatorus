package compilerxyz.eval.value.bool_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueBoolConst(boolean value) implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeBoolConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ValueBoolConst::exporterAnyAsType — value is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
