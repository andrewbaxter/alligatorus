package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;

public final class EvalRootEmitter {
    public static final String EXPR_CONTEXT_FIELD = "__exprContext";

    public static final JavaDataDescriptor DESC_EXPR_CONTEXT =
            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(ExprContext.class));

    public static CodeDynamicReg loadEvalContext(QExprId loc, ExprContext xx) {
        var b = new JavaBytecodeBuilder(loc);
        var xxReg = b.getStatic(TargetDynamic.evalRootOf(xx), EXPR_CONTEXT_FIELD, DESC_EXPR_CONTEXT);
        var vxReg = b.getField(
                JavaInternalName.fromClass(ExprContext.class),
                "vx",
                JavaDataDescriptor.fromClass(EvalContext.class),
                xxReg);
        return b.code(vxReg);
    }

    public static String toInternalName(String binaryName) {
        return binaryName.replace('.', '/');
    }

    private EvalRootEmitter() {}
}
