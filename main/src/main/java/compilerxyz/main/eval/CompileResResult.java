package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.prelude.util.TList;

public sealed interface CompileResResult {
    record Err(TList<ExprErr> errors) implements CompileResResult {}

    record Ok(Artifact artifact) implements CompileResResult {}
}
