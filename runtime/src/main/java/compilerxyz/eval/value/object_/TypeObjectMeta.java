package compilerxyz.eval.value.object_;

import compilerxyz.eval.Unique;
import compilerxyz.util.Opt;
import compilerxyz.util.IList;

public record TypeObjectMeta(
        String typeDesc,
        IList<Unique> trivialIdentityAspects,
        IList<TypeObjectCastRule> castRules,
        IList<TypeObjectCastFromRule> castFromRules,
        Opt<ObjectAspectSource> aspectSource) {
}
