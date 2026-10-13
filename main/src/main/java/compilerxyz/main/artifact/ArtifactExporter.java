package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6400L)
public interface ArtifactExporter {
    EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Opt<Type> ofType, Object self);

    EResult<Imported> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data);
}
