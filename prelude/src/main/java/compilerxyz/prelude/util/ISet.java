package compilerxyz.prelude.util;

import compilerxyz.prelude.annotations.Immutable;
import java.util.Iterator;
import java.util.Set;

@Immutable
public final class ISet<T> implements Iterable<T> {
    private final Set<T> backing;

    private ISet(Set<T> backing) {
        this.backing = backing;
    }

    @SafeVarargs
    public static <T> ISet<T> of(T... elements) {
        return new ISet<>(Set.of(elements));
    }

    public boolean contains(T element) {
        return backing.contains(element);
    }

    @Override
    public Iterator<T> iterator() {
        return backing.iterator();
    }
}
