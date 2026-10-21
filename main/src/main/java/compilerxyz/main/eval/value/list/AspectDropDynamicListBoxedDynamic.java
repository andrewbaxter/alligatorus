package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;

public record AspectDropDynamicListBoxedDynamic<T extends Type>(TypeListBoxedDynamic<T> type)
        implements AspectDropDynamic {

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var elemDropRes = GenAspectDropDynamic.get(xx, loc, type.elementType);
        if (elemDropRes.hasErrors()) return elemDropRes.reerr();
        var elemDrop = elemDropRes.requireValue("TypeListBoxedDynamic::drop element AspectDropDynamic");
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var elementSlot = new BindingId();
        var iteratorSlot = new BindingId();
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
                        elemDropCodeRes.requireValue("TypeListBoxedDynamic::drop element drop")));
        if (bodyRes.hasErrors()) return bodyRes.reerr();

        var iterableName = JavaInternalName.fromClass(Iterable.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setup = new JavaBytecodeBuilder(loc);
        var iteratorSetup = setup.code(
                setup.invokeInterface(iterableName, "iterator", iterMethod, DynUtil.sourceReg(xx, setup, vd)));

        var bind = new JavaBytecodeBuilder(loc);
        bind.store(elementSlot, bind.reg(DynUtil.iteratorNext(loc, iteratorSlot, type.elementDecodeCast(xx))));
        var loopRes =
                DynUtil.foreachSkeleton(loc, iteratorSetup, iteratorSlot, bind.code(), bodyRes.requireValue("body"));
        if (loopRes.hasErrors()) return loopRes.reerr();
        return EResult.ok(loopRes.requireValue("loop").code());
    }
}
