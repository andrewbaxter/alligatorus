package compilerxyz.main.gen;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 2001L, scopeId = 3001)
@BuiltinPath("eval.value")
public interface Value {
    Opt<ArtifactExporter> valueArtifactExporter(ExprContext xx);

    Type valueType();
}
