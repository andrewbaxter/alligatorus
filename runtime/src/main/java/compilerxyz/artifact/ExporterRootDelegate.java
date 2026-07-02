package compilerxyz.artifact;

import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import java.util.function.Function;

public interface ExporterRootDelegate {
  EResult<Artifact> exporterRootDelegateSelfExport(
      ExprContext xx,
      QExprId loc,
      ExporterAny self,
      Function<JsonValue, ArtifactBody> makeBody);

  EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
      ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data);
}
