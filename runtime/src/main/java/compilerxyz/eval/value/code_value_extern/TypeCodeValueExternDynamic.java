package compilerxyz.eval.value.code_value_extern;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

@compilerxyz.annotations.BuiltinTypeId(7020L)
@compilerxyz.annotations.BuiltinIdentity(name = "code_value_extern_type_identity", value = 6002L)
public record TypeCodeValueExternDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return GenTypeIds.TYPE_CODE_VALUE_EXTERN_DYNAMIC_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.of("compilerxyz/eval/CodeValue"));
  }

  @Override
  public String typeDesc() {
    return "CodeValueExtern";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeCodeValueExternDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_CODE_VALUE_EXTERN_DYNAMIC_TYPE_ID, "code_value_extern_dynamic");

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeCodeValueExternDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new compilerxyz.eval.Erb(loc);
    if (!(key.value() instanceof Opt.Some<compilerxyz.gen.Value> keySome)) {
      return EResult.err(loc, "TypeCodeValueExternDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(compilerxyz.eval.Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on CodeValueExtern");
    }
    if (!"code".equals(keyStr.value())) {
      return EResult.err(loc, "Field '" + keyStr.value() + "' not found on CodeValueExtern");
    }
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeCodeValueExternDynamic::field: self has no dynamic code");
    }
    var codeType = compilerxyz.eval.Code.dynamicType();
    var codeValueInternal =
        compilerxyz.eval.dynamic.JavaInternalName.of("compilerxyz/eval/CodeValue");
    var codeDesc =
        compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
            compilerxyz.eval.dynamic.JavaInternalName.of("compilerxyz/eval/Code"));
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(
            loc,
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(codeValueInternal)));
    seq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
            loc,
            codeValueInternal,
            "code",
            compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(codeDesc, TList.of())));
    var resultDyn = new CodeDynamic(loc, seq);
    return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, resultDyn, codeType));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.of(new ConstKey.Str("code"));
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    return switch (s.value()) {
      case "code" -> Opt.some(compilerxyz.eval.Code.dynamicType());

      default -> Opt.none();
    };
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeCodeValueExternDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    if (sourceType.typeAspect(xx, GenIdentities.CODE_VALUE_EXTERN_TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(ValueDynamic.stack(xx, this))));
    }
    return EResult.err(loc, "Cannot convert to CodeValueExtern");
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
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
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
