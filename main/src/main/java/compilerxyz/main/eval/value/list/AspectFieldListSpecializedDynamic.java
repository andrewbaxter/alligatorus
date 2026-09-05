package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListSpecializedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        if (ck.equals(new ConstKey.Str("new"))) {
            return EResult.ok(new CodeValue(
                    self.code(),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            ValueObjectConst.plainTypeType(), new TypeListSpecializedNewDynamic(type)))));
        }
        if (ck.equals(new ConstKey.Str("push"))) {
            return EResult.ok(DynUtil.methodRefOn(self, new TypeListSpecializedPushDynamic(type)));
        }
        return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.of(new ConstKey.Str("new"), new ConstKey.Str("push"));
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        if (key.equals(new ConstKey.Str("new"))) {
            return Opt.some(new TypeListSpecializedNewDynamic(type));
        }
        if (key.equals(new ConstKey.Str("push"))) {
            return Opt.some(new TypeListSpecializedPushDynamic(type));
        }
        return Opt.none();
    }
}
