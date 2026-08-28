package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public interface ArtifactExporter {
    EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self);

    EResult<ExporterAny> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data);
}
