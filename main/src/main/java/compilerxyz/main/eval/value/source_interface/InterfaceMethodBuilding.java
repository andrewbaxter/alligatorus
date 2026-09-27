package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;

public final class InterfaceMethodBuilding {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;
    public final Type argType;
    public final Opt<Type> retType;

    public InterfaceMethodBuilding(String name, Type argType, Opt<Type> retType) {
        this.name = name;
        this.argType = argType;
        this.retType = retType;
    }
}
