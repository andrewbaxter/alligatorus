package compilerxyz.eval.value.list;

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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
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
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeListNewDynamic(Type elementType)
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_LIST_NEW_DYNAMIC_TYPE_ID, TList.of(elementType.typeId())); }


  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ").new";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ListNewDynamic();
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
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "TypeListNewDynamic::call: argument has no compile-time value");
    }
    var argType = argSome.value().valueTypeOf();
    var dynamicElements = new TList<CodeDynamic>();
    var argTupleOpt = AspectTuple.getMaybe(xx, argType);
    if (argTupleOpt instanceof Opt.Some<AspectTuple> argTupleSome) {
      var elements = argTupleSome.value().aspectTupleTakeElements(xx, loc, argSome.value());
      for (var elemRes : elements) {
        if (elemRes.hasErrors()) return elemRes.reerr();
        var elemCv = elemRes.requireValue("takeElements");
        var convertedRes = Eval.typecheckConvert(xx, loc, elementType, elemCv);
        if (convertedRes.hasErrors()) return convertedRes.reerr();
        var consumeRes = AspectConsumeDynamic.get(xx, loc, elementType);
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
      var consumeRes = AspectConsumeDynamic.get(xx, loc, elementType);
      if (consumeRes.hasErrors()) return consumeRes.reerr();
      var dynRes =
          consumeRes
              .requireValue("AspectConsumeDynamic.get")
              .aspectConsumeDynamicToDynamic(xx, loc, convertedRes.requireValue("typecheckConvert"));
      if (dynRes.hasErrors()) return dynRes.reerr();
      dynamicElements.add(dynRes.requireValue("toDynamic"));
    }
    Type instanceType = new TypeListDynamic(elementType);
    var retStackRes = AspectStackValueDynamic.get(xx, loc, instanceType);
    if (retStackRes.hasErrors()) {
      return retStackRes.reerr();
    }
    var tlistIntname = JavaInternalName.of("compilerxyz/util/TList");
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
        retStackRes.requireValue("AspectStackValueDynamic.get").aspectStackValueDynamicToValue(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public Opt<Type> exceptionType() {
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
    return EResult.ok(this);
  }
}
