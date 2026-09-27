package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.Assert;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

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
        errs.add(ExprErr.opaque(loc, message));
        return new EResult<>(Opt.none(), Opt.some(errs));
    }

    public static <T> EResult<T> errs(TList<ExprErr> errors) {
        return new EResult<>(Opt.none(), Opt.some(errors));
    }

    public static <T> EResult<T> mixed(T value, TList<ExprErr> errors) {
        return new EResult<>(Opt.some(value), Opt.some(errors));
    }

    public Opt<T> valueOpt() {
        return value;
    }

    @CodegenDescriptor
    public T requireValue(String context) {
        if (value.optIsNone()) {
            if (errors.optIsSome() && !errors.optUnwrap().isEmpty()) {
                throw new IllegalStateException(context + " returned errors: " + errors.optUnwrap());
            }
            throw new IllegalStateException(context + " returned neither value nor errors");
        }
        return value.optUnwrap();
    }

    @CodegenDescriptor
    public Opt<TList<ExprErr>> errorsOpt() {
        return errors;
    }

    @CodegenDescriptor
    public boolean hasErrors() {
        return errors.optIsSome() && !errors.optUnwrap().isEmpty();
    }

    public void drainInto(TList<ExprErr> sink) {
        if (errors.optIsNone()) return;
        Assert.notSame(
                errors.optUnwrap(),
                sink,
                "EResult.drainInto: sink is this result's own error list; the caller aliased an"
                        + " error accumulator into a result it then drained back into the same accumulator");
        sink.addAll(errors.optUnwrap());
    }

    public <U> EResult<U> reerr() {
        return new EResult<>(Opt.none(), errors);
    }
}
