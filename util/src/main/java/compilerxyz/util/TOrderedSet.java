package compilerxyz.util;

import java.util.AbstractSet;
import java.util.Iterator;
import java.util.LinkedHashSet;

public final class TOrderedSet<T> extends AbstractSet<T> {
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
  public boolean add(T element) {
    return backing.add(element);
  }

  @Override
  public boolean remove(Object element) {
    return backing.remove(element);
  }

  @Override
  public boolean contains(Object element) {
    return backing.contains(element);
  }

  @Override
  public Iterator<T> iterator() {
    return backing.iterator();
  }
}
