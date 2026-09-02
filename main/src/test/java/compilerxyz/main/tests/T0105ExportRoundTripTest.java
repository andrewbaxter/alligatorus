package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0105ExportRoundTripTest {
    private static TList<String> compileLogs(SourceExpr lib, SourceExpr main) {
        var modules = new TMap<String, SourceModule>();
        modules.put("lib", new SourceModule.V1(lib));
        modules.put("main", new SourceModule.V1(main));
        var res = Compile.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, ?> ok;
        switch (res) {
            case Result.Ok<Compile.CompileResult, ?> o -> ok = o;
            case Result.Err<Compile.CompileResult, ?> err -> throw new AssertionError("compile failed: " + res);
        }
        var out = new TList<String>();
        for (var log : ok.value().logs()) out.add(log.message());
        return out;
    }

    private static SourceExpr importLib(EcoMb e) {
        return e.let("r", e.mb.importExpr(e.s("lib")));
    }

    @Test
    public void test00000ConstStringField() {
        var e = new EcoMb();
        var lib = e.rec(e.p("s", e.s("from-field")));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.fld(e.v("r"), "s")));
        assertEquals(TList.of("from-field"), compileLogs(lib, main));
    }

    @Test
    public void test00050ConstStringFieldLowered() {
        var e = new EcoMb();
        var lib = e.lower(e.seq(
                e.let("x", e.listNew(e.mb.typesString(), e.s("effect"))),
                e.push(e.v("x"), e.s("more")),
                e.ret(e.rec(e.p("s", e.s("from-lowered"))))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.fld(e.v("r"), "s")));
        assertEquals(TList.of("from-lowered"), compileLogs(lib, main));
    }

    private static SourceExpr defsetLib(EcoMb e, SourceExpr... extra) {
        var exprs = new TList<SourceExpr>();
        exprs.add(e.let("d", e.define()));
        for (var x : extra) exprs.add(x);
        return e.mb.seq(exprs);
    }

    @Test
    public void test00100FuncField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("fb", e.defFunc(e.v("d"), "greet")),
                e.funcImpl(e.v("fb"), e.mb.typesNull(), e.retSome(e.mb.typesString()), e.stage(e.s("from-func"))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("f", e.fld(e.v("fb"), "func"))))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.call(e.fld(e.v("r"), "f"), e.nil())));
        assertEquals(TList.of("from-func"), compileLogs(lib, main));
    }

    @Test
    public void test00200ObjectTypeAndConstructor() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "thing")),
                e.callM(e.v("obj"), "set_inner", e.rec(e.p("name", e.mb.typesString()))),
                e.methodImpl(
                        e.v("obj"),
                        "name",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.fld(e.v("inner"), "name"))),
                e.let("mk", e.defFunc(e.v("d"), "mk")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.stage(e.call(e.lower(e.fld(e.v("obj"), "new")), e.rec(e.p("name", e.v("arg")))))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("mk", e.fld(e.v("mk"), "func")), e.p("obj_t", e.fld(e.v("obj"), "type"))))));
        var main = e.seq(
                importLib(e),
                e.let("inst", e.call(e.fld(e.v("r"), "mk"), e.s("roundtrip"))),
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.nil())));
        assertEquals(TList.of("roundtrip"), compileLogs(lib, main));
    }

    @Test
    public void test00300UnionCtorField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.fld(
                                e.callM(
                                        e.v("uni"),
                                        "variant",
                                        e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString()))),
                                "value")),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("uni_a", e.v("uni_a"))))));
        var main = e.seq(
                importLib(e),
                e.let("av", e.call(e.fld(e.v("r"), "uni_a"), e.s("in-union"))),
                e.mb.matchOf(
                        e.v("av"),
                        e.s("x"),
                        TList.of(new TestMb.Branch(e.s("a"), e.call(e.mb.log(), e.v("x")))),
                        Opt.none()));
        assertEquals(TList.of("in-union"), compileLogs(lib, main));
    }

    @Test
    public void test00400UnionTypeField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.fld(
                                e.callM(
                                        e.v("uni"),
                                        "variant",
                                        e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString()))),
                                "value")),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("uni_t", e.fld(e.v("uni"), "type"))))));
        var main = e.seq(
                importLib(e), e.let("t", e.fld(e.v("r"), "uni_t")), e.call(e.mb.log(), e.s("union-type-imported")));
        assertEquals(TList.of("union-type-imported"), compileLogs(lib, main));
    }

    @Test
    public void test00500InterfaceTypeField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("iface", e.callM0(e.v("d"), "interface")),
                e.let("iface_m", e.callM(e.v("iface"), "func", e.s("m"))),
                e.callM(
                        e.v("iface_m"),
                        "impl",
                        e.rec(e.p("arg", e.mb.typesNull()), e.p("ret", e.retSome(e.mb.typesString())))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("iface_t", e.fld(e.v("iface"), "type"))))));
        var main = e.seq(
                importLib(e), e.let("t", e.fld(e.v("r"), "iface_t")), e.call(e.mb.log(), e.s("iface-type-imported")));
        assertEquals(TList.of("iface-type-imported"), compileLogs(lib, main));
    }

    @Test
    public void test00600AllMembersCombined() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "thing")),
                e.callM(e.v("obj"), "set_inner", e.rec(e.p("name", e.mb.typesString()))),
                e.methodImpl(
                        e.v("obj"),
                        "name",
                        e.mb.typesNull(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.fld(e.v("inner"), "name"))),
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.fld(
                                e.callM(
                                        e.v("uni"),
                                        "variant",
                                        e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString()))),
                                "value")),
                e.let("iface", e.callM0(e.v("d"), "interface")),
                e.let("iface_m", e.callM(e.v("iface"), "func", e.s("m"))),
                e.callM(
                        e.v("iface_m"),
                        "impl",
                        e.rec(e.p("arg", e.mb.typesNull()), e.p("ret", e.retSome(e.mb.typesString())))),
                e.let("mk", e.defFunc(e.v("d"), "mk")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.stage(e.call(e.lower(e.fld(e.v("obj"), "new")), e.rec(e.p("name", e.v("arg")))))),
                e.finish(e.v("d")),
                e.ret(e.rec(
                        e.p("mk", e.fld(e.v("mk"), "func")),
                        e.p("obj_t", e.fld(e.v("obj"), "type")),
                        e.p("uni_t", e.fld(e.v("uni"), "type")),
                        e.p("uni_a", e.v("uni_a")),
                        e.p("iface_t", e.fld(e.v("iface"), "type"))))));
        var main = e.seq(
                importLib(e),
                e.let("inst", e.call(e.fld(e.v("r"), "mk"), e.s("roundtrip"))),
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.nil())),
                e.let("av", e.call(e.fld(e.v("r"), "uni_a"), e.s("in-union"))),
                e.mb.matchOf(
                        e.v("av"),
                        e.s("x"),
                        TList.of(new TestMb.Branch(e.s("a"), e.call(e.mb.log(), e.v("x")))),
                        Opt.none()));
        assertEquals(TList.of("roundtrip", "in-union"), compileLogs(lib, main));
    }

    @Test
    public void test00700CodeInSignatureRoundTrips() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("fb", e.defFunc(e.v("d"), "takes_code")),
                e.funcImpl(
                        e.v("fb"),
                        e.rec(e.p("c", e.mb.typesCode())),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.mb.as_(e.s("from-code-fn"), e.mb.typesString()))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("f", e.fld(e.v("fb"), "func"))))));
        var main = e.seq(importLib(e), e.let("f", e.fld(e.v("r"), "f")), e.call(e.mb.log(), e.s("imported-code-fn")));
        assertEquals(TList.of("imported-code-fn"), compileLogs(lib, main));
    }

    @Test
    public void test00800TypeValueFieldRoundTrips() {
        var e = new EcoMb();
        var lib = e.rec(e.p("t", e.mb.typesInt()));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.toStr(e.mb.as_(e.n(3.), e.fld(e.v("r"), "t")))));
        assertEquals(TList.of("3"), compileLogs(lib, main));
    }
}
