package compilerxyz.eval.value.number;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeNumberToStringDynamic()
    implements Type, AspectConsumeDynamic, AspectFunction, AspectInitialBind, ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeNumberToStringDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_NUMBER_TO_STRING_DYNAMIC_TYPE_ID;
  }

  @Override
  public String typeDesc() {
    return "number.to_string";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.NumberToStringDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectConsumeDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeNumberToStringDynamic::call: self has no dynamic code");
    }

    var retType = new TypeStringDynamic();
    var retStackRes = AspectStackValueDynamic.get(xx, loc, retType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var helperName = JavaInternalName.fromClass(NumberFormat.class);
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
    return EResult.ok(this);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
