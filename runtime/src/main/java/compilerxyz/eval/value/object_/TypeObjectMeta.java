package compilerxyz.eval.value.object_;

import compilerxyz.eval.Unique;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Shared descriptor metadata between {@link TypeObjectDynamic} and
 * {@link TypeObjectConst}. Paired instances must carry the same
 * {@code TypeObjectMeta} so a dynamic can synthesize its matching const
 * (and vice versa).
 */
public record TypeObjectMeta(
    String typeDesc,
    TList<Unique> trivialIdentityAspects,
    TList<TypeObjectCastRule> castRules,
    TList<TypeObjectCastFromRule> castFromRules,
    Opt<ObjectAspectSource> aspectSource) {}
