package compilerxyz.util;

import java.util.AbstractMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TOrderedMap<K, V> extends AbstractMap<K, V> {
  private final LinkedHashMap<K, V> backing;

  public TOrderedMap() {
    this.backing = new LinkedHashMap<>();
  }

  public TOrderedMap(int initialCapacity) {
    this.backing = new LinkedHashMap<>(initialCapacity);
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
