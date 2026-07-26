package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Type;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;
import compilerxyz.util.TList;

@Builtin(uniqueId = 1047L, identityId = 4045L)
public interface AspectListConst extends Aspect {
  TList<Value> aspectListConstElements(Value value);

  Type aspectListConstElementType(Value value);
}
