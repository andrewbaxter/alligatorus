package compilerxyz.gen;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;

@Builtin(uniqueId = 2001L, scopeId = 3001)
@compilerxyz.annotations.BuiltinPath("eval.value")
@compilerxyz.annotations.BuiltinTypeId(7109L)
public interface Value extends compilerxyz.artifact.Artifactable {
    @compilerxyz.annotations.SourceExposed
    Type valueType();

    @Override
    default compilerxyz.eval.EResult<compilerxyz.artifact.Artifact> exportArtifact(
        compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc) {
      var asp = compilerxyz.gen.GenAspectArtifact.get(xx, loc, valueType());
      if (asp.hasErrors()) return asp.reerr();
      var exporter = asp.requireValue("Value::exportArtifact aspect").aspectArtifactExporter();
      return exporter.artifactExporterExport(xx, loc,
          compilerxyz.artifact.ArtifactSupport.valueAsExporterAny(this));
    }
}
