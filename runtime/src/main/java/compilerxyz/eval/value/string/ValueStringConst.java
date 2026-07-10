package compilerxyz.eval.value.string;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record ValueStringConst(String value) implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeStringConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ValueStringConst::exporterAnyAsType — value is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
