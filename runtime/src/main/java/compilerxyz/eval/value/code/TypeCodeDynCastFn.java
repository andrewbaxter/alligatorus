package compilerxyz.eval.value.code;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeCodeDynCastFn() implements Type, AspectFunction, ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_CODE_DYN_CAST_FN_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "code_dyn.cast";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.CodeDynCastFn();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(
          loc, "TypeCodeDynCastFn::call: argument has no compile-time value");
    }
    Type targetType;
    if (argSome.value() instanceof ValueAnyConst wrapped) {
      targetType = wrapped.inner();
    } else {

      targetType = new TypeCodeDynHandleDynamic();
    }
    return EResult.ok(
        new CodeValue(
            function.code(),
            Opt.some(ValueDynamic.stack(targetType))));
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
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
