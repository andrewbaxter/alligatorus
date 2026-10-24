package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.Exporters;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.eval.value.value_source_object.ValueSourceObjectConst;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;

public final class SourceObjectExport {
    private SourceObjectExport() {}

    public static final String EXPORTER_TYPE_FIELD = "_type";

    public static String exporterBinaryName(String objectBinaryName) {
        return objectBinaryName + "$Exporter";
    }

    public static final JavaDataDescriptor DESC_TYPE = JavaDataDescriptor.fromClass(TypeSourceObjectDynamic.class);
    public static final JavaInternalName INTNAME_BOOLEAN = JavaInternalName.fromClass(Boolean.class);
    public static final JavaDataDescriptor DESC_BOOLEAN = JavaDataDescriptor.fromInternalName(INTNAME_BOOLEAN);

    @CodegenDescriptor
    public static EResult<Artifact> export(
            ExprContext xx,
            QExprId loc,
            TypeSourceObjectDynamic type,
            ArtifactSupport.ArtifactIdHolder cell,
            Object underlying) {
        return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
            var erb = new Erb(loc);
            var typeArt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, type));
            var underArt = erb.mustErMaybeUnreachable(Exporters.exportData(
                    xx, loc, TypeForms.constForm(xx, type.underlyingType.optUnwrap()), underlying));
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            var body = new ValueSourceObjectConst.ArtifactValueSourceObject(typeArt.optUnwrap(), underArt.optUnwrap());
            return EResult.ok(ExporterRoot.SOURCE_OBJECT_VALUE_CONST.body(
                    ValueSourceObjectConstArtifactValueSourceObjectCodec.toValue(body)));
        });
    }

    @CodegenDescriptor
    public static EResult<Object> importInstance(
            ExprContext xx,
            QExprId loc,
            Opt<ArtifactIdSource> id,
            JsonValue data,
            SourceObjectConstructor constructor) {
        ValueSourceObjectConst.ArtifactValueSourceObject body;
        try {
            body = ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(data);
        } catch (RuntimeException e) {
            return EResult.err(
                    loc, "source object instance data: failed to parse ArtifactValueSourceObject: " + e.getMessage());
        }
        var underlyingRes = ArtifactSupport.importArtifact(xx, loc, body.underlying());
        if (underlyingRes.hasErrors()) return underlyingRes.reerr();
        var instance = constructor.sourceObjectConstructorNew(
                underlyingRes.requireValue("SourceObjectExport::import underlying"));
        if (id.optIsSome()) {
            MustCast.mustCast(SourceObjectInstance.class, instance)
                    .sourceObjectInstanceArtifactId()
                    .set(id.optUnwrap());
        }
        return EResult.ok(instance);
    }
}
