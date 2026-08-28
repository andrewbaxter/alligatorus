package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.IdentityToken;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.eval.value.object_.TypeObjectMeta;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Identity of a jump destination. Target-agnostic: emitted jumps, landing
 * points, and exception dests all reference keys; each target maps key
 * identity to its own representation (JVM: a label; structured targets:
 * whatever their control model requires).
 */
@BuiltinFull(uniqueId = 6026L)
@IdentityToken
public final class JumpKey {

}
