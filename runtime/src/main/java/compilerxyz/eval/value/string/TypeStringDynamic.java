package compilerxyz.eval.value.string;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.annotations.BuiltinPath;

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
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectSet;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.gen.AspectStringDynamic;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectSet;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.GenAspectStringDynamic;

@BuiltinPath("types.string")
@BuiltinIdentity(uniqueId = 6145L)
public record TypeStringDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectLift,
        AspectSet,
        compilerxyz.gen.AspectFieldUseDynamic,
        AspectStackValueDynamic,
        AspectStringDynamic,
        ExporterAny {


  private static final compilerxyz.eval.dynamic.JavaInternalName JAVA_STRING =
      compilerxyz.eval.dynamic.JavaInternalName.fromClass(String.class);
  private static final compilerxyz.eval.dynamic.JavaMethodDescriptor DESC_TO_STR =
      compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
          compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class), TList.of());

  private static final Type EMPTY_TUPLE =
      new compilerxyz.eval.value.tuple.Tuple(compilerxyz.util.Opt.none(), new TList<>());

  private static final TMap<ConstKey, Type> METHOD_FIELDS =
      TMap.ofEntries(
          new Pair<>(
              new ConstKey.Str("toLowerCase"),
              new compilerxyz.eval.value.function.TypeMethodDynamic(
                  new TypeStringDynamic(),
                  TList.<compilerxyz.eval.value.function.FunctionParam>of(),
                  new TypeStringDynamic(),
                  JAVA_STRING,
                  "toLowerCase",
                  DESC_TO_STR,
                  false,
                  false)),
          new Pair<>(
              new ConstKey.Str("toUpperCase"),
              new compilerxyz.eval.value.function.TypeMethodDynamic(
                  new TypeStringDynamic(),
                  TList.<compilerxyz.eval.value.function.FunctionParam>of(),
                  new TypeStringDynamic(),
                  JAVA_STRING,
                  "toUpperCase",
                  DESC_TO_STR,
                  false,
                  false)));

  @Override
  public String typeDesc() {
    return "dynamic string";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JavaInternalName.fromClass(String.class));
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeStringDynamic.class, "string_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectStringDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectSet.AspectId)
        || key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectArtifact.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.STRING_DYNAMIC;
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
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    var s = compilerxyz.util.MustCast.mustCast(String.class, externalHostValue);
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(new compilerxyz.eval.value.string.TypeStringConst(), s));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx, QExprId loc, compilerxyz.eval.value.ValueDynamicSource source) {
    return EResult.ok(
        new CodeValue(
            Code.of(loc, DynUtil.renderSource(xx, loc, source)),
            EvalOutcome.ofValue(new compilerxyz.eval.value.ValueDynamic(source, this))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeStringDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(aspectFieldTypeOpt(xx, ck) instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on dynamic string");
    }
    if (!(self_.value() instanceof Opt.Some<Value> strMethodSome)
        || !(strMethodSome.value() instanceof compilerxyz.eval.value.ValueDynamic strMethodVd)) {
      return EResult.err(
          loc, "TypeStringDynamic::field: method receiver has no dynamic source");
    }
    return EResult.ok(
        new CodeValue(
            self_.code(),
            EvalOutcome.ofValue(
                new compilerxyz.eval.value.ValueDynamic(
                    strMethodVd.source(), fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(METHOD_FIELDS.keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    return METHOD_FIELDS.getOpt(key);
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
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_STRING_DYNAMIC_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return GenAspectStringConst.getMaybe(xx, sourceType).optIsSome()
        || GenAspectStringDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return GenAspectStringDynamic.getMaybe(xx, destType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeStringDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    var stringConstOpt = GenAspectStringConst.getMaybe(xx, sourceType);
    if (stringConstOpt instanceof Opt.Some<AspectStringConst> stringConstSome) {
      String strVal = stringConstSome.value().aspectStringConstGet(some.value());
      // Concatenate the source's existing dynamic code (if any) with an LDC of the string
      // literal, matching the semantics of the old Block(existing, literal) construction.
      var seq = new JavaBytecodeSequence();
      var existingDyn = cv.code().dynamic();
      if (existingDyn != null) {
        seq.add(existingDyn);
      }
      seq.add(JavaBytecodeUtils.literalString(loc, strVal));
      var result = new CodeDynamic(loc, seq);
      return EResult.ok(
          new CodeValue(
              Code.of(result),
              EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            EvalOutcome.ofValue(ValueDynamic.stack(this, 1))));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeStringDynamic)) {
            return EResult.err(
                loc, "TypeStringDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeStringDynamic());
          }
          return EResult.err(
              loc, "TypeStringDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
        }
      };

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
