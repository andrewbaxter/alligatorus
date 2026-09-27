package compilerxyz.prelude.util;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public final class TOrderedMap<K, V> {
    private final LinkedHashMap<K, V> backing;

    public TOrderedMap() {
        this.backing = new LinkedHashMap<>();
    }

    public TOrderedMap(int initialCapacity) {
        this.backing = new LinkedHashMap<>(initialCapacity);
    }

    public Opt<V> getOpt(K key) {
        var v = backing.get(key);
        return v == null ? Opt.none() : Opt.some(v);
    }

    public int size() {
        return backing.size();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public boolean containsKey(K key) {
        return backing.containsKey(key);
    }

    public void put(K key, V value) {
        backing.put(key, value);
    }

    public void putIfAbsent(K key, V value) {
        backing.putIfAbsent(key, value);
    }

    public void remove(K key) {
        backing.remove(key);
    }

    public void putAll(TOrderedMap<K, V> source) {
        backing.putAll(source.backing);
    }

    public Set<K> keySet() {
        return backing.keySet();
    }

    public Collection<V> values() {
        return backing.values();
    }

    public Set<Map.Entry<K, V>> entrySet() {
        return backing.entrySet();
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TOrderedMap<?, ?> other && backing.equals(other.backing);
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
