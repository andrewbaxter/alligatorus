package compilerxyz.eval.value.panic;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypePanicDynamic()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_PANIC_DYNAMIC_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "panic";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.PanicDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID) || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    Type stringType = new TypeStringDynamic();
    var erb = new Erb(loc);
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, stringType, argument));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var toDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, stringType));
    if (toDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCodeOpt = erb.mustEr(toDynamicOpt.get().aspectConsumeDynamicToDynamic(xx, loc, converted));
    if (msgCodeOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCode = msgCodeOpt.get();
    var args = new TList<CodeDynamic>();
    args.add(msgCode);
    var panicType =
        new compilerxyz.eval.value.function.TypeFunctionDynamic(
            stringType,
            stringType,
            compilerxyz.eval.dynamic.JavaInternalName.fromClass(
                compilerxyz.eval.dynamic.DynRuntime.class),
            "panic",
            compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                TList.of(
                    compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(ExprContext.class),
                    compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class))),
            true);
    var throwCode = panicType.emitInvocation(loc, xx.vx, args);
    return EResult.ok(new CodeValue(Code.of(loc, throwCode), Opt.none()));
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
    return EResult.ok(this);
  }
}
