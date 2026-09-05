package compilerxyz.main.eval.aspect;

import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.MustCast;

public final class AspectTypeObjectConstData implements AspectType {
    public static final AspectTypeObjectConstData INSTANCE = new AspectTypeObjectConstData();

    private AspectTypeObjectConstData() {}

    @Override
    public Type aspectTypeType(Value value) {
        var voc = MustCast.mustCast(ValueObjectConst.class, value);
        return MustCast.mustCast(Type.class, voc.data());
    }
}
