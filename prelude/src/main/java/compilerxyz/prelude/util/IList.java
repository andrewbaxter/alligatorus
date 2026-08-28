package compilerxyz.prelude.util;

import compilerxyz.prelude.annotations.Immutable;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Immutable
public final class IList<T> implements Iterable<T> {
    private final List<T> backing;

    private IList(List<T> backing) {
        this.backing = backing;
    }

    @SafeVarargs
    public static <T> IList<T> of(T... elements) {
        return new IList<>(List.of(elements));
    }

    public static <T> IList<T> from(Collection<? extends T> source) {
        return new IList<>(List.copyOf(source));
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

    @Override
    public boolean equals(Object o) {
        return o instanceof IList<?> other && other.backing.equals(this.backing);
    }

    @Override
    public int hashCode() {
        return backing.hashCode();
    }
}
