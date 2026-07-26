package compilerxyz.eval.value.zbase32;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.builtin.Zbase32;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.byte_.TypeByteConst;
import compilerxyz.eval.value.byte_.TypeByteDynamicBox;
import compilerxyz.eval.value.list.TypeListConst;
import compilerxyz.eval.value.list.TypeListDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.GenAspectStringDynamic;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

@BuiltinPath("zbase32.decode")
@BuiltinIdentity(uniqueId = 6153L)
public record TypeZbase32Decode()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public String typeDesc() {
    return "zbase32.decode";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META =
      compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeZbase32Decode.class, "zbase32_decode");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_ZBASE32_DECODE_IDENTITY)) {
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
      return EResult.err(loc, "zbase32.decode: argument must have a value");
    }
    var argValue = argSome.value();
    var argType = argValue.valueType();
    if (GenAspectStringConst.getMaybe(xx, argType) instanceof Opt.Some<compilerxyz.gen.AspectStringConst> strConst) {
      var s = strConst.value().aspectStringConstGet(argValue);
      byte[] bytes;
      try {
        bytes = Zbase32.decode(s);
      } catch (IllegalArgumentException e) {
        return EResult.err(loc, "zbase32.decode: " + e.getMessage());
      }
      var elements = new TList<Value>(bytes.length);
      for (var b : bytes) {
        elements.add(new ValueObjectConst(new TypeByteConst(), b));
      }
      var listType = new TypeListConst(new TypeByteDynamicBox());
      return EResult.ok(
          new CodeValue(
              Code.concat(loc, self.code(), argument.code()),
              EvalOutcome.ofValue(new ValueObjectConst(listType, elements))));
    }
    if (GenAspectStringDynamic.getMaybe(xx, argType).optIsSome()) {
      var consumeRes = GenAspectConsumeDynamic.get(xx, loc, argType);
      if (consumeRes.hasErrors()) return consumeRes.reerr();
      var argDynRes =
          consumeRes
              .requireValue("zbase32.decode AspectConsumeDynamic")
              .aspectConsumeDynamicToDynamic(xx, loc, argument);
      if (argDynRes.hasErrors()) return argDynRes.reerr();
      var seq = new JavaBytecodeSequence();
      seq.add(argDynRes.requireValue("zbase32.decode arg dynamic"));
      seq.add(
          JavaBytecodeUtils.callStaticMethod(
              loc,
              JavaInternalName.fromClass(Zbase32.class),
              "decodeToList",
              JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class)),
                  TList.of(JavaDataDescriptor.fromClass(String.class)))));
      var listType = new TypeListDynamic(new TypeByteDynamicBox());
      return EResult.ok(
          new CodeValue(
              Code.of(new CodeDynamic(loc, seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(listType, 1))));
    }
    return EResult.err(loc, "zbase32.decode: argument must be a string, got " + argType.typeDesc());
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
    return EResult.ok(ValueObjectConst.ofType(this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_ZBASE32_DECODE_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
