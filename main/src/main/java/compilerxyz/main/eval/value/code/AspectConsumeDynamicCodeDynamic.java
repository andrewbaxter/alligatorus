package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicCodeDynamic(TypeCodeDynamic type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(Code.class);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && DynUtil.sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: CodeValue has no value");
        }
        if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc) || !(voc.data() instanceof Code codeObj)) {
            return EResult.err(loc, "TypeCodeDynamic::toDynamic: value is not a compile-time Code");
        }
        var entry = new EvalContext.ExternalEntry(new BindingId(), type, new External(Code.class, codeObj));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
