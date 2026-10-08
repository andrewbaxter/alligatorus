package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0200EcosystemLlvmTest.compileOk;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.onPath;
import static compilerxyz.main.tests.T0200EcosystemLlvmTest.writeModule;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.prelude.util.TList;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0220EcosystemWebidlTest {
    private static final String SNIPPET =
            """
            // comment
            [Exposed=Window]
            interface Node : EventTarget {
              const unsigned short ELEMENT_NODE = 1;
              readonly attribute DOMString nodeName;
              [CEReactions] attribute DOMString? textContent;
              [CEReactions] Node appendChild(Node node);
              Promise<undefined> wait(optional unsigned long long ms = 0, DOMString... names);
              getter object (DOMString name);
              static boolean isNode((Node or DOMString) thing);
              iterable<DOMString>;
              constructor(sequence<DOMString> parts);
            };
            partial interface Node {
              undefined normalize();
            };
            interface mixin Slottable { readonly attribute HTMLSlotElement? assignedSlot; };
            Node includes Slottable;
            typedef (Int8Array or Int16Array) ArrayBufferView;
            enum Dir { "ltr", "rtl" };
            dictionary Init { boolean bubbles = false; required long count; };
            callback EventHandlerNonNull = any (Event event);
            /* block
               comment */
            """;

    private static TList<SourceExpr> module(EcoMb e, SourceExpr text, boolean full) {
        var setup = new TList<SourceExpr>();
        setup.add(e.let("webidl", e.importLocal("webidl", "webidl.atz")));
        setup.add(e.let("d", e.define()));
        setup.add(e.optionTypeLet());
        setup.add(e.let("tyname_b", e.defFunc(e.v("d"), "tyname")));
        setup.add(e.funcImpl(
                e.v("tyname_b"),
                e.fld(e.v("webidl"), "itype_t"),
                e.optionTypeSome(e.mb.typesString()),
                e.mb.optNoneType(),
                e.stage(e.seq(
                        e.let("parts", e.listNew(e.mb.typesString())),
                        e.let("n", e.rec(e.p("v", e.intOf(0)))),
                        e.foreach(
                                e.callM0(e.v("arg"), "args"),
                                "a0",
                                e.seq(
                                        e.push(
                                                e.v("parts"),
                                                e.call(e.lower(e.fld(e.v("tyname_b"), "func")), e.v("a0"))),
                                        e.incr(e.fld(e.v("n"), "v")))),
                        e.ret(e.cat(
                                e.callM0(e.v("arg"), "name"),
                                e.ifElse(
                                        e.mb.operatorGt(e.fld(e.v("n"), "v"), e.n(0)),
                                        e.cat(e.s("<"), e.join(e.v("parts"), ", "), e.s(">")),
                                        e.dstr("")),
                                e.ifElse(e.callM0(e.v("arg"), "nullable"), e.dstr("?"), e.dstr(""))))))));
        setup.add(e.let("tyname", e.fld(e.v("tyname_b"), "func")));
        setup.add(e.finish(e.v("d")));
        setup.add(e.ret(e.rec(e.p("tyname", e.v("tyname")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("webidl", e.importLocal("webidl", "webidl.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("tyname", e.fld(e.v("k"), "tyname")));
        if (full) {
            top.add(e.let("table", e.fld(e.v("webidl"), "spec")));
        } else {
            top.add(e.let("table", e.call(e.fld(e.v("webidl"), "table"), e.call(e.fld(e.v("webidl"), "parse"), text))));
        }
        for (var iface : new String[] {"Node"}) {
            top.add(e.let("def", e.callM(e.callM0(e.v("table"), "defs"), "get", e.s(iface))));
            top.add(e.call(
                    e.mb.log(),
                    e.cat(
                            e.callM0(e.v("def"), "kind"),
                            e.s(" "),
                            e.callM0(e.v("def"), "name"),
                            e.s(" : "),
                            e.callM0(e.v("def"), "parent"))));
            top.add(e.foreach(
                    e.callM0(e.v("def"), "members"),
                    "m",
                    e.call(
                            e.mb.log(),
                            e.seq(
                                    e.let("parts", e.listNew(e.mb.typesString())),
                                    e.foreach(
                                            e.callM0(e.v("m"), "args"),
                                            "ar",
                                            e.push(
                                                    e.v("parts"),
                                                    e.cat(
                                                            e.ifElse(
                                                                    e.callM0(e.v("ar"), "optional"),
                                                                    e.dstr("?"),
                                                                    e.dstr("")),
                                                            e.ifElse(
                                                                    e.callM0(e.v("ar"), "variadic"),
                                                                    e.dstr("..."),
                                                                    e.dstr("")),
                                                            e.call(e.v("tyname"), e.callM0(e.v("ar"), "ty")),
                                                            e.s(" "),
                                                            e.callM0(e.v("ar"), "name")))),
                                    e.ret(e.cat(
                                            e.ifElse(e.callM0(e.v("m"), "static_"), e.dstr("static "), e.dstr("")),
                                            e.ifElse(e.callM0(e.v("m"), "readonly"), e.dstr("readonly "), e.dstr("")),
                                            e.callM0(e.v("m"), "special"),
                                            e.s(" "),
                                            e.callM0(e.v("m"), "kind"),
                                            e.s(" "),
                                            e.callM0(e.v("m"), "name"),
                                            e.s("("),
                                            e.join(e.v("parts"), ", "),
                                            e.s(") : "),
                                            e.call(e.v("tyname"), e.callM0(e.v("m"), "ty"))))))));
        }
        return top;
    }

    @Test
    public void test00000Snippet(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("git"), "git not on PATH");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("webidl.atz"), EcoWebidl.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(src.resolve("main.at"), module(e, e.s(SNIPPET), false));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var lines = new TList<String>();
        for (var log : ok.logs()) lines.add(log.message());
        assertEquals(
                """
                interface Node : EventTarget
                 other const() : undefined
                readonly  attr nodeName() : DOMString
                 attr textContent() : DOMString?
                 op appendChild(Node node) : Node
                 op wait(?unsigned long long ms, ...DOMString names) : Promise<undefined>
                getter op (DOMString name) : object
                static  op isNode(union<Node, DOMString> thing) : boolean
                 other iterable() : undefined
                 ctor constructor(sequence<DOMString> parts) : undefined
                 op normalize() : undefined
                readonly  attr assignedSlot() : HTMLSlotElement?""",
                String.join("\n", lines));
    }

    @Test
    public void test00010Webref(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("git"), "git not on PATH");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("webidl.atz"), EcoWebidl.module(new EcoMb()));
        var e = new EcoMb();
        writeModule(src.resolve("main.at"), module(e, e.s(""), true));
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var lines = new TList<String>();
        for (var log : ok.logs()) lines.add(log.message());
        assertEquals("interface Node : EventTarget", lines.get(0));
        var joined = String.join("\n", lines);
        assertEquals(true, joined.contains(" op appendChild(Node node) : Node"), joined);
        assertEquals(true, joined.contains(" attr textContent() : DOMString?"), joined);
    }

    @Test
    public void test00020WasmHooks(@TempDir Path tmp) throws IOException {
        Assumptions.assumeTrue(onPath("git"), "git not on PATH");
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        writeModule(src.resolve("webidl.atz"), EcoWebidl.module(new EcoMb()));
        writeModule(src.resolve(EcoMeta.FILE), EcoMeta.module(new EcoMb()));
        writeModule(src.resolve("wat-2.0.atz"), EcoWat.module(new EcoMb()));
        writeModule(src.resolve("webidl-wasm.atz"), EcoWebidlWasm.module(new EcoMb()));
        var e = new EcoMb();
        var setup = new TList<SourceExpr>();
        setup.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        setup.add(e.let("m", e.call(e.fld(e.v("wasm"), "new"), e.tup())));
        EcoWat.bindTypes(e, setup);
        setup.add(e.let(
                "start_fn_t",
                e.call(
                        e.fld(e.v("wasm"), "fn"),
                        e.rec(e.p("params", e.listNew(e.mb.evalType())), e.p("ret", e.v("t_void"))))));
        setup.add(e.ret(e.rec(e.p("m", e.v("m")), e.p("start_fn_t", e.v("start_fn_t")), e.p("t_i32", e.v("t_i32")))));
        var top = new TList<SourceExpr>();
        top.add(e.let("wasm", e.importLocal("wasm", "wat-2.0.atz")));
        top.add(e.let("webidl", e.importLocal("webidl", "webidl-wasm.atz")));
        top.add(e.let("k", e.lower(e.mb.seq(setup))));
        top.add(e.let("m", e.fld(e.v("k"), "m")));
        top.add(e.let("tag", e.callM(e.v("m"), "cstr", e.s("h1"))));
        top.add(e.let(
                "cell",
                e.call(
                        e.fld(e.v("wasm"), "data_fn"),
                        e.rec(
                                e.p("mod", e.v("m")),
                                e.p("name", e.s("out")),
                                e.p("elem", e.fld(e.v("k"), "t_i32")),
                                e.p("values", e.tup(e.n(0), e.n(0)))))));
        top.add(e.let(
                "create",
                e.call(
                        e.fld(e.v("webidl"), "op"),
                        e.rec(e.p("iface", e.s("Document")), e.p("member", e.s("createElement"))))));
        top.add(e.let(
                "body",
                e.call(e.fld(e.v("webidl"), "get"), e.rec(e.p("iface", e.s("Document")), e.p("member", e.s("body"))))));
        top.add(e.let(
                "text",
                e.call(
                        e.fld(e.v("webidl"), "set"),
                        e.rec(e.p("iface", e.s("HTMLElement")), e.p("member", e.s("textContent"))))));
        top.add(e.call(
                e.fld(e.v("wasm"), "func_fn"),
                e.rec(
                        e.p("mod", e.v("m")),
                        e.p("name", e.s("_start")),
                        e.p("params", e.tup()),
                        e.p("ty", e.fld(e.v("k"), "start_fn_t")),
                        e.p(
                                "body",
                                e.stage(e.seq(
                                        e.let("g", e.call(e.lower(e.fld(e.v("webidl"), "global")), e.tup())),
                                        e.let(
                                                "h1",
                                                e.call(
                                                        e.lower(e.v("create")),
                                                        e.tup(e.v("g"), e.lower(e.v("tag")), e.n(2), e.n(0), e.n(0)))),
                                        e.call(e.lower(e.v("body")), e.tup(e.v("g"), e.lower(e.v("cell")))),
                                        e.call(
                                                e.lower(e.v("text")),
                                                e.tup(e.v("h1"), e.n(1), e.lower(e.v("tag")), e.n(2)))))))));
        top.add(e.callM(e.v("m"), "export", e.s("_start")));
        top.add(e.call(e.mb.log(), e.fld(e.callM0(e.v("m"), "render"), "wat")));
        writeModule(src.resolve("main.at"), top);
        var ok = compileOk(src.resolve("main.at"), tmp.resolve("cache"));
        var wat = ok.logs().get(0).message();
        assertEquals(
                """
                (module
                  (import "webidl" "global" (func $global (result i32)))
                  (import "webidl" "Document.createElement" (func $Document.createElement (param i32) (param i32) (param i32) (param i32) (param i32) (result i32)))
                  (import "webidl" "Document.body" (func $Document.body (param i32) (param i32)))
                  (import "webidl" "Node.textContent=" (func $Node.textContent= (param i32) (param i32) (param i32) (param i32)))
                """,
                wat.substring(0, wat.indexOf("  (memory")));
    }
}
