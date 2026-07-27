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
    var value = r.requireValue("Erb::mustEr value");
    if (value instanceof CodeValue cv && !(cv.value() instanceof Opt.Some<Value>)) {
      errPush(
          "Expression does not fall through (contains an unreachable control jump like"
              + " throw/panic); not valid in this position");
      return Optional.empty();
    }
    return Optional.of(value);
  }

  public <T> Optional<T> mustErMaybeUnreachable(EResult<T> r) {
    if (r.hasErrors()) {
      r.drainInto(this.errors);
      return Optional.empty();
    }
    return Optional.of(r.requireValue("Erb::mustErMaybeUnreachable value"));
  }

  /** An exit reached an evaluation boundary without finding its target label. */
  public static ExprErr exitUnmatchedError(Exit exit) {
    if (exit.label instanceof Opt.Some<String> lbl) {
      return new ExprErr(
          exit.loc,
          TList.of(
              new ExprSubErr.Opaque(
                  "return: no enclosing scope with label '" + lbl.value() + "'")));
    }
    return new ExprErr(
        exit.loc,
        TList.of(new ExprSubErr.Opaque("return: no enclosing control scope")));
  }

  public static ExprErr exitCrossError(Exit exit) {
    if (exit.label instanceof Opt.Some<String> lbl) {
      return new ExprErr(
          exit.loc,
          TList.of(
              new ExprSubErr.Opaque(
                  "return: control exit to label '"
                      + lbl.value()
                      + "' crosses an expression boundary; not valid in this position")));
    }
    return new ExprErr(
        exit.loc,
        TList.of(
            new ExprSubErr.Opaque(
                "return: control exit crosses an expression boundary; not valid in this"
                    + " position")));
  }

  private boolean exitsRejected(EvalRes res) {
    for (var exit : res.exits()) {
      errors.add(exitCrossError(exit));
    }
    return !res.exits().isEmpty();
  }

  /** Operand-position extraction: requires a fall-through value and no pending exits. */
  public Optional<CodeValue> mustCv(EResult<EvalRes> r) {
    if (r.hasErrors()) {
      r.drainInto(this.errors);
      return Optional.empty();
    }
    var res = r.requireValue("Erb::mustCv value");
    if (exitsRejected(res)) {
      return Optional.empty();
    }
    if (!(res.cv().value() instanceof Opt.Some<Value>)) {
      errPush(
          "Expression does not fall through (contains an unreachable control jump like"
              + " throw/panic); not valid in this position");
      return Optional.empty();
    }
    return Optional.of(res.cv());
  }

  /** Like {@link #mustCv} but allows a control-ended value; still rejects pending exits. */
  public Optional<CodeValue> mustCvMaybeUnreachable(EResult<EvalRes> r) {
    if (r.hasErrors()) {
      r.drainInto(this.errors);
      return Optional.empty();
    }
    var res = r.requireValue("Erb::mustCvMaybeUnreachable value");
    if (exitsRejected(res)) {
      return Optional.empty();
    }
    return Optional.of(res.cv());
  }


  public <T> Optional<T> mustRes(Result<T, String> r) {
    return switch (r) {
      case Result.Ok<T, String> o -> Optional.of(o.value());
      case Result.Err<T, String> e -> errNew(e.error());
    };
  }
}
