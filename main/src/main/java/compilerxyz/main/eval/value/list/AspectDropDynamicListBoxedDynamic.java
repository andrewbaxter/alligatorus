package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;
import org.objectweb.asm.Opcodes;

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
        var loadElement = new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, elementSlot));
        var elementCv =
                new CodeValue(Code.target(loadElement), EvalOutcome.ofValue(ValueDynamic.stack(xx, type.elementType)));
        var elemDropCodeRes = elemDrop.aspectDropDynamicDrop(xx, loc, elementCv);
        if (elemDropCodeRes.hasErrors()) return elemDropCodeRes.reerr();
        var elemDropCode = elemDropCodeRes.requireValue("TypeListBoxedDynamic::drop element drop");

        var iterableName = JavaInternalName.fromClass(Iterable.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(vd.source().valueDynamicSourceRealize(xx, loc));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iterableName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var elementStoreSeq = new JavaBytecodeSequence();
        elementStoreSeq.add(DynUtil.iteratorNext(loc, type.elementDecodeCast(xx, loc)));
        elementStoreSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, elementSlot)));
        var loopRes = DynUtil.foreachSkeleton(loc, iteratorSetup, new CodeDynamic(loc, elementStoreSeq), elemDropCode);
        if (loopRes.hasErrors()) return loopRes.reerr();
        var loopCodeCast = CodeDynamic.castOpt(loc, loopRes.requireValue("loop").code());
        if (loopCodeCast.hasErrors()) return loopCodeCast.reerr();
        var loopCode = loopCodeCast.requireValue("loopCode");
        var finalSeq = new JavaBytecodeSequence();
        if (loopCode.optIsSome()) finalSeq.add(loopCode.optUnwrap());
        finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, finalSeq)));
    }
}
