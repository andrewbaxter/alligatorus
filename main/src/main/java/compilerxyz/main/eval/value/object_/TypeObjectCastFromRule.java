package compilerxyz.main.eval.value.object_;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.ValueDynamic;

public sealed interface TypeObjectCastFromRule {

    boolean typeObjectCastFromRuleMatches(ExprContext xx, Type sourceType);

    EResult<CodeValue> typeObjectCastFromRuleApply(ExprContext xx, TypeObjectDynamic dest, CodeValue cv);

    record RestampFromIdentity(Unique identity) implements TypeObjectCastFromRule {
        @Override
        public boolean typeObjectCastFromRuleMatches(ExprContext xx, Type sourceType) {
            return sourceType.typeAspect(xx, identity).optIsSome();
        }

        @Override
        public EResult<CodeValue> typeObjectCastFromRuleApply(ExprContext xx, TypeObjectDynamic dest, CodeValue cv) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, dest))));
        }
    }
}
