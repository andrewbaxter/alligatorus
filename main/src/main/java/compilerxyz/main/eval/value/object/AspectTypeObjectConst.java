package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public record AspectTypeObjectConst(TypeObjectConst type) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        if (value instanceof ValueObjectConst v) {
            return (Type) v.data();
        }
        if (value instanceof Type t) {
            return t;
        }
        throw new IllegalArgumentException(
                type.typeDesc() + "::type_: expected Type or ValueObjectConst, got " + value.getClass());
    }
}
