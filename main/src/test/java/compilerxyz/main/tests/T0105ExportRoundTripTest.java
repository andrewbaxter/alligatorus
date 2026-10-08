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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public final class T0105ExportRoundTripTest {
    private static TList<String> compileLogs(EcoMb e, SourceExpr lib, SourceExpr main) {
        var modules = new TMap<String, SourceModule>();
        modules.put("lib", new SourceModule.V1(TList.of(e.ret(lib))));
        modules.put("main", new SourceModule.V1(TList.of(main)));
        var res = Helpers.compile(Helpers.newGx(modules), new SourceSpecAbsolute.Virtual("main"));
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
        assertEquals(TList.of("from-field"), compileLogs(e, lib, main));
    }

    @Test
    public void test00050ConstStringFieldLowered() {
        var e = new EcoMb();
        var lib = e.lower(e.seq(
                e.let("x", e.listNew(e.mb.typesString(), e.s("effect"))),
                e.push(e.v("x"), e.s("more")),
                e.ret(e.rec(e.p("s", e.s("from-lowered"))))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.fld(e.v("r"), "s")));
        assertEquals(TList.of("from-lowered"), compileLogs(e, lib, main));
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
                e.funcImpl(
                        e.v("fb"),
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.mb.optNoneType(),
                        e.stage(e.s("from-func"))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("f", e.fld(e.v("fb"), "func"))))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.call(e.fld(e.v("r"), "f"), e.tup())));
        assertEquals(TList.of("from-func"), compileLogs(e, lib, main));
    }

    @Test
    public void test00200ObjectTypeAndConstructor() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "thing")),
                e.objectImpl(e.v("obj"), e.rec(e.p("name", e.mb.typesString()))),
                e.methodImpl(
                        e.v("obj"),
                        "name",
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.fld(e.v("inner"), "name"))),
                e.let("mk", e.defFunc(e.v("d"), "mk")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.mb.optNoneType(),
                        e.stage(e.call(e.lower(e.fld(e.v("obj"), "new")), e.rec(e.p("name", e.v("arg")))))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("mk", e.fld(e.v("mk"), "func")), e.p("obj_t", e.fld(e.v("obj"), "type"))))));
        var main = e.seq(
                importLib(e),
                e.let("inst", e.call(e.fld(e.v("r"), "mk"), e.s("roundtrip"))),
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.tup())));
        assertEquals(TList.of("roundtrip"), compileLogs(e, lib, main));
    }

    @Test
    public void test00300UnionCtorField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.callM(
                                e.v("uni"),
                                "variant",
                                e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString())))),
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
        assertEquals(TList.of("in-union"), compileLogs(e, lib, main));
    }

    @Test
    public void test00400UnionTypeField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.callM(
                                e.v("uni"),
                                "variant",
                                e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString())))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("uni_t", e.fld(e.v("uni"), "type"))))));
        var main = e.seq(
                importLib(e), e.let("t", e.fld(e.v("r"), "uni_t")), e.call(e.mb.log(), e.s("union-type-imported")));
        assertEquals(TList.of("union-type-imported"), compileLogs(e, lib, main));
    }

    @Test
    public void test00500InterfaceTypeField() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("iface", e.callM0(e.v("d"), "interface")),
                e.interfaceImpl(e.v("iface"), false),
                e.callM(
                        e.v("iface"),
                        "func",
                        e.rec(
                                e.p("name", e.s("m")),
                                e.p("arg", e.mb.tupleOf()),
                                e.p("ret", e.retSome(e.mb.typesString())))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("iface_t", e.fld(e.v("iface"), "type"))))));
        var main = e.seq(
                importLib(e), e.let("t", e.fld(e.v("r"), "iface_t")), e.call(e.mb.log(), e.s("iface-type-imported")));
        assertEquals(TList.of("iface-type-imported"), compileLogs(e, lib, main));
    }

    @Test
    public void test00600AllMembersCombined() {
        var e = new EcoMb();
        var lib = e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "thing")),
                e.objectImpl(e.v("obj"), e.rec(e.p("name", e.mb.typesString()))),
                e.methodImpl(
                        e.v("obj"),
                        "name",
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.fld(e.v("inner"), "name"))),
                e.let("uni", e.callM0(e.v("d"), "union")),
                e.let(
                        "uni_a",
                        e.callM(
                                e.v("uni"),
                                "variant",
                                e.rec(e.p("tag", e.s("a")), e.p("type_ref", e.mb.typesString())))),
                e.let("iface", e.callM0(e.v("d"), "interface")),
                e.interfaceImpl(e.v("iface"), false),
                e.callM(
                        e.v("iface"),
                        "func",
                        e.rec(
                                e.p("name", e.s("m")),
                                e.p("arg", e.mb.tupleOf()),
                                e.p("ret", e.retSome(e.mb.typesString())))),
                e.let("mk", e.defFunc(e.v("d"), "mk")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.typesString(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.mb.optNoneType(),
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
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.tup())),
                e.let("av", e.call(e.fld(e.v("r"), "uni_a"), e.s("in-union"))),
                e.mb.matchOf(
                        e.v("av"),
                        e.s("x"),
                        TList.of(new TestMb.Branch(e.s("a"), e.call(e.mb.log(), e.v("x")))),
                        Opt.none()));
        assertEquals(TList.of("roundtrip", "in-union"), compileLogs(e, lib, main));
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
                        e.mb.optNoneType(),
                        e.stage(e.mb.as(e.s("from-code-fn"), e.mb.typesString()))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("f", e.fld(e.v("fb"), "func"))))));
        var main = e.seq(importLib(e), e.let("f", e.fld(e.v("r"), "f")), e.call(e.mb.log(), e.s("imported-code-fn")));
        assertEquals(TList.of("imported-code-fn"), compileLogs(e, lib, main));
    }

    @Test
    public void test00800TypeValueFieldRoundTrips() {
        var e = new EcoMb();
        var lib = e.rec(e.p("t", e.mb.typesInt()));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.toStr(e.mb.as(e.n(3.), e.fld(e.v("r"), "t")))));
        assertEquals(TList.of("3"), compileLogs(e, lib, main));
    }

    private static SourceExpr thingDefs(EcoMb e) {
        return e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "thing")),
                e.objectImpl(e.v("obj"), e.rec(e.p("name", e.mb.typesString()))),
                e.methodImpl(
                        e.v("obj"),
                        "name",
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.fld(e.v("inner"), "name"))),
                e.let("fb", e.defFunc(e.v("d"), "greet")),
                e.funcImpl(
                        e.v("fb"),
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.mb.optNoneType(),
                        e.stage(e.s("from-func"))),
                e.finish(e.v("d")),
                e.ret(e.rec(
                        e.p("f", e.fld(e.v("fb"), "func")),
                        e.p("obj_t", e.fld(e.v("obj"), "type")),
                        e.p("obj_new", e.fld(e.v("obj"), "new"))))));
    }

    @Test
    public void test00900LoweredFuncReexported() {
        var e = new EcoMb();
        var lib = e.seq(e.let("k", thingDefs(e)), e.ret(e.rec(e.p("f", e.fld(e.v("k"), "f")))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.call(e.fld(e.v("r"), "f"), e.tup())));
        assertEquals(TList.of("from-func"), compileLogs(e, lib, main));
    }

    @Test
    public void test01000LoweredObjectTypeReexported() {
        var e = new EcoMb();
        var lib = e.seq(e.let("k", thingDefs(e)), e.ret(e.rec(e.p("obj_t", e.fld(e.v("k"), "obj_t")))));
        var main =
                e.seq(importLib(e), e.let("t", e.fld(e.v("r"), "obj_t")), e.call(e.mb.log(), e.s("obj-type-imported")));
        assertEquals(TList.of("obj-type-imported"), compileLogs(e, lib, main));
    }

    @Test
    public void test01050LoweredObjectCtorReexported() {
        var e = new EcoMb();
        var lib = e.seq(e.let("k", thingDefs(e)), e.ret(e.rec(e.p("obj_new", e.fld(e.v("k"), "obj_new")))));
        var main = e.seq(
                importLib(e),
                e.let("inst", e.call(e.fld(e.v("r"), "obj_new"), e.rec(e.p("name", e.s("via-ctor"))))),
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.tup())));
        assertEquals(TList.of("via-ctor"), compileLogs(e, lib, main));
    }

    @Test
    public void test01100InstanceBuiltFromLoweredCtor() {
        var e = new EcoMb();
        var lib = e.seq(
                e.let("k", thingDefs(e)),
                e.ret(e.rec(e.p("inst", e.call(e.fld(e.v("k"), "obj_new"), e.rec(e.p("name", e.s("inst-name"))))))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.callM(e.fld(e.v("r"), "inst"), "name", e.tup())));
        assertEquals(TList.of("inst-name"), compileLogs(e, lib, main));
    }

    @Test
    public void test01200LoweredDefsAndInstanceReexported() {
        var e = new EcoMb();
        var lib = e.seq(
                e.let("k", thingDefs(e)),
                e.ret(e.rec(
                        e.p("f", e.fld(e.v("k"), "f")),
                        e.p("obj_t", e.fld(e.v("k"), "obj_t")),
                        e.p("inst", e.call(e.fld(e.v("k"), "obj_new"), e.rec(e.p("name", e.s("inst-name"))))))));
        var main = e.seq(
                importLib(e),
                e.call(e.mb.log(), e.call(e.fld(e.v("r"), "f"), e.tup())),
                e.call(
                        e.mb.log(),
                        e.callM(e.mb.as(e.fld(e.v("r"), "inst"), e.fld(e.v("r"), "obj_t")), "name", e.tup())));
        assertEquals(TList.of("from-func", "inst-name"), compileLogs(e, lib, main));
    }

    private static SourceExpr cellDefs(EcoMb e) {
        return e.lower(defsetLib(
                e,
                e.let("obj", e.defObject(e.v("d"), "cell")),
                e.objectImpl(
                        e.v("obj"),
                        e.rec(
                                e.p("flag", e.mb.typesBool()),
                                e.p("items", e.listType(e.mb.typesString())),
                                e.p("counts", e.mapType(e.mb.typesString(), e.mb.typesIntBox())))),
                e.methodImpl(
                        e.v("obj"),
                        "reset",
                        e.mb.tupleOf(),
                        e.mb.optNoneType(),
                        e.stage(e.seq(
                                e.mb.set(e.fld(e.v("inner"), "flag"), e.mb.as(e.b(true), e.mb.typesBool())),
                                e.mb.set(e.fld(e.v("inner"), "items"), e.strList(e.s("reset"))),
                                e.mb.set(
                                        e.fld(e.v("inner"), "counts"),
                                        e.mapNew(e.mb.typesString(), e.mb.typesIntBox(), e.p("n", e.intOf(7))))))),
                e.methodImpl(
                        e.v("obj"),
                        "describe",
                        e.mb.tupleOf(),
                        e.retSome(e.mb.typesString()),
                        e.stage(e.cat(
                                e.ifElse(e.fld(e.v("inner"), "flag"), e.dstr("on"), e.dstr("off")),
                                e.s(":"),
                                e.join(e.fld(e.v("inner"), "items"), ","),
                                e.s(":"),
                                e.toStr(e.mb.as(
                                        e.callM(e.fld(e.v("inner"), "counts"), "get", e.s("n")), e.mb.typesInt()))))),
                e.let("mk", e.defFunc(e.v("d"), "make")),
                e.funcImpl(
                        e.v("mk"),
                        e.mb.tupleOf(),
                        e.retSome(e.fld(e.v("obj"), "type")),
                        e.mb.optNoneType(),
                        e.stage(e.call(
                                e.lower(e.fld(e.v("obj"), "new")),
                                e.rec(
                                        e.p("flag", e.b(false)),
                                        e.p("items", e.strList(e.s("a"), e.s("b"))),
                                        e.p(
                                                "counts",
                                                e.mapNew(
                                                        e.mb.typesString(),
                                                        e.mb.typesIntBox(),
                                                        e.p("n", e.intOf(1)))))))),
                e.finish(e.v("d")),
                e.ret(e.rec(e.p("obj_t", e.fld(e.v("obj"), "type")), e.p("make", e.fld(e.v("mk"), "func"))))));
    }

    @Test
    public void test01250SettableBoolListMapFieldsExported() {
        var e = new EcoMb();
        var lib = e.seq(
                e.let("k", cellDefs(e)),
                e.ret(e.rec(e.p("obj_t", e.fld(e.v("k"), "obj_t")), e.p("make", e.fld(e.v("k"), "make")))));
        var main = e.seq(
                importLib(e),
                e.let("c", e.call(e.fld(e.v("r"), "make"), e.tup())),
                e.call(e.mb.log(), e.callM(e.v("c"), "describe", e.tup())),
                e.callM(e.v("c"), "reset", e.tup()),
                e.call(e.mb.log(), e.callM(e.v("c"), "describe", e.tup())));
        assertEquals(TList.of("off:a,b:1", "on:reset:7"), compileLogs(e, lib, main));
    }

    @Test
    public void test01260InstanceWithDynamicFieldsExported() {
        var e = new EcoMb();
        var lib = e.seq(
                e.let("k", cellDefs(e)),
                e.ret(e.rec(
                        e.p("obj_t", e.fld(e.v("k"), "obj_t")),
                        e.p("inst", e.call(e.fld(e.v("k"), "make"), e.tup())))));
        var main = e.seq(
                importLib(e),
                e.let("c", e.mb.as(e.fld(e.v("r"), "inst"), e.fld(e.v("r"), "obj_t"))),
                e.call(e.mb.log(), e.callM(e.v("c"), "describe", e.tup())),
                e.callM(e.v("c"), "reset", e.tup()),
                e.call(e.mb.log(), e.callM(e.v("c"), "describe", e.tup())));
        assertEquals(TList.of("off:a,b:1", "on:reset:7"), compileLogs(e, lib, main));
    }

    private static SourceExpr loweredLib(EcoMb e, SourceExpr value) {
        return e.lower(e.seq(e.let("v", value), e.ret(e.rec(e.p("v", e.v("v"))))));
    }

    @Test
    public void test01300LoweredListRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.listNew(e.mb.typesString(), e.dstr("a"), e.dstr("b")));
        var main = e.seq(importLib(e), e.foreach(e.fld(e.v("r"), "v"), "x", e.call(e.mb.log(), e.v("x"))));
        assertEquals(TList.of("a", "b"), compileLogs(e, lib, main));
    }

    @Test
    public void test01400LoweredMapRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.mapNew(e.mb.typesString(), e.mb.typesString(), e.p("k", e.dstr("map-value"))));
        var main = e.seq(importLib(e), e.call(e.mb.log(), e.callM(e.fld(e.v("r"), "v"), "get", e.dstr("k"))));
        assertEquals(TList.of("map-value"), compileLogs(e, lib, main));
    }

    private static SourceExpr optionArms(EcoMb e, SourceExpr base) {
        return e.matchOf(
                base,
                "x",
                Opt.none(),
                e.p("some", e.call(e.mb.log(), e.v("x"))),
                e.p("none", e.call(e.mb.log(), e.s("none"))));
    }

    @Test
    public void test01500LoweredOptionSomeRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.optSome(e.mb.typesString(), e.dstr("opt-value")));
        var main = e.seq(importLib(e), optionArms(e, e.fld(e.v("r"), "v")));
        assertEquals(TList.of("opt-value"), compileLogs(e, lib, main));
    }

    @Test
    public void test01600LoweredOptionNoneRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.optNone(e.mb.typesString()));
        var main = e.seq(importLib(e), optionArms(e, e.fld(e.v("r"), "v")));
        assertEquals(TList.of("none"), compileLogs(e, lib, main));
    }

    @Test
    public void test01700LoweredSpecializedOptionRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.optSome(e.mb.typesInt(), e.intOf(7)));
        var main = e.seq(
                importLib(e),
                e.matchOf(
                        e.fld(e.v("r"), "v"),
                        "x",
                        Opt.none(),
                        e.p("some", e.call(e.mb.log(), e.toStr(e.v("x")))),
                        e.p("none", e.call(e.mb.log(), e.s("none")))));
        assertEquals(TList.of("7"), compileLogs(e, lib, main));
    }

    @Test
    public void test01800LoweredEResultRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.eresultOk(e.mb.typesString(), e.dstr("ok-value")));
        var main = e.seq(
                importLib(e),
                e.matchOf(
                        e.fld(e.v("r"), "v"),
                        "x",
                        Opt.none(),
                        e.p("ok", e.call(e.mb.log(), e.v("x"))),
                        e.p("errs", e.call(e.mb.log(), e.s("errs")))));
        assertEquals(TList.of("ok-value"), compileLogs(e, lib, main));
    }

    @Test
    public void test01850LoweredEResultErrsRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(
                e,
                e.eresultErr(
                        e.mb.typesString(),
                        e.call(e.evalBuiltin("expr_loc"), e.stage(e.s("anywhere"))),
                        e.dstr("lib-error")));
        var main = e.seq(
                importLib(e),
                e.matchOf(
                        e.fld(e.v("r"), "v"),
                        "x",
                        Opt.none(),
                        e.p("ok", e.call(e.mb.log(), e.v("x"))),
                        e.p(
                                "errs",
                                e.foreach(
                                        e.v("x"),
                                        "err",
                                        e.call(e.mb.log(), e.callM(e.v("err"), "message", e.tup()))))));
        var logs = compileLogs(e, lib, main);
        assertEquals(1, logs.size(), () -> "unexpected: " + logs);
        Assertions.assertTrue(logs.get(0).endsWith("] lib-error"), () -> "unexpected: " + logs);
    }

    @Test
    public void test01900LoweredArrayRoundTrips() {
        var e = new EcoMb();
        var lib = loweredLib(e, e.call(e.mb.evalStrUtf8(), e.dstr("hé")));
        var main = e.seq(
                importLib(e),
                e.call(e.mb.log(), e.toStr(e.fld(e.v("r"), "v", "len"))),
                e.foreach(e.fld(e.v("r"), "v"), "b", e.call(e.mb.log(), e.toStr(e.v("b")))));
        assertEquals(TList.of("3", "104", "-61", "-87"), compileLogs(e, lib, main));
    }

    @Test
    public void test02000InstanceWithContainerFieldsRoundTrips() {
        var e = new EcoMb();
        var strT = e.mb.typesString();
        var fields = e.rec(
                e.p("l", e.listType(strT)),
                e.p("m", e.mapType(strT, strT)),
                e.p("o", e.call(e.mb.typesOption(), strT)));
        var lib = e.seq(
                e.let(
                        "k",
                        e.lower(defsetLib(
                                e,
                                e.let("obj", e.defObject(e.v("d"), "holder")),
                                e.objectImpl(e.v("obj"), fields),
                                e.methodImpl(
                                        e.v("obj"),
                                        "l",
                                        e.mb.tupleOf(),
                                        e.retSome(e.listType(strT)),
                                        e.stage(e.fld(e.v("inner"), "l"))),
                                e.methodImpl(
                                        e.v("obj"),
                                        "m",
                                        e.mb.tupleOf(),
                                        e.retSome(e.mapType(strT, strT)),
                                        e.stage(e.fld(e.v("inner"), "m"))),
                                e.methodImpl(
                                        e.v("obj"),
                                        "o",
                                        e.mb.tupleOf(),
                                        e.retSome(e.call(e.mb.typesOption(), strT)),
                                        e.stage(e.fld(e.v("inner"), "o"))),
                                e.finish(e.v("d")),
                                e.ret(e.rec(e.p("obj_new", e.fld(e.v("obj"), "new"))))))),
                e.ret(e.rec(e.p(
                        "inst",
                        e.call(
                                e.fld(e.v("k"), "obj_new"),
                                e.rec(
                                        e.p("l", e.listNew(strT, e.dstr("l0"))),
                                        e.p("m", e.mapNew(strT, strT, e.p("mk", e.dstr("m0")))),
                                        e.p("o", e.optSome(strT, e.dstr("o0")))))))));
        var inst = e.fld(e.v("r"), "inst");
        var main = e.seq(
                importLib(e),
                e.foreach(e.callM(inst, "l", e.tup()), "x", e.call(e.mb.log(), e.v("x"))),
                e.call(e.mb.log(), e.callM(e.callM(inst, "m", e.tup()), "get", e.dstr("mk"))),
                optionArms(e, e.callM(inst, "o", e.tup())));
        assertEquals(TList.of("l0", "m0", "o0"), compileLogs(e, lib, main));
    }
}
