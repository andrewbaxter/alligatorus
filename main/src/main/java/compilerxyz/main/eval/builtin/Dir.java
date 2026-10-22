package compilerxyz.main.eval.builtin;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.codec.ArtifactDirCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Base64;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6472L, exporter = "DIR")
@BuiltinPath("types.dir")
public record Dir(TOrderedMap<String, byte[]> files) {

    public static ArtifactDir toArtifactDir(Dir dir) {
        var entries = new TList<ArtifactDir.Entry>(dir.files().size());
        for (var entry : dir.files().entrySet()) {
            entries.add(
                    new ArtifactDir.Entry(entry.getKey(), Base64.getEncoder().encodeToString(entry.getValue())));
        }
        return new ArtifactDir(entries);
    }

    public static Dir fromArtifactDir(ArtifactDir body) {
        var files = new TOrderedMap<String, byte[]>();
        for (var entry : body.entries()) {
            files.put(entry.path(), Base64.getDecoder().decode(entry.bytesBase64()));
        }
        return new Dir(files);
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            var dir = MustCast.mustCast(Dir.class, self);
            return EResult.ok(new Artifact.Inline(makeBody.apply(ArtifactDirCodec.toValue(toArtifactDir(dir)))));
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactDir body;
            try {
                body = ArtifactDirCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "Dir data: failed to parse ArtifactDir: " + e.getMessage());
            }
            return EResult.ok(fromArtifactDir(body));
        }
    };
}
