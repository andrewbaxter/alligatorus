package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0200EcosystemLlvmTest.onPath;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.writeModule;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.builtin.Dir;
import compilerxyz.main.langhelp.ArtifactResolve;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Main;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0215EcosystemWebTest {
    @Test
    public void test00000HelloWeb(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("wat2wasm"), "wat2wasm not on PATH");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve(EcoMeta.FILE), EcoMeta.module(new EcoMb()));
        writeModule(src.resolve("wat-2.0.atz"), EcoWat.module(new EcoMb()));
        writeModule(src.resolve("webidl.atz"), EcoWebidl.module(new EcoMb()));
        writeModule(src.resolve("js-es2020.atz"), EcoJs.module(new EcoMb()));
        writeModule(src.resolve("webidl-wasm.atz"), EcoWebidlWasm.module(new EcoMb()));
        writeModule(src.resolve("webidl-js.atz"), EcoWebidlJs.module(new EcoMb()));
        writeModule(src.resolve("web-wasm.atz"), EcoWebWasm.module(new EcoMb()));
        writeModule(src.resolve("hello-web.atz"), EcoHelloWeb.module(new EcoMb()));
        Dir dir;
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache),
                    new SourceSpecAbsolute.Local(src.resolve("hello-web.atz").toString()));
            Compile.CompileResult out =
                    switch (res) {
                        case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
                        case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                            throw new AssertionError("compile failed: " + err.error());
                    };
            assertTrue(out.artifact().optIsSome(), "root module exported nothing");
            dir = switch (ArtifactResolve.artifactResolveDir(
                    cache, out.artifact().optUnwrap())) {
                case Result.Ok<Dir, String> ok -> ok.value();
                case Result.Err<Dir, String> err -> throw new AssertionError("not a dir: " + err.error());
            };
        }
        assertEquals(3, dir.files().size());
        assertEquals(
                EcoWebWasm.HTML, new String(dir.files().getOpt("index.html").optUnwrap(), StandardCharsets.UTF_8));
        var wasm = dir.files().getOpt("main.wasm").optUnwrap();
        assertArrayEquals(new byte[] {0, 'a', 's', 'm'}, Arrays.copyOf(wasm, 4));
        assertEquals(
                """
                function h_new(o) {
                  const $2 = handles.length;
                  const $3 = handles.push(o);
                  return $2;
                }
                function h_get(h) {
                  return handles[h];
                }
                function str(ptr, len) {
                  const $4 = new Uint8Array(instance.exports.memory.buffer, ptr, len);
                  const $5 = $4;
                  const $6 = decoder.decode($5);
                  return $6;
                }
                function wi$7() {
                  const r = globalThis;
                  return h_new(r);
                }
                function wi$8(self) {
                  const recv = h_get(self);
                  const r = recv.document;
                  return h_new(r);
                }
                function wi$9(self, a0p, a0l, o1, a1p, a1l) {
                  const recv = h_get(self);
                  const r = recv.createElement(str(a0p, a0l), (o1 !== 0 ? str(a1p, a1l) : undefined));
                  return h_new(r);
                }
                function wi$10(self, n0, a0p, a0l) {
                  const recv = h_get(self);
                  recv.textContent = (n0 !== 0 ? str(a0p, a0l) : null);
                }
                function wi$11(self, out) {
                  const recv = h_get(self);
                  const r = recv.body;
                  const cell = new Uint32Array(instance.exports.memory.buffer, out, 2);
                  if (r === null || r === undefined) {
                    cell[0] = 0;
                  } else {
                    cell[0] = 1;
                    cell[1] = h_new(r);
                  }
                }
                function wi$12(self, a0) {
                  const recv = h_get(self);
                  const r = recv.appendChild(h_get(a0));
                  return h_new(r);
                }
                function ready(r) {
                  instance = r.instance;
                  const $13 = instance.exports._start();
                }
                let handles;
                let instance;
                let decoder;
                const $0 = new Array();
                handles = $0;
                const $1 = new TextDecoder();
                decoder = $1;
                let imports;
                imports = {webidl: {}};
                imports.webidl["global"] = wi$7;
                imports.webidl["Window.document"] = wi$8;
                imports.webidl["Document.createElement"] = wi$9;
                imports.webidl["Node.textContent="] = wi$10;
                imports.webidl["Document.body"] = wi$11;
                imports.webidl["Node.appendChild"] = wi$12;
                const $14 = fetch("main.wasm");
                const $15 = WebAssembly.instantiateStreaming($14, imports);
                const $16 = $15.then(ready);
                """,
                new String(dir.files().getOpt("main.js").optUnwrap(), StandardCharsets.UTF_8));
        if (onPath("node")) {
            var site = tmp.resolve("site");
            Main.writeDir(dir, site);
            Files.writeString(tmp.resolve("check.js"), NODE_CHECK);
            try {
                var proc = new ProcessBuilder("node", "check.js", "site")
                        .directory(tmp.toFile())
                        .redirectErrorStream(true)
                        .start();
                var output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                assertEquals(0, proc.waitFor(), () -> "node check.js failed:\n" + output);
                assertEquals(EcoHelloWeb.TAG + ":" + EcoHelloWeb.MESSAGE + "\n", output);
            } catch (InterruptedException ex) {
                throw new AssertionError(ex);
            }
        }
    }

    @Test
    public void test00010Wiggle(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("wat2wasm"), "wat2wasm not on PATH");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve(EcoMeta.FILE), EcoMeta.module(new EcoMb()));
        writeModule(src.resolve("wat-2.0.atz"), EcoWat.module(new EcoMb()));
        writeModule(src.resolve("webidl.atz"), EcoWebidl.module(new EcoMb()));
        writeModule(src.resolve("js-es2020.atz"), EcoJs.module(new EcoMb()));
        writeModule(src.resolve("webidl-wasm.atz"), EcoWebidlWasm.module(new EcoMb()));
        writeModule(src.resolve("webidl-js.atz"), EcoWebidlJs.module(new EcoMb()));
        writeModule(src.resolve("web-wasm.atz"), EcoWebWasm.module(new EcoMb()));
        writeModule(src.resolve("glsl-es-3.00.atz"), EcoGlsl.module(new EcoMb()));
        writeModule(src.resolve("webgl-uniforms.atz"), EcoWebglUniforms.module(new EcoMb()));
        writeModule(src.resolve("web-wiggle.atz"), EcoWebWiggle.module(new EcoMb()));
        Dir dir;
        try (var cache = new CacheReal(tmp.resolve("cache"))) {
            var res = Helpers.compile(
                    GlobalContext.newContext(cache, cache, cache),
                    new SourceSpecAbsolute.Local(src.resolve("web-wiggle.atz").toString()));
            Compile.CompileResult out =
                    switch (res) {
                        case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> ok.value();
                        case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                            throw new AssertionError("compile failed: " + err.error());
                    };
            dir = switch (ArtifactResolve.artifactResolveDir(
                    cache, out.artifact().optUnwrap())) {
                case Result.Ok<Dir, String> ok -> ok.value();
                case Result.Err<Dir, String> err -> throw new AssertionError("not a dir: " + err.error());
            };
        }
        assertEquals(3, dir.files().size());
        var js = new String(dir.files().getOpt("main.js").optUnwrap(), StandardCharsets.UTF_8);
        assertTrue(js.contains("imports.webidl[\"WebGL2RenderingContext.bufferData#1\"]"), js);
        assertTrue(js.contains("new Uint8Array(instance.exports.memory.buffer, a1p, a1l)"), js);
        assertTrue(js.contains("imports.webidl[\"callback\"]"), js);
        var site = tmp.resolve("site");
        Main.writeDir(dir, site);
        var keep = System.getenv("WIGGLE_SITE");
        if (keep != null) Main.writeDir(dir, Path.of(keep));
    }

    private static final String NODE_CHECK =
            """
            const fs = require("fs");
            const site = process.argv[2];
            const wasmBytes = fs.readFileSync(site + "/main.wasm");
            const log = [];
            globalThis.document = {
              createElement(tag, options) {
                if (options !== undefined) throw new Error("options passed: " + options);
                return {tag, textContent: null};
              },
              body: {
                appendChild(child) {
                  log.push(child.tag + ":" + child.textContent);
                  return child;
                },
              },
            };
            globalThis.fetch = (url) => Promise.resolve({url});
            WebAssembly.instantiateStreaming = (source, imports) =>
              source.then(() => WebAssembly.instantiate(wasmBytes, imports));
            (0, eval)(fs.readFileSync(site + "/main.js", "utf8"));
            process.on("beforeExit", () => {
              for (const line of log) console.log(line);
              log.length = 0;
            });
            """;
}
