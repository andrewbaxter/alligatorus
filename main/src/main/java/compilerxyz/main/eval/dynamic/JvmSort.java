package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.Opt;
import org.objectweb.asm.Opcodes;

public final class JvmSort {
    private JvmSort() {}

    public static int loadOpcode(ExprContext xx, Type type) {
        return typedOpcode(xx, type, Opcodes.ILOAD, Opcodes.LLOAD, Opcodes.FLOAD, Opcodes.DLOAD, Opcodes.ALOAD);
    }

    public static int storeOpcode(ExprContext xx, Type type) {
        return typedOpcode(xx, type, Opcodes.ISTORE, Opcodes.LSTORE, Opcodes.FSTORE, Opcodes.DSTORE, Opcodes.ASTORE);
    }

    private static int typedOpcode(ExprContext xx, Type type, int iOp, int jOp, int fOp, int dOp, int aOp) {
        return switch (GenAspectConsumeDynamic.getMaybe(xx, type)) {
            case Opt.Some<AspectConsumeDynamic> some -> {
                var desc = some.value().aspectConsumeDynamicJvmClass(xx).value();
                if (desc.isEmpty()) yield aOp;
                yield switch (desc.charAt(0)) {
                    case 'D' -> dOp;
                    case 'J' -> jOp;
                    case 'F' -> fOp;
                    case 'I', 'Z', 'B', 'S', 'C' -> iOp;
                    default -> aOp;
                };
            }
            case Opt.None<AspectConsumeDynamic> n -> aOp;
        };
    }
}
