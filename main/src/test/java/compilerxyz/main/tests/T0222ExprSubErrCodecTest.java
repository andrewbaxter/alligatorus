package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.eval.ArtifactExprErr;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.gen.codec.ArtifactExprErrCodec;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.main.syntax.RemoteOrigin;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.junit.jupiter.api.Test;

public final class T0222ExprSubErrCodecTest {
    @Test
    public void test00100ImportFailedRoundTrip() {
        var spec = new SourceSpecAbsolute.Remote(
                new RemoteOrigin.Http(
                        "https://example.invalid/lib.zip#frag", new HashValue.Sha256(new byte[] {1, 2, (byte) 255})),
                "src/m.at");
        var err = new ExprErr(
                new QExprId(new SourceId(7), Opt.some(42L)),
                TList.of(
                        new ExprSubErr.ImportFailed(spec, "the module has compile errors"),
                        new ExprSubErr.Opaque("x")));
        var json = ArtifactExprErrCodec.toValue(ArtifactExprErr.fromExprErr(err));
        var back = ArtifactExprErrCodec.fromValue(json).toExprErr();
        assertEquals(err.loc(), back.loc());
        assertEquals(2, back.errors().size());
        switch (back.errors().get(0)) {
            case ExprSubErr.ImportFailed f -> {
                assertEquals(spec.sourceSpecAbsoluteSpecKey(), f.spec().sourceSpecAbsoluteSpecKey());
                assertEquals("the module has compile errors", f.reason());
            }
            case ExprSubErr.Opaque o -> throw new AssertionError("expected an import failure, got " + o);
        }
        assertEquals("x", back.errors().get(1).exprSubErrMessage());
        assertEquals(
                "import of " + spec.sourceSpecAbsoluteSpecKey() + " failed: the module has compile errors",
                back.errors().get(0).exprSubErrMessage());
    }

    @Test
    public void test00200SpecKeysParseBack() {
        for (var spec : new SourceSpecAbsolute[] {
            new SourceSpecAbsolute.Local("/a/b.at"),
            new SourceSpecAbsolute.Virtual("v"),
            new SourceSpecAbsolute.Remote(new RemoteOrigin.Git("https://x/y.git", "0".repeat(40)), ""),
            new SourceSpecAbsolute.Remote(new RemoteOrigin.Git("git@x:y/z@w.git", "a".repeat(40)), "d/e!f.at"),
        }) {
            var key = spec.sourceSpecAbsoluteSpecKey();
            assertEquals(key, SourceSpecAbsolute.sourceSpecAbsoluteParseKey(key).sourceSpecAbsoluteSpecKey());
        }
    }
}
