package compilerxyz.eval.value.list;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectTuple;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.DynUtil;

@BuiltinLight(uniqueId = 6119L)
public record TypeListNewDynamic(Type elementType)
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ").new";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_LIST_NEW_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (argument.value().optIsNone()) {
      return EResult.err(loc, "TypeListNewDynamic::call: argument has no compile-time value");
    }
    var argValue = argument.value().optUnwrap();
    var argType = argValue.valueType();
    var dynamicElements = new TList<CodeDynamic>();
    var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
    if (argTupleOpt.optIsSome()) {
      var elements = argTupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, argValue);
      for (var elemRes : elements) {
        if (elemRes.hasErrors()) return elemRes.reerr();
        var elemCv = elemRes.requireValue("takeElements");
        var convertedRes = Eval.typecheckConvert(xx, loc, elementType, elemCv);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
        if (consumeRes.hasErrors()) return consumeRes.reerr();
        var dynRes =
            consumeRes
                .requireValue("AspectConsumeDynamic.get")
                .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
        if (dynRes.hasErrors()) return dynRes.reerr();
        dynamicElements.add(dynRes.requireValue("toDynamic"));
      }
    } else {
      var convertedRes = Eval.typecheckConvert(xx, loc, elementType, argument);
      if (convertedRes.hasErrors()) return convertedRes.reerr();
      var consumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
      if (consumeRes.hasErrors()) return consumeRes.reerr();
      var dynRes =
          consumeRes
              .requireValue("AspectConsumeDynamic.get")
              .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
      if (dynRes.hasErrors()) return dynRes.reerr();
      dynamicElements.add(dynRes.requireValue("toDynamic"));
    }
    Type instanceType = new TypeListDynamic(DynUtil.substituteBoxed(xx, elementType));
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
    if (retStackRes.hasErrors()) {
      return retStackRes.reerr();
    }
    var tlistIntname = JavaInternalName.fromClass(TList.class);
    var descObject = JavaDataDescriptor.fromClass(Object.class);
    var addDesc =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));
    var seq = new JavaBytecodeSequence();
    var selfDyn = self.code().codeDynamic();
    if (selfDyn != null) seq.add(selfDyn);
    seq.add(
        JavaBytecodeUtils.instantiate(
            loc,
            tlistIntname,
            JavaMethodDescriptor.fromConstructorParts(TList.of()),
            new CodeDynamic(loc, new JavaBytecodeSequence())));
    for (var element : dynamicElements) {
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(element);
      seq.add(JavaBytecodeUtils.callMethod(loc, tlistIntname, "add", addDesc));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    }
    return EResult.ok(
        retStackRes.requireValue("AspectStackValueDynamic.get").aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_LIST_NEW_DYNAMIC_IDENTITY, TypeListNewDynamic.class);
    return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
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
