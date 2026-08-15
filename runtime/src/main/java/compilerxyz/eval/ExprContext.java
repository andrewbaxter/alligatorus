package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;

import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.gen.Target;
import compilerxyz.eval.value.object_.ObjectAspectSource;
import compilerxyz.eval.value.object_.ObjectDataField;
import compilerxyz.eval.value.object_.ObjectDropKind;
import compilerxyz.eval.value.object_.ObjectMethod;
import compilerxyz.eval.value.object_.TypeObjectCastFromRule;
import compilerxyz.eval.value.object_.TypeObjectCastRule;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
@BuiltinFull(uniqueId = 6001L)
public final class ExprContext {
  public final Opt<Type> currentExceptionType;
  public final ImportStateId importState;
  public final Scope scope;
  public final Target target;
  public final EvalContext vx;

  public ExprContext(
      Scope scope,
      Target target,
      EvalContext vx,
      ImportStateId importState,
      Opt<Type> currentExceptionType) {
    this.scope = scope;
    this.target = target;
    this.vx = vx;
    this.importState = importState;
    this.currentExceptionType = currentExceptionType;
  }

}
