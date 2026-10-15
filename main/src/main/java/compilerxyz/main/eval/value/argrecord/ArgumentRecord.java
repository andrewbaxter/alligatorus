package compilerxyz.main.eval.value.argrecord;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.TOrderedMap;

public record ArgumentRecord(TOrderedMap<ConstKey, ArgumentEntry> fields, BindingSetter setter) implements Value {
    public Type valueType() {
        return TypeArgumentRecord.of(fields);
    }

    @Override
    public EResult<ArtifactExporter> valueExporter(ExprContext xx, QExprId loc) {
        return Exporters.exporterOfType(xx, loc, valueType());
    }
}
