package compilerxyz.eval;

import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.Optional;

public final class Erb {
  private final TList<ExprErr> errors;
  private final QExprId loc;

  public Erb(QExprId loc) {
    this.loc = loc;
    this.errors = new TList<>();
  }

  public QExprId loc() {
    return loc;
  }

  public TList<ExprErr> errors() {
    return errors;
  }

  public void errPush(String message) {
    errors.add(new ExprErr(loc, TList.of(new ExprSubErr.Opaque(message))));
  }

  public <T> Optional<T> errNew(String message) {
    errPush(message);
    return Optional.empty();
  }

  public <T> Optional<T> mustEr(EResult<T> r) {
    if (r.hasErrors()) {
      r.drainInto(this.errors);
      return Optional.empty();
    }
    return Optional.of(r.requireValue("Erb::mustEr value"));
  }

  public Optional<CodeValue> mustReachable(EResult<CodeValue> r) {
    if (r.hasErrors()) {
      r.drainInto(this.errors);
      return Optional.empty();
    }
    var cv = r.requireValue("Erb::mustReachable value");
    if (!(cv.value() instanceof Opt.Some<Value>)) {
      errPush(
          "Expression does not fall through (contains an unreachable control jump like"
              + " throw/panic); not valid in this position");
      return Optional.empty();
    }
    return Optional.of(cv);
  }

  public <T> Optional<T> mustRes(Result<T, String> r) {
    return switch (r) {
      case Result.Ok<T, String> o -> Optional.of(o.value());
      case Result.Err<T, String> e -> errNew(e.error());
    };
  }
}
