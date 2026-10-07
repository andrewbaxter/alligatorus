package compilerxyz.main.eval.builtin;

import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactDir(TList<Entry> entries) implements SafeJsonSerializable {
    public record Entry(String path, String bytesBase64) implements SafeJsonSerializable {}
}
