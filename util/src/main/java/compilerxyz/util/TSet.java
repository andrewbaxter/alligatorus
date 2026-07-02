package compilerxyz.util;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;
import java.util.function.Function;

public final class TSet<T> extends AbstractSet<T> {
  private final HashSet<T> backing;

  public TSet() {
    this.backing = new HashSet<>();
  }

  public TSet(int initialCapacity) {
    this.backing = new HashSet<>(initialCapacity);
  }

  @SafeVarargs
  public static <T> TSet<T> of(T... elements) {
    var out = new TSet<T>(elements.length);
    for (var e : elements) out.backing.add(e);
    return out;
  }

  public static <T> TSet<T> from(Iterable<? extends T> source) {
    var out = new TSet<T>();
    for (var e : source) out.backing.add(e);
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

  public <U> TSet<U> map(Function<T, U> f) {
    var out = new TSet<U>(backing.size());
    for (var e : backing) out.backing.add(f.apply(e));
    return out;
  }

  @Override
  public Iterator<T> iterator() {
    return backing.iterator();
  }

  public Set<T> asUnmodifiableSet() {
    return Collections.unmodifiableSet(backing);
  }
}
