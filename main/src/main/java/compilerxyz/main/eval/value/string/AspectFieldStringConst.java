package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldStringConst implements AspectField {
    public static final AspectFieldStringConst INSTANCE = new AspectFieldStringConst();

    private AspectFieldStringConst() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dynType = new TypeStringDynamic();
        var convertedRes = dynType.typeCastFrom(xx, loc, self);
        if (convertedRes.hasErrors()) {
            return convertedRes.reerr();
        }
        return GenAspectField.getMaybe(xx, dynType)
                .optUnwrap()
                .aspectFieldField(xx, loc, convertedRes.requireValue("TypeStringDynamic::typeCastFrom"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, new TypeStringDynamic()).optUnwrap().aspectFieldTypeOpt(xx, key);
    }
}
