package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.Artifactable;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;

@BuiltinFull(uniqueId = 2001L, scopeId = 3001)
@BuiltinPath("eval.value")
public interface Value extends Artifactable {
    Type valueType();

    static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Value self) {
      var asp = GenAspectArtifact.get(xx, loc, self.valueType());
      if (asp.hasErrors()) return asp.reerr();
      var exporter = asp.requireValue("Value::artifactableExportArtifact aspect").aspectArtifactExporter();
      return exporter.artifactExporterExport(xx, loc, ArtifactSupport.valueAsExporterAny(self));
    }
}
