package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.source_union_new.TypeSourceUnionNewDynamic.SourceUnionNewConstBinding;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.Value;

public final class AspectBindSourceUnionNewDynamic implements AspectBind {
    public static final AspectBindSourceUnionNewDynamic INSTANCE = new AspectBindSourceUnionNewDynamic();

    private AspectBindSourceUnionNewDynamic() {}

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceUnionNewDynamic::bind: CodeValue has no compile-time value");
        }
        Value v = cv.value().optUnwrap();
        return EResult.ok(new BindResult(cv.code(), new SourceUnionNewConstBinding(v)));
    }
}
