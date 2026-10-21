package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Unique;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;

public record TypeObjectMeta(
        String typeDesc,
        IList<Unique> trivialIdentityAspects,
        IList<TypeObjectCastRule> castRules,
        IList<TypeObjectCastFromRule> castFromRules,
        Opt<ObjectAspectSource> aspectSource,
        boolean identityEq,
        ObjectLiftKind lift) {}
