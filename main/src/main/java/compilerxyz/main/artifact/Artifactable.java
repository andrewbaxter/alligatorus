package compilerxyz.main.artifact;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6417L)
public interface Artifactable {
    EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc);

    static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Artifactable self) {
        var exporterRes = self.artifactableExporter(xx, loc);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes
                .requireValue("Artifactable::exportArtifact exporter")
                .artifactExporterExport(xx, loc, Opt.none(), self);
    }

    static EResult<Artifact> exportData(ExprContext xx, QExprId loc, Type type, Object data) {
        var exporterRes = exporterOfType(xx, loc, type);
        if (exporterRes.hasErrors()) return exporterRes.reerr();
        return exporterRes
                .requireValue("Artifactable::exportData exporter")
                .artifactExporterExport(xx, loc, Opt.some(type), data);
    }

    static EResult<Artifact> exportValue(ExprContext xx, QExprId loc, Value value) {
        Object data = value instanceof ValueObjectConst voc ? voc.data() : value;
        return exportData(xx, loc, value.valueType(), data);
    }

    static EResult<ArtifactExporter> exporterOfType(ExprContext xx, QExprId loc, Type type) {
        var constForm = GenAspectDynamicConst.getMaybe(xx, type);
        var artifactType =
                constForm.optIsSome() && GenAspectArtifact.getMaybe(xx, type).optIsNone()
                        ? constForm.optUnwrap().aspectDynamicConstConstForm()
                        : type;
        var asp = GenAspectArtifact.get(xx, loc, artifactType);
        if (asp.hasErrors()) return asp.reerr();
        return EResult.ok(
                asp.requireValue("Artifactable::exporterOfType aspect").aspectArtifactExporter());
    }
}
