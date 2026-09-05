package compilerxyz.main.eval.value.null_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectStackValueDynamic;

public record AspectStackValueDynamicNullDynamic(TypeNullDynamic type) implements AspectStackValueDynamic {

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, type);
    }
}
