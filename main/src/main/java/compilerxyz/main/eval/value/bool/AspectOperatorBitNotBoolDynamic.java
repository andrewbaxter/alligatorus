package compilerxyz.main.eval.value.bool;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.gen.AspectOperatorBitNot;
import org.objectweb.asm.Opcodes;

public final class AspectOperatorBitNotBoolDynamic implements AspectOperatorBitNot {
    public static final AspectOperatorBitNotBoolDynamic INSTANCE = new AspectOperatorBitNotBoolDynamic();

    private AspectOperatorBitNotBoolDynamic() {}

    @Override
    public EResult<CodeValue> aspectOperatorBitNotBitNot(ExprContext xx, QExprId loc, CodeValue base) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var out = b.op(
                Opcodes.IXOR,
                JavaDataDescriptor.DESC_BOOL,
                b.reg(baseRes.requireValue("bool not base")),
                b.constBool(true));
        return EResult.ok(DynUtil.regToValue(b.code(), out, new TypeBoolDynamic()));
    }
}
