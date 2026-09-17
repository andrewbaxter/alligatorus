package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldSourceObjectConst(TypeSourceObjectConst type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        TypeSourceObjectDynamic dynType = type.dynamic();
        if (DynUtil.dynamicHasCode(self)) {
            return GenAspectField.getMaybe(xx, dynType).optUnwrap().aspectFieldField(xx, loc, self, key);
        }
        var selfCodeRes = new AspectConsumeDynamicSourceObjectConst(type).aspectConsumeDynamicToDynamic(xx, loc, self);
        if (selfCodeRes.hasErrors()) return selfCodeRes.reerr();
        var selfDynamic = selfCodeRes.requireValue("TypeSourceObjectConst::field toDynamic");
        return GenAspectField.getMaybe(xx, dynType).optUnwrap().aspectFieldField(xx, loc, selfDynamic, key);
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }
}
