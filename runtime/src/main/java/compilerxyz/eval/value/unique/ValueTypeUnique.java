package compilerxyz.eval.value.unique;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeUnique() implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeRoot.Unique();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeUnique());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
