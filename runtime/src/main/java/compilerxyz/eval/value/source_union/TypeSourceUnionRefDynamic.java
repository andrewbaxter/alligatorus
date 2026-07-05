package compilerxyz.eval.value.source_union;

import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeSourceUnionRefDynamic()
    implements Type,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        AspectLift,
        ExporterAny {

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "union_ref";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceUnionRefDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.UNION_REF_TYPE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectLift.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
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
        loc, "TypeSourceUnionRefDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Value> lift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(externalHostValue instanceof SourceUnionRef ref)) {
      return EResult.err(
          loc,
          "TypeSourceUnionRefDynamic::lift: expected SourceUnionRef, got "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
    return EResult.ok(new ValueAnyConst(new TypeSourceUnionConst(ref.data())));
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
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> dropDynamicLight(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

}
