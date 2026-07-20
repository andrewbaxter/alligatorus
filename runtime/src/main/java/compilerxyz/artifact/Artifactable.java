package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;

/**
 * Marks an object that knows how to export itself as an {@link Artifact}. Implementations
 * delegate to their associated {@link ArtifactExporter}. Unlike the removed
 * {@code ArtifactSupport.exportArtifact(Value)}, this doesn't require the object to be a
 * {@link compilerxyz.gen.Value} — Types can implement Artifactable directly without wrapping.
 */
public interface Artifactable {
  EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc);
}
