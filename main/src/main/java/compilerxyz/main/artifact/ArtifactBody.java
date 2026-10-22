package compilerxyz.main.artifact;

import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6537L)
@BuiltinPath("types.artifact_body")
public record ArtifactBody(JsonValue data, ArtifactBodyExporter exporter) {}
