package compilerxyz.main.langhelp.serve;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.builtin.ArtifactApp;
import compilerxyz.main.eval.builtin.ArtifactDir;
import compilerxyz.main.eval.value.array.ArtifactArrayValue;
import compilerxyz.main.eval.value.object.ArtifactValueObjectConst;
import compilerxyz.main.eval.value.record.ArtifactRecordType;
import compilerxyz.main.eval.value.source_object.ArtifactDataDefSetObject;
import compilerxyz.main.eval.value.value_source_object.ValueSourceObjectConst.ArtifactValueSourceObject;
import compilerxyz.main.gen.codec.ArtifactAppCodec;
import compilerxyz.main.gen.codec.ArtifactArrayValueCodec;
import compilerxyz.main.gen.codec.ArtifactDataDefSetObjectCodec;
import compilerxyz.main.gen.codec.ArtifactDirCodec;
import compilerxyz.main.gen.codec.ArtifactRecordTypeCodec;
import compilerxyz.main.gen.codec.ArtifactValueObjectConstCodec;
import compilerxyz.main.gen.codec.ValueSourceObjectConstArtifactValueSourceObjectCodec;
import compilerxyz.main.langhelp.ArtifactResolve;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.langserver.Export;
import compilerxyz.main.langserver.Source;
import compilerxyz.main.safejson.JsonValueNumberFloat;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.function.Supplier;

public final class ServeExport {
    private ServeExport() {}

    private record Resolved(ArtifactBody body, String exporter) {}

    private static Result<Resolved, String> resolve(CacheReal cache, Artifact artifact) {
        ArtifactBody body;
        switch (ArtifactResolve.artifactResolveBody(cache, artifact)) {
            case Result.Ok<ArtifactBody, String> ok -> body = ok.value();
            case Result.Err<ArtifactBody, String> err -> {
                return Result.err(err.error());
            }
        }
        return switch (body.exporter()) {
            case ArtifactBodyExporter.SourceRef ignored -> Result.ok(new Resolved(body, "source_ref"));
            case ArtifactBodyExporter.Root ignored ->
                Result.ok(new Resolved(body, ExporterRoot.ROOT.exporterRootTag()));
            case ArtifactBodyExporter.Body ignored ->
                switch (ArtifactResolve.artifactResolveExporter(body)) {
                    case Result.Ok<ExporterRoot, String> ok ->
                        Result.ok(new Resolved(body, ok.value().exporterRootTag()));
                    case Result.Err<ExporterRoot, String> err -> Result.err(err.error());
                };
        };
    }

    private static <T> Result<T, String> decode(String what, Supplier<T> decode) {
        try {
            return Result.ok(decode.get());
        } catch (RuntimeException e) {
            return Result.err(what + " is malformed: " + e.getMessage());
        }
    }

    private static Opt<Artifact> unwrap(Resolved resolved) {
        var tag = resolved.exporter();
        if (tag.equals(ExporterRoot.OBJECT_CONST.exporterRootTag())) {
            return switch (decode(
                    "const value",
                    () -> ArtifactValueObjectConstCodec.fromValue(
                            resolved.body().data()))) {
                case Result.Ok<ArtifactValueObjectConst, String> ok ->
                    Opt.some(ok.value().data());
                case Result.Err<ArtifactValueObjectConst, String> err -> Opt.none();
            };
        }
        if (tag.equals(ExporterRoot.SOURCE_OBJECT_VALUE_CONST.exporterRootTag())
                || tag.equals(ExporterRoot.SOURCE_OBJECT_TYPE_INSTANCE.exporterRootTag())) {
            return switch (decode(
                    "source object instance",
                    () -> ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(
                            resolved.body().data()))) {
                case Result.Ok<ArtifactValueSourceObject, String> ok ->
                    Opt.some(ok.value().underlying());
                case Result.Err<ArtifactValueSourceObject, String> err -> Opt.none();
            };
        }
        return Opt.none();
    }

    public static Opt<Export> exportDescribe(CacheReal cache, Artifact artifact) {
        var current = artifact;
        while (true) {
            Resolved resolved;
            switch (resolve(cache, current)) {
                case Result.Ok<Resolved, String> ok -> resolved = ok.value();
                case Result.Err<Resolved, String> err -> {
                    return Opt.none();
                }
            }
            var tag = resolved.exporter();
            if (tag.equals(ExporterRoot.OBJECT_CONST.exporterRootTag())) {
                var inner = unwrap(resolved);
                if (inner.optIsNone()) return Opt.some(new Export(tag, Opt.none(), Opt.none()));
                current = inner.optUnwrap();
                continue;
            }
            if (!tag.equals(ExporterRoot.SOURCE_OBJECT_VALUE_CONST.exporterRootTag())
                    && !tag.equals(ExporterRoot.SOURCE_OBJECT_TYPE_INSTANCE.exporterRootTag())) {
                return Opt.some(new Export(tag, Opt.none(), Opt.none()));
            }
            ArtifactValueSourceObject instance;
            switch (decode(
                    "source object instance",
                    () -> ValueSourceObjectConstArtifactValueSourceObjectCodec.fromValue(
                            resolved.body().data()))) {
                case Result.Ok<ArtifactValueSourceObject, String> ok -> instance = ok.value();
                case Result.Err<ArtifactValueSourceObject, String> err -> {
                    return Opt.some(new Export(tag, Opt.none(), Opt.none()));
                }
            }
            ArtifactBody typeBody;
            switch (ArtifactResolve.artifactResolveBody(cache, instance.typeArtifact())) {
                case Result.Ok<ArtifactBody, String> ok -> typeBody = ok.value();
                case Result.Err<ArtifactBody, String> err -> {
                    return Opt.some(new Export(tag, Opt.none(), Opt.none()));
                }
            }
            ArtifactDataDefSetObject type;
            switch (decode("source object type", () -> ArtifactDataDefSetObjectCodec.fromValue(typeBody.data()))) {
                case Result.Ok<ArtifactDataDefSetObject, String> ok -> type = ok.value();
                case Result.Err<ArtifactDataDefSetObject, String> err -> {
                    return Opt.some(new Export(tag, Opt.none(), Opt.none()));
                }
            }
            Opt<Source> typeSource =
                    switch (type.identity()) {
                        case Unique.Builtin b -> Opt.none();
                        case Unique.Source s ->
                            cache.sourcesSpecOf(s.compileId().source()).optMap(ServeCompiler::source);
                    };
            return Opt.some(new Export(tag, type.name(), typeSource));
        }
    }

    public static Result<byte[], String> exportRead(CacheReal cache, Artifact artifact, TList<String> path) {
        var current = artifact;
        var remaining = 0;
        while (true) {
            Resolved resolved;
            switch (resolve(cache, current)) {
                case Result.Ok<Resolved, String> ok -> resolved = ok.value();
                case Result.Err<Resolved, String> err -> {
                    return Result.err(err.error());
                }
            }
            var tag = resolved.exporter();
            var inner = unwrap(resolved);
            if (inner.optIsSome()) {
                current = inner.optUnwrap();
                continue;
            }
            var at = new StringBuilder("/");
            for (int i = 0; i < remaining; i++) {
                if (i > 0) at.append("/");
                at.append(path.get(i));
            }
            if (remaining < path.size()) {
                var segment = path.get(remaining);
                remaining += 1;
                if (tag.equals(ExporterRoot.RECORD.exporterRootTag())) {
                    ArtifactRecordType record;
                    switch (decode(
                            "record",
                            () -> ArtifactRecordTypeCodec.fromValue(
                                    resolved.body().data()))) {
                        case Result.Ok<ArtifactRecordType, String> ok -> record = ok.value();
                        case Result.Err<ArtifactRecordType, String> err -> {
                            return Result.err(err.error());
                        }
                    }
                    var field = record.fields().getOpt(new ConstKey.Str(segment));
                    if (field.optIsNone()) return Result.err("the record at " + at + " has no field " + segment);
                    current = field.optUnwrap().artifact();
                    continue;
                }
                if (tag.equals(ExporterRoot.DIR.exporterRootTag())) {
                    if (remaining != path.size()) {
                        return Result.err("the dir at " + at + " is reached by " + segment + ", which cannot be a dir");
                    }
                    ArtifactDir dir;
                    switch (decode(
                            "dir",
                            () -> ArtifactDirCodec.fromValue(resolved.body().data()))) {
                        case Result.Ok<ArtifactDir, String> ok -> dir = ok.value();
                        case Result.Err<ArtifactDir, String> err -> {
                            return Result.err(err.error());
                        }
                    }
                    for (var entry : dir.entries()) {
                        if (entry.path().equals(segment)) {
                            return Result.ok(Base64.getDecoder().decode(entry.bytesBase64()));
                        }
                    }
                    return Result.err("the dir at " + at + " has no entry " + segment);
                }
                return Result.err("the export at " + at + " is " + tag + ", which has no " + segment);
            }
            if (tag.equals(ExporterRoot.APP.exporterRootTag())) {
                return switch (decode(
                        "app", () -> ArtifactAppCodec.fromValue(resolved.body().data()))) {
                    case Result.Ok<ArtifactApp, String> ok ->
                        Result.ok(Base64.getDecoder().decode(ok.value().bytesBase64()));
                    case Result.Err<ArtifactApp, String> err -> Result.err(err.error());
                };
            }
            if (tag.equals(ExporterRoot.STRING_CONST.exporterRootTag())) {
                return switch (resolved.body().data()) {
                    case JsonValueString s -> Result.ok(s.value().getBytes(StandardCharsets.UTF_8));
                    default -> Result.err("the string at " + at + " is malformed");
                };
            }
            if (tag.equals(ExporterRoot.ARRAY_CONST.exporterRootTag())) {
                ArtifactArrayValue array;
                switch (decode(
                        "array",
                        () -> ArtifactArrayValueCodec.fromValue(resolved.body().data()))) {
                    case Result.Ok<ArtifactArrayValue, String> ok -> array = ok.value();
                    case Result.Err<ArtifactArrayValue, String> err -> {
                        return Result.err(err.error());
                    }
                }
                var out = new byte[array.elements().size()];
                for (int i = 0; i < out.length; i++) {
                    Resolved element;
                    switch (resolve(cache, array.elements().get(i))) {
                        case Result.Ok<Resolved, String> ok -> element = ok.value();
                        case Result.Err<Resolved, String> err -> {
                            return Result.err(err.error());
                        }
                    }
                    var elementTag = element.exporter();
                    if (!elementTag.equals(ExporterRoot.BYTE_CONST.exporterRootTag())
                            && !elementTag.equals(ExporterRoot.UBYTE_CONST.exporterRootTag())) {
                        return Result.err("the array at " + at + " holds " + elementTag + ", not bytes");
                    }
                    switch (element.body().data()) {
                        case JsonValueNumberInt n -> out[i] = (byte) n.value();
                        case JsonValueNumberFloat n -> out[i] = (byte) (long) n.value();
                        default -> {
                            return Result.err("the byte array at " + at + " is malformed");
                        }
                    }
                }
                return Result.ok(out);
            }
            return Result.err("the export at " + at + " is " + tag + ", which has no bytes");
        }
    }
}
