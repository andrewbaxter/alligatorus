package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public final class AspectTypeSourceObjectConst implements AspectType {
    public static final AspectTypeSourceObjectConst INSTANCE = new AspectTypeSourceObjectConst();

    private AspectTypeSourceObjectConst() {}

    @Override
    public Type aspectTypeType(Value value) {
        if (!(value instanceof ValueObjectConst voc)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: expected ValueObjectConst, got "
                    + (value == null ? "null" : value.getClass().getName()));
        }
        if (!(voc.data() instanceof Type t)) {
            throw new IllegalStateException("TypeSourceObjectConst::type_: wrapped data is not a Type, got "
                    + (voc.data() == null ? "null" : voc.data().getClass().getName()));
        }
        return t;
    }
}
