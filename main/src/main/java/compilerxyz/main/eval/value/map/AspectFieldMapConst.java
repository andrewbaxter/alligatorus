package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dynRes = new AspectConsumeDynamicMapConst<>(type).aspectConsumeDynamicToDynamic(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        return new AspectFieldMapDynamic<>(new TypeMapDynamic<>(type.keyType, type.valueType))
                .aspectFieldField(xx, loc, dynRes.requireValue("TypeMapConst::field self"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new AspectFieldMapDynamic<>(new TypeMapDynamic<>(type.keyType, type.valueType)).aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return new AspectFieldMapDynamic<>(new TypeMapDynamic<>(type.keyType, type.valueType))
                .aspectFieldTypeOpt(xx, key);
    }
}
