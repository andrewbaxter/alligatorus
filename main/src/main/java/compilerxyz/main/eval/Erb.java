package compilerxyz.main.eval;

import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;

public final class Erb {
    private final TList<ExprErr> errors;
    private final QExprId loc;

    public Erb(QExprId loc) {
        this.loc = loc;
        this.errors = new TList<>();
    }

    public TList<ExprErr> errors() {
        return errors;
    }

    public void errPush(String message) {
        errors.add(ExprErr.opaque(loc, message));
    }

    public <T> Opt<T> mustEr(EResult<T> r) {
        if (r.hasErrors()) {
            r.drainInto(this.errors);
            return Opt.none();
        }
        var value = r.requireValue("Erb::mustEr value");
        if (value instanceof CodeValue cv && cv.value().optIsNone()) {
            errPush("Expression does not fall through (contains an unreachable control jump like"
                    + " throw/panic); not valid in this position");
            return Opt.none();
        }
        return Opt.some(value);
    }

    public <T> Opt<T> mustErMaybeUnreachable(EResult<T> r) {
        if (r.hasErrors()) {
            r.drainInto(this.errors);
            return Opt.none();
        }
        return Opt.some(r.requireValue("Erb::mustErMaybeUnreachable value"));
    }

    public Opt<CodeValue> mustCv(ExprContext xx, EResult<EvalRes> r) {
        if (r.hasErrors()) {
            r.drainInto(this.errors);
            return Opt.none();
        }
        var res = r.requireValue("Erb::mustCv value");
        xx.scope.adopt(res.scope());
        if (res.cv().value().optIsNone()) {
            errPush("Expression does not fall through (contains an unreachable control jump like"
                    + " throw/panic); not valid in this position");
            return Opt.none();
        }
        return Opt.some(res.cv());
    }

    public <T> Opt<T> mustRes(Result<T, String> r) {
        return switch (r) {
            case Result.Ok<T, String> o -> Opt.some(o.value());
            case Result.Err<T, String> e -> {
                errPush(e.error());
                yield Opt.none();
            }
        };
    }
}
