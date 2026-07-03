package compilerxyz.eval.value.string;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
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
import compilerxyz.eval.value.TypeRoot;
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
import compilerxyz.gen.AspectType;

public record TypeStringDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectInitialBind,
        AspectLift,
        AspectSet,
        AspectStackValueDynamic,
        AspectStringDynamic,
        ExporterAny {

  private static final compilerxyz.eval.dynamic.JavaInternalName DYN_RUNTIME =
      compilerxyz.eval.dynamic.JavaInternalName.fromClass(compilerxyz.eval.dynamic.DynRuntime.class);
  private static final compilerxyz.eval.dynamic.JavaMethodDescriptor DESC_STR_TO_STR =
      compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
          compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class),
          TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class)));

  private static final TMap<ConstKey, Type> METHOD_FIELDS =
      TMap.ofEntries(
          new Pair<>(
              new ConstKey.Str("toLowerCase"),
              new compilerxyz.eval.value.function.TypeMethodDynamic(
                  new TypeStringDynamic(),
                  new TypeNullDynamic(),
                  new TypeStringDynamic(),
                  DYN_RUNTIME,
                  "stringToLowerCase",
                  DESC_STR_TO_STR,
                  false)),
          new Pair<>(
              new ConstKey.Str("toUpperCase"),
              new compilerxyz.eval.value.function.TypeMethodDynamic(
                  new TypeStringDynamic(),
                  new TypeNullDynamic(),
                  new TypeStringDynamic(),
                  DYN_RUNTIME,
                  "stringToUpperCase",
                  DESC_STR_TO_STR,
                  false)));

  @Override
  public String typeDesc() {
    return "dynamic string";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.StringDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectStringDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)
        || key.equals(AspectSet.ID)
        || key.equals(AspectArtifact.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.STRING_DYNAMIC;
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override  @Override
  public EResult<Value> lift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof String s)) {
      return EResult.err(
          loc,
          "TypeStringDynamic::lift: expected java.lang.String, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    return EResult.ok(new ValueStringConst(s));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<CodeValue> set(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
    return DynUtil.dynamicSet(xx, loc, base, value);
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeStringDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(fieldTypeOpt(xx, ck) instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on dynamic string");
    }
    return EResult.ok(
        new CodeValue(
            self_.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return TList.from(METHOD_FIELDS.keySet());
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return METHOD_FIELDS.getOpt(key);
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
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return AspectStringConst.getMaybe(xx, sourceType).optIsSome()
        || AspectStringDynamic.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return AspectStringDynamic.getMaybe(xx, destType).optIsSome()
        || destType.typeAspect(xx, AspectType.ID).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeStringDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    var stringConstOpt = AspectStringConst.getMaybe(xx, sourceType);
    if (stringConstOpt instanceof Opt.Some<AspectStringConst> stringConstSome) {
      String strVal = stringConstSome.value().get(some.value());
      // Concatenate the source's existing dynamic code (if any) with an LDC of the string
      // literal, matching the semantics of the old Block(existing, literal) construction.
      var seq = new JavaBytecodeSequence();
      var existingDyn = CodeDynamic.fromCode(cv.code());
      if (existingDyn != null) {
        seq.add(existingDyn);
      }
      seq.add(JavaBytecodeUtils.literalString(loc, strVal));
      var result = new CodeDynamic(loc, seq);
      return EResult.ok(
          new CodeValue(
              Code.of(result),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
    }
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    return EResult.ok(
        new CodeValue(
            cv.code(),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
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
}
