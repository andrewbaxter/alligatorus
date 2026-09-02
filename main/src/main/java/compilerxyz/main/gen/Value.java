package compilerxyz.main.gen;

import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 2001L, scopeId = 3001)
@BuiltinPath("eval.value")
public interface Value extends Artifactable {
    Type valueType();
}
