package compilerxyz.prelude.util;

import java.util.HashSet;
import java.util.Iterator;

public final class TSet<T> implements Iterable<T> {
    private final HashSet<T> backing;

    public TSet() {
        this.backing = new HashSet<>();
    }

    public TSet(int initialCapacity) {
        this.backing = new HashSet<>(initialCapacity);
    }

    public boolean contains(T element) {
        return backing.contains(element);
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }

    public boolean add(T element) {
        return backing.add(element);
    }

    public void addAll(Iterable<? extends T> elements) {
        for (var e : elements) backing.add(e);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TSet<?> o && backing.equals(o.backing);
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
