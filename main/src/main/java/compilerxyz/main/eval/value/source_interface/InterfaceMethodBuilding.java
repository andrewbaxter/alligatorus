package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;

public final class InterfaceMethodBuilding {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;
    public Opt<Type> argType = Opt.none();
    public Opt<Type> retType = Opt.none();

    public InterfaceMethodBuilding(String name) {
        this.name = name;
    }
}
