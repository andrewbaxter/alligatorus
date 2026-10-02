package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TList;

public record ValueMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type, TList<MapConstEntry> entries)
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
