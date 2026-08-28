package compilerxyz.util;

import compilerxyz.annotations.CodegenDescriptor;
import java.util.NoSuchElementException;
import java.util.function.Function;

public sealed interface Opt<T> {
    @CodegenDescriptor
    @SuppressWarnings("unchecked")
    static <T> Opt<T> none() {
        return (Opt<T>) None.INSTANCE;
    }

    static <T> Opt<T> some(T value) {
        return new Some<>(value);
    }

    static <T> Opt<T> ofNullable(T value) {
        return value == null ? Opt.none() : Opt.some(value);
    }

    boolean optIsSome();

    boolean optIsNone();

    @CodegenDescriptor
    T optUnwrap();

    T optUnwrapOr(T fallback);

    <U> Opt<U> optMap(Function<T, U> f);

    record Some<T>(@CodegenDescriptor T value) implements Opt<T> {
        @Override
        public boolean optIsSome() {
            return true;
        }

        @Override
        public boolean optIsNone() {
            return false;
        }

        @Override
        public T optUnwrap() {
            return value;
        }

        @Override
        public T optUnwrapOr(T fallback) {
            return value;
        }

        @Override
        public <U> Opt<U> optMap(Function<T, U> f) {
            return Opt.some(f.apply(value));
        }
    }

    record None<T>() implements Opt<T> {
        private static final None<?> INSTANCE = new None<>();

        @Override
        public boolean optIsSome() {
            return false;
        }

        @Override
        public boolean optIsNone() {
            return true;
        }

        @Override
        public T optUnwrap() {
            throw new NoSuchElementException("unwrap() on None");
        }

        @Override
        public T optUnwrapOr(T fallback) {
            return fallback;
        }

        @Override
        public <U> Opt<U> optMap(Function<T, U> f) {
            return Opt.none();
        }
    }
}
