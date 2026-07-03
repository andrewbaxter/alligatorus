package compilerxyz.eval.value.any;

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
import compilerxyz.eval.External;
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
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.gen.GenIdentities;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.EvalContext;

public record TypeAnyDynamic()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectFunction,
        AspectInitialBind,
        AspectStackValueDynamic,
        AspectType,
        ExporterAny {

  @Override
  public compilerxyz.eval.dynamic.JavaInternalName consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT;
  }

  @Override
  public String typeDesc() {
    return "dynamic type";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.AnyDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectFunction.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectType.ID)
        || key.equals(GenIdentities.TYPE_TYPE_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.ANY_DYNAMIC;
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
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeAnyDynamic::field: receiver has no runtime code");
    }
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeAnyDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on any");
    }
    String keyName = keyStr.value();
    var fieldUse = DynUtil.buildFieldGetSequence(loc, selfDyn, keyName);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, fieldUse),
            Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.some(this);
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var funcDyn = self.code().dynamic();
    if (funcDyn == null) {
      return EResult.err(loc, "TypeAnyDynamic::call: function has no runtime code");
    }
    if (!(argument.value() instanceof Opt.Some<Value> argSome)) {
      return EResult.err(loc, "TypeAnyDynamic::call: argument has no compile-time value");
    }
    var argType = argSome.value().valueTypeOf();
    var consumeRes = AspectConsumeDynamic.get(xx, loc, argType);
    if (consumeRes.hasErrors()) {
      return consumeRes.reerr();
    }
    var argCodeRes = consumeRes.valueOrNull().toDynamic(xx, loc, argument);
    if (argCodeRes.hasErrors()) {
      return argCodeRes.reerr();
    }
    return EResult.err(
        loc,
        "TypeAnyDynamic::call: calling an any-typed value is not supported (no static callee info)");
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public Type type_(Value value) {
    return this;
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
    return AspectType.getMaybe(xx, sourceType).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    var existingDyn = cv.code().dynamic();
    if (existingDyn != null) {
      return EResult.ok(cv);
    }
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeAnyDynamic::typeCastFrom: CodeValue has no value");
    }
    var metaType = some.value().valueTypeOf();
    var aspectTypeRes = AspectType.get(xx, loc, metaType);
    if (aspectTypeRes.hasErrors()) {
      return aspectTypeRes.reerr();
    }
    var resolvedType = aspectTypeRes.valueOrNull().type_(some.value());
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(new EvalContext.ExternalEntry(
        key, this, new External.TypeExternal(resolvedType)));
    var bindingUse = BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, bindingUse),
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
          if (!(self instanceof TypeAnyDynamic)) {
            return EResult.err(
                loc, "TypeAnyDynamic::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(new JsonValueNull())));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (data instanceof JsonValueNull) {
            return EResult.ok(new TypeAnyDynamic());
          }
          return EResult.err(
              loc, "TypeAnyDynamic::exporterRootDelegateSelfFromArtifactData: expected null, got " + data);
        }
      };
}
