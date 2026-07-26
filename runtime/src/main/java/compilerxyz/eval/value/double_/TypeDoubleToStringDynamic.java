package compilerxyz.eval.value.double_;

import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.numeric.NumericFormat;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;

@BuiltinIdentity(uniqueId = 6178L)
public record TypeDoubleToStringDynamic()
    implements Type, AspectConsumeDynamic, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeDoubleToStringDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public String typeDesc() {
    return "double.to_string";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META =
      compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeDoubleToStringDynamic.class, "double_to_string_dynamic");

  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_DOUBLE_TO_STRING_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeDoubleToStringDynamic::call: self has no dynamic code");
    }
    var retType = new TypeStringDynamic();
    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var helperName = JavaInternalName.fromClass(NumericFormat.class);
    var stringDesc = JavaDataDescriptor.fromClass(String.class);
    var formatDesc =
        JavaMethodDescriptor.fromParts(stringDesc, TList.of(JavaDataDescriptor.DESC_DOUBLE));

    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, helperName, "format", formatDesc));
    return EResult.ok(
        retStackRes
            .requireValue("AspectStackValueDynamic.get")
            .aspectStackValueDynamicToValue(xx, loc, new CodeDynamic(loc, seq)));
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
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_DOUBLE_TO_STRING_DYNAMIC_IDENTITY).optIsSome();
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
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
