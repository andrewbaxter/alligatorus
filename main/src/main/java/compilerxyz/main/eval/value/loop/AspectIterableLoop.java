package compilerxyz.main.eval.value.loop;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorSlot;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.list.AspectIterableListBoxedDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.IList;

public final class AspectIterableLoop implements AspectIterable {
    public static final AspectIterableLoop INSTANCE = new AspectIterableLoop();

    private AspectIterableLoop() {}

    @Override
    public Type aspectIterableElementType() {
        return new Tuple(IList.of());
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorSlot(new BindingId());
    }

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(new Tuple(IList.of()))));
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var bodyCodeRes = AspectIterableListBoxedDynamic.foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var loopKey = new JumpKey();
        var b = new JavaBytecodeBuilder(loc);
        var baseAdd = b.addCode(loc, base.code());
        if (baseAdd.optIsSome()) return EResult.errs(baseAdd.optUnwrap());
        b.land(loopKey);
        b.add(elementBindCast.requireValue("foreach element bind"));
        var bodyAdd = b.addCode(loc, bodyCodeRes.requireValue("foreach body"));
        if (bodyAdd.optIsSome()) return EResult.errs(bodyAdd.optUnwrap());
        b.jump(loopKey);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }
}
