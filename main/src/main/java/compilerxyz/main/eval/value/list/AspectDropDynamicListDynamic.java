package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorIndexed;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectDropDynamicListDynamic<T extends Type>(TypeListDynamic<T> type) implements AspectDropDynamic {
    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var elemDropRes = GenAspectDropDynamic.get(xx, loc, type.elementType);
        if (elemDropRes.hasErrors()) return elemDropRes.reerr();
        var elemDrop = elemDropRes.requireValue("TypeListDynamic::drop element AspectDropDynamic");
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var elementSlot = new BindingId();
        var slots = new BindingIteratorIndexed(new BindingId(), new BindingId());
        var elementLoad = new JavaBytecodeBuilder(loc);
        var elementCv = DynUtil.regToValue(
                elementLoad.code(),
                elementLoad.load(elementSlot, JvmSort.descriptor(xx, type.elementType)),
                type.elementType);
        var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
        if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
        var bodyRes = xx.target.targetSeq(
                loc,
                TList.of(
                        Code.target(elementLoad.code()),
                        elemDropCodeRes.requireValue("TypeListDynamic::drop element drop")));
        if (bodyRes.hasErrors()) return bodyRes.reerr();
        var bind = new JavaBytecodeBuilder(loc);
        var list = bind.load(slots.baseSlot(), type.storeDescriptor());
        var index = bind.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT);
        var raw = bind.invokeVirtual(type.storeName, "get", type.getDescriptor(xx), list, index);
        bind.store(elementSlot, type.elementDecode(xx, bind, raw));
        var b = new JavaBytecodeBuilder(loc);
        b.store(slots.baseSlot(), DynUtil.sourceReg(xx, b, vd));
        var loopRes = AspectIterableListDynamic.indexLoop(
                loc, type, slots, b, Opt.some(bind.code()), bodyRes.requireValue("body"));
        if (loopRes.hasErrors()) return loopRes.reerr();
        return EResult.ok(Code.target(b.code()));
    }
}
