package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectLiftMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(TOrderedMap.class, externalHostValue);
        return EResult.ok(new ValueObjectConst(new TypeMapConst<>(type.keyType, type.valueType), host));
    }
}
