package compilerxyz.eval;

import compilerxyz.artifact.Artifact;
import compilerxyz.util.TList;

public sealed interface CompileResResult {
  record Err(TList<ExprErr> errors) implements CompileResResult {}

  record Ok(Artifact artifact) implements CompileResResult {}
}
