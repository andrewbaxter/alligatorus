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

  public V computeIfAbsentT(K key, Function<K, V> f) {
    return backing.computeIfAbsent(key, f);
  }

  public V getOrNull(K key) {
    return backing.get(key);
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
