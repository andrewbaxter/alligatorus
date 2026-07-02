package compilerxyz.eval.value.string;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.EResult;
import compilerxyz.eval.QExprId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;

public final class TypeStringConstExporter implements ArtifactExporter {
  public static final TypeStringConstExporter INSTANCE = new TypeStringConstExporter();

  private TypeStringConstExporter() {}

  @Override
  public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self) {
    return ExporterRoot.STRING_CONST.artifactExporterExport(xx, loc, self);
  }

  @Override
  public EResult<ExporterAny> artifactExporterImportFromData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
    return ExporterRoot.STRING_CONST.artifactExporterImportFromData(xx, loc, id, data);
  }
}
