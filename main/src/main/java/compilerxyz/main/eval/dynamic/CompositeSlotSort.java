package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Comparator;

public final class CompositeSlotSort {
    private CompositeSlotSort() {}

    public record Result<K>(TList<Type> sortedTypes, TMap<K, Integer> slotOfKey) {}

    public static <K> Result<K> sort(ExprContext xx, TOrderedMap<K, Type> entries, Comparator<K> tiebreaker) {
        var descOfKey = new TMap<K, String>();
        for (var e : entries.entrySet()) {
            descOfKey.put(
                    e.getKey(),
                    CompositeClassEmitter.slotDescriptor(xx, e.getValue()).value());
        }
        var keys = new TList<K>(entries.size());
        keys.addAll(entries.keySet());
        keys.sort((a, b) -> {
            int c = descOfKey.get(a).compareTo(descOfKey.get(b));
            if (c != 0) return c;
            return tiebreaker.compare(a, b);
        });
        var sortedTypes = new TList<Type>(keys.size());
        var slotOfKey = new TMap<K, Integer>();
        for (int i = 0; i < keys.size(); i++) {
            var k = keys.get(i);
            sortedTypes.add(entries.getOrNull(k));
            slotOfKey.put(k, i);
        }
        return new Result<>(sortedTypes, slotOfKey);
    }
}
