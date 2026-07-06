package compilerxyz.eval.value.function;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.TList;

public record TypeMethodDynamic(
    Type receiver,
    Type arg,
    Type ret,
    JavaInternalName klass,
    String methodName,
    JavaMethodDescriptor descriptor)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_METHOD_DYNAMIC_TYPE_ID, TList.of(receiver.typeId(), arg.typeId(), ret.typeId())); }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "dynamic method";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.FunctionDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var receiverDyn = self.code().dynamic();
    if (receiverDyn == null) {
      return EResult.err(loc, "TypeMethodDynamic::call: receiver has no dynamic code");
    }
    var convertedRes = Eval.typecheckConvert(xx, loc, arg, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("TypeMethodDynamic::call converted arg");
    var argsDynRes = destructureOrLower(xx, loc, arg, converted);
    if (argsDynRes.hasErrors()) return argsDynRes.reerr();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, ret);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var seq = new JavaBytecodeSequence();
    seq.add(receiverDyn);
    for (var argCode : argsDynRes.requireValue("TypeMethodDynamic::call args")) {
      seq.add(argCode);
    }
    seq.add(JavaBytecodeUtils.callMethod(loc, klass, methodName, descriptor));
    return EResult.ok(
        retStackRes
            .requireValue("TypeMethodDynamic::call ret stack")
            .toValue(loc, new CodeDynamic(loc, seq)));
  }

  private static EResult<TList<CodeDynamic>> destructureOrLower(
      ExprContext xx, QExprId loc, Type argType, CodeValue converted) {
    var tupleOpt = compilerxyz.gen.AspectTuple.getMaybe(xx, argType);
    if (tupleOpt instanceof Opt.Some<compilerxyz.gen.AspectTuple> tupleSome) {
      if (!(converted.value() instanceof Opt.Some<Value> some)) {
        return EResult.err(loc, "TypeMethodDynamic::destructure: arg has no compile-time value");
      }
      var elementTypes = tupleSome.value().elementTypes();
      var elemCvs = tupleSome.value().takeElements(xx, loc, some.value());
      if (elementTypes.size() != elemCvs.size()) {
        throw new IllegalStateException(
            "TypeMethodDynamic::destructure: elementTypes/takeElements size mismatch");
      }
      var out = new TList<CodeDynamic>(elementTypes.size());
      for (int i = 0; i < elementTypes.size(); i++) {
        var elemCvRes = elemCvs.get(i);
        if (elemCvRes.hasErrors()) return elemCvRes.reerr();
        var elemCv = elemCvRes.requireValue("TypeMethodDynamic::destructure element");
        var consumeRes = AspectConsumeDynamic.get(xx, loc, elementTypes.get(i));
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var consume = consumeRes.requireValue("TypeMethodDynamic::destructure consume");
        var elemDyn = consume.toDynamic(xx, loc, elemCv);
        if (elemDyn.hasErrors()) return elemDyn.reerr();
        var elemCode = elemDyn.requireValue("TypeMethodDynamic::destructure elem toDynamic");
        out.add(withCastIfNeeded(loc, elemCode, consume.consumeDynamicJvmClass()));
      }
      return EResult.ok(out);
    }
    var consumeRes = AspectConsumeDynamic.get(xx, loc, argType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var consume = consumeRes.requireValue("TypeMethodDynamic::call arg consume");
    var argDyn = consume.toDynamic(xx, loc, converted);
    if (argDyn.hasErrors()) return argDyn.reerr();
    var argCode = argDyn.requireValue("TypeMethodDynamic::call arg");
    return EResult.ok(TList.of(withCastIfNeeded(loc, argCode, consume.consumeDynamicJvmClass())));
  }

  private static CodeDynamic withCastIfNeeded(
      QExprId loc,
      CodeDynamic code,
      compilerxyz.eval.dynamic.JavaDataDescriptor targetDesc) {
    var v = targetDesc.value();
    if (!v.startsWith("L") || v.equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())) {
      return code;
    }
    var seq = new JavaBytecodeSequence();
    seq.add(code);
    seq.add(JavaBytecodeUtils.cast(loc, targetDesc));
    return new CodeDynamic(loc, seq);
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeMethodDynamic::toDynamic: CodeValue has no dynamic code");
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

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
