package compilerxyz.eval.value.source;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.artifact.Artifact;

@BuiltinLight(uniqueId = 6154L)
public record TypeSourceSpec() implements Type, AspectBind, AspectDropDynamicLight, ExporterAny {

  @Override
  public String typeDesc() {
    return "source_spec";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_SPEC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectBind.AspectId) || key.equals(GenAspectDropDynamicLight.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsSome()) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
    }
    return EResult.err(loc, "TypeSourceSpec::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
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
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_SOURCE_SPEC_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
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
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
