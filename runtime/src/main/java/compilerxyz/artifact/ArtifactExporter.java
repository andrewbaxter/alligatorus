package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;

public interface ArtifactExporter {
    EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self);

    EResult<ExporterAny> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data);
}
