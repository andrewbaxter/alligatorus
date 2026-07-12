package compilerxyz.eval;

import compilerxyz.util.TList;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;

public final class EResult<T> {
  private final @Nullable T value;
  private final @Nullable TList<ExprErr> errors;

  private EResult(@Nullable T value, @Nullable TList<ExprErr> errors) {
    this.value = value;
    this.errors = errors;
  }

  public static <T> EResult<T> ok(T value) {
    return new EResult<>(value, null);
  }

  public static <T> EResult<T> err(QExprId loc, String message) {
    var errs = new TList<ExprErr>();
    errs.add(new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message))));
    return new EResult<>(null, errs);
  }

  public static <T> EResult<T> errs(TList<ExprErr> errors) {
    return new EResult<>(null, errors);
  }

  public static <T> EResult<T> mixed(T value, TList<ExprErr> errors) {
    return new EResult<>(value, errors);
  }

  public @Nullable T valueOrNull() {
    return value;
  }

  public T requireValue(String context) {
    if (value == null) {
      throw new IllegalStateException(
          context + " returned neither value nor errors");
    }
    return value;
  }

  public @Nullable TList<ExprErr> errorsOrNull() {
    return errors;
  }

  public boolean hasErrors() {
    return errors != null && !errors.isEmpty();
  }

  public void drainInto(TList<ExprErr> sink) {
    if (errors != null) sink.addAll(errors);
  }

  public <U> EResult<U> map(Function<T, U> f) {
    return new EResult<>(value == null ? null : f.apply(value), errors);
  }

  public <U> EResult<U> reerr() {
    return new EResult<>(null, errors);
  }
}
