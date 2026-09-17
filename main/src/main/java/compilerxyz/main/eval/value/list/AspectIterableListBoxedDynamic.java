package compilerxyz.main.eval.value.list;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorSlot;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;

public record AspectIterableListBoxedDynamic<T extends Type>(TypeListBoxedDynamic<T> type) implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var iteratorSlot = mustCast(BindingIteratorSlot.class, loop).iteratorSlot();
        return EResult.ok(DynUtil.regToValue(
                DynUtil.iteratorNext(loc, iteratorSlot, type.elementDecodeCast(xx)), type.elementType));
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
        var iterableName = JavaInternalName.fromClass(Iterable.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setup = new JavaBytecodeBuilder(loc);
        var iterator =
                setup.invokeInterface(iterableName, "iterator", iterMethod, setup.reg(baseRes.requireValue("base")));
        var iteratorSetup = setup.code(iterator);

        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }

        var bodyCodeRes = foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var bodyCode = bodyCodeRes.requireValue("foreach body");
        var iteratorSlot = mustCast(BindingIteratorSlot.class, loop).iteratorSlot();
        return DynUtil.foreachSkeleton(loc, iteratorSetup, iteratorSlot, elementBindDyn.optUnwrap(), bodyCode);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorSlot(new BindingId());
    }

    public static EResult<Code> foreachBody(ExprContext xx, QExprId loc, CodeValue body) {
        if (body.value().optIsNone()) return EResult.ok(body.code());
        var dropRes = xx.target.targetDrop(xx, loc, body);
        if (dropRes.hasErrors()) return dropRes.reerr();
        return xx.target.targetSeq(loc, TList.of(body.code(), dropRes.requireValue("foreach body drop")));
    }
}
