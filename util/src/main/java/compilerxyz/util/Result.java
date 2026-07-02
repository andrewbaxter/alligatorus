package compilerxyz.util;

import java.util.function.Function;

public sealed interface Result<T, E> {
  record Ok<T, E>(T value) implements Result<T, E> {}

  record Err<T, E>(E error) implements Result<T, E> {}

  static <T, E> Result<T, E> ok(T value) {
    return new Ok<>(value);
  }

  static <T, E> Result<T, E> err(E error) {
    return new Err<>(error);
  }

  default boolean resultIsOk() {
    return switch (this) {
      case Ok<T, E> o -> true;
      case Err<T, E> e -> false;
    };
  }

  default boolean resultIsErr() {
    return switch (this) {
      case Ok<T, E> o -> false;
      case Err<T, E> e -> true;
    };
  }

  default T resultUnwrap() {
    return switch (this) {
      case Ok<T, E> o -> o.value();
      case Err<T, E> e -> throw new IllegalStateException("unwrap() on Err: " + e.error());
    };
  }

  default E resultUnwrapErr() {
    return switch (this) {
      case Ok<T, E> o -> throw new IllegalStateException("unwrapErr() on Ok");
      case Err<T, E> e -> e.error();
    };
  }

  default <U> Result<U, E> resultMap(Function<T, U> f) {
    return switch (this) {
      case Ok<T, E> o -> Result.ok(f.apply(o.value()));
      case Err<T, E> e -> Result.err(e.error());
    };
  }

  default <F> Result<T, F> resultMapErr(Function<E, F> f) {
    return switch (this) {
      case Ok<T, E> o -> Result.ok(o.value());
      case Err<T, E> e -> Result.err(f.apply(e.error()));
    };
  }

  default <U> Result<U, E> resultAndThen(Function<T, Result<U, E>> f) {
    return switch (this) {
      case Ok<T, E> o -> f.apply(o.value());
      case Err<T, E> e -> Result.err(e.error());
    };
  }
}
