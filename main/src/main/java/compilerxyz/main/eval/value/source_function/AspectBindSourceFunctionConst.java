package compilerxyz.main.eval.value.source_function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst.SourceFunctionConstBinding;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.Value;

public final class AspectBindSourceFunctionConst implements AspectBind {
    public static final AspectBindSourceFunctionConst INSTANCE = new AspectBindSourceFunctionConst();

    private AspectBindSourceFunctionConst() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceFunctionConst::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(cv.code(), new SourceFunctionConstBinding(v)));
    }
}
