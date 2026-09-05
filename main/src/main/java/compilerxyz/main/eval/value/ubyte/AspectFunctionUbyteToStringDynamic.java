package compilerxyz.main.eval.value.ubyte;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.value.string.TypeStringDynamic;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionUbyteToStringDynamic implements AspectFunction {
    public static final AspectFunctionUbyteToStringDynamic INSTANCE = new AspectFunctionUbyteToStringDynamic();

    private AspectFunctionUbyteToStringDynamic() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeUbyteToStringDynamic::call: self has no dynamic code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var retType = new TypeStringDynamic();
        var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType);
        if (retStackRes.hasErrors()) return retStackRes.reerr();

        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callStaticMethod(loc, GenCodegenDescriptors.NUMERIC_FORMAT_FORMAT_UNSIGNED_BYTE));
        return EResult.ok(retStackRes
                .requireValue("AspectStackValueDynamic.get")
                .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
