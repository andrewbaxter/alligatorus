package compilerxyz.eval.value.code;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.gen.GenBuiltinInterfaces;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.GenIdentities;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.dynamic.DynUtil;

public record TypeCodeDynHandleDynamic()
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectField,
        AspectInitialBind,
        ExporterAny {

  @Override
  public String typeDesc() {
    return "code_dyn";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.CodeDynHandleDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenIdentities.TYPE_CODE_DYN_HANDLE_IDENTITY)
        || key.equals(GenBuiltinInterfaces.builtinCodeDynIfaceIdentity())
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeCodeDynHandleDynamic::field: key has no compile-time value");
    }
    var keyValue = keySome.value();
    var keyMeta = keyValue.valueTypeOf();
    var stringAspectRes = AspectStringConst.get(xx, loc, keyMeta);
    if (stringAspectRes.hasErrors()) return stringAspectRes.reerr();
    var keyName = stringAspectRes.valueOrNull().get(keyValue);
    if ("cast".equals(keyName)) {
      var selfDyn = self.code().dynamic();
      if (selfDyn == null) {
        return EResult.err(loc, "code_dyn.cast: receiver has no dynamic code");
      }
      return EResult.ok(
          new CodeValue(
              Code.of(loc, selfDyn),
              Opt.some(new ValueAnyConst(new TypeCodeDynCastFn()))));
    }
    return EResult.err(loc, "Field '" + keyName + "' not found on code_dyn handle");
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    if ("cast".equals(s.value())) return Opt.some(new TypeCodeDynCastFn());
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
    return EResult.err(loc, "TypeCodeDynHandleDynamic::toDynamic: CodeValue has no dynamic code");
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
}
