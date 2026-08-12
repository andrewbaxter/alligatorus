package compilerxyz.util;

import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class TMap<K, V> extends AbstractMap<K, V> {
  private final HashMap<K, V> backing;

  public TMap() {
    this.backing = new HashMap<>();
  }

  public TMap(int initialCapacity) {
    this.backing = new HashMap<>(initialCapacity);
  }

  public static <K, V> TMap<K, V> of() {
    return new TMap<>();
  }

  @SafeVarargs
  public static <K, V> TMap<K, V> ofEntries(Pair<K, V>... entries) {
    var out = new TMap<K, V>(entries.length);
    for (var e : entries) out.backing.put(e.first(), e.second());
    return out;
  }

  public Opt<V> getOpt(K key) {
    var v = backing.get(key);
    if (v == null && !backing.containsKey(key)) return Opt.none();
    return Opt.some(v);
  }

  public V getOrNull(K key) {
    return backing.get(key);
  }

  public V computeIfAbsentT(K key, Function<K, V> mapper) {
    var existing = backing.get(key);
    if (existing != null) return existing;
    var value = mapper.apply(key);
    backing.put(key, value);
    return value;
  }

  public TSet<K> keysT() {
    return TSet.from(backing.keySet());
  }

  public TList<V> valuesT() {
    return TList.from(backing.values());
  }

  @Override
  public V put(K key, V value) {
    return backing.put(key, value);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return backing.entrySet();
  }
}
