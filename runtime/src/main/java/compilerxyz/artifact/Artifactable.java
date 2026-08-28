package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

public interface Artifactable {
    EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc);
}
