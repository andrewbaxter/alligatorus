package compilerxyz.eval.value.option;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;

@BuiltinPath("types.option")
@BuiltinIdentity(uniqueId = 6131L)
public record TypeOptionConstructorConst()
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "option constructor";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeOptionConstructorConst.class, "option_constructor_const");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_OPTION_CONSTRUCTOR_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(
          loc, "TypeOptionConstructorConst::call: argument must have a compile-time value");
    }
    var argValue = argSome.value();
    var argMeta = argValue.valueType();
    var aspectTypeRes = GenAspectType.get(xx, loc, argMeta);
    if (aspectTypeRes.hasErrors()) return aspectTypeRes.reerr();
    var elementType =
        aspectTypeRes
            .requireValue("TypeOptionConstructorConst::call AspectType")
            .aspectTypeType(argValue);
    Type optType = new TypeOptionDynamic(compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, elementType));
    return EResult.ok(
        new CodeValue(Code.none(loc), EvalOutcome.ofValue(compilerxyz.eval.value.object_.ValueObjectConst.ofType(optType))));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_OPTION_CONSTRUCTOR_CONST_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
