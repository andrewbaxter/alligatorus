package compilerxyz.eval.value.eresult;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;

public record TypeEResultConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_ERESULT_CONSTRUCTOR_CONST_TYPE_ID;
  }

  @Override
  public String typeDesc() {
    return "eresult constructor";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.EResultConstructorConst();
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
          loc, "TypeEResultConstructorConst::call: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueTypeOf();
    var aspectTypeRes = AspectType.get(xx, loc, argMeta);
    if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
    var elementType =
        aspectTypeRes
            .requireValue("TypeEResultConstructorConst::call AspectType")
            .type_(argValue);
    Type eresultType = new TypeEResultDynamic(elementType);
    return EResult.ok(new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(eresultType))));
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
