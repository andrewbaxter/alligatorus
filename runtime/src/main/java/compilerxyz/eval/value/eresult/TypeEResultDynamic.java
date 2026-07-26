package compilerxyz.eval.value.eresult;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.annotations.Builtin;

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
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenTrivialIdentityAspects;

@Builtin(uniqueId = 6017L)
public record TypeEResultDynamic(Type elementType)
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        compilerxyz.gen.AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
  }

  @Override
  public String typeDesc() {
    return "eresult(" + elementType.typeDesc() + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeEResultDynamic.class, "eresult_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
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
    if (ck instanceof ConstKey.Str hs && hs.value().equals("has_errors")) {
      var selfDyn = self.code().dynamic();
      if (selfDyn == null) {
        return EResult.err(
            loc, "TypeEResultDynamic::aspectFieldField(has_errors): self has no runtime code");
      }
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
              loc,
              JavaInternalName.fromClass(EResult.class),
              "hasErrors",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.DESC_BOOL)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
              EvalOutcome.ofValue(
                  compilerxyz.eval.value.ValueDynamic.stack(new compilerxyz.eval.value.bool_.TypeBoolDynamic(), 1))));
    }
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
              JavaInternalName.fromClass(EResult.class),
              "requireValue",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT,
                  JavaDataDescriptor.fromClass(String.class))));
      var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
      if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
      var elemDesc = elemConsumeRes.requireValue("TypeEResultDynamic::field value AspectConsumeDynamic")
          .aspectConsumeDynamicJvmClass(xx);
      if (elemDesc.value().startsWith("L")
          && !elemDesc.value().equals("Ljava/lang/Object;")) {
        seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(loc, elemDesc));
      }
      return EResult.ok(new CodeValue(
          Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
          EvalOutcome.ofValue(compilerxyz.eval.value.ValueDynamic.stack(elementType, 1))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on eresult");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(compilerxyz.eval.value.object_.ValueObjectConst.ofType(f.type()))));
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
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
  }
  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY, TypeEResultDynamic.class) instanceof Opt.Some<TypeEResultDynamic> oSome
        && elementType.typeIsEqual(xx, oSome.value().elementType);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return typeIsEqual(xx, sourceType);
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.binding.BindingDynamicBinding(dynamicKey, this);
  }
}
