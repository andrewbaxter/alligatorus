package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenLists;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeListConst::toDynamic: CodeValue has no compile-time value");
        }
        var elements = TypeListConst.elementsOf(cv.value().optUnwrap());
        return switch (GenLists.listDynamic(xx, type.elementType)) {
            case TypeListSpecializedDynamic<?> spec -> {
                var dynamicElements = new TList<CodeDynamic>();
                for (var element : elements) {
                    var convRes = Eval.typecheckConvert(
                            xx, loc, type.elementType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (convRes.hasErrors()) yield convRes.reerr();
                    var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType);
                    if (elemConsumeRes.hasErrors()) yield elemConsumeRes.reerr();
                    var elemDynRes = elemConsumeRes
                            .requireValue("TypeListConst::toDynamic element AspectConsumeDynamic")
                            .aspectConsumeDynamicToDynamic(
                                    xx, loc, convRes.requireValue("TypeListConst::toDynamic element convert"));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    dynamicElements.add(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic"));
                }
                yield EResult.ok(spec.emitNew(xx, loc, dynamicElements));
            }
            case TypeListBoxedDynamic<?> boxed -> {
                var listName = JavaInternalName.fromClass(TList.class);
                var seq = new JavaBytecodeSequence();
                seq.add(JavaBytecodeUtils.instantiate(
                        loc,
                        listName,
                        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of()),
                        new CodeDynamic(loc, new JavaBytecodeSequence())));
                var addDesc = JavaMethodDescriptor.fromParts(
                        JavaDataDescriptor.DESC_BOOL, TList.of(JvmConstants.DESC_OBJECT));
                for (var element : elements) {
                    var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, element.valueType());
                    if (elemConsumeRes.hasErrors()) yield elemConsumeRes.reerr();
                    var elemDynRes = elemConsumeRes
                            .requireValue("TypeListConst::toDynamic element AspectConsumeDynamic")
                            .aspectConsumeDynamicToDynamic(
                                    xx, loc, new CodeValue(Code.none(loc), EvalOutcome.ofValue(element)));
                    if (elemDynRes.hasErrors()) yield elemDynRes.reerr();
                    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
                    seq.add(elemDynRes.requireValue("TypeListConst::toDynamic element dynamic"));
                    seq.add(JavaBytecodeUtils.callMethod(loc, listName, "add", addDesc));
                    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
                }
                yield EResult.ok(new CodeDynamic(loc, seq));
            }
        };
    }
}
