package compilerxyz.eval.value.string;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueString;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeStringConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectStringConst,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_STRING_CONST_TYPE_ID; }


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "string";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.StringConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectStringConst.ID)
        || key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.STRING_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          JsonValue data;
          if (self instanceof ValueStringConst v) {
            data = new JsonValueString(v.value());
          } else if (self instanceof TypeStringConst || self instanceof ValueTypeStringConst) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeStringConst::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull ignored -> EResult.ok(new ValueTypeStringConst());
            case JsonValueString s -> EResult.ok(new ValueStringConst(s.value()));
            default -> EResult.err(
                loc, "TypeStringConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(Code.none(loc), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeStringConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof ValueStringConst v) {
      return EResult.ok(JavaBytecodeUtils.literalString(loc, v.value()));
    }
    return EResult.err(loc, "TypeStringConst::toDynamic: expected ValueStringConst");
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var dynType = new TypeStringDynamic();
    var convertedRes = dynType.typeCastFrom(xx, loc, self);
    if (convertedRes.hasErrors()) {
      return convertedRes.reerr();
    }
    return dynType.field(xx, loc, convertedRes.requireValue("TypeStringDynamic::typeCastFrom"), key);
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return new TypeStringDynamic().fieldTypeOpt(xx, key);
  }

  @Override
  public String get(Value value) {
    if (value instanceof ValueStringConst v) return v.value();
    throw new IllegalArgumentException(
        "TypeStringConst::get: expected ValueStringConst, got " + value.getClass());
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeStringConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueTypeStringConst());
  }


  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
