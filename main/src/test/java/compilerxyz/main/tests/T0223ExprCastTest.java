package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0223ExprCastTest {
    @Test
    public void test00000RuntimeValueToExprIsError(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        Helpers.writeModule(
                src.resolve("main.at"),
                TList.of(
                        e.let("x", e.mb.as(e.n(3.), e.mb.typesInt())),
                        e.let("y", e.mb.as(e.v("x"), e.mb.typesExpr())),
                        e.call(e.mb.log(), e.s("ok"))));
        var err = assertThrows(
                AssertionError.class, () -> Helpers.compileOk(src.resolve("main.at"), tmp.resolve("cache")));
        var message = String.valueOf(err.getMessage());
        assertTrue(message.contains("Incompatible types"), message);
    }
}
