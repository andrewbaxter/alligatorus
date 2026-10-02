package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public record ValueEResultConst<T extends Type>(TypeEResultConst<T> type, Result<Value, TList<Value>> result)
        implements Value {
    @Override
    public Type valueType() {
        return type;
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, type);
    }
}
