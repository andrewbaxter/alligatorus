package compilerxyz.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TOrderedMap<K, V> implements Map<K, V> {
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
    public int size() {
        return backing.size();
    }

    @Override
    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
        return backing.containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        return backing.containsValue(value);
    }

    @Override
    public V get(Object key) {
        return backing.get(key);
    }

    @Override
    public V put(K key, V value) {
        return backing.put(key, value);
    }

    @Override
    public V remove(Object key) {
        return backing.remove(key);
    }

    @Override
    public void putAll(Map<? extends K, ? extends V> source) {
        backing.putAll(source);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public Set<K> keySet() {
        return backing.keySet();
    }

    @Override
    public Collection<V> values() {
        return backing.values();
    }

    @Override
    public Set<Map.Entry<K, V>> entrySet() {
        return backing.entrySet();
    }

    @Override
    public boolean equals(Object other) {
        return backing.equals(other);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }

    @Override
    public String toString() {
        return backing.toString();
    }
}
