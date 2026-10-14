package compilerxyz.main.gen;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 2001L, scopeId = 3001)
@BuiltinPath("eval.value")
public interface Value {
    EResult<ArtifactExporter> valueExporter(ExprContext xx, QExprId loc);

    Type valueType();
}
