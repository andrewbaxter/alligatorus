package compilerxyz.main.langhelp;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.Artifacts;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.builtin.App;
import compilerxyz.main.eval.value.object.ArtifactValueObjectConst;
import compilerxyz.main.gen.codec.ArtifactAppCodec;
import compilerxyz.main.gen.codec.ArtifactValueObjectConstCodec;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.Result;

public final class ArtifactResolve {
    private ArtifactResolve() {}

    public static Result<ArtifactBody, String> artifactResolveBody(Artifacts artifacts, Artifact artifact) {
        return switch (artifact) {
            case Artifact.Body b -> Result.ok(b.body());
            case Artifact.Ref r -> artifacts.artifactsGet(r.id());
        };
    }

    public static Result<ExporterRoot, String> artifactResolveExporter(ArtifactBody body) {
        return switch (body.exporter()) {
            case ArtifactBodyExporter.Root ignored -> Result.ok(ExporterRoot.ROOT);
            case ArtifactBodyExporter.SourceRef sr -> Result.err("artifact exported by a source exporter");
            case ArtifactBodyExporter.Body inner -> {
                if (!(inner.inner().data() instanceof JsonValueString tag)) {
                    yield Result.err("artifact exporter tag is not a string");
                }
                var found = ExporterRoot.fromTag(tag.value());
                if (found.optIsNone()) yield Result.err("unknown artifact exporter tag " + tag.value());
                yield Result.ok(found.optUnwrap());
            }
        };
    }

    public static Result<App, String> artifactResolveApp(Artifacts artifacts, Artifact artifact) {
        ArtifactBody outer;
        switch (artifactResolveBody(artifacts, artifact)) {
            case Result.Ok<ArtifactBody, String> ok -> outer = ok.value();
            case Result.Err<ArtifactBody, String> err -> {
                return Result.err(err.error());
            }
        }
        ExporterRoot outerExporter;
        switch (artifactResolveExporter(outer)) {
            case Result.Ok<ExporterRoot, String> ok -> outerExporter = ok.value();
            case Result.Err<ExporterRoot, String> err -> {
                return Result.err(err.error());
            }
        }
        if (outerExporter != ExporterRoot.OBJECT_CONST) {
            return Result.err("root export is not an app (exporter " + outerExporter + ")");
        }
        ArtifactValueObjectConst object;
        try {
            object = ArtifactValueObjectConstCodec.fromValue(outer.data());
        } catch (RuntimeException e) {
            return Result.err("root export object is malformed: " + e.getMessage());
        }
        ArtifactBody data;
        switch (artifactResolveBody(artifacts, object.data())) {
            case Result.Ok<ArtifactBody, String> ok -> data = ok.value();
            case Result.Err<ArtifactBody, String> err -> {
                return Result.err(err.error());
            }
        }
        ExporterRoot dataExporter;
        switch (artifactResolveExporter(data)) {
            case Result.Ok<ExporterRoot, String> ok -> dataExporter = ok.value();
            case Result.Err<ExporterRoot, String> err -> {
                return Result.err(err.error());
            }
        }
        if (dataExporter != ExporterRoot.APP) {
            return Result.err("root export is not an app (object exporter " + dataExporter + ")");
        }
        try {
            return Result.ok(App.fromArtifactApp(ArtifactAppCodec.fromValue(data.data())));
        } catch (RuntimeException e) {
            return Result.err("root export app is malformed: " + e.getMessage());
        }
    }
}
