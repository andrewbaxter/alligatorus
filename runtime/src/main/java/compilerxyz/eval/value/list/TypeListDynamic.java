package compilerxyz.eval.value.list;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
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
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.function.TypeFunctionDynamic;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.gen.AspectStringDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;

public record TypeListDynamic(Type elementType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectField,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "list(" + elementType.typeDesc() + ")";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.ListDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectIterable.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.LIST_DYNAMIC;
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
              "TypeListDynamic::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port"
                  + " deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeListDynamic::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact"
                  + " (eval/artifact.rs) port deferred");
        }
      };

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeListDynamic::field: key has no compile-time value");
    }
    var keyType = keySome.value().valueTypeOf();
    var stringConstRes = AspectStringConst.get(xx, loc, keyType);
    if (stringConstRes.hasErrors()) {
      return stringConstRes.reerr();
    }
    var stringConst = stringConstRes.valueOrNull();
    if (stringConst == null) {
      throw new IllegalStateException("AspectStringConst.get returned neither value nor errors");
    }
    String keyName = stringConst.get(keySome.value());
    if (keyName.equals("new")) {
      Type newType = new TypeListNewDynamic(elementType);
      return EResult.ok(
          new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(newType))));
    }
    var fieldTypeOpt = fieldTypeOpt(xx, new ConstKey.Str(keyName));
    if (!(fieldTypeOpt instanceof Opt.Some<Type> fieldTypeSome)) {
      return EResult.err(loc, "Field '" + keyName + "' not found on list");
    }
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeListDynamic::field: receiver has no runtime code");
    }
    var fieldUse = new CodeDynamic(loc, DynUtil.buildFieldGetSequence(selfDyn, keyName));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, fieldUse),
            Opt.some(
                new ValueDynamic(
                    new ValueDynamic.Source.Field(selfDyn, keyName), fieldTypeSome.value()))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    return switch (s.value()) {
      case "new" -> Opt.some(new TypeListNewDynamic(elementType));
      case "push" ->
          Opt.some(
              new TypeFunctionDynamic(
                  elementType, new TypeNullDynamic()));
      case "join" -> {
        if (AspectStringDynamic.getMaybe(xx, elementType).optIsSome()) {
          yield Opt.some(
              new TypeFunctionDynamic(
                  new TypeStringDynamic(),
                  new TypeStringDynamic()));
        }
        yield Opt.none();
      }
      default -> Opt.none();
    };
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeListDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type elementType() {
    return elementType;
  }

  @Override
  public EResult<CodeValue> foreach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = AspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) {
      return baseConsumeRes.reerr();
    }
    var baseDynRes = baseConsumeRes.valueOrNull().toDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) {
      return baseDynRes.reerr();
    }
    CodeDynamic bodyDyn = body.code().dynamic();
    if (bodyDyn == null) {
      bodyDyn =
          new CodeDynamic(loc, new JavaBytecode.LoweredSequence(JavaBytecodeUtils.jsonLiteral(new JsonValueNull())));
    }
    var foreachDyn =
        new CodeDynamic(loc, new JavaBytecode.Foreach(baseDynRes.valueOrNull(), elementScopeId, bodyDyn));
    Type nullType = new TypeNullDynamic();
    var stackRes = AspectStackValueDynamic.get(xx, loc, nullType);
    if (stackRes.hasErrors()) {
      return stackRes.reerr();
    }
    return EResult.ok(stackRes.valueOrNull().toValue(loc, foreachDyn));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
