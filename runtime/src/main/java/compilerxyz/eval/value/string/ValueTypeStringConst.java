package compilerxyz.eval.value.string;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeStringConst() implements Value, ExporterAny {
  @Override
  public Type valueTypeOf() {
    return new TypeRoot.StringConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeStringConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
