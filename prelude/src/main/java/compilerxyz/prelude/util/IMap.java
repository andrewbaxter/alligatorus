package compilerxyz.prelude.util;

import compilerxyz.prelude.annotations.Immutable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Immutable
public final class IMap<K, V> implements Iterable<Map.Entry<K, V>> {
    private final Map<K, V> backing;

    private IMap(Map<K, V> backing) {
        this.backing = backing;
    }

    @SafeVarargs
    public static <K, V> IMap<K, V> ofEntries(Pair<K, V>... entries) {
        var staging = new HashMap<K, V>(entries.length);
        for (var e : entries) staging.put(e.first(), e.second());
        return new IMap<>(Map.copyOf(staging));
    }

    public static <K, V> IMap<K, V> from(Map<? extends K, ? extends V> source) {
        return new IMap<>(Map.copyOf(source));
    }

    public Opt<V> getOpt(K key) {
        var v = backing.get(key);
        return v == null ? Opt.none() : Opt.some(v);
    }

    public V getOrNull(K key) {
        return backing.get(key);
    }

    public boolean containsKey(K key) {
        return backing.containsKey(key);
    }

    public TList<K> keys() {
        return TList.from(backing.keySet());
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return backing.entrySet().iterator();
    }
}
