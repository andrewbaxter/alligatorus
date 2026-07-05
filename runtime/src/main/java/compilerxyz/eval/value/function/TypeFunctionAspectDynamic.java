package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeFunctionAspectDynamic()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_FUNCTION_ASPECT_DYNAMIC_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "fn-aspect";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionAspectDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    return EResult.err(
        loc,
        "TypeFunctionAspectDynamic::call: needs ExprContext externals + AspectAspect +"
            + " TypePullAspect/TypeOptionDynamic + ASPECT_LOOKUP_FN_SCOPE_ID emission (deferred)");
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
