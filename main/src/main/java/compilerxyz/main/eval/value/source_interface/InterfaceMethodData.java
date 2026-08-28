package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.GenType;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class InterfaceMethodData {
    public static Type dynamicType() {
        return GenType.dynamicType();
    }

    public final String name;
    public final TList<String> dataFieldNames;
    public Type argType;
    public Opt<Type> retType = Opt.none();

    public Opt<String> hostMethodName = Opt.none();
    public Opt<JavaMethodDescriptor> hostMethodDescriptor = Opt.none();

    public InterfaceMethodData(String name) {
        this.name = name;
        this.dataFieldNames = new TList<>();
    }
}
