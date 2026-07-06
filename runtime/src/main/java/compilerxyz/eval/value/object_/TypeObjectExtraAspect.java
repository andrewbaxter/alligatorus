package compilerxyz.eval.value.object_;

import compilerxyz.eval.ExprContext;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@FunctionalInterface
public interface TypeObjectExtraAspect {
  Opt<Aspect> typeObjectExtraAspect(ExprContext xx, Unique key);
}
