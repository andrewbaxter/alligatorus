package compilerxyz.main.eval.value.eresult;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.TypeForms;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldEResultConst<T extends Type>(TypeEResultConst<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dynRes = new AspectConsumeDynamicEResultConst<>(type).aspectConsumeDynamicToDynamic(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return new AspectFieldEResultDynamic<>(new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, type.elementType)))
                .aspectFieldField(xx, loc, dynRes.requireValue("TypeEResultConst::field self"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new AspectFieldEResultDynamic<>(new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, type.elementType)))
                .aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return new AspectFieldEResultDynamic<>(new TypeEResultDynamic<>(TypeForms.dynamicForm(xx, type.elementType)))
                .aspectFieldTypeOpt(xx, key);
    }
}
