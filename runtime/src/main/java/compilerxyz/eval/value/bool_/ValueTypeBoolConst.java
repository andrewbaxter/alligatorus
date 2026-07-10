package compilerxyz.eval.value.bool_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeBoolConst() implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeRoot.BoolConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeBoolConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
