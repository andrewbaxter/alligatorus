package compilerxyz.main.eval.value.empty_;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;

public record ValueEmpty(Type type) implements Value {
    public Type valueType() {
        return type;
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Value.exportArtifact(xx, loc, this);
    }
}
