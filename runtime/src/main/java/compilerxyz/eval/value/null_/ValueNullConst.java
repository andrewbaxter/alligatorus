package compilerxyz.eval.value.null_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record ValueNullConst() implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeNullConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException("ValueNullConst::exporterAnyAsType — value is not a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
