package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;

public final class JvmSort {
    private JvmSort() {}

    public static JavaDataDescriptor descriptor(ExprContext xx, Type type) {
        return switch (GenAspectConsumeDynamic.getMaybe(xx, type)) {
            case Opt.Some<AspectConsumeDynamic> some -> some.value().aspectConsumeDynamicJvmClass(xx);
            case Opt.None<AspectConsumeDynamic> n ->
                throw new IllegalStateException(
                        "JvmSort: type (" + type.typeDesc() + ") does not implement AspectConsumeDynamic");
        };
    }
}
