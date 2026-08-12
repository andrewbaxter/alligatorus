package compilerxyz.util;

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
}
