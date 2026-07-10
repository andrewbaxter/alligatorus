package compilerxyz.eval.value.unique;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;

public record ValueUnique(Unique unique) implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeUnique();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ValueUnique::exporterAnyAsType — value is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
