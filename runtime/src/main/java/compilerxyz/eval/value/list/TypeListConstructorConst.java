package compilerxyz.eval.value.list;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
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
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.gen.AspectPostDeserialize;

public record TypeListConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "list constructor";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ListConstructorConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(
          loc, "TypeListConstructorConst::call: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueTypeOf();
    var aspectTypeRes = AspectType.get(xx, loc, argMeta);
    if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
    var elementType =
        aspectTypeRes
            .requireValue("TypeListConstructorConst::call AspectType")
            .type_(argValue);
    elementType = compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, elementType);
    Type listType = new TypeListDynamic(elementType);
    return EResult.ok(
        new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(listType))));
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
