package compilerxyz.eval.value.list;

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
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

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
      var elements = argTupleSome.value().takeElements(argSome.value());
      for (var elemRes : elements) {
        if (elemRes.hasErrors()) {
          return elemRes.reerr();
        }
        var elemCv = elemRes.valueOrNull();
        if (elemCv == null) {
          return EResult.err(loc, "TypeListNewDynamic::call: tuple element failed to evaluate");
        }
        var convertedRes = Eval.typecheckConvert(xx, loc, elementType, elemCv);
        if (convertedRes.hasErrors()) {
          return convertedRes.reerr();
        }
        var consumeRes = AspectConsumeDynamic.get(xx, loc, elementType);
        if (consumeRes.hasErrors()) {
          return consumeRes.reerr();
        }
        var dynRes = consumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
        if (dynRes.hasErrors()) {
          return dynRes.reerr();
        }
        dynamicElements.add(dynRes.valueOrNull());
      }
    } else {
      var convertedRes = Eval.typecheckConvert(xx, loc, elementType, argument);
      if (convertedRes.hasErrors()) {
        return convertedRes.reerr();
      }
      var consumeRes = AspectConsumeDynamic.get(xx, loc, elementType);
      if (consumeRes.hasErrors()) {
        return consumeRes.reerr();
      }
      var dynRes = consumeRes.valueOrNull().toDynamic(xx, loc, convertedRes.valueOrNull());
      if (dynRes.hasErrors()) {
        return dynRes.reerr();
      }
      dynamicElements.add(dynRes.valueOrNull());
    }
    Type instanceType = new TypeListDynamic(elementType);
    var retStackRes = AspectStackValueDynamic.get(xx, loc, instanceType);
    if (retStackRes.hasErrors()) {
      return retStackRes.reerr();
    }
    var tlistIntname = JavaInternalName.of("compilerxyz/util/TList");
    var addDesc =
        JavaMethodDescriptor.fromParts(JvmConstants.DESC_BOOL, JvmConstants.DESC_OBJECT);
    var seqElements = new TList<JavaBytecode.LoweredSequence.Element>();
    seqElements.addAll(
        JavaBytecodeUtils.instantiate(
            -1, tlistIntname, JavaMethodDescriptor.VOID_NOARGS, new TList<>()));
    for (var element : dynamicElements) {
      seqElements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.DUP)));
      seqElements.add(new JavaBytecode.LoweredSequence.Code(element));
      seqElements.add(
          new JavaBytecode.LoweredSequence.Bytecode(
              JavaBytecodeUtils.callMethod(-1, tlistIntname, "add", addDesc)));
      seqElements.add(new JavaBytecode.LoweredSequence.Bytecode(JavaBytecodeUtils.inst(Opcodes.POP)));
    }
    var newArrayCode = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(seqElements));
    return EResult.ok(retStackRes.valueOrNull().toValue(loc, newArrayCode));
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
