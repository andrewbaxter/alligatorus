package compilerxyz.eval.value.any;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;

public final class ExporterAnyDelegate implements ArtifactExporter {
  public static final ExporterAnyDelegate INSTANCE = new ExporterAnyDelegate();

  private ExporterAnyDelegate() {}

  @Override
  public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, ExporterAny self) {
    if (!(self instanceof ValueAnyConst wrapper)) {
      return EResult.err(loc, "ExporterAnyDelegate: expected ValueAnyConst, got " + self.getClass());
    }
    return ArtifactSupport.exportArtifact(xx, loc, wrapper.inner());
  }

  @Override
  public EResult<ExporterAny> artifactExporterImportFromData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
    return EResult.err(loc, "ExporterAnyDelegate::import should not be invoked");
  }
}
