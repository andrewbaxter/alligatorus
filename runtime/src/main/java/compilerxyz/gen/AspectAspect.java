package compilerxyz.gen;

import compilerxyz.annotations.BuiltinAspect;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.source_interface.InterfaceData;

@BuiltinAspect(
    uniqueId = 1029L,
    kind = "Aspect",
    javadoc = "Identity aspect for aspects (types representing values holding aspect implementations).")
@compilerxyz.annotations.BuiltinTypeId(7201L)
public interface AspectAspect extends Aspect {
  Unique aspectAspectGetUnique(InterfaceData value);

  InterfaceData aspectAspectGetIfaceData(InterfaceData value);
}
