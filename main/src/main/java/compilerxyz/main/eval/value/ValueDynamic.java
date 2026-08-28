package compilerxyz.main.eval.value;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinPath;

public record ValueDynamic(ValueDynamicSource source, Type type_) implements Value {
    public Type valueType() {
        return type_;
    }

    @BuiltinPath(value = "eval.stack", scopeId = 5013L)
    public static Value stack(Type type, long width) {
        return new ValueDynamic(new ValueDynamicSource.Expr((int) width), type);
    }

    @Override
    public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
        return Value.exportArtifact(xx, loc, this);
    }
}
