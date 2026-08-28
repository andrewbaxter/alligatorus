package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;

public interface Artifactable {
    EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc);
}
