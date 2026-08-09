package compilerxyz.eval.value.option;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinFull;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.ExprErr;
import compilerxyz.safejson.JsonValues;
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
import compilerxyz.gen.TargetBindSub;
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

@BuiltinFull(uniqueId = 6008L)
public final class TypeOptionDynamic
    implements
        
        compilerxyz.gen.AspectSet,
        compilerxyz.gen.AspectFieldUseDynamic,Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        compilerxyz.gen.AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectStackValueDynamic,
        ExporterAny {

  public final Type elementType;
  public final ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new ArtifactSupport.ArtifactIdHolder();

  public TypeOptionDynamic(Type elementType) {
    this.elementType = elementType;
  }

  public Type elementType() {
    return elementType;
  }



  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    // Options are real Opt objects at runtime (Opt.Some for some, Opt.none()
    // for none) — never bare nullable values — so Opt<Opt<X>> is
    // unambiguous. The runtime data type is Opt, not Object.
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(compilerxyz.util.Opt.class));
  }

  @Override
  public String typeDesc() {
    return "option(" + elementType.typeDesc() + ")";
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
        || key.equals(compilerxyz.gen.GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.GenAspectSet.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.value.ValueDynamicSource source) {
    return compilerxyz.eval.EResult.ok(
        new compilerxyz.eval.CodeValue(
            compilerxyz.eval.Code.of(
                loc, compilerxyz.eval.dynamic.DynUtil.renderSource(xx, loc, source)),
            compilerxyz.eval.EvalOutcome.ofValue(
                new compilerxyz.eval.value.ValueDynamic(source, this))));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> aspectSetSet(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue base,
      compilerxyz.eval.CodeValue value) {
    return compilerxyz.eval.dynamic.DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
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

  private static Artifact nullArtifact() {
    return new Artifact.Body(
        new ArtifactBody(
            new compilerxyz.safejson.JsonValueNull(),
            new compilerxyz.artifact.ArtifactBodyExporter.Root()));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeOptionDynamic option)) {
            return EResult.err(
                loc,
                "TypeOptionDynamic::exporterRootDelegateSelfExport: expected TypeOptionDynamic,"
                    + " got "
                    + self.getClass());
          }
          var cell = option.artifactIdCell;
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  cell,
                  () -> {
                    var elementArtRes = option.elementType().exportArtifact(xx, loc);
                    elementArtRes.drainInto(errors);
                    var elementArt =
                        elementArtRes.valueOrNull() != null
                            ? elementArtRes.requireValue(
                                "TypeOptionDynamic::exporterExport element")
                            : nullArtifact();
                    var body = new ArtifactOptionType(elementArt);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(loc, "TypeOptionDynamic requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactOptionType body;
                try {
                  body = JsonValues.fromValue(ArtifactOptionType.class, raw);
                } catch (RuntimeException e) {
                  return new compilerxyz.util.Result.Err<>(
                      "TypeOptionDynamic data: failed to parse ArtifactOptionType: "
                          + e.getMessage());
                }
                return new compilerxyz.util.Result.Ok<>(body);
              },
              (body) -> {
                var holder = new ExporterAny[] {null};
                ExporterAny stub =
                    new ExporterAny() {
                      @Override
                      public EResult<Type> exporterAnyAsType() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeOptionDynamic import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsType();
                      }

                      @Override
                      public EResult<Value> exporterAnyAsValue() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeOptionDynamic import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsValue();
                      }
                    };
                var deps = new TList<Artifact>();
                deps.add(body.elementType());
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var elementRes =
                          ArtifactSupport.independentFinalizeImportTypeArtifact(
                              xx2, loc2, body.elementType());
                      var elementT =
                          elementRes.requireValue("TypeOptionDynamic::finalizer element type");
                      var out = new TypeOptionDynamic(elementT);
                      out.artifactIdCell.set(idValue);
                      holder[0] = out;
                    };
                return new compilerxyz.util.Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
              });
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
      var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
      if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
      var elemDesc =
          elemConsumeRes
              .requireValue("TypeOptionDynamic::field value AspectConsumeDynamic")
              .aspectConsumeDynamicJvmClass(xx);
      if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
        seq.add(compilerxyz.eval.dynamic.JavaBytecodeUtils.cast(loc, elemDesc));
      }
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new compilerxyz.eval.dynamic.CodeLoc.Some(loc), seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(elementType, 1))));
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
              EvalOutcome.ofValue(ValueDynamic.stack(new compilerxyz.eval.value.bool_.TypeBoolDynamic(), 1))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on option");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(
              new CodeValue(Code.none(loc), EvalOutcome.ofValue(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), f.type()))));
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
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

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return new compilerxyz.eval.value.binding.BindingDynamicBinding(dynamicKey, this);
  }


}
