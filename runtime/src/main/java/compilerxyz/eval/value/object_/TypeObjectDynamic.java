package compilerxyz.eval.value.object_;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
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
import compilerxyz.eval.dynamic.DynRuntime;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.interface_spec.TypeInterfaceSpec;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.gen.AspectAspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeObjectDynamic(
    TypeId typeId,
    String typeDesc,
    JavaInternalName jvmClass,
    JavaDataDescriptor jvmClassDesc,
    TList<ObjectDataField> dataFields,
    TList<ObjectMethod> methods,
    ObjectDropKind dropKind,
    TList<Unique> identityAspects,
    Opt<Unique> aspectUnique,
    Opt<JavaInternalName> interfaceRepresented)
    implements
        Type,
        compilerxyz.eval.TypeAspect,
        AspectAspect,
        AspectField,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectDropDynamicLight,
        AspectBind,
        AspectBindDynamic,
        AspectInitialBind,
        AspectStackValueDynamic {

  @Override
  public Unique id() {
    if (!(aspectUnique instanceof Opt.Some<Unique> some)) {
      throw new IllegalStateException(
          typeDesc + "::id: TypeObjectDynamic has no aspectUnique — not an aspect metatype");
    }
    return some.value();
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TypeObjectDynamic o && typeId.equals(o.typeId);
  }

  @Override
  public int hashCode() {
    return typeId.hashCode();
  }

  @Override
  public String typeDesc() {
    return typeDesc;
  }

  @Override
  public Type valueTypeOf() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    if ((!dataFields.isEmpty() || !methods.isEmpty()) && key.equals(AspectField.ID)) {
      return Opt.some(this);
    }
    if (aspectUnique instanceof Opt.Some && key.equals(AspectAspect.ID)) {
      return Opt.some(this);
    }
    switch (dropKind) {
      case DYNAMIC -> {
        if (key.equals(AspectDropDynamic.ID)) return Opt.some(this);
      }
      case DYNAMIC_LIGHT -> {
        if (key.equals(AspectDropDynamicLight.ID)) return Opt.some(this);
      }
    }
    for (var idAspect : identityAspects) {
      if (key.equals(idAspect)) return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public Unique getUnique(InterfaceData value) {
    if (!(aspectUnique instanceof Opt.Some<Unique> some)) {
      throw new IllegalStateException(
          typeDesc + "::getUnique: TypeObjectDynamic does not represent an aspect");
    }
    return some.value();
  }

  @Override
  public InterfaceData getIfaceData(InterfaceData value) {
    throw new UnsupportedOperationException(
        typeDesc + "::getIfaceData: TypeObjectDynamic has no InterfaceData");
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    if (interfaceRepresented instanceof Opt.Some
        && (destType instanceof TypeInterfaceSpec
            || destType
                instanceof compilerxyz.eval.value.source_interface.TypeAnyInterfaceDynamic)) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (interfaceRepresented instanceof Opt.Some) {
      if (destType instanceof TypeInterfaceSpec) {
        var sourceCode = cv.code().dynamic();
        if (sourceCode == null) {
          return EResult.err(
              loc,
              "cast " + typeDesc + " → " + destType.typeDesc() + ": source has no runtime code");
        }
        var args = new TList<CodeDynamic>();
        args.add(sourceCode);
        var call = DynRuntime.OBJECT_TO_INTERFACE_SPEC.emitInvocation(loc, xx.vx, args);
        return EResult.ok(
            new CodeValue(
                Code.of(loc, call), Opt.some(ValueDynamic.stack(new TypeInterfaceSpec()))));
      }
      if (destType
          instanceof compilerxyz.eval.value.source_interface.TypeAnyInterfaceDynamic) {
        var sourceCode = cv.code().dynamic();
        if (sourceCode == null) {
          return EResult.err(
              loc,
              "cast " + typeDesc + " → " + destType.typeDesc() + ": source has no runtime code");
        }
        var args = new TList<CodeDynamic>();
        args.add(sourceCode);
        var call = DynRuntime.OBJECT_TO_INTERFACE_DATA.emitInvocation(loc, xx.vx, args);
        return EResult.ok(
            new CodeValue(
                Code.of(loc, call), Opt.some(ValueDynamic.stack(destType))));
      }
    }
    return EResult.err(loc, "Cannot cast " + typeDesc + " to " + destType.typeDesc());
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, typeDesc + "::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str ckStr)) {
      return EResult.err(loc, typeDesc + ": unknown field " + ck.toCanonical());
    }
    var keyStr = ckStr.value();
    var dyn = self_.code().dynamic();
    if (dyn == null) {
      return EResult.err(loc, typeDesc + ": self has no dynamic code");
    }
    for (var f : dataFields) {
      if (f.langName().equals(keyStr)) {
        var loadSeq = new JavaBytecodeSequence();
        loadSeq.add(dyn);
        loadSeq.add(
            JavaBytecodeUtils.accessField(loc, jvmClass, f.jvmFieldName(), f.jvmFieldDesc()));
        return EResult.ok(
            new CodeValue(
                Code.of(loc, new CodeDynamic(loc, loadSeq)),
                Opt.some(ValueDynamic.stack(f.fieldType()))));
      }
    }
    for (var m : methods) {
      if (m.langName().equals(keyStr)) {
        return EResult.ok(
            new CodeValue(self_.code(), Opt.some(ValueDynamic.stack(m.funcType()))));
      }
    }
    return EResult.err(loc, typeDesc + ": unknown field '" + keyStr);
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    if (!(key instanceof ConstKey.Str keyStr)) {
      return Opt.none();
    }
    var name = keyStr.value();
    for (var f : dataFields) {
      if (f.langName().equals(name)) return Opt.some(f.fieldType());
    }
    for (var m : methods) {
      if (m.langName().equals(name)) return Opt.some(m.funcType());
    }
    return Opt.none();
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public JavaDataDescriptor consumeDynamicJvmClass() {
    return jvmClassDesc;
  }

  @Override
  public EResult<Code> drop(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Code> dropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<AspectBind.BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }
}
