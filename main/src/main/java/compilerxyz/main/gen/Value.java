package compilerxyz.main.gen;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 2001L, scopeId = 3001)
@BuiltinPath("eval.value")
public interface Value extends Artifactable {
    Type valueType();

    static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Value self) {
        var asp = GenAspectArtifact.get(xx, loc, self.valueType());
        if (asp.hasErrors()) return asp.reerr();
        var exporter =
                asp.requireValue("Value::artifactableExportArtifact aspect").aspectArtifactExporter();
        return exporter.artifactExporterExport(xx, loc, ArtifactSupport.valueAsExporterAny(self));
    }
}
