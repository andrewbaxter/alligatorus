package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.int_.TypeIntDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionListGetDynamic(TypeListGetDynamic type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var indexConvertedRes = Eval.typecheckConvert(xx, loc, new TypeIntDynamic(), argument);
        if (indexConvertedRes.hasErrors()) return indexConvertedRes.reerr();
        var indexRes = DynUtil.materialize(xx, loc, indexConvertedRes.requireValue("get index converted"));
        if (indexRes.hasErrors()) return indexRes.reerr();
        var selfRes = DynUtil.valueReg(loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var list = b.reg(selfRes.requireValue("selfDyn"));
        var index = b.reg(indexRes.requireValue("toDynamic index"));
        return switch (type.listType()) {
            case TypeListSpecializedDynamic<?> specialized -> {
                var getDesc = JavaMethodDescriptor.fromParts(
                        specialized.elementDescriptor(xx), TList.of(JavaDataDescriptor.DESC_INT));
                var out = b.invokeVirtual(specialized.storeName, "get", getDesc, list, index);
                yield EResult.ok(DynUtil.regToValue(b.code(), out, specialized.elementType));
            }
            case TypeListBoxedDynamic<?> boxed -> {
                var getDesc =
                        JavaMethodDescriptor.fromParts(JvmConstants.DESC_OBJECT, TList.of(JavaDataDescriptor.DESC_INT));
                var raw = b.invokeVirtual(JavaInternalName.fromClass(TList.class), "get", getDesc, list, index);
                var out = boxed.elementDecodeCast(xx).elementDecode(b, raw);
                yield EResult.ok(DynUtil.regToValue(b.code(), out, (Type) boxed.elementType));
            }
        };
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
