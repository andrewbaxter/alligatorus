package compilerxyz.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public final class TOrderedSet<T> implements Set<T> {
  private final LinkedHashSet<T> backing;

  public TOrderedSet() {
    this.backing = new LinkedHashSet<>();
  }

  public TOrderedSet(int initialCapacity) {
    this.backing = new LinkedHashSet<>(initialCapacity);
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
  public boolean retainAll(Collection<?> elements) {
    return backing.retainAll(elements);
  }

  @Override
  public boolean removeAll(Collection<?> elements) {
    return backing.removeAll(elements);
  }

  @Override
  public void clear() {
    backing.clear();
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
