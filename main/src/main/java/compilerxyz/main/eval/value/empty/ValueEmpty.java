package compilerxyz.main.eval.value.empty;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Exporters;
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
    public EResult<ArtifactExporter> valueExporter(ExprContext xx, QExprId loc) {
        return Exporters.exporterOfType(xx, loc, type);
    }
}
