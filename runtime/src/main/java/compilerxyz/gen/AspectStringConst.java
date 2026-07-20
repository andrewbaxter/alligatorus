package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.Value;

@BuiltinAspect(uniqueId = 1009L, kind = "StringConst", javadoc = "Identity aspect for const str")
@compilerxyz.annotations.BuiltinTypeId(7233L)
public interface AspectStringConst extends Aspect {
  String aspectStringConstGet(Value value);
}
