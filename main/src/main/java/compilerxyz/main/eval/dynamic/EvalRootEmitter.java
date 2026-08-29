package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;

public final class EvalRootEmitter {
    public static final String EXPR_CONTEXT_FIELD = "__exprContext";

    public static final JavaDataDescriptor DESC_EXPR_CONTEXT =
            JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(ExprContext.class));

    public static CodeDynamic loadEvalContext(QExprId loc, ExprContext xx) {
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.accessStaticField(
                new CodeLoc.Some(loc),
                JavaInternalName.of(xx.vx.evalRootInternalName),
                EXPR_CONTEXT_FIELD,
                DESC_EXPR_CONTEXT));
        seq.add(JavaBytecodeUtils.accessField(
                loc,
                JavaInternalName.fromClass(ExprContext.class),
                "vx",
                JavaDataDescriptor.fromClass(EvalContext.class)));
        return new CodeDynamic(loc, seq);
    }

    public static String toInternalName(String binaryName) {
        return binaryName.replace('.', '/');
    }

    private EvalRootEmitter() {}
}
