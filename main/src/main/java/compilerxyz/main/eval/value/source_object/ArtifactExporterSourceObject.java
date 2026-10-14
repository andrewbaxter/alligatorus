package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.SourceObjectEmitter;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.util.Opt;

public record ArtifactExporterSourceObject(TypeSourceObjectDynamic type) implements ArtifactExporter {
    @Override
    public EResult<Artifact> artifactExporterExport(ExprContext xx, QExprId loc, Object self) {
        Object underlyingHost;
        try {
            underlyingHost = self.getClass()
                    .getField(SourceObjectEmitter.DATA_INSTANCE_FIELD)
                    .get(self);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new IllegalStateException(
                    "ArtifactExporterSourceObject: " + self.getClass().getName() + " is not an instance of "
                            + type.typeDesc(),
                    e);
        }
        var erb = new Erb(loc);
        var typeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type));
        var underArt = erb.mustErMaybeUnreachable(Exporters.exportData(
                xx, loc, TypeForms.constForm(xx, type.underlyingType.optUnwrap()), underlyingHost));
        if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
        var body = new ValueSourceObjectConst.ArtifactValueSourceObject(typeArt.optUnwrap(), underArt.optUnwrap());
        return EResult.ok(new Artifact.Body(ExporterRoot.SOURCE_OBJECT_VALUE_CONST.body(
                ValueSourceObjectConstArtifactValueSourceObjectCodec.toValue(body))));
    }

    @Override
    public EResult<Object> artifactExporterImportFromData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
        return ExporterRoot.SOURCE_OBJECT_VALUE_CONST.artifactExporterImportFromData(xx, loc, id, data);
    }
}
