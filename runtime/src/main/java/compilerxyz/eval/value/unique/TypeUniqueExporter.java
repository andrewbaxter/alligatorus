package compilerxyz.eval.value.unique;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;

public final class TypeUniqueExporter implements ArtifactExporter {
  public static final TypeUniqueExporter INSTANCE = new TypeUniqueExporter();

  private TypeUniqueExporter() {}

  @Override
  public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self) {
    return ExporterRoot.UNIQUE.artifactExporterExport(xx, loc, self);
  }

  @Override
  public EResult<ExporterAny> artifactExporterImportFromData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
    return ExporterRoot.UNIQUE.artifactExporterImportFromData(xx, loc, id, data);
  }
}
