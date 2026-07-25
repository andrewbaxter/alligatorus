package compilerxyz.eval.value.throw_;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
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
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynException;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import org.objectweb.asm.Opcodes;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;

@BuiltinPath("throw")
@BuiltinIdentity(uniqueId = 6146L)
public record TypeThrowDynamic()
    implements Type, AspectFunction, AspectInitialBind, ExporterAny {


  @Override
  public String typeDesc() {
    return "throw";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeThrowDynamic.class, "throw_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_THROW_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId) || key.equals(GenAspectInitialBind.AspectId)) {
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
    var consumeRes = compilerxyz.gen.GenAspectConsumeDynamic.get(xx, loc, excType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var valueDynRes =
        consumeRes
            .requireValue("TypeThrowDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (valueDynRes.hasErrors()) return valueDynRes.reerr();
    var valueCode = valueDynRes.requireValue("TypeThrowDynamic::call toDynamic");
    var excInternal = JavaInternalName.fromClass(DynException.class);
    var ctorDesc =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.DESC_VOID,
            TList.of(JavaDataDescriptor.fromClass(Object.class)));
    var seq = new JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.newInstance(loc, excInternal));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(valueCode);
    seq.add(JavaBytecodeUtils.invokeSpecial(loc, excInternal, "<init>", ctorDesc));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.ATHROW));
    var throwCode = new CodeDynamic(loc, seq);
    return EResult.ok(new CodeValue(Code.of(loc, throwCode), EvalOutcome.CONTROL_ENDED));
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
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_THROW_DYNAMIC_IDENTITY).optIsSome();
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
