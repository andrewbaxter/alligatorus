package compilerxyz.eval.value.empty_;

import compilerxyz.artifact.Artifact;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;

public record ValueEmpty(Type type) implements Value {
    public Type valueType() {
        return type;
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Value.exportArtifact(xx, loc, this);
    }
}
