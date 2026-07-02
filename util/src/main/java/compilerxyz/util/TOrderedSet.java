package compilerxyz.util;

import java.util.AbstractSet;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public final class TOrderedSet<T> extends AbstractSet<T> {
  private final LinkedHashSet<T> backing;

  public TOrderedSet() {
    this.backing = new LinkedHashSet<>();
  }

  public TOrderedSet(int initialCapacity) {
    this.backing = new LinkedHashSet<>(initialCapacity);
  }

  @SafeVarargs
  public static <T> TOrderedSet<T> of(T... elements) {
    var out = new TOrderedSet<T>(elements.length);
    for (var e : elements) out.backing.add(e);
    return out;
  }

  public static <T> TOrderedSet<T> from(Iterable<? extends T> source) {
    var out = new TOrderedSet<T>();
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

  public <U> TOrderedSet<U> map(Function<T, U> f) {
    var out = new TOrderedSet<U>(backing.size());
    for (var e : backing) out.backing.add(f.apply(e));
    return out;
  }

  public Opt<T> getFirst() {
    if (backing.isEmpty()) return Opt.none();
    return Opt.some(backing.iterator().next());
  }

  public Opt<T> getLast() {
    if (backing.isEmpty()) return Opt.none();
    T last = null;
    for (var e : backing) last = e;
    return Opt.some(last);
  }

  @Override
  public Iterator<T> iterator() {
    return backing.iterator();
  }

  public Set<T> asUnmodifiableSet() {
    return Collections.unmodifiableSet(backing);
  }
}
