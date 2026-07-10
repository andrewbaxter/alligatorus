package compilerxyz.eval.value.null_;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;
import compilerxyz.eval.value.TypeRoot;

public record ValueTypeNullConst() implements Value, ExporterAny {
  @Override
  public Type valueType() {
    return new TypeRoot.NullConst();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeNullConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
