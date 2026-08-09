package compilerxyz.eval.value.panic;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinPath;

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
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynPanic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;

@BuiltinPath("panic")
@BuiltinLight(uniqueId = 6133L)
public record TypePanicDynamic()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {


  @Override
  public String typeDesc() {
    return "panic";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_PANIC_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
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
    var toDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, stringType));
    if (toDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCodeOpt = erb.mustEr(toDynamicOpt.get().aspectConsumeDynamicToDynamic(xx, loc, converted));
    if (msgCodeOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var msgCode = msgCodeOpt.get();
    var panicInternal = JavaInternalName.fromClass(DynPanic.class);
    var ctorDesc =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.DESC_VOID,
            TList.of(JavaDataDescriptor.fromClass(String.class)));
    var seq = new JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.newInstance(loc, panicInternal));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(msgCode);
    seq.add(JavaBytecodeUtils.invokeSpecial(loc, panicInternal, "<init>", ctorDesc));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    var throwCode = new CodeDynamic(loc, seq);
    return EResult.ok(new CodeValue(Code.of(loc, throwCode), EvalOutcome.CONTROL_ENDED));
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_PANIC_DYNAMIC_IDENTITY).optIsSome();
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
