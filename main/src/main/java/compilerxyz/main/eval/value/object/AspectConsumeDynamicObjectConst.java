package compilerxyz.main.eval.value.object;

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
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicObjectConst(TypeObjectConst type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(type.externalHostClass());
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && DynUtil.sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, type.typeDesc() + "::toDynamic: no compile-time value");
        }
        Object hostValue = cv.value().optUnwrap();
        if (hostValue instanceof ValueObjectConst wrap) {
            hostValue = wrap.data();
        }
        var entry =
                new EvalContext.ExternalEntry(new BindingId(), type, new External(type.externalHostClass(), hostValue));
        xx.vx.externals.add(entry);
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.hasErrors()) return carryRes.reerr();
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
