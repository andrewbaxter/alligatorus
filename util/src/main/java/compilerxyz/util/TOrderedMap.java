package compilerxyz.util;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class TOrderedMap<K, V> extends AbstractMap<K, V> {
  private final LinkedHashMap<K, V> backing;

  public TOrderedMap() {
    this.backing = new LinkedHashMap<>();
  }

  public TOrderedMap(int initialCapacity) {
    this.backing = new LinkedHashMap<>(initialCapacity);
  }

  public static <K, V> TOrderedMap<K, V> of() {
    return new TOrderedMap<>();
  }

  @SafeVarargs
  public static <K, V> TOrderedMap<K, V> ofEntries(Pair<K, V>... entries) {
    var out = new TOrderedMap<K, V>(entries.length);
    for (var e : entries) out.backing.put(e.first(), e.second());
    return out;
  }

  public Opt<V> getOpt(K key) {
    var v = backing.get(key);
    if (v == null && !backing.containsKey(key)) return Opt.none();
    return Opt.some(v);
  }

  public @Nullable V getOrNull(K key) {
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

  public Opt<Map.Entry<K, V>> firstEntry() {
    var it = backing.entrySet().iterator();
    if (!it.hasNext()) return Opt.none();
    return Opt.some(it.next());
  }

  @Override
  public @Nullable V put(K key, V value) {
    return backing.put(key, value);
  }

  @Override
  public Set<Map.Entry<K, V>> entrySet() {
    return backing.entrySet();
  }
}
