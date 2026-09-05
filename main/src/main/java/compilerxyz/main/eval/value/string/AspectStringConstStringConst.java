package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectStringConst;
import compilerxyz.main.gen.Value;

public final class AspectStringConstStringConst implements AspectStringConst {
    public static final AspectStringConstStringConst INSTANCE = new AspectStringConstStringConst();

    private AspectStringConstStringConst() {}

    @Override
    public String aspectStringConstGet(Value value) {
        if (value instanceof ValueObjectConst v && v.data() instanceof String s) return s;
        throw new IllegalArgumentException(
                "TypeStringConst::get: expected string ValueObjectConst, got " + value.getClass());
    }
}
