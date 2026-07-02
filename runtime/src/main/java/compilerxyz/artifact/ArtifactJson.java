package compilerxyz.artifact;

import compilerxyz.eval.CompileId;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ImportStateId;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public final class ArtifactJson {
  private ArtifactJson() {}

  public static JsonValue encodeArtifact(Artifact a) {
    var m = new TOrderedMap<String, JsonValue>();
    switch (a) {
      case Artifact.Body b -> m.put("body", encodeArtifactBody(b.body()));
      case Artifact.Ref r -> m.put("ref", encodeArtifactIdSource(r.id()));
    }
    return new JsonValueObject(m);
  }

  public static EResult<Artifact> decodeArtifact(QExprId loc, JsonValue v) {
    if (!(v instanceof JsonValueObject obj) || obj.entries().size() != 1) {
      return EResult.err(loc, "decodeArtifact: expected tagged object");
    }
    var entry = obj.entries().entrySet().iterator().next();
    return switch (entry.getKey()) {
      case "body" -> {
        var inner = decodeArtifactBody(loc, entry.getValue());
        if (inner.hasErrors()) yield inner.reerr();
        yield EResult.ok(new Artifact.Body(inner.valueOrNull()));
      }
      case "ref" -> {
        var inner = decodeArtifactIdSource(loc, entry.getValue());
        if (inner.hasErrors()) yield inner.reerr();
        yield EResult.ok(new Artifact.Ref(inner.valueOrNull()));
      }
      default -> EResult.err(loc, "decodeArtifact: unknown tag " + entry.getKey());
    };
  }

  public static JsonValue encodeArtifactBody(ArtifactBody body) {
    var m = new TOrderedMap<String, JsonValue>();
    m.put("data", body.data());
    m.put("exporter", encodeArtifactBodyExporter(body.exporter()));
    return new JsonValueObject(m);
  }

  public static EResult<ArtifactBody> decodeArtifactBody(QExprId loc, JsonValue v) {
    if (!(v instanceof JsonValueObject obj)) {
      return EResult.err(loc, "decodeArtifactBody: expected object");
    }
    var data = obj.entries().get("data");
    var exporterJson = obj.entries().get("exporter");
    if (data == null || exporterJson == null) {
      return EResult.err(loc, "decodeArtifactBody: missing data/exporter");
    }
    var exp = decodeArtifactBodyExporter(loc, exporterJson);
    if (exp.hasErrors()) return exp.reerr();
    return EResult.ok(new ArtifactBody(data, exp.valueOrNull()));
  }

  public static JsonValue encodeArtifactBodyExporter(ArtifactBodyExporter ex) {
    var m = new TOrderedMap<String, JsonValue>();
    switch (ex) {
      case ArtifactBodyExporter.Body b -> m.put("body", encodeArtifactBody(b.inner()));
      case ArtifactBodyExporter.Root r ->
          m.put("root", new JsonValueObject(new TOrderedMap<>()));
      case ArtifactBodyExporter.SourceRef sr -> m.put("source_ref", encodeArtifactIdSource(sr.id()));
    }
    return new JsonValueObject(m);
  }

  public static EResult<ArtifactBodyExporter> decodeArtifactBodyExporter(
      QExprId loc, JsonValue v) {
    if (!(v instanceof JsonValueObject obj) || obj.entries().size() != 1) {
      return EResult.err(loc, "decodeArtifactBodyExporter: expected tagged object");
    }
    var entry = obj.entries().entrySet().iterator().next();
    return switch (entry.getKey()) {
      case "body" -> {
        var inner = decodeArtifactBody(loc, entry.getValue());
        if (inner.hasErrors()) yield inner.reerr();
        yield EResult.ok(new ArtifactBodyExporter.Body(inner.valueOrNull()));
      }
      case "root" -> EResult.ok(new ArtifactBodyExporter.Root());
      case "source_ref" -> {
        var inner = decodeArtifactIdSource(loc, entry.getValue());
        if (inner.hasErrors()) yield inner.reerr();
        yield EResult.ok(new ArtifactBodyExporter.SourceRef(inner.valueOrNull()));
      }
      default -> EResult.err(loc, "decodeArtifactBodyExporter: unknown tag " + entry.getKey());
    };
  }

  public static JsonValue encodeArtifactIdSource(ArtifactIdSource id) {
    var m = new TOrderedMap<String, JsonValue>();
    var ci = new TOrderedMap<String, JsonValue>();
    ci.put("import_state", new JsonValueNumber(id.compileId().importState().value()));
    ci.put("source", new JsonValueNumber(id.compileId().source().value()));
    m.put("compile_id", new JsonValueObject(ci));
    m.put("index", new JsonValueNumber(id.index()));
    return new JsonValueObject(m);
  }

  public static EResult<ArtifactIdSource> decodeArtifactIdSource(QExprId loc, JsonValue v) {
    if (!(v instanceof JsonValueObject obj)) {
      return EResult.err(loc, "decodeArtifactIdSource: expected object");
    }
    if (!(obj.entries().get("compile_id") instanceof JsonValueObject ci
        && obj.entries().get("index") instanceof JsonValueNumber index
        && ci.entries().get("import_state") instanceof JsonValueNumber is
        && ci.entries().get("source") instanceof JsonValueNumber src)) {
      return EResult.err(loc, "decodeArtifactIdSource: malformed");
    }
    return EResult.ok(
        new ArtifactIdSource(
            new CompileId(
                new ImportStateId((long) is.value()), new SourceId((long) src.value())),
            (long) index.value()));
  }

  public static JsonValue encodeArtifactList(TList<Artifact> list) {
    var arr = new TList<JsonValue>(list.size());
    for (var a : list) arr.add(encodeArtifact(a));
    return new JsonValueArray(arr);
  }

  public static EResult<TList<Artifact>> decodeArtifactList(QExprId loc, JsonValue v) {
    if (!(v instanceof JsonValueArray arr)) {
      return EResult.err(loc, "decodeArtifactList: expected array");
    }
    var out = new TList<Artifact>(arr.elements().size());
    for (var e : arr.elements()) {
      var a = decodeArtifact(loc, e);
      if (a.hasErrors()) return a.reerr();
      out.add(a.valueOrNull());
    }
    return EResult.ok(out);
  }

  public static EResult<String> decodeString(QExprId loc, JsonValue v) {
    if (v instanceof JsonValueString s) return EResult.ok(s.value());
    return EResult.err(loc, "decodeString: expected string");
  }

  public static JsonValue encodeString(String s) {
    return new JsonValueString(s);
  }

  public static EResult<JsonValue> requiredField(QExprId loc, JsonValueObject obj, String name) {
    var v = obj.entries().get(name);
    if (v == null) return EResult.err(loc, "missing field '" + name + "'");
    return EResult.ok(v);
  }

  public static Opt<JsonValueObject> asObject(JsonValue v) {
    if (v instanceof JsonValueObject o) return Opt.some(o);
    return Opt.none();
  }
}
