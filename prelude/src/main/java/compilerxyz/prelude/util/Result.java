package compilerxyz.prelude.util;

public sealed interface Result<T, E> {
    static <T, E> Result<T, E> ok(T value) {
        return new Ok<>(value);
    }

    static <T, E> Result<T, E> err(E error) {
        return new Err<>(error);
    }

    boolean resultIsOk();

    boolean resultIsErr();

    record Ok<T, E>(T value) implements Result<T, E> {
        @Override
        public boolean resultIsOk() {
            return true;
        }

        @Override
        public boolean resultIsErr() {
            return false;
        }
    }

    record Err<T, E>(E error) implements Result<T, E> {
        @Override
        public boolean resultIsOk() {
            return false;
        }

        @Override
        public boolean resultIsErr() {
            return true;
        }
    }
}
