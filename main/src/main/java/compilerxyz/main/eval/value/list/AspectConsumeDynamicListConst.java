package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenLists;
import compilerxyz.prelude.util.TList;

public record AspectConsumeDynamicListConst<T extends Type>(TypeListConst<T> type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return switch (GenLists.listDynamic(xx, type.elementType)) {
            case TypeListSpecializedDynamic<?> spec ->
                GenAspectConsumeDynamic.getMaybe(xx, spec).optUnwrap().aspectConsumeDynamicJvmClass(xx);
            case TypeListBoxedDynamic<?> boxed ->
                JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class));
        };
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeListConst::toDynamic: CodeValue has no compile-time value");
        }
        var elements = TypeListConst.elementsOf(cv.value().optUnwrap());
        return switch (GenLists.listDynamic(xx, type.elementType)) {
            case TypeListSpecializedDynamic<?> spec -> {
                var dynamicElements = new TList<CodeDynamicReg>();
                for (var element : elements) {
                    var elemDynRes = DynUtil.materialize(
                            xx, loc, type.elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    dynamicElements.add(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic"));
                }
                var carry = new JavaBytecodeBuilder(loc);
                var carryRes = carry.addCode(loc, cv.code());
                if (carryRes.optIsSome()) yield EResult.errs(carryRes.optUnwrap());
                var list = carry.reg(spec.emitNew(xx, loc, dynamicElements));
                yield EResult.ok(DynUtil.regToValue(carry.code(), list, spec));
            }
            case TypeListBoxedDynamic<?> boxed -> {
                var listName = JavaInternalName.fromClass(TList.class);
                var addDesc = JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_VOID, TList.of(JvmConstants.DESC_OBJECT));
                var b = new JavaBytecodeBuilder(loc);
                var carryRes = b.addCode(loc, cv.code());
                if (carryRes.optIsSome()) yield EResult.errs(carryRes.optUnwrap());
                var regs = new TList<JavaBytecodeReg>(elements.size());
                for (var element : elements) {
                    var elemDynRes =
                            DynUtil.materialize(xx, loc, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    regs.add(b.reg(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic")));
                }
                var list = b.newInstance(
                        listName, JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of()));
                for (var reg : regs) b.invokeVirtualVoid(listName, "add", addDesc, list, reg);
                yield EResult.ok(DynUtil.regToValue(b.code(), list, boxed));
            }
        };
    }
}
