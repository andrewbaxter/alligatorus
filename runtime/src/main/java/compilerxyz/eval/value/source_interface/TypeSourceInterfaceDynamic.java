package compilerxyz.eval.value.source_interface;

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
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.eval.Code;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeSourceInterfaceDynamic()
    implements Type, AspectBind, AspectConsumeDynamic, AspectInitialBind, ExporterAny {

  @Override
  public compilerxyz.eval.dynamic.JavaInternalName consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT;
  }

  @Override
  public String typeDesc() {
    return "definition_set_interface_ref";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceInterfaceDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.INTERFACE_REF_TYPE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeSourceInterfaceDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
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
    return sourceType.typeAspect(xx, GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || sourceType.typeAspect(xx, GenIdentities.INTERFACE_CONST_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, GenIdentities.INTERFACE_CONST_IDENTITY).optIsSome()
        || destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeSourceInterfaceDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    if (sourceType.typeAspect(xx, GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || sourceType.typeAspect(xx, GenIdentities.INTERFACE_CONST_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), this))));
    }
    return EResult.err(loc, "Cannot convert to interface_ref");
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, GenIdentities.INTERFACE_REF_TYPE_IDENTITY).optIsSome()
        || destType.typeAspect(xx, GenIdentities.INTERFACE_CONST_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), destType))));
    }
    if (destType.typeAspect(xx, GenIdentities.TYPE_TYPE_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc,
            "cast interface_ref → " + destType.typeDesc() + ": source has no runtime code");
      }
      var args = new TList<CodeDynamic>();
      args.add(sourceCode);
      var call =
          compilerxyz.eval.dynamic.DynRuntime.INTERFACE_REF_TO_TYPE_TYPE.emitInvocation(
              loc, xx.vx, args);
      return EResult.ok(
          new CodeValue(
              Code.of(loc, call),
              Opt.some(new ValueDynamic(new ValueDynamic.Source.Expr(), destType))));
    }
    return EResult.err(loc, "Cannot convert interface_ref");
  }
}
