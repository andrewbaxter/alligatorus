package compilerxyz.main.eval.value.source_object_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.source_object_new.TypeSourceObjectNewDynamic.SourceObjectNewConstBinding;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.Value;

public final class AspectBindSourceObjectNewDynamic implements AspectBind {
    public static final AspectBindSourceObjectNewDynamic INSTANCE = new AspectBindSourceObjectNewDynamic();

    private AspectBindSourceObjectNewDynamic() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectNewDynamic::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(cv.code(), new SourceObjectNewConstBinding(v)));
    }
}
