package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.dynamic.DynUtil;

public sealed interface TypeObjectCastFromRule {

    boolean typeObjectCastFromRuleMatches(ExprContext xx, Type sourceType);

    EResult<CodeValue> typeObjectCastFromRuleApply(TypeObjectDynamic dest, CodeValue cv);

    record RestampFromIdentity(Unique identity) implements TypeObjectCastFromRule {
        @Override
        public boolean typeObjectCastFromRuleMatches(ExprContext xx, Type sourceType) {
            return sourceType.typeAspect(xx, identity).optIsSome();
        }

        @Override
        public EResult<CodeValue> typeObjectCastFromRuleApply(TypeObjectDynamic dest, CodeValue cv) {
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, dest));
        }
    }
}
