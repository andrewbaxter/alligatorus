package compilerxyz.artifact;

import compilerxyz.safejson.JsonValue;

public record ArtifactBody(JsonValue data, ArtifactBodyExporter exporter) {}
