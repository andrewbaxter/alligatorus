package compilerxyz.main.eval.value.float_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldFloatConst implements AspectField {
    public static final AspectFieldFloatConst INSTANCE = new AspectFieldFloatConst();

    private AspectFieldFloatConst() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dyn = new TypeFloatDynamic();
        var convertedRes = Numerics.castTo(xx, loc, self, NumericKind.FLOAT, dyn);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        return GenAspectField.getMaybe(xx, dyn)
                .optUnwrap()
                .aspectFieldField(xx, loc, convertedRes.requireValue("TypeFloatConst::field cast"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, new TypeFloatDynamic()).optUnwrap().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, new TypeFloatDynamic()).optUnwrap().aspectFieldTypeOpt(xx, key);
    }
}
