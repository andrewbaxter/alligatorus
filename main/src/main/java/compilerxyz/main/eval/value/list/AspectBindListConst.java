package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingValue;
import compilerxyz.main.gen.AspectBind;

public record AspectBindListConst<T extends Type>(TypeListConst<T> type) implements AspectBind {

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome()) {
            return EResult.ok(
                    new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
        }
        return EResult.err(loc, "TypeListConst::bind: CodeValue has no compile-time value");
    }
}
