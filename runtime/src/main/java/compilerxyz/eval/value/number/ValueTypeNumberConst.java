package compilerxyz.eval.value.number;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeNumberConst() implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeRoot.NumberConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeNumberConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
