package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0200EcosystemLlvmTest.compileOk;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.onPath;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.writeModule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0225EcosystemGlslTest {
    static void validate(Path dir, String stage, String source) throws IOException {
        if (!onPath("glslangValidator")) return;
        var file = dir.resolve("shader." + stage);
        Files.writeString(file, source);
        try {
            var proc = new ProcessBuilder("glslangValidator", file.toString())
                    .directory(dir.toFile())
                    .redirectErrorStream(true)
                    .start();
            var output = new String(proc.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            Assertions.assertEquals(0, proc.waitFor(), () -> "glslangValidator rejected " + stage + ":\n" + output);
        } catch (InterruptedException ex) {
            throw new AssertionError(ex);
        }
    }

    @Test
    public void test00000WiggleShaders(@TempDir Path tmp) throws IOException {
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("glsl-es-3.00.atz"), EcoGlsl.module(new EcoMb()));
        var e = new EcoMb();
        var top = new TList<SourceExpr>();
        top.add(e.let("glsl", e.importLocal("glsl", "glsl-es-3.00.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(EcoWebWiggle.shaderSetup(e)))));
        for (var n : new String[] {"vs", "fs", "main_t"}) top.add(e.let(n, e.fld(e.v("k"), n)));
        top.add(e.let(
                "u",
                e.call(
                        e.fld(e.v("glsl"), "uniform_block_fn"),
                        e.rec(
                                e.p("mod", e.v("fs")),
                                e.p("name", e.s(EcoWebWiggle.BLOCK)),
                                e.p("layout", e.s("std140")),
                                e.p("fields", EcoWebWiggle.blockFields(e))))));
        EcoWebWiggle.shaders(e, top);
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("vs"), "render"), "glsl")));
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("fs"), "render"), "glsl")));
        writeModule(src.resolve("main.at"), top);
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        assertEquals(2, ok.logs().size());
        var vert = ok.logs().get(0).message();
        var frag = ok.logs().get(1).message();
        assertEquals(
                """
                #version 300 es
                precision highp float;
                void main() {
                  int t0 = gl_VertexID & 1;
                  int t1 = t0 << 2;
                  float t2 = float(t1);
                  float t3 = t2 - 1.0;
                  float t4 = t3;
                  int t5 = gl_VertexID & 2;
                  int t6 = t5 << 1;
                  float t7 = float(t6);
                  float t8 = t7 - 1.0;
                  float t9 = t8;
                  vec4 t10 = vec4(t4, t9, 0.0, 1.0);
                  gl_Position = t10;
                }
                """,
                vert);
        assertEquals(
                """
                #version 300 es
                precision highp float;
                layout(std140) uniform u_block {
                  float time;
                  vec2 resolution;
                } u;
                out vec4 color;
                void main() {
                  vec2 t0 = gl_FragCoord.xy / u.resolution;
                  vec2 t1 = t0 * 2.0;
                  vec2 t2 = t1 - 1.0;
                  vec2 t3 = t2;
                  vec2 t4 = abs(t3);
                  vec2 t5 = t4;
                  float t6 = max(t5.x, t5.y);
                  float t7 = t6;
                  vec2 t8 = t3 / t7;
                  vec2 t9 = t8;
                  float t28;
                  bool t10 = t5.y >= t5.x;
                  if (t10) {
                    float t18;
                    bool t11 = t3.y > 0.0;
                    if (t11) {
                      float t12 = t9.x + 1.0;
                      float t13 = t12 * 0.5;
                      t18 = t13;
                    } else {
                      float t14 = -t9.x;
                      float t15 = t14 + 1.0;
                      float t16 = t15 * 0.5;
                      float t17 = t16 + 2.0;
                      t18 = t17;
                    }
                    t28 = t18;
                  } else {
                    float t27;
                    bool t19 = t3.x > 0.0;
                    if (t19) {
                      float t20 = -t9.y;
                      float t21 = t20 + 1.0;
                      float t22 = t21 * 0.5;
                      float t23 = t22 + 1.0;
                      t27 = t23;
                    } else {
                      float t24 = t9.y + 1.0;
                      float t25 = t24 * 0.5;
                      float t26 = t25 + 3.0;
                      t27 = t26;
                    }
                    t28 = t27;
                  }
                  float t29 = t28;
                  float t30 = t29 * 6.283185307179586;
                  float t31 = u.time * 3.0;
                  float t32 = t30 - t31;
                  float t33 = sin(t32);
                  float t34 = t33 * 0.05;
                  float t35 = t34;
                  float t36 = t35 + 0.6;
                  float t37 = t7 - t36;
                  float t38 = t37;
                  float t39 = fwidth(t38);
                  float t40 = t39;
                  float t41 = -t40;
                  float t42 = smoothstep(t41, t40, t38);
                  float t43 = t42;
                  vec3 t44 = vec3(0.0);
                  vec3 t45 = vec3(0.5);
                  vec3 t46 = mix(t44, t45, t43);
                  vec4 t47 = vec4(t46, 1.0);
                  color = t47;
                }
                """,
                frag);
        validate(tmp, "vert", vert);
        validate(tmp, "frag", frag);
    }
}
