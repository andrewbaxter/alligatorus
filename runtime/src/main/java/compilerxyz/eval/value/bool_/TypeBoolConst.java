package compilerxyz.eval.value.bool_;

import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBoolConst;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBoolConst;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectPostDeserialize;

@BuiltinLight(uniqueId = 6102L)
public record TypeBoolConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBoolConst,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        ExporterAny {


  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "bool";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_BOOL_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectBoolConst.AspectId)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.BOOL_CONST;
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
          if (self instanceof compilerxyz.eval.value.object_.ValueObjectConst v
              && v.data() instanceof Boolean bv) {
            data = new JsonValueBoolean(bv);
          } else if (self instanceof TypeBoolConst) {
            data = new JsonValueNull();
          } else {
            return EResult.err(
                loc, "TypeBoolConst::exporterRootDelegateSelfExport: unexpected self type " + self.getClass());
          }
          return EResult.ok(new Artifact.Body(makeBody.apply(data)));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return switch (data) {
            case JsonValueNull ignored -> EResult.ok(new TypeBoolConst());
            case JsonValueBoolean b -> EResult.ok(
                new compilerxyz.eval.value.object_.ValueObjectConst(new TypeBoolConst(), b.value()));
            default -> EResult.err(
                loc, "TypeBoolConst::exporterRootDelegateSelfFromArtifactData: unexpected " + data);
          };
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeBoolConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Boolean bv) {
      var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
      seq.add(JavaBytecodeUtils.literalBool(loc, bv));
      seq.add(boxBoolean(loc));
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    return EResult.err(loc, "TypeBoolConst::toDynamic: expected bool ValueObjectConst");
  }

  @Override
  public boolean aspectBoolConstGet(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst v
        && v.data() instanceof Boolean bv) return bv;
    throw new IllegalArgumentException(
        "TypeBoolConst::get: expected bool ValueObjectConst, got " + value.getClass());
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeBoolConst());
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
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_BOOL_CONST_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  private static compilerxyz.eval.dynamic.CodeDynamic boxBoolean(compilerxyz.eval.QExprId loc) {
    return JavaBytecodeUtils.callStaticMethod(
        loc,
        compilerxyz.eval.dynamic.JavaInternalName.fromClass(Boolean.class),
        "valueOf",
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(
                compilerxyz.eval.dynamic.JavaInternalName.fromClass(Boolean.class)),
            compilerxyz.util.TList.of(compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_BOOL)));
  }
}
