package compilerxyz.eval.value.unique;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.External;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.GenIdentities;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public record TypeUniqueConst()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectOperatorEq,
        ExporterAny {

  @Override
  public TypeId typeId() { return GenTypeIds.TYPE_UNIQUE_CONST_TYPE_ID; }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromClass(Unique.class);
  }

  @Override
  public String typeDesc() { return "unique_const"; }

  @Override
  public Type valueType() { return new TypeRoot.Unique(); }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.TYPE_UNIQUE_IDENTITY)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectOperatorEq.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new BindResult(Code.none(loc), new BindingValue(some.value())));
    }
    return EResult.err(loc, "TypeUniqueConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeUniqueConst::toDynamic: no compile-time value");
    }
    Object hostValue = some.value();
    if (hostValue instanceof ValueObjectConst wrap) {
      hostValue = wrap.data();
    }
    if (!(hostValue instanceof Unique u)) {
      return EResult.err(
          loc,
          "TypeUniqueConst::toDynamic: expected Unique, got "
              + (hostValue == null ? "null" : hostValue.getClass().getName()));
    }
    var key = new JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(key, this, new External(Unique.class, u)));
    return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
  }

  @Override
  public EResult<CodeValue> aspectOperatorEqEq(
      ExprContext xx, QExprId loc, CodeValue base, CodeValue reference) {
    var b = extractUnique(base);
    var r = extractUnique(reference);
    if (b instanceof Opt.Some<Unique> bs && r instanceof Opt.Some<Unique> rs) {
      return EResult.ok(
          new CodeValue(
              Code.none(loc),
              Opt.some(
                  new ValueObjectConst(
                      new compilerxyz.eval.value.bool_.TypeBoolConst(),
                      bs.value().equals(rs.value())))));
    }
    return new TypeUnique().aspectOperatorEqEq(xx, loc, base, reference);
  }

  private static Opt<Unique> extractUnique(CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some
        && some.value() instanceof ValueObjectConst v
        && v.data() instanceof Unique u) {
      return Opt.some(u);
    }
    return Opt.none();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenIdentities.TYPE_UNIQUE_IDENTITY) instanceof Opt.Some<Aspect>;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    return EResult.ok(cv);
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) { return false; }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException("TypeUniqueConst::typeCastTo unreachable");
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Type> exporterAnyAsType() { return EResult.ok(new TypeUniqueConst()); }

  @Override
  public EResult<Value> exporterAnyAsValue() { return EResult.ok(this); }
}
