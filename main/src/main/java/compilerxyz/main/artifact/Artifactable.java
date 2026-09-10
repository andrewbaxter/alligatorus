package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6417L)
public interface Artifactable {
    EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc);

    static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Artifactable self) {
        var exporterRes = self.artifactableExporter(xx, loc);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes.requireValue("Artifactable::exportArtifact exporter").artifactExporterExport(xx, loc, self);
    }

    static EResult<ArtifactExporter> exporterOfType(ExprContext xx, QExprId loc, Type type) {
        var asp = GenAspectArtifact.get(xx, loc, type);
        if (asp.hasErrors()) return asp.reerr();
        return EResult.ok(
                asp.requireValue("Artifactable::exporterOfType aspect").aspectArtifactExporter());
    }
}
