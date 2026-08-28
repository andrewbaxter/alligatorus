package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;

public interface ExporterAny {
    EResult<Type> exporterAnyAsType();

    EResult<Value> exporterAnyAsValue();
}
