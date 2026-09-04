package compilerxyz.main.gen;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.source_interface.InterfaceMethodBuilding;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 6345L)
public final class SourceInterfaceMethodResult {
    public final InterfaceMethodBuilding method;

    public SourceInterfaceMethodResult(InterfaceMethodBuilding method) {
        this.method = method;
    }

    public void impl(Type arg, Opt<Type> ret) {
        if (this.method.argType.optIsSome()) {
            throw new IllegalStateException("Interface method already has an arg type");
        }
        this.method.argType = Opt.some(arg);
        if (this.method.retType.optIsSome()) {
            throw new IllegalStateException("Interface method already has a ret type");
        }
        this.method.retType = ret;
    }
}
