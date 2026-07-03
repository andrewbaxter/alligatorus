package compilerxyz.eval.value.any;

import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.External;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeAnyConst()
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectField,
        AspectFunction,
        AspectInitialBind,
        AspectType,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "type";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.AnyConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectFunction.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterAnyDelegate.INSTANCE;
  }

  @Override
  public Type type_(Value value) {
    if (value instanceof ValueAnyConst v) return v.inner();
    throw new IllegalArgumentException(
        "TypeAnyConst::type_: expected ValueAnyConst, got " + value.getClass());
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(self.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof ValueAnyConst wrapper)) {
      return EResult.err(loc, "TypeAnyConst::field: expected ValueAnyConst self");
    }
    var inner = wrapper.inner();
    var asp = AspectField.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.valueOrNull().field(xx, loc, self, key);
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(self.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof ValueAnyConst wrapper)) {
      return EResult.err(loc, "TypeAnyConst::call: expected ValueAnyConst self");
    }
    var inner = wrapper.inner();
    var asp = AspectFunction.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.valueOrNull().call(xx, loc, self, argument);
  }

  @Override
  public Opt<Type> exceptionType() {
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
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeAnyConst::toDynamic: CodeValue has no value");
    }
    var metaType = some.value().valueTypeOf();
    var aspectTypeRes = AspectType.get(xx, loc, metaType);
    if (aspectTypeRes.hasErrors()) {
      return aspectTypeRes.reerr();
    }
    var resolvedType = aspectTypeRes.valueOrNull().type_(some.value());
    var key = new compilerxyz.eval.dynamic.JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(key, this, new External.TypeExternal(resolvedType)));
    return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(new TypeAnyConst());
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueTypeAnyConst());
  }

}
