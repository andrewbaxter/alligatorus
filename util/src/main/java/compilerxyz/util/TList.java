package compilerxyz.util;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;

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

  public static <T> TList<T> empty() {
    return new TList<T>();
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

  public @Nullable T getOrNull(int index) {
    if (index < 0 || index >= backing.size()) return null;
    return backing.get(index);
  }

  public Opt<T> getOpt(int index) {
    if (index < 0 || index >= backing.size()) return Opt.none();
    return Opt.some(backing.get(index));
  }

  public Opt<T> firstOpt() {
    if (backing.isEmpty()) return Opt.none();
    return Opt.some(backing.get(0));
  }

  public Opt<T> lastOpt() {
    if (backing.isEmpty()) return Opt.none();
    return Opt.some(backing.get(backing.size() - 1));
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

  public <U> TList<U> map(Function<T, U> f) {
    var out = new TList<U>(backing.size());
    for (var e : backing) out.backing.add(f.apply(e));
    return out;
  }

  public TList<T> filter(Predicate<? super T> predicate) {
    var out = new TList<T>();
    for (var e : backing) {
      if (predicate.test(e)) out.backing.add(e);
    }
    return out;
  }

  public <U> TList<U> flatMap(Function<? super T, ? extends Iterable<U>> f) {
    var out = new TList<U>();
    for (var e : backing) {
      for (var u : f.apply(e)) out.backing.add(u);
    }
    return out;
  }

  public TList<T> sortedBy(Comparator<? super T> comparator) {
    var out = new TList<T>(backing.size());
    out.backing.addAll(backing);
    out.backing.sort(comparator);
    return out;
  }

  @Override
  public Iterator<T> iterator() {
    return backing.iterator();
  }

  public List<T> asUnmodifiableList() {
    return Collections.unmodifiableList(backing);
  }
}
