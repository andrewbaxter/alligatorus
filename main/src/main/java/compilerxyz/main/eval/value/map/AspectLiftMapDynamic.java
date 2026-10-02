package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;

public record AspectLiftMapDynamic<K extends Type, V extends Type>(
        TypeMapDynamic<K, V> type, AspectLift keyLift, AspectLift valueLift) implements AspectLift {

    @Override
    public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
        var host = MustCast.mustCast(TOrderedMap.class, externalHostValue);
        var entries = new TList<MapConstEntry>(host.size());
        for (var raw : host.entrySet()) {
            var hostEntry = MustCast.mustCast(Map.Entry.class, raw);
            var keyRes = keyLift.aspectLiftLift(xx, loc, hostEntry.getKey());
            if (keyRes.hasErrors()) return keyRes.reerr();
            var valueRes = valueLift.aspectLiftLift(xx, loc, hostEntry.getValue());
            if (valueRes.hasErrors()) return valueRes.reerr();
            entries.add(new MapConstEntry(
                    keyRes.requireValue("TypeMapDynamic::lift key"),
                    valueRes.requireValue("TypeMapDynamic::lift value")));
        }
        return EResult.ok(new ValueMapConst<>(new TypeMapConst<>(type.keyType, type.valueType), entries));
    }
}
