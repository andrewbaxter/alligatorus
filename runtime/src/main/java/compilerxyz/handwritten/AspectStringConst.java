package compilerxyz.handwritten;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1009L, kind = "StringConst", javadoc = "Identity aspect for const str")
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
