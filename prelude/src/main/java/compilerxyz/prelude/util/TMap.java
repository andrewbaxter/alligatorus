package compilerxyz.prelude.util;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class TMap<K, V> {
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
        return v == null ? Opt.none() : Opt.some(v);
    }

    public V computeIfAbsentT(K key, Function<K, V> f) {
        return backing.computeIfAbsent(key, f);
    }

    public int size() {
        return backing.size();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
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

    public void putAll(TMap<K, V> source) {
        backing.putAll(source.backing);
    }

    public void clear() {
        backing.clear();
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
    public String toString() {
        return backing.toString();
    }
}
