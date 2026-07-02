package compilerxyz.eval.value.any;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeAnyConst() implements Value, ExporterAny {
  @Override
  public Type valueTypeOf() {
    return new TypeRoot.AnyConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeAnyConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
