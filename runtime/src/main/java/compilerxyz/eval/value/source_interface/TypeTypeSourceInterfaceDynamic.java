package compilerxyz.eval.value.source_interface;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenCodegenDescriptors;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.GenAspectLift;

/**
 * The type-of-type for {@link TypeSourceInterfaceDynamic}: the {@code Type}
 * carried by a value that holds an interface type (mirrors {@link
 * compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst} for objects).
 *
 * <p>{@code type_} is empty for the generic descriptor returned by {@link
 * #dynamicType()} (the field type of a {@code TypeSourceInterfaceDynamic}, which
 * is not tied to a specific interface), and present for a specific interface
 * type-value. The interface-value casts (to a generic {@code Type} by restamp,
 * or to {@code InterfaceData} by invoking {@code getInterfaceData()}) are
 * data-independent, so they are available in both cases — expressed here as real
 * methods rather than {@code TypeObjectConst} cast-rule data.
 */
@BuiltinLight(uniqueId = 6196L)
public record TypeTypeSourceInterfaceDynamic(Opt<TypeSourceInterfaceDynamic> type_)
    implements
        Type,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectLift,
        AspectType,
        ExporterAny {

  /**
   * The generic (interface-agnostic) instance — the descriptor a plain {@link
   * TypeSourceInterfaceDynamic} value carries (see {@code
   * TypeSourceInterfaceDynamic.dynamicType()}). A specific interface type-value
   * uses {@code Opt.some(..)} instead.
   */
  public static final TypeTypeSourceInterfaceDynamic GENERIC =
      new TypeTypeSourceInterfaceDynamic(Opt.none());

  private TypeSourceInterfaceDynamic require(String op) {
    if (type_.optIsSome()) return type_.optUnwrap();
    throw new IllegalStateException(
        "TypeTypeSourceInterfaceDynamic::" + op + " on the generic (interface-agnostic) descriptor");
  }

  @Override
  public String typeDesc() {
    if (type_.optIsSome()) return type_.optUnwrap().typeDesc();
    return "interface";
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY)
        || key.equals(GenTrivialIdentityAspects.TYPE_IDENTITY)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectType.AspectId)) {
      return Opt.some(this);
    }
    if (type_.optIsSome() && key.equals(type_.optUnwrap().data().identity)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY).optIsSome()
        || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome();
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
      return EResult.ok(
          new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
    if (destType.typeAspect(xx, GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY).optIsSome()) {
      var sourceCode = cv.code().codeDynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc, "cast interface → interface_data: source has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(sourceCode);
      seq.add(
          JavaBytecodeUtils.callMethod(
              loc, GenCodegenDescriptors.TYPE_SOURCE_INTERFACE_DYNAMIC_GET_INTERFACE_DATA));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, seq)),
              EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
    return EResult.err(loc, "Cannot cast interface to " + destType.typeDesc());
  }

  @Override
  public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
    var host = (TypeSourceInterfaceDynamic) externalHostValue.optUnwrap();
    return EResult.ok(
        new ValueObjectConst(
            new TypeTypeSourceInterfaceDynamic(Opt.some(host)), host));
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromClass(TypeSourceInterfaceDynamic.class);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(loc, "TypeTypeSourceInterfaceDynamic::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var type = require("aspectFieldField");
    if (key.value().optIsNone()) {
      return EResult.err(
          loc, "TypeTypeSourceInterfaceDynamic::field: key has no compile-time value");
    }
    var ckRes = Eval.extractConstKey(xx, loc, key.value().optUnwrap());
    ConstKey ck;
    switch (ckRes) {
      case Result.Ok<ConstKey, String> ok -> ck = ok.value();
      case Result.Err<ConstKey, String> e -> { return EResult.err(loc, e.error()); }
    }
    ConstKey.Str keyStr;
    switch (ck) {
      case ConstKey.Str s -> keyStr = s;
      case ConstKey.Uint u -> {
        return EResult.err(
            loc, "Method " + ck.constKeyToCanonical() + " not found on interface");
      }
    }
    var keyName = keyStr.value();
    InterfaceMethodData match = null;
    for (var m : type.data().methods) {
      if (m.name.equals(keyName)) {
        match = m;
        break;
      }
    }
    if (match == null) {
      return EResult.err(loc, "Method '" + keyName + "' not found on interface");
    }
    var refType = new TypeInterfaceMethodDynamic(type.data(), match);
    Code carry = self.code();
    if (carry.codeDynamic() != null) {
      var dropRes = DynUtil.dropDefault(xx, loc, self);
      if (dropRes.hasErrors()) return dropRes.reerr();
      var seq = new JavaBytecodeSequence();
      seq.add(carry.codeDynamic());
      var dropDyn = dropRes.requireValue("TypeTypeSourceInterfaceDynamic::field drop").codeDynamic();
      if (dropDyn != null) seq.add(dropDyn);
      carry = Code.of(loc, new CodeDynamic(loc, seq));
    }
    return EResult.ok(
        new CodeValue(carry, EvalOutcome.ofValue(new ValueObjectConst(refType, refType))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    if (type_.optIsNone()) {
      return Opt.none();
    }
    ConstKey.Str s;
    switch (key) {
      case ConstKey.Str ks -> s = ks;
      case ConstKey.Uint u -> { return Opt.none(); }
    }
    for (var m : type_.optUnwrap().data().methods) {
      if (m.name.equals(s.value())) {
        return Opt.some(new TypeInterfaceMethodDynamic(type_.optUnwrap().data(), m));
      }
    }
    return Opt.none();
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeTypeSourceInterfaceDynamic(type_));
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (value instanceof ValueObjectConst voc && voc.data() instanceof Type t) {
      return t;
    }
    return require("aspectTypeType");
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
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(
        getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(
        xx,
        other,
        GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_INTERFACE_DYNAMIC_IDENTITY,
        TypeTypeSourceInterfaceDynamic.class);
    if (oOpt.optIsNone()) {
      return false;
    }
    if (type_.optIsSome() && oOpt.optUnwrap().type_.optIsSome()) {
      return type_.optUnwrap().typeIsEqual(xx, oOpt.optUnwrap().type_.optUnwrap());
    }
    return type_.optIsNone() && oOpt.optUnwrap().type_.optIsNone();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
}
