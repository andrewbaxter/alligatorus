package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public interface ExporterAny {
    EResult<Type> exporterAnyAsType();

    EResult<Value> exporterAnyAsValue();
}
