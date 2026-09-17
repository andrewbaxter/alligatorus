package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.AspectStackValueDynamic;

public record AspectStackValueDynamicListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectStackValueDynamic {

    @Override
    public CodeValue aspectStackValueDynamicToValue(
            ExprContext xx, QExprId loc, CodeDynamic code, ValueDynamicSource source) {
        return DynUtil.sourceToValue(code, source, type);
    }
}
