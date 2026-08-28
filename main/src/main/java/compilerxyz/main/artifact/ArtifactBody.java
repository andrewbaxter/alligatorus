package compilerxyz.main.artifact;

import compilerxyz.main.safejson.JsonValue;

public record ArtifactBody(JsonValue data, ArtifactBodyExporter exporter) {}
