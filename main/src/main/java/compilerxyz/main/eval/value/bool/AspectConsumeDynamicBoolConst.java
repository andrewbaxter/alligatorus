package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.TList;

public final class AspectConsumeDynamicBoolConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicBoolConst INSTANCE = new AspectConsumeDynamicBoolConst();

    private AspectConsumeDynamicBoolConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof Boolean bv) {
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var boxed = b.invokeStatic(
                    JavaInternalName.fromClass(Boolean.class),
                    "valueOf",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Boolean.class)),
                            TList.of(JavaDataDescriptor.DESC_BOOL)),
                    b.constBool(bv));
            return EResult.ok(DynUtil.regToValue(b.code(), boxed, new TypeBoolDynamicBox()));
        }
        return EResult.err(loc, "TypeBoolConst::toDynamic: expected bool ValueObjectConst");
    }
}
