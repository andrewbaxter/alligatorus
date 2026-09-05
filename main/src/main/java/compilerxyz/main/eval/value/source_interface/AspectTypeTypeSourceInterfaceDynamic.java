package compilerxyz.main.eval.value.source_interface;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public record AspectTypeTypeSourceInterfaceDynamic(TypeTypeSourceInterfaceDynamic type) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        if (value instanceof ValueObjectConst voc && voc.data() instanceof Type t) {
            return t;
        }
        return type.require("aspectTypeType");
    }
}
