package compilerxyz.main.artifact;

import compilerxyz.main.safejson.Json.DeserializationException;
import compilerxyz.main.safejson.Json.FieldPath;
import compilerxyz.main.safejson.JsonValue;

public final class ArtifactCodecs {
    private ArtifactCodecs() {}

    public static final class ArtifactCodec {
        private ArtifactCodec() {}

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
    }
}
