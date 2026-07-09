package compilerxyz.eval.value.bind_result;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeBindResultDynamic()
    implements Type, AspectConsumeDynamic, AspectField, AspectStackValueDynamic, ExporterAny {

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_BIND_RESULT_DYNAMIC_TYPE_ID;
  }

  @Override
  public JavaDataDescriptor consumeDynamicJvmClass() {
    return JavaDataDescriptor.fromClass(AspectBind.BindResult.class);
  }

  @Override
  public String typeDesc() {
    return "BindResult";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.BindResultDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectStackValueDynamic.ID)
        || key.equals(AspectField.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  private static final JavaInternalName INTNAME_BIND_RESULT =
      JavaInternalName.of("compilerxyz/gen/AspectBind$BindResult");

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeBindResultDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str s)) {
      return EResult.err(loc, "BindResult: unknown field " + ck.toCanonical());
    }
    var selfDyn = self_.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeBindResultDynamic::field: self has no runtime code");
    }
    switch (s.value()) {
      case "code" -> {
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callMethod(
            loc,
            INTNAME_BIND_RESULT,
            "code",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(Code.class))));
        return EResult.ok(new CodeValue(
            Code.of(loc, new CodeDynamic(new CodeLoc.Some(loc), seq)),
            Opt.some(ValueDynamic.stack(compilerxyz.eval.Code.dynamicType()))));
      }
      case "binding" -> {
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callMethod(
            loc,
            INTNAME_BIND_RESULT,
            "binding",
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(compilerxyz.eval.Binding.class))));
        return EResult.ok(new CodeValue(
            Code.of(loc, new CodeDynamic(new CodeLoc.Some(loc), seq)),
            Opt.some(ValueDynamic.stack(compilerxyz.eval.Binding.dynamicType()))));
      }
      default -> {
        return EResult.err(loc, "BindResult: unknown field '" + s.value() + "'");
      }
    }
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str s)) return Opt.none();
    return switch (s.value()) {
      case "code" -> Opt.some(compilerxyz.eval.Code.dynamicType());
      case "binding" -> Opt.some(compilerxyz.eval.Binding.dynamicType());
      default -> Opt.none();
    };
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    var out = new TList<ConstKey>();
    out.add(new ConstKey.Str("code"));
    out.add(new ConstKey.Str("binding"));
    return out;
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, compilerxyz.eval.QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeBindResultDynamic::toDynamic: CodeValue has no dynamic code");
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
