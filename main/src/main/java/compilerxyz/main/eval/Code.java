package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6016L)
public sealed interface Code permits Code.None, Code.Target {

    record None(QExprId loc) implements Code {
        @Override
        public boolean codeIsNone() {
            return true;
        }
    }

    record Target(TargetCode impl) implements Code {
        @Override
        public boolean codeIsNone() {
            return false;
        }
    }

    boolean codeIsNone();

    static Code none(QExprId loc) {
        return new None(loc);
    }

    @CodegenDescriptor
    static Code target(TargetCode impl) {
        return new Target(impl);
    }

    static <T extends TargetCode> EResult<Opt<T>> codeCast(QExprId loc, Code code, Class<T> targetCode) {
        return switch (code) {
            case None n -> EResult.ok(Opt.none());
            case Target t -> {
                if (targetCode.isInstance(t.impl())) {
                    yield EResult.ok(Opt.some(targetCode.cast(t.impl())));
                }
                yield EResult.err(
                        loc,
                        "code belongs to a different target (expected " + targetCode.getSimpleName() + ", got "
                                + t.impl().getClass().getSimpleName() + ")");
            }
        };
    }

    @CodegenDescriptor
    static EResult<TargetCode> castTargetCode(TargetCode impl, QExprId loc, Class<?> targetClass) {
        if (targetClass.isInstance(impl)) {
            return EResult.ok(impl);
        }
        return EResult.err(
                loc,
                "code belongs to a different target (expected " + targetClass.getSimpleName() + ", got "
                        + impl.getClass().getSimpleName() + ")");
    }

    @CodegenDescriptor
    static TargetCode targetImpl(Code code) {
        return switch (code) {
            case Code.Target t -> t.impl();
            case Code.None n -> throw new IllegalStateException("Code::targetImpl: code is None, not Target");
        };
    }
}
