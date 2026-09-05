package compilerxyz.main.eval.value.uint;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.numeric.Numerics;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFieldUintDynamicBox implements AspectField {
    public static final AspectFieldUintDynamicBox INSTANCE = new AspectFieldUintDynamicBox();

    private AspectFieldUintDynamicBox() {}

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var unboxed = new TypeUintDynamic();
        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeUintDynamicBox::field: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(Numerics.unbox(loc, NumericKind.UINT));
        var unboxedSelf = new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, unboxed)));
        return GenAspectField.getMaybe(xx, unboxed).optUnwrap().aspectFieldField(xx, loc, unboxedSelf, key);
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
