package compilerxyz.prelude.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.function.IntFunction;
import java.util.stream.Stream;

public final class TList<T> implements Iterable<T>, ListStore {
    private final ArrayList<T> backing;

    public TList() {
        this.backing = new ArrayList<>();
    }

    public TList(int initialCapacity) {
        this.backing = new ArrayList<>(initialCapacity);
    }

    @SafeVarargs
    public static <T> TList<T> of(T... elements) {
        var out = new TList<T>(elements.length);
        for (var e : elements) out.backing.add(e);
        return out;
    }

    public static <T> TList<T> from(Iterable<? extends T> source) {
        var out = new TList<T>();
        for (var e : source) out.backing.add(e);
        return out;
    }

    public int size() {
        return backing.size();
    }

    public boolean isEmpty() {
        return backing.isEmpty();
    }

    public boolean contains(T element) {
        return backing.contains(element);
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }

    public Stream<T> stream() {
        return backing.stream();
    }

    public void add(T element) {
        backing.add(element);
    }

    public void addAll(Iterable<? extends T> elements) {
        for (var e : elements) backing.add(e);
    }

    public void clear() {
        backing.clear();
    }

    public T get(int index) {
        return backing.get(index);
    }

    public void set(int index, T value) {
        backing.set(index, value);
    }

    public int indexOf(T element) {
        return backing.indexOf(element);
    }

    public T removeAt(int index) {
        return backing.remove(index);
    }

    public T[] toArray(IntFunction<T[]> generator) {
        return backing.toArray(generator);
    }

    public T removeLast() {
        return backing.removeLast();
    }

    public void sort(Comparator<? super T> comparator) {
        backing.sort(comparator);
    }

    @Override
    public int listStoreSize() {
        return backing.size();
    }

    @Override
    public Object listStoreBoxed(int index) {
        return backing.get(index);
    }

    @Override
    public void listStoreAddBoxed(Object value) {
        backing.add((T) value);
    }

    @Override
    public ListStore listStoreCopy() {
        return TList.from(this);
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof TList<?> o && backing.equals(o.backing);
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
