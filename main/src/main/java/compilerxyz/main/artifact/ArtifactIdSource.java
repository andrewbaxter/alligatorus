package compilerxyz.main.artifact;

import compilerxyz.main.eval.CompileId;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6416L)
@BuiltinPath("types.artifact_id_source")
public record ArtifactIdSource(CompileId compileId, long index) {}
