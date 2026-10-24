package compilerxyz.main.artifact;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueNumberFloat;
import compilerxyz.main.safejson.JsonValueNumberInt;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;

public final class ArtifactJson {
    private ArtifactJson() {}

    @BuiltinPath(value = "eval.artifact_json_encode", scopeId = 5062L)
    public static JsonValue encodeArtifact(Artifact a) {
        var m = new TOrderedMap<String, JsonValue>();
        switch (a) {
            case Artifact.Inline b -> m.put("body", encodeArtifactBody(b.body()));
            case Artifact.Ref r -> m.put("ref", encodeArtifactIdSource(r.id()));
        }
        return new JsonValueObject(m);
    }

    @BuiltinPath(value = "eval.artifact_json_decode", scopeId = 5063L)
    public static Artifact decodeArtifact(JsonValue v) {
        var entry = tagged(v, "decodeArtifact");
        return switch (entry.getKey()) {
            case "body" -> new Artifact.Inline(decodeArtifactBody(entry.getValue()));
            case "ref" -> new Artifact.Ref(decodeArtifactIdSource(entry.getValue()));
            default -> throw new IllegalStateException("decodeArtifact: unknown tag " + entry.getKey());
        };
    }

    public static JsonValue encodeArtifactBody(ArtifactBody body) {
        var m = new TOrderedMap<String, JsonValue>();
        m.put("data", body.data());
        var exporterM = new TOrderedMap<String, JsonValue>();
        switch (body.exporter()) {
            case ArtifactBodyExporter.Body b -> exporterM.put("body", encodeArtifactBody(b.inner()));
            case ArtifactBodyExporter.Root r -> exporterM.put("root", new JsonValueObject(new TOrderedMap<>()));
            case ArtifactBodyExporter.SourceRef sr -> exporterM.put("source_ref", encodeArtifactIdSource(sr.id()));
        }
        m.put("exporter", new JsonValueObject(exporterM));
        return new JsonValueObject(m);
    }

    public static ArtifactBody decodeArtifactBody(JsonValue v) {
        var obj = object(v, "decodeArtifactBody");
        var data = field(obj, "data", "decodeArtifactBody");
        var exporterEntry = tagged(field(obj, "exporter", "decodeArtifactBody"), "decodeArtifactBodyExporter");
        ArtifactBodyExporter exporter =
                switch (exporterEntry.getKey()) {
                    case "body" -> new ArtifactBodyExporter.Body(decodeArtifactBody(exporterEntry.getValue()));
                    case "root" -> new ArtifactBodyExporter.Root();
                    case "source_ref" ->
                        new ArtifactBodyExporter.SourceRef(decodeArtifactIdSource(exporterEntry.getValue()));
                    default ->
                        throw new IllegalStateException(
                                "decodeArtifactBodyExporter: unknown tag " + exporterEntry.getKey());
                };
        return new ArtifactBody(data, exporter);
    }

    public static JsonValue encodeArtifactIdSource(ArtifactIdSource id) {
        var m = new TOrderedMap<String, JsonValue>();
        var ci = new TOrderedMap<String, JsonValue>();
        ci.put(
                "import_state",
                new JsonValueNumberInt(id.compileId().importState().value()));
        ci.put("source", new JsonValueNumberInt(id.compileId().source().value()));
        m.put("compile_id", new JsonValueObject(ci));
        m.put("index", new JsonValueNumberInt(id.index()));
        return new JsonValueObject(m);
    }

    public static ArtifactIdSource decodeArtifactIdSource(JsonValue v) {
        var obj = object(v, "decodeArtifactIdSource");
        var ci = object(field(obj, "compile_id", "decodeArtifactIdSource"), "decodeArtifactIdSource compile_id");
        return new ArtifactIdSource(
                new CompileId(
                        new ImportStateId(number(ci, "import_state", "decodeArtifactIdSource")),
                        new SourceId(number(ci, "source", "decodeArtifactIdSource"))),
                number(obj, "index", "decodeArtifactIdSource"));
    }

    public static JsonValue encodeArtifactList(TList<Artifact> list) {
        var arr = new TList<JsonValue>(list.size());
        for (var a : list) arr.add(encodeArtifact(a));
        return new JsonValueArray(arr);
    }

    public static TList<Artifact> decodeArtifactList(JsonValue v) {
        JsonValueArray arr =
                switch (v) {
                    case JsonValueArray a -> a;
                    case JsonValueNull n -> throw new IllegalStateException("decodeArtifactList: expected array");
                    case JsonValueBoolean b -> throw new IllegalStateException("decodeArtifactList: expected array");
                    case JsonValueNumber n -> throw new IllegalStateException("decodeArtifactList: expected array");
                    case JsonValueString s -> throw new IllegalStateException("decodeArtifactList: expected array");
                    case JsonValueObject o -> throw new IllegalStateException("decodeArtifactList: expected array");
                };
        var out = new TList<Artifact>(arr.len());
        for (var e : arr.elements()) out.add(decodeArtifact(e));
        return out;
    }

    private static JsonValueObject object(JsonValue v, String what) {
        return switch (v) {
            case JsonValueObject o -> o;
            case JsonValueNull n -> throw new IllegalStateException(what + ": expected object");
            case JsonValueBoolean b -> throw new IllegalStateException(what + ": expected object");
            case JsonValueNumber n -> throw new IllegalStateException(what + ": expected object");
            case JsonValueString s -> throw new IllegalStateException(what + ": expected object");
            case JsonValueArray a -> throw new IllegalStateException(what + ": expected object");
        };
    }

    private static Map.Entry<String, JsonValue> tagged(JsonValue v, String what) {
        var obj = object(v, what);
        if (obj.len() != 1) {
            throw new IllegalStateException(what + ": expected tagged object");
        }
        var key = obj.keys().get(0);
        return Map.entry(key, obj.get(key).optUnwrap());
    }

    private static JsonValue field(JsonValueObject obj, String key, String what) {
        var found = obj.get(key);
        if (found.optIsNone()) {
            throw new IllegalStateException(what + ": missing '" + key + "'");
        }
        return found.optUnwrap();
    }

    private static long number(JsonValueObject obj, String key, String what) {
        return switch (field(obj, key, what)) {
            case JsonValueNumberInt n -> n.value();
            case JsonValueNumberFloat n -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
            case JsonValueNull n -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
            case JsonValueBoolean b -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
            case JsonValueString s -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
            case JsonValueArray a -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
            case JsonValueObject o -> throw new IllegalStateException(what + ": expected number at '" + key + "'");
        };
    }
}
