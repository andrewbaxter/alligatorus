package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()
                && cv.value().optUnwrap() instanceof ValueObjectConst v
                && v.data() instanceof Boolean bv) {
            var seq = new JavaBytecodeSequence();
            seq.add(JavaBytecodeUtils.literalBool(loc, bv));
            seq.add(JavaBytecodeUtils.callStaticMethod(
                    loc,
                    JavaInternalName.fromClass(Boolean.class),
                    "valueOf",
                    JavaMethodDescriptor.fromParts(
                            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Boolean.class)),
                            TList.of(JavaDataDescriptor.DESC_BOOL))));
            return EResult.ok(new CodeDynamic(loc, seq));
        }
        return EResult.err(loc, "TypeBoolConst::toDynamic: expected bool ValueObjectConst");
    }
}
