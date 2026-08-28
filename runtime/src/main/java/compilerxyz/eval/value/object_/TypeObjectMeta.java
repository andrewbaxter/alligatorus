package compilerxyz.eval.value.object_;

import compilerxyz.eval.Unique;
import compilerxyz.util.IList;
import compilerxyz.util.Opt;

public record TypeObjectMeta(
        String typeDesc,
        IList<Unique> trivialIdentityAspects,
        IList<TypeObjectCastRule> castRules,
        IList<TypeObjectCastFromRule> castFromRules,
        Opt<ObjectAspectSource> aspectSource) {}
