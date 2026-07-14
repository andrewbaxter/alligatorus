package compilerxyz.eval.value.eresult;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;

public record TypeEResultDynamic(Type elementType)
    implements
        Type,
        AspectConsumeDynamic,
        AspectField,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() {
    return new TypeId.Composite(
        GenTypeIds.TYPE_ERESULT_DYNAMIC_TYPE_ID, TList.of(elementType.hasTypeIdTypeId()));
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.of("compilerxyz/eval/EResult"));
  }

  @Override
  public String typeDesc() {
    return "eresult(" + elementType.typeDesc() + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeEResultDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_ERESULT_DYNAMIC_TYPE_ID, "eresult_dynamic");

  @Override
  public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  private sealed interface FieldSpec {
    Type fieldType();

    record Factory(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }
  }

  private TMap<ConstKey, FieldSpec> fieldSpecs() {
    return TMap.ofEntries(
        new Pair<>(
            new ConstKey.Str("ok"),
            new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(elementType, true))),
        new Pair<>(
            new ConstKey.Str("err"),
            new FieldSpec.Factory(new TypeEResultVariantConstructorDynamic(elementType, false))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeEResultDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (ck instanceof ConstKey.Str s && s.value().equals("value")) {
      var selfDyn = self.code().dynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeEResultDynamic::aspectFieldField(value): self has no runtime code");
      }
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.literalString(
              new compilerxyz.eval.dynamic.CodeLoc.Some(loc), "eresult.value"));
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
              loc,
              JavaInternalName.of("compilerxyz/eval/EResult"),
              "requireValue",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT,
                  JavaDataDescriptor.fromClass(String.class))));
      var elemConsumeRes = AspectConsumeDynamic.get(xx, loc, elementType);
      if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
      var elemDesc = elemConsumeRes.requireValue("TypeEResultDynamic::field value AspectConsumeDynamic")
          .aspectConsumeDynamicJvmClass(xx);
      if (elemDesc.value().startsWith("L")
          && !elemDesc.value().equals("Ljava/lang/Object;")) {
        seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(loc, elemDesc));
      }
      return EResult.ok(new CodeValue(
          Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
          Opt.some(compilerxyz.eval.value.ValueDynamic.stack(xx, elementType))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on eresult");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(new CodeValue(Code.none(loc), Opt.some(new ValueObjectConst(compilerxyz.eval.value.object_.ObjectConstOfType.dynamicType(), f.type()))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs().getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldType());
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeEResultDynamic::toDynamic: dynamic type must carry dynamic code");
    }
    return EResult.ok(dyn);
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
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
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
