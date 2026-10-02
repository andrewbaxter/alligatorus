package compilerxyz.main.eval.value.array;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorIndexed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.list.AspectIterableListBoxedDynamic;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;

public record AspectIterableArrayDynamic<T extends Type>(TypeArrayDynamic<T> type) implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var b = new JavaBytecodeBuilder(loc);
        var out = type.loopElement(xx, b, mustCast(BindingIteratorIndexed.class, loop));
        return EResult.ok(DynUtil.regToValue(b.code(), out, type.elementType));
    }

    @Override
    public Type aspectIterableElementType() {
        return type.elementType;
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var bodyCodeRes = AspectIterableListBoxedDynamic.foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }
        var b = new JavaBytecodeBuilder(loc);
        return type.foreach(
                xx,
                loc,
                b,
                b.reg(baseRes.requireValue("base")),
                mustCast(BindingIteratorIndexed.class, loop),
                elementBindDyn.optUnwrap(),
                bodyCodeRes.requireValue("foreach body"));
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorIndexed(new BindingId(), new BindingId());
    }
}
