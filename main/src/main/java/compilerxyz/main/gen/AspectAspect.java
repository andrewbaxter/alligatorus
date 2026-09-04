package compilerxyz.main.gen;

import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 1029L, identityId = 4029L)
public interface AspectAspect extends Aspect {
    Unique aspectAspectGetUnique(InterfaceCell value);

    InterfaceCell aspectAspectGetIfaceCell(InterfaceCell value);
}
