package compilerxyz.prelude.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class TMap<K, V> implements Map<K, V> {
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
