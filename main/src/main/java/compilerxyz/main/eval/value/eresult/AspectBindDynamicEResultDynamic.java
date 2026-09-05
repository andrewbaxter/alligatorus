package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.util.Opt;

public record AspectBindDynamicEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectBindDynamic {

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }
}
