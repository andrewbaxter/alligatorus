package compilerxyz.util;

import java.util.NoSuchElementException;
import java.util.function.Function;
import java.util.function.Supplier;

public sealed interface Opt<T> {
  record Some<T>(T value) implements Opt<T> {}

  record None<T>() implements Opt<T> {
    private static final None<?> INSTANCE = new None<>();
  }

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

  default boolean optIsSome() {
    return switch (this) {
      case Some<T> s -> true;
      case None<T> n -> false;
    };
  }

  default boolean optIsNone() {
    return switch (this) {
      case Some<T> s -> false;
      case None<T> n -> true;
    };
  }

  default T optUnwrap() {
    return switch (this) {
      case Some<T> s -> s.value();
      case None<T> n -> throw new NoSuchElementException("unwrap() on None");
    };
  }

  default T optUnwrapOr(T fallback) {
    return switch (this) {
      case Some<T> s -> s.value();
      case None<T> n -> fallback;
    };
  }

  default T optUnwrapOrElse(Supplier<T> fallback) {
    return switch (this) {
      case Some<T> s -> s.value();
      case None<T> n -> fallback.get();
    };
  }

  default <U> Opt<U> optMap(Function<T, U> f) {
    return switch (this) {
      case Some<T> s -> Opt.some(f.apply(s.value()));
      case None<T> n -> Opt.none();
    };
  }

  default <U> Opt<U> optAndThen(Function<T, Opt<U>> f) {
    return switch (this) {
      case Some<T> s -> f.apply(s.value());
      case None<T> n -> Opt.none();
    };
  }
}
