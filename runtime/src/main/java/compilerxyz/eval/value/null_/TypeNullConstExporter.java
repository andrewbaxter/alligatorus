package compilerxyz.eval.value.null_;

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

public final class TypeNullConstExporter implements ArtifactExporter {
  public static final TypeNullConstExporter INSTANCE = new TypeNullConstExporter();

  private TypeNullConstExporter() {}

  @Override
  public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self) {
    return ExporterRoot.NULL_CONST.artifactExporterExport(xx, loc, self);
  }

  @Override
  public EResult<ExporterAny> artifactExporterImportFromData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
    return ExporterRoot.NULL_CONST.artifactExporterImportFromData(xx, loc, id, data);
  }
}
