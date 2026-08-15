package compilerxyz.eval.value.unique;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;

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
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.binding.BindingValue;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectOperatorEq;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectOperatorEq;
import compilerxyz.eval.value.bool_.TypeBoolConst;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.util.MustCast;

@BuiltinLight(uniqueId = 6148L)
public record TypeUniqueConst()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectOperatorEq,
        ExporterAny {

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromClass(Unique.class);
  }

  @Override
  public String typeDesc() { return "unique_const"; }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectOperatorEq.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsSome()) {
      return EResult.ok(new BindResult(cv.code(), new BindingValue(cv.value().optUnwrap())));
    }
    return EResult.err(loc, "TypeUniqueConst::bind: CodeValue has no compile-time value");
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (cv.value().optIsNone()) {
      return EResult.err(loc, "TypeUniqueConst::toDynamic: no compile-time value");
    }
    Object hostValue = cv.value().optUnwrap();
    if (hostValue instanceof ValueObjectConst wrap) {
      hostValue = wrap.data();
    }
    var u = MustCast.mustCast(Unique.class, hostValue);
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
    if (b.optIsSome() && r.optIsSome()) {
      return EResult.ok(
          new CodeValue(
              Code.concat(loc, base.code(), reference.code()),
              EvalOutcome.ofValue(
                  new ValueObjectConst(
                      new TypeBoolConst(),
                      b.optUnwrap().equals(r.optUnwrap())))));
    }
    return new TypeUniqueDynamic().aspectOperatorEqEq(xx, loc, base, reference);
  }

  private static Opt<Unique> extractUnique(CodeValue cv) {
    if (cv.value().optIsSome()
        && cv.value().optUnwrap() instanceof ValueObjectConst v
        && v.data() instanceof Unique u) {
      return Opt.some(u);
    }
    return Opt.none();
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return other.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_CONST_IDENTITY).optIsSome();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return sourceType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_UNIQUE_DYNAMIC_IDENTITY).optIsSome();
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
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Type> exporterAnyAsType() { return EResult.ok(new TypeUniqueConst()); }

  @Override
  public EResult<Value> exporterAnyAsValue() { return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this)); }
}
