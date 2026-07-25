package compilerxyz.gen;


import compilerxyz.annotations.Builtin;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.value.source_interface.InterfaceData;

@Builtin(uniqueId = 1029L)
public interface AspectAspect extends Aspect {
  Unique aspectAspectGetUnique(InterfaceData value);

  InterfaceData aspectAspectGetIfaceData(InterfaceData value);
}
