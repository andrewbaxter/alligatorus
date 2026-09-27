package compilerxyz.main.eval.value.jvm_object;

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
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.MustCast;

public final class AspectConsumeDynamicJvmObjectConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicJvmObjectConst INSTANCE = new AspectConsumeDynamicJvmObjectConst();

    private AspectConsumeDynamicJvmObjectConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var hostValue = MustCast.mustCast(ValueObjectConst.class, cv.value().optUnwrap())
                .data();
        var dynamicType = new TypeJvmObjectDynamic();
        var entry = new EvalContext.ExternalEntry(new BindingId(), dynamicType, new External(Object.class, hostValue));
        xx.vx.externals.add(entry);
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, dynamicType));
    }
}
