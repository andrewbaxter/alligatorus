package compilerxyz.eval.value.constkey;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectStackValueDynamic;

@BuiltinFull(uniqueId = 6015L)
@BuiltinPath("types.constkey")
public record TypeConstkeyDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {


  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromClass(compilerxyz.eval.ConstKey.class);
  }

  @Override
  public String typeDesc() {
    return "constkey";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      return EResult.err(loc, "TypeConstkeyDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceValue = cv.value().optUnwrap();
    var sourceType = sourceValue.valueType();
    var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
    if (stringConstOpt.optIsNone()) {
      return EResult.err(loc, "Cannot convert " + sourceType.typeDesc() + " to constkey");
    }
    var s = stringConstOpt.optUnwrap().aspectStringConstGet(sourceValue);
    var strInternal =
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(ConstKey.Str.class);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            compilerxyz.util.TList.of(
                JavaDataDescriptor.fromClass(String.class)));
    var seq = new JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.newInstance(loc, strInternal));
    seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
    seq.add(JavaBytecodeUtils.literalString(loc, s));
    seq.add(JavaBytecodeUtils.invokeSpecial(loc, strInternal, "<init>", ctorDesc));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(new Some(loc), seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(new TypeConstkeyDynamic(), 1))));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeConstkeyDynamic());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeConstkeyDynamic()));
  }

  @Override
  public compilerxyz.eval.EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY).optIsSome();
  }
  @Override
  public compilerxyz.eval.EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_CONSTKEY_DYNAMIC_IDENTITY).optIsSome()) {
      return EResult.ok(cv);
    }
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
