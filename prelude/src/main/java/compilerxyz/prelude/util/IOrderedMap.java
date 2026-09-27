package compilerxyz.prelude.util;

import compilerxyz.prelude.annotations.Immutable;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

@Immutable
public final class IOrderedMap<K, V> implements Iterable<Map.Entry<K, V>> {
    private final Map<K, V> backing;

    private IOrderedMap(Map<K, V> backing) {
        this.backing = backing;
    }

    public static <K, V> IOrderedMap<K, V> from(TOrderedMap<K, V> source) {
        var staging = new LinkedHashMap<K, V>(source.size());
        for (var e : source.entrySet()) staging.put(e.getKey(), e.getValue());
        return new IOrderedMap<>(Collections.unmodifiableMap(staging));
    }

    public Opt<V> getOpt(K key) {
        var v = backing.get(key);
        return v == null ? Opt.none() : Opt.some(v);
    }

    public int size() {
        return backing.size();
    }

    public TList<K> keys() {
        return TList.from(backing.keySet());
    }

    @Override
    public Iterator<Map.Entry<K, V>> iterator() {
        return backing.entrySet().iterator();
    }
}
