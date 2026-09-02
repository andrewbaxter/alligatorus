package compilerxyz.main.eval.value;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.Value;

public record ValueDynamic(ValueDynamicSource source, Type type_) implements Value {
    public Type valueType() {
        return type_;
    }

    public static Value stack(ExprContext xx, Type type) {
        return new ValueDynamic(
                new ValueDynamicSource.Expr(JvmSort.descriptor(xx, type).slotWidth()), type);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
