package compilerxyz.prelude.util;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.function.IntFunction;

public final class TOrderedSet<T> implements Iterable<T> {
    private final LinkedHashSet<T> backing;

    public TOrderedSet() {
        this.backing = new LinkedHashSet<>();
    }

    public TOrderedSet(int initialCapacity) {
        this.backing = new LinkedHashSet<>(initialCapacity);
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }

    public boolean add(T element) {
        return backing.add(element);
    }

    public T[] toArray(IntFunction<T[]> generator) {
        return backing.toArray(generator);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TOrderedSet<?> o && backing.equals(o.backing);
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
