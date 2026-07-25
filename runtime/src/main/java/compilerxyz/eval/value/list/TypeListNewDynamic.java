package compilerxyz.eval.value.list;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinIdentity;
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

@BuiltinIdentity(uniqueId = 6119L)
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

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeListNewDynamic.class, "list_new_dynamic");

                public Type valueType() {
    return META;
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
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "TypeListNewDynamic::call: argument has no compile-time value");
    }
    var argType = argSome.value().valueType();
    var dynamicElements = new TList<CodeDynamic>();
    var argTupleOpt = GenAspectTuple.getMaybe(xx, argType);
    if (argTupleOpt instanceof Opt.Some<AspectTuple> argTupleSome) {
      var elements = argTupleSome.value().aspectTupleTakeElements(xx, loc, argSome.value());
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
    Type instanceType = new TypeListDynamic(compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, elementType));
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, instanceType);
    if (retStackRes.hasErrors()) {
      return retStackRes.reerr();
    }
    var tlistIntname = JavaInternalName.fromClass(TList.class);
    var descObject = JavaDataDescriptor.fromClass(Object.class);
    var addDesc =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of(descObject));
    var seq = new JavaBytecodeSequence();
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
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_LIST_NEW_DYNAMIC_IDENTITY, TypeListNewDynamic.class) instanceof Opt.Some<TypeListNewDynamic> oSome
        && elementType.typeIsEqual(xx, oSome.value().elementType);
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
