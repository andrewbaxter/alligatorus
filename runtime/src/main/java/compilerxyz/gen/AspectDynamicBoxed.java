package compilerxyz.gen;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;

@Builtin(uniqueId = 1033L)
@BuiltinTypeId(7209L)
public interface AspectDynamicBoxed extends Aspect {
  Type aspectDynamicBoxedBoxedForm();
}
