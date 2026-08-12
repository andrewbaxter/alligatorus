package compilerxyz.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
public final class TList<T> extends AbstractList<T> {
  private final ArrayList<T> backing;

  public TList() {
    this.backing = new ArrayList<>();
  }

  public TList(int initialCapacity) {
    this.backing = new ArrayList<>(initialCapacity);
  }

  private TList(ArrayList<T> backing) {
    this.backing = backing;
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
  public T get(int index) {
    return backing.get(index);
  }

  @Override
  public boolean add(T element) {
    return backing.add(element);
  }

  public TList<T> addAll(Iterable<? extends T> elements) {
    for (var e : elements) backing.add(e);
    return this;
  }

  @Override
  public T removeLast() {
    return backing.removeLast();
  }

  @Override
  public T remove(int index) {
    return backing.remove(index);
  }

  @Override
  public T set(int index, T value) {
    return backing.set(index, value);
  }

  @Override
  public Iterator<T> iterator() {
    return backing.iterator();
  }
}
