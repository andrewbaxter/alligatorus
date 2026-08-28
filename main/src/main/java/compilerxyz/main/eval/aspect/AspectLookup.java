package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.prelude.util.Opt;

public final class AspectLookup {
    private AspectLookup() {}

    public static <A extends Aspect> Opt<A> getMaybe(ExprContext xx, Type type, Unique id, Class<A> aspectClass) {
        return switch (type.typeAspect(xx, id)) {
            case Opt.Some<Aspect> s -> Opt.some(aspectClass.cast(s.value()));
            case Opt.None<Aspect> n -> Opt.none();
        };
    }

    public static <A extends Aspect> EResult<A> get(
            ExprContext xx, QExprId loc, Type type, Unique id, Class<A> aspectClass) {
        var maybe = getMaybe(xx, type, id, aspectClass);
        if (maybe.optIsSome()) return EResult.ok(maybe.optUnwrap());
        return EResult.err(loc, "Type " + type.typeDesc() + " does not implement " + aspectClass.getSimpleName());
    }
}
