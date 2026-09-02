package compilerxyz.main.eval.value.empty_;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
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
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
