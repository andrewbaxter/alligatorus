package compilerxyz.main.eval.value;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.object.TypeObjectConst;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.GenTrivialIdentityAspects;

public final class TypeOfTypeCast {
    private TypeOfTypeCast() {}

    public static boolean canCastTo(ExprContext xx, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome();
    }

    public static EResult<CodeValue> castTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType, Type held) {
        if (!canCastTo(xx, destType)) {
            return EResult.err(loc, "Cannot cast type(" + held.typeDesc() + ") to " + destType.typeDesc());
        }
        var restamped = new CodeValue(
                cv.code(), EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), held)));
        if (AspectLookup.getMaybe(
                        xx, destType, GenTrivialIdentityAspects.TYPE_OBJECT_CONST_IDENTITY, TypeObjectConst.class)
                .optIsSome()) {
            return EResult.ok(restamped);
        }
        return DynUtil.consumeThenRestamp(xx, loc, restamped, destType);
    }
}
