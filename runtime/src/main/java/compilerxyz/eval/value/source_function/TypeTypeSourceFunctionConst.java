package compilerxyz.eval.value.source_function;

import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.empty_.ValueEmpty;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import org.jspecify.annotations.Nullable;

public final class TypeTypeSourceFunctionConst {
  private TypeTypeSourceFunctionConst() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final Impl INSTANCE = new Impl();
  }

  private static final class Impl implements Type, AspectLift, AspectConsumeDynamic,
      AspectBind, AspectBindDynamic, AspectInitialBind, AspectDropDynamicLight {
    @Override
    public TypeId hasTypeIdTypeId() {
      return GenTypeIds.SOURCE_FUNCTION_TYPE_TYPE_ID;
    }

    @Override
    public String typeDesc() {
      return "definition_set_function";
    }

                                public Type valueType() {
      return this;
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
      if (key.equals(GenAspectLift.AspectId)
          || key.equals(GenAspectConsumeDynamic.AspectId)
          || key.equals(GenAspectBind.AspectId)
          || key.equals(GenAspectBindDynamic.AspectId)
          || key.equals(GenAspectInitialBind.AspectId)
          || key.equals(GenAspectDropDynamicLight.AspectId)) {
        return Opt.some(this);
      }
      return Opt.none();
    }

    @Override
    public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
      return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
      return DynUtil.bindDynamicDefault(xx, loc, cv);
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
      return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
      return DynUtil.dropDefault(xx, loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(
        ExprContext xx, QExprId loc, CodeValue cv) {
      return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
      return JavaDataDescriptor.fromClass(TypeSourceFunctionConst.class);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
      return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
      return EResult.err(loc, typeDesc() + "::typeCastFrom: not supported");
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
      return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
      return EResult.err(loc, typeDesc() + "::typeCastTo: not supported");
    }

    @Override
    public EResult<Value> aspectLiftLift(
        ExprContext xx, QExprId loc, @Nullable Object externalHostValue) {
      if (externalHostValue instanceof ValueObjectConst voc
          && voc.data() instanceof TypeSourceFunctionConst func) {
        return EResult.ok(new ValueEmpty(func));
      }
      if (externalHostValue instanceof TypeSourceFunctionConst func) {
        return EResult.ok(new ValueEmpty(func));
      }
      return EResult.err(
          loc,
          typeDesc() + "::lift: unexpected host "
              + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
    }
  }
}
