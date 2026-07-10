package compilerxyz.eval.value.any;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public record ValueAnyConst(Type inner) implements Value, ExporterAny {
  @Override
  public Type valueTypeOf() {
    return compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    throw new UnsupportedOperationException(
        "ValueAnyConst::exporterAnyAsType: a wrapped-any value is not itself a type");
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
