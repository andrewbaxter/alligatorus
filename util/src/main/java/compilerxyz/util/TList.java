package compilerxyz.util;

import java.util.*;

public final class TList<T> implements List<T> {
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

    public static <T> TList<T> from(Collection<? extends T> source) {
        var out = new TList<T>(source.size());
        out.backing.addAll(source);
        return out;
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
    public boolean contains(Object element) {
        return backing.contains(element);
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }

    @Override
    public Object[] toArray() {
        return backing.toArray();
    }

    @Override
    public <U> U[] toArray(U[] target) {
        return backing.toArray(target);
    }

    @Override
    public boolean add(T element) {
        return backing.add(element);
    }

    @Override
    public boolean remove(Object element) {
        return backing.remove(element);
    }

    @Override
    public boolean containsAll(Collection<?> elements) {
        return backing.containsAll(elements);
    }

    @Override
    public boolean addAll(Collection<? extends T> elements) {
        return backing.addAll(elements);
    }

    @Override
    public boolean addAll(int index, Collection<? extends T> elements) {
        return backing.addAll(index, elements);
    }

    @Override
    public boolean removeAll(Collection<?> elements) {
        return backing.removeAll(elements);
    }

    @Override
    public boolean retainAll(Collection<?> elements) {
        return backing.retainAll(elements);
    }

    @Override
    public void clear() {
        backing.clear();
    }

    @Override
    public T get(int index) {
        return backing.get(index);
    }

    @Override
    public T set(int index, T value) {
        return backing.set(index, value);
    }

    @Override
    public void add(int index, T value) {
        backing.add(index, value);
    }

    @Override
    public T remove(int index) {
        return backing.remove(index);
    }

    @Override
    public int indexOf(Object element) {
        return backing.indexOf(element);
    }

    @Override
    public int lastIndexOf(Object element) {
        return backing.lastIndexOf(element);
    }

    @Override
    public ListIterator<T> listIterator() {
        return backing.listIterator();
    }

    @Override
    public ListIterator<T> listIterator(int index) {
        return backing.listIterator(index);
    }

    @Override
    public List<T> subList(int fromIndex, int toIndex) {
        return backing.subList(fromIndex, toIndex);
    }

    @Override
    public T removeLast() {
        return backing.removeLast();
    }

    public TList<T> addAll(Iterable<? extends T> elements) {
        for (var e : elements) backing.add(e);
        return this;
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
