package compilerxyz.main.eval.value.source_union_new;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.GenAspectBind;

public record AspectBindTypeSourceUnionNewConst(TypeTypeSourceUnionNewConst type) implements AspectBind {

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return GenAspectBind.getMaybe(xx, type.asCtor()).optUnwrap().aspectBindBind(xx, loc, cv);
    }
}
