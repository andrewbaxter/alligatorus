package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.BindSubDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.prelude.util.Opt;

public record AspectBindDynamicListDynamic<T extends Type>(TypeListDynamic<T> type) implements AspectBindDynamic {

    @Override
    public EResult<Opt<BindSubDynamic>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(xx, loc, cv);
    }
}
