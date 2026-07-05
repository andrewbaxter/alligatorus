package compilerxyz.eval.dynamic;

import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.util.Comparator;

public final class CompositeSlotSort {
  private CompositeSlotSort() {}

  public record Result<K>(TList<Type> sortedTypes, TMap<K, Integer> slotOfKey) {}

  public static <K> Result<K> sort(TOrderedMap<K, Type> entries, Comparator<K> tiebreaker) {
    var keys = new TList<K>(entries.size());
    keys.addAll(entries.keySet());
    keys.sort(
        (a, b) -> {
          int c =
              TypeId.CANONICAL_ORDER.compare(
                  entries.getOrNull(a).typeId(), entries.getOrNull(b).typeId());
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
