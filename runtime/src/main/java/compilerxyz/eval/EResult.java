package compilerxyz.eval;

import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class EResult<T> {
  private final Opt<T> value;
  private final Opt<TList<ExprErr>> errors;

  private EResult(Opt<T> value, Opt<TList<ExprErr>> errors) {
    this.value = value;
    this.errors = errors;
  }

  public static <T> EResult<T> ok(T value) {
    return new EResult<>(Opt.some(value), Opt.none());
  }

  public static <T> EResult<T> err(QExprId loc, String message) {
    var errs = new TList<ExprErr>();
    errs.add(new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message))));
    return new EResult<>(Opt.none(), Opt.some(errs));
  }

  public static <T> EResult<T> errs(TList<ExprErr> errors) {
    return new EResult<>(Opt.none(), Opt.some(errors));
  }

  public static <T> EResult<T> mixed(T value, TList<ExprErr> errors) {
    return new EResult<>(Opt.some(value), Opt.some(errors));
  }

  public Opt<T> valueOrNull() {
    return value;
  }

  public T requireValue(String context) {
    if (value.optIsNone()) {
      if (errors.optIsSome() && !errors.optUnwrap().isEmpty()) {
        throw new IllegalStateException(context + " returned errors: " + errors.optUnwrap());
      }
      throw new IllegalStateException(
          context + " returned neither value nor errors");
    }
    return value.optUnwrap();
  }

  public Opt<TList<ExprErr>> errorsOrNull() {
    return errors;
  }

  public boolean hasErrors() {
    return errors.optIsSome() && !errors.optUnwrap().isEmpty();
  }

  public void drainInto(TList<ExprErr> sink) {
    if (errors.optIsSome()) sink.addAll(errors.optUnwrap());
  }

  public <U> EResult<U> reerr() {
    return new EResult<>(Opt.none(), errors);
  }
}
