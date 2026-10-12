package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.BindingId;
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

public record AspectConsumeDynamicTypeSourceInterfaceDynamic(TypeTypeSourceInterfaceDynamic type)
        implements AspectConsumeDynamic {
    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(TypeSourceInterfaceDynamic.class);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, type.typeDesc() + "::toDynamic: no compile-time value");
        }
        var value = cv.value().optUnwrap();
        if (DynUtil.sourceOf(value).optIsSome()) {
            return EResult.ok(cv);
        }
        var host = ((ValueObjectConst) value).data();
        var entry = new EvalContext.ExternalEntry(
                new BindingId(), type, new External(TypeSourceInterfaceDynamic.class, host));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
