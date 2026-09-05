package compilerxyz.main.eval.value.uint;

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

public final class AspectFieldUintConst implements AspectField {
    public static final AspectFieldUintConst INSTANCE = new AspectFieldUintConst();

    private AspectFieldUintConst() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dyn = new TypeUintDynamic();
        var convertedRes = Numerics.castTo(xx, loc, self, NumericKind.UINT, dyn);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        return GenAspectField.getMaybe(xx, dyn)
                .optUnwrap()
                .aspectFieldField(xx, loc, convertedRes.requireValue("TypeUintConst::field cast"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, new TypeUintDynamic()).optUnwrap().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, new TypeUintDynamic()).optUnwrap().aspectFieldTypeOpt(xx, key);
    }
}
