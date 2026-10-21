package compilerxyz.main.eval.value.array;

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
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;

public record AspectDropDynamicArrayDynamic<T extends Type>(TypeArrayDynamic<T> type) implements AspectDropDynamic {

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var elemDropRes = GenAspectDropDynamic.get(xx, loc, type.elementType);
        if (elemDropRes.hasErrors()) return elemDropRes.reerr();
        var elemDrop = elemDropRes.requireValue("TypeArrayDynamic::drop element AspectDropDynamic");
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var slots = new BindingIteratorIndexed(new BindingId(), new BindingId());
        var elementLoad = new JavaBytecodeBuilder(loc);
        var element = type.loopElement(xx, elementLoad, slots);
        var elementLoadCode = elementLoad.code();
        var elementCv = DynUtil.regToValue(new JavaBytecodeBuilder(loc).code(), element, type.elementType);
        var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
        if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var loopRes = type.foreach(
                xx,
                loc,
                b,
                DynUtil.sourceReg(xx, b, vd),
                slots,
                elementLoadCode,
                elemDropCodeRes.requireValue("TypeArrayDynamic::drop element drop"));
        if (loopRes.hasErrors()) return loopRes.reerr();
        return EResult.ok(loopRes.requireValue("loop").code());
    }
}
