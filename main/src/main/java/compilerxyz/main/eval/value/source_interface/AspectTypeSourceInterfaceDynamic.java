package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.GenInterfaceCell;
import compilerxyz.main.gen.Value;

public final class AspectTypeSourceInterfaceDynamic implements AspectType {
    public static final AspectTypeSourceInterfaceDynamic INSTANCE = new AspectTypeSourceInterfaceDynamic();

    private AspectTypeSourceInterfaceDynamic() {}

    @Override
    public Type aspectTypeType(Value value) {
        return GenInterfaceCell.dynamicType();
    }
}
