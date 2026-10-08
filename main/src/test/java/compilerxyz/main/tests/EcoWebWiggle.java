package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.nio.charset.StandardCharsets;

public final class EcoWebWiggle {
    private EcoWebWiggle() {}

    public static final String BLOCK = "u";
    public static final double SIZE_CM = 3;

    private static SourceExpr g(EcoMb e, String name) {
        return e.lower(e.fld(e.v("glsl"), name));
    }

    private static SourceExpr fn(EcoMb e, String name, SourceExpr... args) {
        return e.call(e.lower(e.fld(e.v("glsl"), "builtins", name)), e.tup(args));
    }

    private static SourceExpr fn(EcoMb e, String name, SourceExpr arg) {
        return e.call(e.lower(e.fld(e.v("glsl"), "builtins", name)), arg);
    }

    public static SourceExpr blockFields(EcoMb e) {
        return e.tup(
                e.tup(e.s("time"), e.fld(e.v("glsl"), "float")), e.tup(e.s("resolution"), e.fld(e.v("glsl"), "vec2")));
    }

    public static TList<SourceExpr> shaderSetup(EcoMb e) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("glsl", e.importLocal("glsl", "glsl-es-3.00.atz")));
        setup.add(e.let("vs", e.call(e.fld(e.v("glsl"), "new"), e.tup())));
        setup.add(e.let("fs", e.call(e.fld(e.v("glsl"), "new"), e.tup())));
        setup.add(e.let(
                "main_t",
                e.call(
                        e.fld(e.v("glsl"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.fld(e.v("glsl"), "void"))))));
        setup.add(e.ret(e.rec(e.p("vs", e.v("vs")), e.p("fs", e.v("fs")), e.p("main_t", e.v("main_t")))));
        return setup;
    }

    public static void shaders(EcoMb e, TList<SourceExpr> out) {
        var id = e.lower(e.v("gl_VertexID"));
        var vertexBody = e.stage(e.seq(
                e.let(
                        "x",
                        e.mb.operatorSub(
                                e.call(g(e, "float"), e.mb.operatorShl(e.mb.operatorAnd(id, e.n(1)), e.n(2))), e.n(1))),
                e.let(
                        "y",
                        e.mb.operatorSub(
                                e.call(g(e, "float"), e.mb.operatorShl(e.mb.operatorAnd(id, e.n(2)), e.n(1))), e.n(1))),
                e.mb.set(
                        e.lower(e.v("gl_Position")), e.call(g(e, "vec4"), e.tup(e.v("x"), e.v("y"), e.n(0), e.n(1))))));
        var u = e.lower(e.v("u"));
        var frag = e.lower(e.v("gl_FragCoord"));
        var qx = e.fld(e.v("q"), "x");
        var qy = e.fld(e.v("q"), "y");
        var half = e.n(0.5);
        var sTop = e.mb.operatorMul(e.mb.operatorAdd(qx, e.n(1)), half);
        var sRight = e.mb.operatorAdd(e.mb.operatorMul(e.mb.operatorAdd(e.mb.operatorNeg(qy), e.n(1)), half), e.n(1));
        var sBottom = e.mb.operatorAdd(e.mb.operatorMul(e.mb.operatorAdd(e.mb.operatorNeg(qx), e.n(1)), half), e.n(2));
        var sLeft = e.mb.operatorAdd(e.mb.operatorMul(e.mb.operatorAdd(qy, e.n(1)), half), e.n(3));
        var fragmentBody = e.stage(e.seq(
                e.let(
                        "p",
                        e.mb.operatorSub(
                                e.mb.operatorMul(e.mb.operatorDiv(e.fld(frag, "xy"), e.fld(u, "resolution")), e.n(2)),
                                e.n(1))),
                e.let("a", fn(e, "abs", e.v("p"))),
                e.let("d", fn(e, "max", e.fld(e.v("a"), "x"), e.fld(e.v("a"), "y"))),
                e.let("q", e.mb.operatorDiv(e.v("p"), e.v("d"))),
                e.let(
                        "s",
                        e.ifElse(
                                e.mb.operatorGe(e.fld(e.v("a"), "y"), e.fld(e.v("a"), "x")),
                                e.ifElse(e.mb.operatorGt(e.fld(e.v("p"), "y"), e.n(0)), sTop, sBottom),
                                e.ifElse(e.mb.operatorGt(e.fld(e.v("p"), "x"), e.n(0)), sRight, sLeft))),
                e.let(
                        "wave",
                        e.mb.operatorMul(
                                fn(
                                        e,
                                        "sin",
                                        e.mb.operatorSub(
                                                e.mb.operatorMul(e.v("s"), e.n(6.283185307179586)),
                                                e.mb.operatorMul(e.fld(u, "time"), e.n(3)))),
                                e.n(0.05))),
                e.let("dist", e.mb.operatorSub(e.v("d"), e.mb.operatorAdd(e.v("wave"), e.n(0.6)))),
                e.let("aa", fn(e, "fwidth", e.v("dist"))),
                e.let("cov", fn(e, "smoothstep", e.mb.operatorNeg(e.v("aa")), e.v("aa"), e.v("dist"))),
                e.mb.set(
                        e.lower(e.v("color")),
                        e.call(
                                g(e, "vec4"),
                                e.tup(
                                        fn(
                                                e,
                                                "mix",
                                                e.call(g(e, "vec3"), e.n(0)),
                                                e.call(g(e, "vec3"), e.n(0.5)),
                                                e.v("cov")),
                                        e.n(1))))));
        for (var gv : new String[] {"gl_VertexID", "gl_Position", "gl_FragCoord"}) {
            out.add(e.let(gv, e.call(e.fld(e.v("glsl"), "gl_fn"), e.rec(e.p("name", e.s(gv))))));
        }
        out.add(e.let(
                "color",
                e.call(
                        e.fld(e.v("glsl"), "out_fn"),
                        e.rec(
                                e.p("mod", e.v("fs")),
                                e.p("name", e.s("color")),
                                e.p("ty", e.fld(e.v("glsl"), "vec4"))))));
        out.add(e.call(
                e.fld(e.v("glsl"), "func_fn"),
                e.rec(
                        e.p("mod", e.v("vs")),
                        e.p("name", e.s("main")),
                        e.p("params", e.tup()),
                        e.p("ty", e.v("main_t")),
                        e.p("body", vertexBody))));
        out.add(e.call(
                e.fld(e.v("glsl"), "func_fn"),
                e.rec(
                        e.p("mod", e.v("fs")),
                        e.p("name", e.s("main")),
                        e.p("params", e.tup()),
                        e.p("ty", e.v("main_t")),
                        e.p("body", fragmentBody))));
    }

    private static SourceExpr call(EcoMb e, String hook, SourceExpr... args) {
        return e.call(e.lower(e.v(hook)), args.length == 1 ? args[0] : e.tup(args));
    }

    private static SourceExpr[] str(EcoMb e, String name, String text) {
        return new SourceExpr[] {e.lower(e.v(name)), e.n(text.getBytes(StandardCharsets.UTF_8).length)};
    }

    private static SourceExpr cell(EcoMb e, String data, int index) {
        return e.mb.field(e.lower(e.v(data)), e.n(index));
    }

    public static final String BODY_STYLE =
            "margin:0;height:100vh;display:flex;align-items:center;justify-content:center;background:#808080";
    public static final String CANVAS_STYLE = "width:" + (int) SIZE_CM + "cm;height:" + (int) SIZE_CM + "cm";

    private static SourceExpr byteCount(EcoMb e, SourceExpr text) {
        return e.seq(
                e.let("__n", e.rec(e.p("v", e.intOf(0)))),
                e.foreach(e.call(e.mb.evalStrUtf8(), text), "_", e.incr(e.fld(e.v("__n"), "v"))),
                e.ret(e.fld(e.v("__n"), "v")));
    }

    public static TList<SourceExpr> module(EcoMb e) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        EcoWat.bindTypes(e, setup);
        setup.add(e.let(
                "start_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_void"))))));
        setup.add(e.let(
                "frame_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType(), e.v("t_f64"))), e.p("ret", e.v("t_void"))))));
        var setupOut = new TList<EcoMb.P>();
        for (var n : new String[] {"m", "start_fn_t", "frame_fn_t", "t_i32", "t_u32", "t_f32", "t_f64"}) {
            setupOut.add(e.p(n, e.v(n)));
        }
        setup.add(e.ret(e.rec(setupOut.toArray(EcoMb.P[]::new))));
        var shaderSetup = shaderSetup(e);

        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("webidl", e.importLocal("webidl", "webidl-wasm.atz")));
        top.add(e.let("web", e.importLocal("web", "web-wasm.atz")));
        top.add(e.let("glsl", e.importLocal("glsl", "glsl-es-3.00.atz")));
        top.add(e.let("uniforms", e.importLocal("uniforms", "webgl-uniforms.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        for (var n : new String[] {"m", "start_fn_t", "frame_fn_t", "t_i32", "t_u32", "t_f32", "t_f64"}) {
            top.add(e.let(n, e.fld(e.v("k"), n)));
        }
        top.add(e.let("g", e.lower(e.mb.seq(shaderSetup))));
        for (var n : new String[] {"vs", "fs", "main_t"}) top.add(e.let(n, e.fld(e.v("g"), n)));
        top.add(e.let(
                "u",
                e.call(
                        e.fld(e.v("uniforms"), "glsl_fn"),
                        e.rec(e.p("mod", e.v("fs")), e.p("name", e.s(BLOCK)), e.p("fields", blockFields(e))))));
        top.add(e.let(
                "ub",
                e.call(
                        e.fld(e.v("uniforms"), "wasm_fn"),
                        e.rec(e.p("mod", e.v("m")), e.p("name", e.s(BLOCK)), e.p("fields", blockFields(e))))));
        shaders(e, top);
        top.add(e.let("vtext", e.fld(e.callM0(e.v("vs"), "render"), "glsl")));
        top.add(e.let("ftext", e.fld(e.callM0(e.v("fs"), "render"), "glsl")));
        top.add(e.let("vsrc", e.callM(e.v("m"), "cstr", e.v("vtext"))));
        top.add(e.let("fsrc", e.callM(e.v("m"), "cstr", e.v("ftext"))));
        top.add(e.let("vlen", byteCount(e, e.v("vtext"))));
        top.add(e.let("flen", byteCount(e, e.v("ftext"))));
        for (var s : new String[][] {
            {"s_canvas", "canvas"},
            {"s_style", "style"},
            {"s_body_style", BODY_STYLE},
            {"s_canvas_style", CANVAS_STYLE},
            {"s_webgl2", "webgl2"},
            {"s_block", BLOCK + "_block"},
            {"s_frame", "frame"}
        }) {
            top.add(e.let(s[0], e.callM(e.v("m"), "cstr", e.s(s[1]))));
        }
        for (var d : new String[][] {{"cells", "3"}, {"out", "2"}}) {
            var zeros = new TList<SourceExpr>();
            for (int i = 0; i < Integer.parseInt(d[1]); i++) zeros.add(e.n(0));
            top.add(e.let(
                    d[0],
                    e.call(
                            e.fld(e.v("wasm"), "data_fn"),
                            e.rec(
                                    e.p("mod", e.v("m")),
                                    e.p("name", e.s(d[0])),
                                    e.p("elem", e.v("t_i32")),
                                    e.p("values", e.tup(zeros.toArray(SourceExpr[]::new)))))));
        }
        var hooks = new String[][] {
            {"h_document", "get", "Window", "document"},
            {"h_body", "get", "Document", "body"},
            {"h_create", "op", "Document", "createElement"},
            {"h_set_attr", "op", "Element", "setAttribute"},
            {"h_append", "op", "Node", "appendChild"},
            {"h_dpr", "get", "Window", "devicePixelRatio"},
            {"h_client_width", "get", "Element", "clientWidth"},
            {"h_width", "set", "HTMLCanvasElement", "width"},
            {"h_height", "set", "HTMLCanvasElement", "height"},
            {"h_get_context", "op", "HTMLCanvasElement", "getContext"},
            {"h_raf", "op", "Window", "requestAnimationFrame"},
            {"h_create_shader", "op", "WebGL2RenderingContext", "createShader"},
            {"h_shader_source", "op", "WebGL2RenderingContext", "shaderSource"},
            {"h_compile_shader", "op", "WebGL2RenderingContext", "compileShader"},
            {"h_create_program", "op", "WebGL2RenderingContext", "createProgram"},
            {"h_attach", "op", "WebGL2RenderingContext", "attachShader"},
            {"h_link", "op", "WebGL2RenderingContext", "linkProgram"},
            {"h_use_program", "op", "WebGL2RenderingContext", "useProgram"},
            {"h_create_buffer", "op", "WebGL2RenderingContext", "createBuffer"},
            {"h_bind_buffer", "op", "WebGL2RenderingContext", "bindBuffer"},
            {"h_bind_buffer_base", "op", "WebGL2RenderingContext", "bindBufferBase"},
            {"h_block_index", "op", "WebGL2RenderingContext", "getUniformBlockIndex"},
            {"h_block_binding", "op", "WebGL2RenderingContext", "uniformBlockBinding"},
            {"h_viewport", "op", "WebGL2RenderingContext", "viewport"},
            {"h_buffer_data", "op", "WebGL2RenderingContext", "bufferData#1"},
            {"h_draw_arrays", "op", "WebGL2RenderingContext", "drawArrays"},
            {"c_vertex", "const", "WebGL2RenderingContext", "VERTEX_SHADER"},
            {"c_fragment", "const", "WebGL2RenderingContext", "FRAGMENT_SHADER"},
            {"c_uniform_buffer", "const", "WebGL2RenderingContext", "UNIFORM_BUFFER"},
            {"c_dynamic_draw", "const", "WebGL2RenderingContext", "DYNAMIC_DRAW"},
            {"c_triangles", "const", "WebGL2RenderingContext", "TRIANGLES"}
        };
        var out = cell(e, "out", 1);
        var canvas = str(e, "s_canvas", "canvas");
        var style = str(e, "s_style", "style");
        var bodyStyle = str(e, "s_body_style", BODY_STYLE);
        var canvasStyle = str(e, "s_canvas_style", CANVAS_STYLE);
        var webgl2 = str(e, "s_webgl2", "webgl2");
        var block = str(e, "s_block", BLOCK + "_block");
        var frame = str(e, "s_frame", "frame");
        var px = e.v("px");
        var startBody = e.stage(e.seq(
                e.let("win", e.call(e.lower(e.fld(e.v("webidl"), "global")), e.tup())),
                e.mb.set(cell(e, "cells", 0), e.v("win")),
                e.let("doc", call(e, "h_document", e.v("win"))),
                call(e, "h_body", e.v("doc"), e.lower(e.v("out"))),
                e.let("body", out),
                call(e, "h_set_attr", e.v("body"), style[0], style[1], bodyStyle[0], bodyStyle[1]),
                e.let("canvas", call(e, "h_create", e.v("doc"), canvas[0], canvas[1], e.n(0), e.n(0), e.n(0))),
                call(e, "h_set_attr", e.v("canvas"), style[0], style[1], canvasStyle[0], canvasStyle[1]),
                call(e, "h_append", e.v("body"), e.v("canvas")),
                e.let("dpr", call(e, "h_dpr", e.v("win"))),
                e.let("cw", call(e, "h_client_width", e.v("canvas"))),
                e.let(
                        "px",
                        e.call(
                                e.lower(e.v("t_i32")),
                                e.mb.operatorMul(e.call(e.lower(e.v("t_f64")), e.v("cw")), e.v("dpr")))),
                call(e, "h_width", e.v("canvas"), e.call(e.lower(e.v("t_u32")), px)),
                call(e, "h_height", e.v("canvas"), e.call(e.lower(e.v("t_u32")), px)),
                call(e, "h_get_context", e.v("canvas"), webgl2[0], webgl2[1], e.n(0), e.n(0), e.lower(e.v("out"))),
                e.let("gl", out),
                e.mb.set(cell(e, "cells", 1), e.v("gl")),
                call(e, "h_create_shader", e.v("gl"), e.lower(e.v("c_vertex")), e.lower(e.v("out"))),
                e.let("vsh", out),
                call(e, "h_shader_source", e.v("gl"), e.v("vsh"), e.lower(e.v("vsrc")), e.lower(e.v("vlen"))),
                call(e, "h_compile_shader", e.v("gl"), e.v("vsh")),
                call(e, "h_create_shader", e.v("gl"), e.lower(e.v("c_fragment")), e.lower(e.v("out"))),
                e.let("fsh", out),
                call(e, "h_shader_source", e.v("gl"), e.v("fsh"), e.lower(e.v("fsrc")), e.lower(e.v("flen"))),
                call(e, "h_compile_shader", e.v("gl"), e.v("fsh")),
                e.let("prog", call(e, "h_create_program", e.v("gl"))),
                call(e, "h_attach", e.v("gl"), e.v("prog"), e.v("vsh")),
                call(e, "h_attach", e.v("gl"), e.v("prog"), e.v("fsh")),
                call(e, "h_link", e.v("gl"), e.v("prog")),
                call(e, "h_use_program", e.v("gl"), e.n(1), e.v("prog")),
                e.let("buf", call(e, "h_create_buffer", e.v("gl"))),
                call(e, "h_bind_buffer", e.v("gl"), e.lower(e.v("c_uniform_buffer")), e.n(1), e.v("buf")),
                call(e, "h_bind_buffer_base", e.v("gl"), e.lower(e.v("c_uniform_buffer")), e.n(0), e.n(1), e.v("buf")),
                e.let("idx", call(e, "h_block_index", e.v("gl"), e.v("prog"), block[0], block[1])),
                call(e, "h_block_binding", e.v("gl"), e.v("prog"), e.v("idx"), e.n(0)),
                call(e, "h_viewport", e.v("gl"), e.n(0), e.n(0), px, px),
                e.mb.set(
                        e.fld(e.lower(e.v("ub")), "resolution"),
                        e.tup(e.call(e.lower(e.v("t_f32")), px), e.call(e.lower(e.v("t_f32")), px))),
                e.let("cb", e.call(e.lower(e.fld(e.v("webidl"), "callback")), e.tup(frame[0], frame[1]))),
                e.mb.set(cell(e, "cells", 2), e.v("cb")),
                call(e, "h_raf", e.v("win"), e.v("cb"))));
        var frameBody = e.stage(e.seq(
                e.mb.set(
                        e.fld(e.lower(e.v("ub")), "time"),
                        e.call(e.lower(e.v("t_f32")), e.mb.operatorMul(e.v("t"), e.n(0.001)))),
                e.let("gl", cell(e, "cells", 1)),
                call(
                        e,
                        "h_buffer_data",
                        e.v("gl"),
                        e.lower(e.v("c_uniform_buffer")),
                        e.n(1),
                        e.fld(e.lower(e.v("ub")), "ptr"),
                        e.fld(e.lower(e.v("ub")), "size"),
                        e.lower(e.v("c_dynamic_draw"))),
                call(e, "h_draw_arrays", e.v("gl"), e.lower(e.v("c_triangles")), e.n(0), e.n(3)),
                call(e, "h_raf", cell(e, "cells", 0), cell(e, "cells", 2))));
        for (var h : hooks) {
            top.add(e.let(
                    h[0],
                    e.call(e.fld(e.v("webidl"), h[1]), e.rec(e.p("iface", e.s(h[2])), e.p("member", e.s(h[3]))))));
        }
        top.add(e.call(
                e.fld(e.v("wasm"), "func_fn"),
                e.rec(
                        e.p("mod", e.v("m")),
                        e.p("name", e.s("_start")),
                        e.p("params", e.tup()),
                        e.p("ty", e.v("start_fn_t")),
                        e.p("body", startBody))));
        top.add(e.call(
                e.fld(e.v("wasm"), "func_fn"),
                e.rec(
                        e.p("mod", e.v("m")),
                        e.p("name", e.s("frame")),
                        e.p("params", e.tup(e.s("t"))),
                        e.p("ty", e.v("frame_fn_t")),
                        e.p("body", frameBody))));
        top.add(e.callM(e.v("m"), "export", e.s("_start")));
        top.add(e.callM(e.v("m"), "export", e.s("frame")));
        top.add(e.ret(e.call(e.fld(e.v("web"), "build"), e.callM0(e.v("m"), "render"))));
        return top;
    }
}
