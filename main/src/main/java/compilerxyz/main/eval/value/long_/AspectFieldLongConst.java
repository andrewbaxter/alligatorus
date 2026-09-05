package compilerxyz.main.eval.value.long_;

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

public final class AspectFieldLongConst implements AspectField {
    public static final AspectFieldLongConst INSTANCE = new AspectFieldLongConst();

    private AspectFieldLongConst() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dyn = new TypeLongDynamic();
        var convertedRes = Numerics.castTo(xx, loc, self, NumericKind.LONG, dyn);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        return GenAspectField.getMaybe(xx, dyn)
                .optUnwrap()
                .aspectFieldField(xx, loc, convertedRes.requireValue("TypeLongConst::field cast"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, new TypeLongDynamic()).optUnwrap().aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, new TypeLongDynamic()).optUnwrap().aspectFieldTypeOpt(xx, key);
    }
}
