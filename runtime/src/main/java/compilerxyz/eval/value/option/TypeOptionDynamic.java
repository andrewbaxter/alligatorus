package compilerxyz.eval.value.option;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.Builtin;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.function.Function;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectStackValueDynamic;

@Builtin(uniqueId = 6008L)
public record TypeOptionDynamic(Type elementType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "option(" + elementType.typeDesc() + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeOptionDynamic.class, "option_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_OPTION_DYNAMIC_IDENTITY)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_OPTION_DYNAMIC_IDENTITY, TypeOptionDynamic.class) instanceof Opt.Some<TypeOptionDynamic> oSome
        && elementType.typeIsEqual(xx, oSome.value().elementType);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return typeIsEqual(xx, sourceType);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.OPTION_DYNAMIC;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          return EResult.err(
              loc,
              "TypeOptionDynamic::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port"
                  + " deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeOptionDynamic::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact"
                  + " (eval/artifact.rs) port deferred");
        }
      };

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
            new ConstKey.Str("some"),
            new FieldSpec.Factory(new TypeOptionVariantConstructorDynamic(elementType, true))),
        new Pair<>(
            new ConstKey.Str("none"),
            new FieldSpec.Factory(new TypeOptionVariantConstructorDynamic(elementType, false))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeOptionDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (ck instanceof ConstKey.Str s && s.value().equals("value")) {
      var selfDyn = self.code().dynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeOptionDynamic::aspectFieldField(value): self has no runtime code");
      }
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(
              loc,
              compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                  compilerxyz.eval.dynamic.JavaInternalName.fromClass(Opt.Some.class))));
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.callMethod(
              loc,
              compilerxyz.eval.dynamic.JavaInternalName.fromClass(Opt.Some.class),
              "value",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(xx, elementType))));
    }
    if (ck instanceof ConstKey.Str s2 && s2.value().equals("is_some")) {
      var selfDyn = self.code().dynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeOptionDynamic::aspectFieldField(is_some): self has no runtime code");
      }
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          compilerxyz.eval.dynamic.JavaBytecodeUtils.instanceOf(
              loc, compilerxyz.eval.dynamic.JavaInternalName.fromClass(Opt.Some.class)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(xx, new compilerxyz.eval.value.bool_.TypeBoolDynamic()))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on option");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(
              new CodeValue(Code.none(loc), EvalOutcome.ofValue(compilerxyz.eval.value.object_.ValueObjectConst.ofType(f.type()))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (key instanceof ConstKey.Str s && s.value().equals("is_some")) {
      return Opt.some(new compilerxyz.eval.value.bool_.TypeBoolDynamic());
    }
    var spec = fieldSpecs().getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldType());
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeOptionDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
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
