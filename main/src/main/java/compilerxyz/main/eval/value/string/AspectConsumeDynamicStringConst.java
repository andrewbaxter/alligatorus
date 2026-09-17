package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;

public final class AspectConsumeDynamicStringConst implements AspectConsumeDynamic {
    public static final AspectConsumeDynamicStringConst INSTANCE = new AspectConsumeDynamicStringConst();

    private AspectConsumeDynamicStringConst() {}

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof String s) {
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.hasErrors()) return carryRes.reerr();
            var raw = b.constString(s);
            return EResult.ok(DynUtil.regToValue(b.code(), raw, new TypeStringDynamic()));
        }
        return EResult.err(loc, "TypeStringConst::toDynamic: expected string ValueObjectConst");
    }
}
