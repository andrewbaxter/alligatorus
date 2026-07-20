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
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectConsumeDynamic;
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
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;

@compilerxyz.annotations.BuiltinTypeId(7114L)
public record TypeListJoinDynamic(Type elementType)
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
    return EResult.err(loc, "TypeListJoinDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public TypeId hasTypeIdTypeId() {
    return new TypeId.Composite(
        GenTypeIds.TYPE_LIST_JOIN_DYNAMIC_TYPE_ID, TList.of(elementType.hasTypeIdTypeId()));
  }

  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ").join";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeListJoinDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_LIST_JOIN_DYNAMIC_TYPE_ID, "list_join_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
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
    var sepType = new TypeStringDynamic();
    var convertedRes = Eval.typecheckConvert(xx, loc, sepType, argument);
    if (convertedRes.hasErrors()) return convertedRes.reerr();
    var converted = convertedRes.requireValue("typecheckConvert");
    var consumeRes = GenAspectConsumeDynamic.get(xx, loc, sepType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var argDynRes =
        consumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (argDynRes.hasErrors()) return argDynRes.reerr();
    var argDyn = argDynRes.requireValue("toDynamic");

    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeListJoinDynamic::call: self has no dynamic code");
    }

    var retStackRes = GenAspectStackValueDynamic.get(xx, loc, sepType);
    if (retStackRes.hasErrors()) return retStackRes.reerr();

    var stringName = JavaInternalName.fromClass(String.class);
    var charSeqDesc =
        JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(CharSequence.class));
    var iterableDesc =
        JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Iterable.class));
    var stringDesc = JavaDataDescriptor.fromClass(String.class);
    var joinDesc = JavaMethodDescriptor.fromParts(stringDesc, TList.of(charSeqDesc, iterableDesc));

    var seq = new JavaBytecodeSequence();
    seq.add(argDyn);
    seq.add(selfDyn);
    seq.add(JavaBytecodeUtils.callStaticMethod(loc, stringName, "join", joinDesc));
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
