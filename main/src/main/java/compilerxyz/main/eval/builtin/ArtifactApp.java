package compilerxyz.main.eval.builtin;

import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.Opt;

public record ArtifactApp(Opt<String> executor, String bytesBase64) implements SafeJsonSerializable {}
