package compilerxyz.main.artifact;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.safejson.Json.DeserializationException;
import compilerxyz.main.safejson.Json.FieldPath;
import compilerxyz.main.safejson.Json.Style;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.prelude.util.Opt;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeMap;

public final class ArtifactCodecs {
    private ArtifactCodecs() {}

    public static final class ArtifactCodec {
        private ArtifactCodec() {}

        public static void encode(JsonWriter writer, Artifact value, Style style) throws IOException {
            writer.beginObject();
            switch (value) {
                case Artifact.Body b -> {
                    writer.name("body");
                    ArtifactBodyCodec.encode(writer, b.body(), style);
                }
                case Artifact.Ref r -> {
                    writer.name("ref");
                    ArtifactIdSourceCodec.encode(writer, r.id());
                }
            }
            writer.endObject();
        }

        public static JsonValue toValue(Artifact value) {
            return ArtifactJson.encodeArtifact(value);
        }

        public static Artifact fromValue(JsonValue value, FieldPath path) {
            try {
                return ArtifactJson.decodeArtifact(value);
            } catch (IllegalStateException e) {
                throw new DeserializationException(String.valueOf(e.getMessage()), path, e);
            }
        }

        public static Artifact decode(JsonReader reader, FieldPath path) throws IOException {
            reader.beginObject();
            if (!reader.hasNext()) {
                throw new DeserializationException("Empty object for Artifact sealed variant", path);
            }
            var tag = reader.nextName();
            var variantPath = path.field(tag);
            Artifact result;
            switch (tag) {
                case "body" -> result = new Artifact.Body(ArtifactBodyCodec.decode(reader, variantPath));
                case "ref" -> result = new Artifact.Ref(ArtifactIdSourceCodec.decode(reader, variantPath));
                default -> throw new DeserializationException("Unknown Artifact variant: '" + tag + "'", path);
            }
            if (reader.hasNext()) {
                throw new DeserializationException("Artifact sealed object must have exactly one variant", path);
            }
            reader.endObject();
            return result;
        }
    }

    public static final class ArtifactBodyCodec {
        private ArtifactBodyCodec() {}

        public static void encode(JsonWriter writer, ArtifactBody value, Style style) throws IOException {
            writer.beginObject();
            if (style == Style.CANONICAL) {

                var sorted = new TreeMap<String, Runnable>(Comparator.naturalOrder());
                sorted.put("data", () -> {
                    try {
                        writer.name("data");
                        JsonValues.encode(writer, value.data(), style);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                sorted.put("exporter", () -> {
                    try {
                        writer.name("exporter");
                        ArtifactBodyExporterCodec.encode(writer, value.exporter(), style);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                for (var entry : sorted.entrySet()) {
                    entry.getValue().run();
                }
            } else {
                writer.name("data");
                JsonValues.encode(writer, value.data(), style);
                writer.name("exporter");
                ArtifactBodyExporterCodec.encode(writer, value.exporter(), style);
            }
            writer.endObject();
        }

        public static JsonValue toValue(ArtifactBody value) {
            return ArtifactJson.encodeArtifactBody(value);
        }

        public static ArtifactBody fromValue(JsonValue value, FieldPath path) {
            try {
                return ArtifactJson.decodeArtifactBody(value);
            } catch (IllegalStateException e) {
                throw new DeserializationException(String.valueOf(e.getMessage()), path, e);
            }
        }

        public static ArtifactBody decode(JsonReader reader, FieldPath path) throws IOException {
            Opt<JsonValue> data = Opt.none();
            Opt<ArtifactBodyExporter> exporter = Opt.none();
            reader.beginObject();
            while (reader.hasNext()) {
                var fname = reader.nextName();
                switch (fname) {
                    case "data" -> {
                        data = Opt.some(JsonValues.decode(reader, path.field("data")));
                    }
                    case "exporter" -> {
                        var exporterPath = path.field("exporter");
                        reader.beginObject();
                        if (!reader.hasNext()) {
                            throw new DeserializationException(
                                    "Empty object for ArtifactBodyExporter sealed variant", exporterPath);
                        }
                        var exporterTag = reader.nextName();
                        var exporterVariantPath = exporterPath.field(exporterTag);
                        switch (exporterTag) {
                            case "body" ->
                                exporter = Opt.some(new ArtifactBodyExporter.Body(decode(reader, exporterVariantPath)));
                            case "root" -> {
                                reader.beginObject();
                                if (reader.hasNext()) {
                                    throw new DeserializationException(
                                            "Unexpected field in ArtifactBodyExporter.Root payload",
                                            exporterVariantPath);
                                }
                                reader.endObject();
                                exporter = Opt.some(new ArtifactBodyExporter.Root());
                            }
                            case "source_ref" ->
                                exporter = Opt.some(new ArtifactBodyExporter.SourceRef(
                                        ArtifactIdSourceCodec.decode(reader, exporterVariantPath)));
                            default ->
                                throw new DeserializationException(
                                        "Unknown ArtifactBodyExporter variant: '" + exporterTag + "'", exporterPath);
                        }
                        if (reader.hasNext()) {
                            throw new DeserializationException(
                                    "ArtifactBodyExporter sealed object must have exactly one variant", exporterPath);
                        }
                        reader.endObject();
                    }
                    default ->
                        throw new DeserializationException(
                                "Unknown field '" + fname + "' in ArtifactBody", path.field(fname));
                }
            }
            reader.endObject();
            if (data.optIsNone()) {
                throw new DeserializationException("Missing required field 'data' in ArtifactBody", path.field("data"));
            }
            if (exporter.optIsNone()) {
                throw new DeserializationException(
                        "Missing required field 'exporter' in ArtifactBody", path.field("exporter"));
            }
            return new ArtifactBody(data.optUnwrap(), exporter.optUnwrap());
        }
    }

    public static final class ArtifactBodyExporterCodec {
        private ArtifactBodyExporterCodec() {}

        public static void encode(JsonWriter writer, ArtifactBodyExporter value, Style style) throws IOException {
            writer.beginObject();
            switch (value) {
                case ArtifactBodyExporter.Body b -> {
                    writer.name("body");
                    ArtifactBodyCodec.encode(writer, b.inner(), style);
                }
                case ArtifactBodyExporter.Root r -> {
                    writer.name("root");
                    writer.beginObject();
                    writer.endObject();
                }
                case ArtifactBodyExporter.SourceRef sr -> {
                    writer.name("source_ref");
                    ArtifactIdSourceCodec.encode(writer, sr.id());
                }
            }
            writer.endObject();
        }
    }

    public static final class ArtifactIdSourceCodec {
        private ArtifactIdSourceCodec() {}

        public static void encode(JsonWriter writer, ArtifactIdSource value) throws IOException {
            writer.beginObject();

            writer.name("compile_id");
            writer.beginObject();
            writer.name("import_state");
            writer.value(value.compileId().importState().value());
            writer.name("source");
            writer.value(value.compileId().source().value());
            writer.endObject();
            writer.name("index");
            writer.value(value.index());
            writer.endObject();
        }

        public static JsonValue toValue(ArtifactIdSource value) {
            return ArtifactJson.encodeArtifactIdSource(value);
        }

        public static ArtifactIdSource fromValue(JsonValue value, FieldPath path) {
            try {
                return ArtifactJson.decodeArtifactIdSource(value);
            } catch (IllegalStateException e) {
                throw new DeserializationException(String.valueOf(e.getMessage()), path, e);
            }
        }

        public static ArtifactIdSource decode(JsonReader reader, FieldPath path) throws IOException {
            long importState = 0;
            long source = 0;
            long index = 0;
            boolean seenCompile = false;
            boolean seenIndex = false;
            boolean seenImport = false;
            boolean seenSource = false;
            reader.beginObject();
            while (reader.hasNext()) {
                var fname = reader.nextName();
                switch (fname) {
                    case "compile_id" -> {
                        seenCompile = true;
                        var ciPath = path.field("compile_id");
                        reader.beginObject();
                        while (reader.hasNext()) {
                            var ciName = reader.nextName();
                            switch (ciName) {
                                case "import_state" -> {
                                    seenImport = true;
                                    importState = reader.nextLong();
                                }
                                case "source" -> {
                                    seenSource = true;
                                    source = reader.nextLong();
                                }
                                default ->
                                    throw new DeserializationException(
                                            "Unknown field '" + ciName + "' in compile_id", ciPath.field(ciName));
                            }
                        }
                        reader.endObject();
                    }
                    case "index" -> {
                        seenIndex = true;
                        index = reader.nextLong();
                    }
                    default ->
                        throw new DeserializationException(
                                "Unknown field '" + fname + "' in ArtifactIdSource", path.field(fname));
                }
            }
            reader.endObject();
            if (!seenCompile) {
                throw new DeserializationException("Missing required field 'compile_id'", path.field("compile_id"));
            }
            if (!seenIndex) {
                throw new DeserializationException("Missing required field 'index'", path.field("index"));
            }
            if (!seenImport) {
                throw new DeserializationException(
                        "Missing required field 'import_state'",
                        path.field("compile_id").field("import_state"));
            }
            if (!seenSource) {
                throw new DeserializationException(
                        "Missing required field 'source'",
                        path.field("compile_id").field("source"));
            }
            return new ArtifactIdSource(new CompileId(new ImportStateId(importState), new SourceId(source)), index);
        }
    }
}
