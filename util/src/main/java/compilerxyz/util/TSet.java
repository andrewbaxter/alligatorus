package compilerxyz.util;

import java.util.AbstractSet;
import java.util.HashSet;
import java.util.Iterator;

public final class TSet<T> extends AbstractSet<T> {
  private final HashSet<T> backing;

  public TSet() {
    this.backing = new HashSet<>();
  }

  public TSet(int initialCapacity) {
    this.backing = new HashSet<>(initialCapacity);
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
