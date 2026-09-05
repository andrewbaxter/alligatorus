package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof String s) {
            return EResult.ok(JavaBytecodeUtils.literalString(loc, s));
        }
        return EResult.err(loc, "TypeStringConst::toDynamic: expected string ValueObjectConst");
    }
}
