package compilerxyz.main.artifact;

import compilerxyz.main.eval.CompileId;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6416L)
public record ArtifactIdSource(CompileId compileId, long index) {}
