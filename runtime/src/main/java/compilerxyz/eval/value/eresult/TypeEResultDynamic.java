package compilerxyz.eval.value.eresult;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.annotations.BuiltinFull;

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
import compilerxyz.gen.TargetBindSub;
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
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.MatchDynamic;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.eval.value.list.TypeListDynamic;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectUnion.Variant;
import compilerxyz.gen.AspectUnion;

@BuiltinFull(uniqueId = 6017L)
public record TypeEResultDynamic(Type elementType)
    implements
        Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectStackValueDynamic,
        AspectUnion,
        MatchDynamic,
        ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(EResult.class));
  }

  @Override
  public String typeDesc() {
    return "eresult(" + elementType.typeDesc() + ")";
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
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(compilerxyz.gen.GenAspectUnion.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  private static Type errsPayloadType() {
    return new TypeListDynamic(compilerxyz.gen.GenExprErr.dynamicType());
  }

  @Override
  public TList<Variant> aspectUnionVariants() {
    var out = new TList<Variant>();
    out.add(new Variant(new ConstKey.Str("ok"), "ok", elementType));
    out.add(new Variant(new ConstKey.Str("errs"), "errs", errsPayloadType()));
    return out;
  }

  @Override
  public EResult<CodeDynamic> matchDynamicTest(ExprContext xx, QExprId loc, int variantIndex) {
    // Stack: [eresult] -> [boolean]. Variant 0 = ok (no errors), 1 = errs.
    var seq = new JavaBytecodeSequence();
    seq.add(
        compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
            loc,
            JavaInternalName.fromClass(EResult.class),
            "hasErrors",
            compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL)));
    if (variantIndex == 0) {
      // ok = !hasErrors
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.inst(
              loc, org.objectweb.asm.Opcodes.ICONST_1));
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.IXOR));
    }
    return EResult.ok(new CodeDynamic(loc, seq));
  }

  @Override
  public EResult<CodeDynamic> matchDynamicExtract(
      ExprContext xx, QExprId loc, int variantIndex, Type variantType) {
    // Stack: [eresult] -> [payload].
    var seq = new JavaBytecodeSequence();
    if (variantIndex == 0) {
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.literalString(
              new Some(loc), "eresult match ok"));
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
      var elemDesc =
          elemConsumeRes
              .requireValue("TypeEResultDynamic::matchExtract ok consume")
              .aspectConsumeDynamicJvmClass(xx);
      if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
        seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(loc, elemDesc));
      }
    } else {
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
              loc,
              JavaInternalName.fromClass(EResult.class),
              "errorsOrNull",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromInternalName(
                      JavaInternalName.fromClass(compilerxyz.util.Opt.class)))));
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.callInterfaceMethod(
              loc,
              JavaInternalName.fromClass(compilerxyz.util.Opt.class),
              "optUnwrap",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromClass(Object.class))));
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(
              loc,
              JavaDataDescriptor.fromInternalName(
                  JavaInternalName.fromClass(compilerxyz.util.TList.class))));
    }
    return EResult.ok(new CodeDynamic(loc, seq));
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
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  private sealed interface FieldSpec {
    Type fieldSpecFieldType();

    record Factory(Type type) implements FieldSpec {
      @Override
      public Type fieldSpecFieldType() {
        return type;
      }
    }
  }

  private TMap<ConstKey, FieldSpec> fieldSpecs() {
    return TMap.ofEntries(
        new Pair<>(
            new ConstKey.Str("ok"),
            new FieldSpec.Factory(
                new TypeEResultVariantConstructorDynamic(
                    elementType, TypeEResultVariantConstructorDynamic.Kind.OK))),
        new Pair<>(
            new ConstKey.Str("err"),
            new FieldSpec.Factory(
                new TypeEResultVariantConstructorDynamic(
                    elementType, TypeEResultVariantConstructorDynamic.Kind.ERR))),
        new Pair<>(
            new ConstKey.Str("errs"),
            new FieldSpec.Factory(
                new TypeEResultVariantConstructorDynamic(
                    elementType, TypeEResultVariantConstructorDynamic.Kind.ERRS))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, "TypeEResultDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    var ck = ckOpt.optUnwrap();
    var ckStr = switch (ck) {
      case ConstKey.Str s -> Opt.some(s);
      case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
    };
    if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("has_errors")) {
      var selfDyn = self.code().codeDynamic();
      if (selfDyn == null) {
        return EResult.err(
            loc, "TypeEResultDynamic::aspectFieldField(has_errors): self has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
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
              Code.of(loc, new CodeDynamic(new Some(loc), seq)),
              EvalOutcome.ofValue(
                  compilerxyz.eval.value.ValueDynamic.stack(new TypeBoolDynamic(), 1))));
    }
    if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
      var selfDyn = self.code().codeDynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeEResultDynamic::aspectFieldField(value): self has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.literalString(
              new Some(loc), "eresult.value"));
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
          Code.of(loc, new CodeDynamic(new Some(loc), seq)),
          EvalOutcome.ofValue(compilerxyz.eval.value.ValueDynamic.stack(elementType, 1))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on eresult");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs().getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldSpecFieldType());
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }
  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_E_RESULT_DYNAMIC_IDENTITY, TypeEResultDynamic.class);
    return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return typeIsEqual(xx, sourceType);
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
