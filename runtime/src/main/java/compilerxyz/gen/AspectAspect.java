package compilerxyz.gen;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.source_interface.InterfaceData;

@BuiltinFull(uniqueId = 1029L, identityId = 4029L)
public interface AspectAspect extends Aspect {
    Unique aspectAspectGetUnique(InterfaceData value);

    InterfaceData aspectAspectGetIfaceData(InterfaceData value);
}
