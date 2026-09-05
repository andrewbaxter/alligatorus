package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenLists;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldListConst<T extends Type>(TypeListConst<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var dynRes = type.dynamicCv(xx, loc, self);
        if (dynRes.hasErrors()) return dynRes.reerr();
        var fieldRes = GenAspectField.get(xx, loc, GenLists.listDynamic(xx, type.elementType));
        if (fieldRes.hasErrors()) return fieldRes.reerr();
        return fieldRes.requireValue("TypeListConst::field AspectField")
                .aspectFieldField(xx, loc, dynRes.requireValue("TypeListConst::field self"), key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return GenAspectField.getMaybe(xx, GenLists.listDynamic(xx, type.elementType))
                .optUnwrap()
                .aspectFieldKeys(xx);
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return GenAspectField.getMaybe(xx, GenLists.listDynamic(xx, type.elementType))
                .optUnwrap()
                .aspectFieldTypeOpt(xx, key);
    }
}
