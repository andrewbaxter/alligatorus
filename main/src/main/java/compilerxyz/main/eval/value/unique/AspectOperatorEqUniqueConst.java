package compilerxyz.main.eval.value.unique;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.value.bool.TypeBoolConst;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectOperatorEq;
import compilerxyz.main.gen.GenAspectOperatorEq;

public final class AspectOperatorEqUniqueConst implements AspectOperatorEq {
    public static final AspectOperatorEqUniqueConst INSTANCE = new AspectOperatorEqUniqueConst();

    private AspectOperatorEqUniqueConst() {}

    @Override
    public EResult<CodeValue> aspectOperatorEqEq(ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
        var b = TypeUniqueConst.extractUnique(base);
        var r = TypeUniqueConst.extractUnique(reference);
        if (b.optIsSome() && r.optIsSome()) {
            var concatRes = Eval.codeConcat(xx, loc, base.code(), reference.code());
            if (concatRes.hasErrors()) return concatRes.reerr();
            return EResult.ok(new CodeValue(
                    concatRes.requireValue("concat"),
                    EvalOutcome.ofValue(new ValueObjectConst(
                            new TypeBoolConst(), b.optUnwrap().equals(r.optUnwrap())))));
        }
        return GenAspectOperatorEq.getMaybe(xx, new TypeUniqueDynamic())
                .optUnwrap()
                .aspectOperatorEqEq(xx, loc, base, reference);
    }
}
