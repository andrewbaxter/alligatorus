package compilerxyz.eval.value;


import compilerxyz.annotations.BuiltinPath;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.gen.Value;
import compilerxyz.eval.EResult;
import compilerxyz.artifact.Artifact;
import compilerxyz.eval.QExprId;

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
