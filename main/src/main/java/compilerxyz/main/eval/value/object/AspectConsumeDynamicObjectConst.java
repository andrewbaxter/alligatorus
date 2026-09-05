package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicObjectConst(TypeObjectConst type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(type.externalHostClass());
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (DynUtil.dynamicHasCode(cv)) {
            return DynUtil.dynamicConsumeToDynamic(loc, cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, type.typeDesc() + "::toDynamic: no compile-time value");
        }
        Object hostValue = cv.value().optUnwrap();
        if (hostValue instanceof ValueObjectConst wrap) {
            hostValue = wrap.data();
        }
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(
                new EvalContext.ExternalEntry(key, type, new External(type.externalHostClass(), hostValue)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }
}
