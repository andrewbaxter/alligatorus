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
import compilerxyz.gen.GenCodegenDescriptors;
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
import compilerxyz.artifact.ArtifactBodyExporter.Root;
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.bool_.TypeBoolDynamic;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectSet;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectFieldUseDynamic;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.util.Result;

@BuiltinFull(uniqueId = 6008L)
public final class TypeOptionDynamic
    implements
        
        AspectSet,
        AspectFieldUseDynamic,Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectElementUseDynamicBind,
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
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    // Options are real Opt objects at runtime (Opt.Some for some, Opt.none()
    // for none) — never bare nullable values — so Opt<Opt<X>> is
    // unambiguous. The runtime data type is Opt, not Object.
    return JavaDataDescriptor.fromInternalName(
        JavaInternalName.fromClass(compilerxyz.util.Opt.class));
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
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectSet.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx,
      QExprId loc,
      ValueDynamicSource source) {
    return EResult.ok(
        new CodeValue(
            Code.of(
                loc, DynUtil.renderSource(xx, loc, source)),
            EvalOutcome.ofValue(
                new ValueDynamic(source, this))));
  }

  @Override
  public EResult<CodeValue> aspectSetSet(
      ExprContext xx,
      QExprId loc,
      CodeValue base,
      CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_OPTION_DYNAMIC_IDENTITY, TypeOptionDynamic.class);
    return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType);
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
            new JsonValueNull(),
            new Root()));
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
                    var elementArtRes = option.elementType().artifactableExportArtifact(xx, loc);
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
          if (id.optIsNone()) {
            return EResult.err(loc, "TypeOptionDynamic requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
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
                  return new Result.Err<>(
                      "TypeOptionDynamic data: failed to parse ArtifactOptionType: "
                          + e.getMessage());
                }
                return new Result.Ok<>(body);
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
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
              });
        }
      };

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
            new ConstKey.Str("some"),
            new FieldSpec.Factory(new TypeOptionVariantConstructorDynamic(elementType, true))),
        new Pair<>(
            new ConstKey.Str("none"),
            new FieldSpec.Factory(new TypeOptionVariantConstructorDynamic(elementType, false))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, "TypeOptionDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    var ck = ckOpt.optUnwrap();
    var ckStr = switch (ck) {
      case ConstKey.Str s -> Opt.some(s);
      case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
    };
    if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("value")) {
      var selfDyn = self.code().codeDynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeOptionDynamic::aspectFieldField(value): self has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          JavaBytecodeUtils.cast(
              loc,
              JavaDataDescriptor.fromInternalName(
                  JavaInternalName.fromClass(Opt.Some.class))));
      seq.add(JavaBytecodeUtils.callMethod(loc, GenCodegenDescriptors.SOME_VALUE));
      var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, elementType);
      if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
      var elemDesc =
          elemConsumeRes
              .requireValue("TypeOptionDynamic::field value AspectConsumeDynamic")
              .aspectConsumeDynamicJvmClass(xx);
      if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
        seq.add(JavaBytecodeUtils.cast(loc, elemDesc));
      }
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new Some(loc), seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(elementType, 1))));
    }
    if (ckStr.optIsSome() && ckStr.optUnwrap().value().equals("is_some")) {
      var selfDyn = self.code().codeDynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "TypeOptionDynamic::aspectFieldField(is_some): self has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(selfDyn);
      seq.add(
          JavaBytecodeUtils.instanceOf(
              loc, JavaInternalName.fromClass(Opt.Some.class)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(new Some(loc), seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(new TypeBoolDynamic(), 1))));
    }
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on option");
    }
    return switch (spec) {
      case FieldSpec.Factory f ->
          EResult.ok(
              new CodeValue(Code.none(loc), EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
    };
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var keyStr = switch (key) {
      case ConstKey.Str s -> Opt.some(s);
      case ConstKey.Uint u -> Opt.<ConstKey.Str>none();
    };
    if (keyStr.optIsSome() && keyStr.optUnwrap().value().equals("is_some")) {
      return Opt.some(new TypeBoolDynamic());
    }
    var spec = fieldSpecs().getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.fieldSpecFieldType());
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
    var dyn = cv.code().codeDynamic();
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }


}
