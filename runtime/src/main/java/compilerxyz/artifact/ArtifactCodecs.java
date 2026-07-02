package compilerxyz.artifact;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import compilerxyz.safejson.Codec;
import compilerxyz.safejson.CodecRegistry;
import compilerxyz.safejson.Json;
import compilerxyz.safejson.Json.DeserializationException;
import compilerxyz.safejson.Json.FieldPath;
import compilerxyz.safejson.Json.Style;
import compilerxyz.safejson.JsonValue;
import java.io.IOException;
import java.util.Comparator;
import java.util.TreeMap;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.SourceId;

public final class ArtifactCodecs {
  private ArtifactCodecs() {}

  public static void registerAll() {
    CodecRegistry.register(Artifact.class, ArtifactCodec.INSTANCE);
    CodecRegistry.register(Artifact.Body.class, ArtifactCodec.INSTANCE_BODY);
    CodecRegistry.register(Artifact.Ref.class, ArtifactCodec.INSTANCE_REF);
    CodecRegistry.register(ArtifactBody.class, ArtifactBodyCodec.INSTANCE);
    CodecRegistry.register(ArtifactBodyExporter.class, ArtifactBodyExporterCodec.INSTANCE);
    CodecRegistry.register(
        ArtifactBodyExporter.Body.class, ArtifactBodyExporterCodec.INSTANCE_BODY);
    CodecRegistry.register(
        ArtifactBodyExporter.Root.class, ArtifactBodyExporterCodec.INSTANCE_ROOT);
    CodecRegistry.register(
        ArtifactBodyExporter.SourceRef.class, ArtifactBodyExporterCodec.INSTANCE_SOURCE_REF);
    CodecRegistry.register(ArtifactIdSource.class, ArtifactIdSourceCodec.INSTANCE);
  }

  static final class ArtifactCodec implements Codec<Artifact> {
    static final ArtifactCodec INSTANCE = new ArtifactCodec();
    static final Codec<Artifact.Body> INSTANCE_BODY =
        new Codec<>() {
          @Override
          public void codecEncode(JsonWriter writer, Artifact.Body value, Style style)
              throws IOException {
            INSTANCE.codecEncode(writer, value, style);
          }

          @Override
          public Artifact.Body codecDecode(JsonReader reader, FieldPath path) throws IOException {
            return (Artifact.Body) INSTANCE.codecDecode(reader, path);
          }
        };
    static final Codec<Artifact.Ref> INSTANCE_REF =
        new Codec<>() {
          @Override
          public void codecEncode(JsonWriter writer, Artifact.Ref value, Style style)
              throws IOException {
            INSTANCE.codecEncode(writer, value, style);
          }

          @Override
          public Artifact.Ref codecDecode(JsonReader reader, FieldPath path) throws IOException {
            return (Artifact.Ref) INSTANCE.codecDecode(reader, path);
          }
        };

    @Override
    public void codecEncode(JsonWriter writer, Artifact value, Style style) throws IOException {
      writer.beginObject();
      switch (value) {
        case Artifact.Body b -> {
          writer.name("body");
          ArtifactBodyCodec.INSTANCE.codecEncode(writer, b.body(), style);
        }
        case Artifact.Ref r -> {
          writer.name("ref");
          ArtifactIdSourceCodec.INSTANCE.codecEncode(writer, r.id(), style);
        }
      }
      writer.endObject();
    }

    @Override
    public Artifact codecDecode(JsonReader reader, FieldPath path) throws IOException {
      reader.beginObject();
      if (!reader.hasNext()) {
        throw new DeserializationException("Empty object for Artifact sealed variant", path);
      }
      var tag = reader.nextName();
      var variantPath = path.field(tag);
      Artifact result;
      switch (tag) {
        case "body" ->
            result = new Artifact.Body(ArtifactBodyCodec.INSTANCE.codecDecode(reader, variantPath));
        case "ref" ->
            result = new Artifact.Ref(ArtifactIdSourceCodec.INSTANCE.codecDecode(reader, variantPath));
        default ->
            throw new DeserializationException("Unknown Artifact variant: '" + tag + "'", path);
      }
      if (reader.hasNext()) {
        throw new DeserializationException(
            "Artifact sealed object must have exactly one variant", path);
      }
      reader.endObject();
      return result;
    }
  }

  static final class ArtifactBodyCodec implements Codec<ArtifactBody> {
    static final ArtifactBodyCodec INSTANCE = new ArtifactBodyCodec();

    @Override
    public void codecEncode(JsonWriter writer, ArtifactBody value, Style style) throws IOException {
      writer.beginObject();
      if (style == Style.CANONICAL) {

        var sorted = new TreeMap<String, Runnable>(Comparator.naturalOrder());
        sorted.put(
            "data",
            () -> {
              try {
                writer.name("data");
                JsonValue.safeJsonAdapter.serialize(writer, value.data(), style);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
        sorted.put(
            "exporter",
            () -> {
              try {
                writer.name("exporter");
                ArtifactBodyExporterCodec.INSTANCE.codecEncode(writer, value.exporter(), style);
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            });
        for (var entry : sorted.entrySet()) {
          entry.getValue().run();
        }
      } else {
        writer.name("data");
        JsonValue.safeJsonAdapter.serialize(writer, value.data(), style);
        writer.name("exporter");
        ArtifactBodyExporterCodec.INSTANCE.codecEncode(writer, value.exporter(), style);
      }
      writer.endObject();
    }

    @Override
    public ArtifactBody codecDecode(JsonReader reader, FieldPath path) throws IOException {
      JsonValue data = null;
      ArtifactBodyExporter exporter = null;
      boolean seenData = false;
      boolean seenExporter = false;
      reader.beginObject();
      while (reader.hasNext()) {
        var fname = reader.nextName();
        switch (fname) {
          case "data" -> {
            seenData = true;
            data = JsonValue.safeJsonAdapter.deserialize(reader, path.field("data"));
          }
          case "exporter" -> {
            seenExporter = true;
            exporter = ArtifactBodyExporterCodec.INSTANCE.codecDecode(reader, path.field("exporter"));
          }
          default ->
              throw new DeserializationException(
                  "Unknown field '" + fname + "' in ArtifactBody", path.field(fname));
        }
      }
      reader.endObject();
      if (!seenData) {
        throw new DeserializationException(
            "Missing required field 'data' in ArtifactBody", path.field("data"));
      }
      if (!seenExporter) {
        throw new DeserializationException(
            "Missing required field 'exporter' in ArtifactBody", path.field("exporter"));
      }
      return new ArtifactBody(data, exporter);
    }
  }

  static final class ArtifactBodyExporterCodec implements Codec<ArtifactBodyExporter> {
    static final ArtifactBodyExporterCodec INSTANCE = new ArtifactBodyExporterCodec();
    static final Codec<ArtifactBodyExporter.Body> INSTANCE_BODY =
        new Codec<>() {
          @Override
          public void codecEncode(JsonWriter writer, ArtifactBodyExporter.Body value, Style style)
              throws IOException {
            INSTANCE.codecEncode(writer, value, style);
          }

          @Override
          public ArtifactBodyExporter.Body codecDecode(JsonReader reader, FieldPath path)
              throws IOException {
            return (ArtifactBodyExporter.Body) INSTANCE.codecDecode(reader, path);
          }
        };
    static final Codec<ArtifactBodyExporter.Root> INSTANCE_ROOT =
        new Codec<>() {
          @Override
          public void codecEncode(JsonWriter writer, ArtifactBodyExporter.Root value, Style style)
              throws IOException {
            INSTANCE.codecEncode(writer, value, style);
          }

          @Override
          public ArtifactBodyExporter.Root codecDecode(JsonReader reader, FieldPath path)
              throws IOException {
            return (ArtifactBodyExporter.Root) INSTANCE.codecDecode(reader, path);
          }
        };
    static final Codec<ArtifactBodyExporter.SourceRef> INSTANCE_SOURCE_REF =
        new Codec<>() {
          @Override
          public void codecEncode(JsonWriter writer, ArtifactBodyExporter.SourceRef value, Style style)
              throws IOException {
            INSTANCE.codecEncode(writer, value, style);
          }

          @Override
          public ArtifactBodyExporter.SourceRef codecDecode(JsonReader reader, FieldPath path)
              throws IOException {
            return (ArtifactBodyExporter.SourceRef) INSTANCE.codecDecode(reader, path);
          }
        };

    @Override
    public void codecEncode(JsonWriter writer, ArtifactBodyExporter value, Style style)
        throws IOException {
      writer.beginObject();
      switch (value) {
        case ArtifactBodyExporter.Body b -> {
          writer.name("body");
          ArtifactBodyCodec.INSTANCE.codecEncode(writer, b.inner(), style);
        }
        case ArtifactBodyExporter.Root r -> {
          writer.name("root");
          writer.beginObject();
          writer.endObject();
        }
        case ArtifactBodyExporter.SourceRef sr -> {
          writer.name("source_ref");
          ArtifactIdSourceCodec.INSTANCE.codecEncode(writer, sr.id(), style);
        }
      }
      writer.endObject();
    }

    @Override
    public ArtifactBodyExporter codecDecode(JsonReader reader, FieldPath path) throws IOException {
      reader.beginObject();
      if (!reader.hasNext()) {
        throw new DeserializationException(
            "Empty object for ArtifactBodyExporter sealed variant", path);
      }
      var tag = reader.nextName();
      var variantPath = path.field(tag);
      ArtifactBodyExporter result;
      switch (tag) {
        case "body" ->
            result =
                new ArtifactBodyExporter.Body(
                    ArtifactBodyCodec.INSTANCE.codecDecode(reader, variantPath));
        case "root" -> {

          reader.beginObject();
          if (reader.hasNext()) {
            throw new DeserializationException(
                "Unexpected field in ArtifactBodyExporter.Root payload", variantPath);
          }
          reader.endObject();
          result = new ArtifactBodyExporter.Root();
        }
        case "source_ref" ->
            result =
                new ArtifactBodyExporter.SourceRef(
                    ArtifactIdSourceCodec.INSTANCE.codecDecode(reader, variantPath));
        default ->
            throw new DeserializationException(
                "Unknown ArtifactBodyExporter variant: '" + tag + "'", path);
      }
      if (reader.hasNext()) {
        throw new DeserializationException(
            "ArtifactBodyExporter sealed object must have exactly one variant", path);
      }
      reader.endObject();
      return result;
    }
  }

  static final class ArtifactIdSourceCodec implements Codec<ArtifactIdSource> {
    static final ArtifactIdSourceCodec INSTANCE = new ArtifactIdSourceCodec();

    @Override
    public void codecEncode(JsonWriter writer, ArtifactIdSource value, Style style) throws IOException {
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

    @Override
    public ArtifactIdSource codecDecode(JsonReader reader, FieldPath path) throws IOException {
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
        throw new DeserializationException(
            "Missing required field 'compile_id'", path.field("compile_id"));
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
            "Missing required field 'source'", path.field("compile_id").field("source"));
      }
      return new ArtifactIdSource(
          new CompileId(
              new ImportStateId(importState),
              new SourceId(source)),
          index);
    }
  }
}
