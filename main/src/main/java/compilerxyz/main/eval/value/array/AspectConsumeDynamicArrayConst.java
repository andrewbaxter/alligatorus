package compilerxyz.main.eval.value.array;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicArrayConst<T extends Type>(TypeArrayConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JvmSort.descriptor(xx, type.elementType).arrayOf();
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeArrayConst::toDynamic: CodeValue has no compile-time value");
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var elements = TypeArrayConst.elementsOf(cv.value().optUnwrap());
        var regs = new TList<JavaBytecodeReg>(elements.size());
        for (var element : elements) {
            var elementRes = DynUtil.materialize(
                    xx, loc, type.elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
            if (elementRes.hasErrors()) return elementRes.reerr();
            regs.add(b.reg(elementRes.requireValue("TypeArrayConst::toDynamic element")));
        }
        var array = b.newArray(JvmSort.descriptor(xx, type.elementType), regs);
        return EResult.ok(DynUtil.regToValue(b.code(), array, new TypeArrayDynamic<>(type.elementType)));
    }
}
