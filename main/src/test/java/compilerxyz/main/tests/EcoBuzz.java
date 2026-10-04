package compilerxyz.main.tests;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceLiteral;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;
import java.util.function.Function;

final class EcoBuzz {
    private EcoBuzz() {}

    static final double BUZZ_SCALE = 32768.0;
    static final int MAX_BLOCK = 256;
    static final int NO_VALUE = -1;
    static final int NOTE_OFF = 255;

    static final IList<String> TYPE_NAMES = IList.of(
            "t_i1", "t_i8", "t_i16", "t_i32", "t_i64", "t_u8", "t_u16", "t_u32", "t_u64", "t_f32", "t_f64", "t_void",
            "t_pv", "t_pu8", "t_pi8", "t_pf32", "t_pi32");

    static SourceExpr lw(EcoMb e, String name) {
        return e.v("lw_" + name);
    }

    private static void collectLowered(Object node, TList<String> out) {
        if (node instanceof SourceExpr expr) {
            switch (expr.variant()) {
                case SourceExpr.ScopeUse use -> {
                    switch (use.key().variant()) {
                        case SourceExpr.Literal lit -> {
                            switch (lit.value()) {
                                case SourceLiteral.Str str -> {
                                    if (str.value().startsWith("lw_")) {
                                        var name = str.value().substring("lw_".length());
                                        if (!out.contains(name)) out.add(name);
                                    }
                                }
                                default -> {}
                            }
                        }
                        default -> collectLowered(use.key(), out);
                    }
                }
                default -> collectLowered(expr.variant(), out);
            }
            return;
        }
        if (node instanceof TList<?> list) {
            for (var element : list) collectLowered(element, out);
            return;
        }
        if (node instanceof Record record) {
            for (var component : record.getClass().getRecordComponents()) {
                try {
                    collectLowered(component.getAccessor().invoke(record), out);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(ex);
                }
            }
        }
    }

    static final class Part {
        final String name;
        final TList<SourceExpr> body = new TList<>();
        final Function<String, SourceExpr> typeOf;

        Part(String name, Function<String, SourceExpr> typeOf) {
            this.name = name;
            this.typeOf = typeOf;
        }

        TList<String> defined() {
            var out = new TList<String>();
            for (var expr : body) {
                switch (expr.variant()) {
                    case SourceExpr.Bind b -> out.add(b.name());
                    default -> {}
                }
            }
            return out;
        }

        TList<String> inputs() {
            var names = new TList<String>();
            collectLowered(body, names);
            var defined = defined();
            var out = new TList<String>();
            for (var n : names) {
                if (defined.contains(n) || TYPE_NAMES.contains(n) || n.equals("range_i32") || n.equals("loop")) {
                    continue;
                }
                out.add(n);
            }
            return out;
        }

        TList<String> outputs(TList<String> usedLater) {
            var out = new TList<String>();
            for (var n : defined()) if (usedLater.contains(n)) out.add(n);
            return out;
        }
    }

    static SourceExpr body(EcoMb e, SourceExpr body) {
        var names = new TList<String>();
        collectLowered(body, names);
        var lets = new TList<SourceExpr>();
        for (var name : names) lets.add(e.let("lw_" + name, e.lowerV(name)));
        switch (body.variant()) {
            case SourceExpr.Seq inner -> lets.addAll(inner.exprs());
            default -> lets.add(body);
        }
        return e.stage(e.seq(lets.toArray(SourceExpr[]::new)));
    }

    static SourceExpr add(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorAdd(a, b);
    }

    static SourceExpr sub(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorSub(a, b);
    }

    static SourceExpr mul(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorMul(a, b);
    }

    static SourceExpr div(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorDiv(a, b);
    }

    static SourceExpr lt(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorLt(a, b);
    }

    static SourceExpr le(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorLe(a, b);
    }

    static SourceExpr gt(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorGt(a, b);
    }

    static SourceExpr ge(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorGe(a, b);
    }

    static SourceExpr eq(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorEq(a, b);
    }

    static SourceExpr ne(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorNe(a, b);
    }

    static SourceExpr and(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorAnd(a, b);
    }

    static SourceExpr or(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorOr(a, b);
    }

    static SourceExpr shl(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.mb.operatorShl(a, b);
    }

    static SourceExpr set(EcoMb e, SourceExpr place, SourceExpr value) {
        return e.mb.set(place, value);
    }

    static SourceExpr idx(EcoMb e, SourceExpr base, SourceExpr i) {
        return e.mb.field(base, i);
    }

    static SourceExpr idx(EcoMb e, SourceExpr base, int i) {
        return e.mb.field(base, e.n(i));
    }

    static SourceExpr fld(EcoMb e, SourceExpr base, String... keys) {
        return e.fld(base, keys);
    }

    static SourceExpr conv(EcoMb e, String type, SourceExpr v) {
        return e.call(lw(e, type), v);
    }

    static SourceExpr f32(EcoMb e, SourceExpr v) {
        return conv(e, "t_f32", v);
    }

    static SourceExpr i32(EcoMb e, SourceExpr v) {
        return conv(e, "t_i32", v);
    }

    static SourceExpr call(EcoMb e, String fn, SourceExpr... args) {
        return e.call(lw(e, fn), args.length == 1 ? args[0] : e.tup(args));
    }

    static SourceExpr callV(EcoMb e, SourceExpr fn, SourceExpr... args) {
        return e.call(fn, args.length == 1 ? args[0] : e.tup(args));
    }

    static SourceExpr forRange(EcoMb e, String i, SourceExpr count, SourceExpr... body) {
        return e.foreach(call(e, "range_i32", count), i, e.seq(body));
    }

    static SourceExpr whileLoop(EcoMb e, String label, SourceExpr cond, SourceExpr... body) {
        var inner = new TList<SourceExpr>();
        inner.add(e.ifOnly(e.not(cond), e.mb.returnLabel(label)));
        for (var b : body) inner.add(b);
        return e.mb.labeled(label, e.foreach(lw(e, "loop"), "_", e.seq(inner.toArray(SourceExpr[]::new))));
    }

    static SourceExpr ifOnly(EcoMb e, SourceExpr cond, SourceExpr... then) {
        return e.ifOnly(cond, e.seq(then));
    }

    static SourceExpr min(EcoMb e, SourceExpr a, SourceExpr b) {
        return e.ifElse(lt(e, a, b), a, b);
    }

    static SourceExpr llvm(EcoMb e, String name) {
        return e.fld(e.v("llvm"), name);
    }

    static SourceExpr llvmL(EcoMb e, String name) {
        return e.fld(e.lowerV("llvm"), name);
    }

    static void prelude(EcoMb e, TList<SourceExpr> out) {
        out.add(e.let("llvm", e.importLocal("llvm", "llvm-ir-22.at")));
        for (var t : new String[] {"i1", "i8", "i16", "i32", "i64", "u8", "u16", "u32", "u64", "f32", "f64", "void"}) {
            out.add(e.let("t_" + t, llvm(e, t)));
        }
        out.add(e.let("t_pv", e.call(llvm(e, "ptr"), e.v("t_void"))));
        out.add(e.let("t_pu8", e.call(llvm(e, "ptr"), e.v("t_u8"))));
        out.add(e.let("t_pi8", e.call(llvm(e, "ptr"), e.v("t_i8"))));
        out.add(e.let("t_pf32", e.call(llvm(e, "ptr"), e.v("t_f32"))));
        out.add(e.let("t_pi32", e.call(llvm(e, "ptr"), e.v("t_i32"))));
        out.add(e.let("t_range_i32", e.call(llvm(e, "range"), e.v("t_i32"))));
    }

    static SourceExpr fnType(EcoMb e, SourceExpr ret, SourceExpr... params) {
        return e.call(llvm(e, "fn"), e.rec(e.p("params", e.listNew(e.mb.evalType(), params)), e.p("ret", ret)));
    }

    static SourceExpr field(EcoMb e, String name, SourceExpr ty) {
        return e.call(llvm(e, "field"), e.rec(e.p("name", e.s(name)), e.p("ty", ty)));
    }

    static SourceExpr struct(EcoMb e, String name, SourceExpr... fields) {
        return e.call(
                llvm(e, "struct"), e.rec(e.p("name", e.s(name)), e.p("fields", e.listNew(llvm(e, "field_t"), fields))));
    }

    static SourceExpr array(EcoMb e, SourceExpr elem, SourceExpr count) {
        return e.call(llvm(e, "array"), e.rec(e.p("elem", elem), e.p("count", count)));
    }

    static SourceExpr ptr(EcoMb e, SourceExpr pointee) {
        return e.call(llvm(e, "ptr"), pointee);
    }

    static SourceExpr globalT(EcoMb e, SourceExpr ty) {
        return e.call(llvm(e, "global_t"), e.rec(e.p("ty", ty), e.p("mem", e.b(false))));
    }

    static SourceExpr globalMemT(EcoMb e, SourceExpr ty) {
        return e.call(llvm(e, "global_t"), e.rec(e.p("ty", ty), e.p("mem", e.b(true))));
    }

    static void rebindTypes(
            EcoMb e, TList<SourceExpr> out, SourceExpr typesInstance, SourceExpr rangeI32, SourceExpr loop) {
        out.add(e.let("ty", typesInstance));
        for (var name : TYPE_NAMES) out.add(e.let(name, e.callM0(e.v("ty"), name)));
        out.add(e.let("range_i32", rangeI32));
        out.add(e.let("loop", loop));
    }

    static SourceExpr funcDef(
            EcoMb e, SourceExpr mod, String name, String[] params, SourceExpr fnType, SourceExpr body) {
        var names = new TList<SourceExpr>();
        for (var p : params) names.add(e.s(p));
        return e.call(
                llvmL(e, "func_fn"),
                e.rec(
                        e.p("mod", mod),
                        e.p("name", e.s(name)),
                        e.p("params", e.tup(names.toArray(SourceExpr[]::new))),
                        e.p("ty", fnType),
                        e.p("body", body(e, body))));
    }

    static SourceExpr externDef(EcoMb e, SourceExpr mod, String name, SourceExpr fnType) {
        return e.call(llvmL(e, "extern_fn"), e.rec(e.p("mod", mod), e.p("name", e.s(name)), e.p("ty", fnType)));
    }

    static TList<SourceExpr> machineModule(
            EcoMb e,
            String name,
            TList<SourceExpr> moduleDefs,
            String[] fnNames,
            TList<Part> parts,
            TList<SourceExpr> builderBody,
            SourceExpr[] builderResults) {
        var exprs = new TList<SourceExpr>();
        prelude(e, exprs);
        exprs.add(e.let("dsp_m", e.importLocal("dsp", "buzz-dsp.at")));
        exprs.add(e.let("wave_t", e.fld(e.v("dsp_m"), "wave_t")));
        exprs.add(e.let("wave_table_t", e.fld(e.v("dsp_m"), "wave_table_t")));
        exprs.add(e.let("p_wave_table_t", e.call(llvm(e, "ptr"), e.v("wave_table_t"))));
        exprs.addAll(moduleDefs);
        exprs.add(e.let("d", e.define()));
        exprs.add(e.optionTypeLet());
        exprs.add(e.let("m_o", e.defObject(e.v("d"), name)));
        var innerFields = new TList<EcoMb.P>();
        for (var fn : fnNames) innerFields.add(e.p(fn, globalT(e, e.v(fn + "_fn_t"))));
        exprs.add(e.objectImpl(e.v("m_o"), e.rec(innerFields.toArray(EcoMb.P[]::new))));
        for (var fn : fnNames) {
            exprs.add(e.methodImpl(
                    e.v("m_o"),
                    fn,
                    e.tup(),
                    e.optionTypeSome(globalT(e, e.v(fn + "_fn_t"))),
                    e.stage(e.fld(e.v("inner"), fn))));
        }
        exprs.add(e.methodImpl(
                e.v("m_o"), "state_t", e.tup(), e.optionTypeSome(e.mb.evalType()), e.stage(e.lowerV("state_t"))));
        for (int pi = 0; pi < parts.size(); pi++) {
            var part = parts.get(pi);
            var usedLater = new TList<String>();
            for (int pj = pi + 1; pj < parts.size(); pj++) collectLowered(parts.get(pj).body, usedLater);
            collectLowered(builderBody, usedLater);
            exprs.add(e.let("part_" + part.name, e.defFunc(e.v("d"), name + " " + part.name)));
            var argFields = new TList<EcoMb.P>();
            argFields.add(e.p("dsp", e.fld(e.v("dsp_m"), "dsp_t")));
            var partBody = new TList<SourceExpr>();
            partBody.add(e.let("ll", e.callM0(e.fld(e.v("arg"), "dsp"), "m")));
            rebindTypes(
                    e,
                    partBody,
                    e.callM0(e.fld(e.v("arg"), "dsp"), "types"),
                    e.callM0(e.fld(e.v("arg"), "dsp"), "range_i32"),
                    e.callM0(e.fld(e.v("arg"), "dsp"), "loop"));
            for (var in : part.inputs()) {
                argFields.add(e.p(in, part.typeOf.apply(in)));
                partBody.add(e.let(in, e.fld(e.v("arg"), in)));
            }
            partBody.addAll(part.body);
            var retFields = new TList<EcoMb.P>();
            var retValues = new TList<EcoMb.P>();
            for (var out : part.outputs(usedLater)) {
                retFields.add(e.p(out, part.typeOf.apply(out)));
                retValues.add(e.p(out, e.v(out)));
            }
            partBody.add(e.ret(e.rec(retValues.toArray(EcoMb.P[]::new))));
            exprs.add(e.funcImpl(
                    e.v("part_" + part.name),
                    e.call(e.mb.builtin("types", "record"), e.rec(argFields.toArray(EcoMb.P[]::new))),
                    e.optionTypeSome(e.call(e.mb.builtin("types", "record"), e.rec(retFields.toArray(EcoMb.P[]::new)))),
                    e.mb.optNoneType(),
                    e.stage(e.seq(partBody.toArray(SourceExpr[]::new)))));
        }
        exprs.add(e.let("m_b", e.defFunc(e.v("d"), name)));
        var body = new TList<SourceExpr>();
        body.add(e.let("ll", e.callM0(e.v("arg"), "m")));
        rebindTypes(
                e,
                body,
                e.callM0(e.v("arg"), "types"),
                e.callM0(e.v("arg"), "range_i32"),
                e.callM0(e.v("arg"), "loop"));
        for (int pi = 0; pi < parts.size(); pi++) {
            var part = parts.get(pi);
            var usedLater = new TList<String>();
            for (int pj = pi + 1; pj < parts.size(); pj++) collectLowered(parts.get(pj).body, usedLater);
            collectLowered(builderBody, usedLater);
            var callFields = new TList<EcoMb.P>();
            callFields.add(e.p("dsp", e.v("arg")));
            for (var in : part.inputs()) callFields.add(e.p(in, e.v(in)));
            body.add(e.let(
                    "r_" + part.name,
                    e.call(
                            e.lower(e.fld(e.v("part_" + part.name), "func")),
                            e.rec(callFields.toArray(EcoMb.P[]::new)))));
            for (var out : part.outputs(usedLater)) body.add(e.let(out, e.fld(e.v("r_" + part.name), out)));
        }
        body.addAll(builderBody);
        var ctor = new TList<EcoMb.P>();
        for (var i = 0; i < fnNames.length; i++) ctor.add(e.p(fnNames[i], builderResults[i]));
        body.add(e.ret(e.call(e.lower(e.fld(e.v("m_o"), "new")), e.rec(ctor.toArray(EcoMb.P[]::new)))));
        exprs.add(e.funcImpl(
                e.v("m_b"),
                e.fld(e.v("dsp_m"), "dsp_t"),
                e.optionTypeSome(e.fld(e.v("m_o"), "type")),
                e.mb.optNoneType(),
                e.stage(e.seq(body.toArray(SourceExpr[]::new)))));
        exprs.add(e.finish(e.v("d")));
        exprs.add(e.ret(e.rec(e.p("new", e.fld(e.v("m_b"), "func")), e.p("state_t", e.v("state_t")))));
        return exprs;
    }
}
