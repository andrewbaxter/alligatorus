package compilerxyz.eval.value.throw_;

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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeThrowDynamic()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_THROW_DYNAMIC_TYPE_ID; }


  @Override
  public String typeDesc() {
    return "throw";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.ThrowDynamic();
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
    var erb = new Erb(loc);
    var excType = xx.currentExceptionType;
    if (excType == null) {
      return EResult.err(
          loc,
          "throw not allowed: enclosing function did not declare an exception type");
    }
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, excType, argument));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var packedOpt =
        erb.mustEr(
            compilerxyz.eval.dynamic.DynUtil.destructurePackedObject(xx, loc, excType, converted));
    if (packedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var args = new TList<CodeDynamic>();
    args.add(packedOpt.get());
    var throwType =
        new compilerxyz.eval.value.function.TypeFunctionDynamic(
            excType,
            excType,
            JavaInternalName.fromClass(compilerxyz.eval.dynamic.DynRuntime.class),
            "throwValue",
            JavaMethodDescriptor.fromParts(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
                TList.of(
                    compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(ExprContext.class),
                    compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class))),
            true);
    var throwCode = throwType.emitInvocation(loc, xx.vx, args);
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
