package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.eval.Value;

public interface ExporterAny {
  EResult<Type> exporterAnyAsType();

  EResult<Value> exporterAnyAsValue();
}
