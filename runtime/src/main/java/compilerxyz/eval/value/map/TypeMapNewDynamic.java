package compilerxyz.eval.value.map;

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
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectTuple;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeMapNewDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectFunction,
        AspectInitialBind,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ").new";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.MapNewDynamic();
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
    var instanceType = new TypeMapDynamic(keyType, valueType);
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "TypeMapNewDynamic::call: argument has no compile-time value");
    }
    var argType = argSome.value().valueTypeOf();
    var argTupleOpt = AspectTuple.getMaybe(xx, argType);
    if (argTupleOpt instanceof Opt.Some<AspectTuple> argTupleSome
        && argTupleSome.value().elementTypes().isEmpty()) {
      var retStackRes = AspectStackValueDynamic.get(xx, loc, instanceType);
      if (retStackRes.hasErrors()) {
        return retStackRes.reerr();
      }

      var torderedMapIntname = JavaInternalName.of("compilerxyz/util/TOrderedMap");
      var seqElements =
          JavaBytecodeUtils.instantiate(
              -1, torderedMapIntname, JavaMethodDescriptor.VOID_NOARGS, new TList<>());
      var newMap = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(seqElements));
      return EResult.ok(retStackRes.valueOrNull().toValue(loc, newMap));
    }
    return Eval.typecheckConvert(xx, loc, instanceType, argument);
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
