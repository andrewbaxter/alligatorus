package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Type;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import java.lang.IllegalStateException;
import compilerxyz.util.Opt;

@BuiltinPushStruct
public final class SourceInterfaceMethodResult {
    public final InterfaceMethodData method;

    public SourceInterfaceMethodResult(InterfaceMethodData method) {
        this.method = method;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceInterfaceMethodResult();
    }

    public void impl(ExprContext xx, Type arg, Opt<Type> ret) {
        if (this.method.argType != null) {
            throw new IllegalStateException("Interface method already has an arg type");
        }
        this.method.argType = arg;
        if (this.method.retType.optIsSome()) {
            throw new IllegalStateException("Interface method already has a ret type");
        }
        this.method.retType = ret;
    }
}
