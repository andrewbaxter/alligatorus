package compilerxyz.eval.value.object_;

import compilerxyz.annotations.BuiltinTypeId;

import compilerxyz.gen.Binding;
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
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.gen.AspectAspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectStackValueDynamic;
import org.jspecify.annotations.Nullable;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectAspect;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectStackValueDynamic;

@SuppressWarnings("rawtypes")
@BuiltinTypeId(7121L)
public record TypeObjectDynamic(
    TypeObjectMeta meta,
    JavaInternalName jvmClass,
    JavaDataDescriptor jvmClassDesc,
    Class hostClass,
    TList<ObjectDataField> dataFields,
    TList<ObjectMethod> methods,
    ObjectDropKind dropKind,
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
        AspectLift,
        AspectStackValueDynamic {

  public String typeDesc() {
    return meta.typeDesc();
  }

  public TList<Unique> trivialIdentityAspects() {
    return meta.trivialIdentityAspects();
  }

  public TList<TypeObjectCastRule> castRules() {
    return meta.castRules();
  }

  public TList<TypeObjectCastFromRule> castFromRules() {
    return meta.castFromRules();
  }

  public Opt<ObjectAspectSource> aspectSource() {
    return meta.aspectSource();
  }

  @Override
  public Unique id() {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(
          typeDesc() + "::id: TypeObjectDynamic is not an aspect metatype");
    }
    if (!(some.value() instanceof ObjectAspectSource.Static staticSrc)) {
      throw new IllegalStateException(
          typeDesc() + "::id: aspect id is not statically known");
    }
    return staticSrc.id();
  }

  public InterfaceData getInterfaceData() {
    if (!(interfaceRepresented instanceof Opt.Some<JavaInternalName> ir)) {
      throw new IllegalStateException(
          typeDesc() + "::getInterfaceData: does not represent an interface");
    }
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> aspSome)
        || !(aspSome.value() instanceof ObjectAspectSource.Static staticSrc)) {
      throw new IllegalStateException(
          typeDesc() + "::getInterfaceData: missing static aspect source");
    }
    var kind =
        compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry.kindForAspectId(
            staticSrc.id());
    var data = new InterfaceData(null, staticSrc.id(), kind);
    data.description = typeDesc();
    data.compiledBinaryName = ir.value().value().replace('/', '.');
    for (var m : methods) {
      if (!(m.funcType() instanceof compilerxyz.eval.value.function.TypeMethodDynamic mt)) {
        throw new IllegalStateException(
            typeDesc() + "::getInterfaceData: method " + m.langName()
                + " funcType is not TypeMethodDynamic");
      }
      var md = new compilerxyz.eval.value.source_interface.InterfaceMethodData(m.langName());
      md.argType = mt.arg();
      md.retType = mt.ret();
      md.hostMethodName = mt.methodName();
      md.hostMethodDescriptor = mt.descriptor();
      if (mt.arg() instanceof compilerxyz.eval.value.record.Record rec) {
        for (var key : rec.fields().keySet()) {
          if (key instanceof ConstKey.Str s) {
            md.dataFieldNames.add(s.value());
          } else {
            md.dataFieldNames.add(key.toCanonical());
          }
        }
      }
      data.methods.add(md);
    }
    return data;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TypeObjectDynamic o && meta.typeId().equals(o.meta.typeId());
  }

  @Override
  public int hashCode() {
    return meta.typeId().hashCode();
  }

  @Override
  public TypeId hasTypeIdTypeId() {
    return meta.typeId();
  }

                public Type valueType() {
    return this;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    if ((!dataFields.isEmpty() || !methods.isEmpty()) && key.equals(GenAspectField.AspectId)) {
      return Opt.some(this);
    }
    if (aspectSource() instanceof Opt.Some && key.equals(GenAspectAspect.AspectId)) {
      return Opt.some(this);
    }
    switch (dropKind) {
      case DYNAMIC -> {
        if (key.equals(GenAspectDropDynamic.AspectId)) return Opt.some(this);
      }
      case DYNAMIC_LIGHT -> {
        if (key.equals(GenAspectDropDynamicLight.AspectId)) return Opt.some(this);
      }
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (key.equals(idAspect)) return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public Unique aspectAspectGetUnique(InterfaceData value) {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(
          typeDesc() + "::getUnique: TypeObjectDynamic does not represent an aspect");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static staticSrc -> staticSrc.id();
      case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
    };
  }

  @Override
  public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new UnsupportedOperationException(
          typeDesc() + "::getIfaceData: TypeObjectDynamic has no aspect source");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static ignored ->
          throw new UnsupportedOperationException(
              typeDesc() + "::getIfaceData: static aspect has no InterfaceData");
      case ObjectAspectSource.FromInterfaceData ignored -> value;
    };
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    for (var rule : castFromRules()) {
      if (rule.matches(xx, sourceType)) return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    for (var rule : castFromRules()) {
      if (rule.matches(xx, sourceType)) {
        return rule.apply(xx, this, loc, cv, sourceType);
      }
    }
    return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    if (interfaceRepresented instanceof Opt.Some
        && destType.typeAspect(xx, compilerxyz.gen.GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
            .optIsSome()) {
      return true;
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (destType.typeAspect(xx, idAspect).optIsSome()) return true;
    }
    for (var rule : castRules()) {
      if (rule.matches(xx, destType)) return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (interfaceRepresented instanceof Opt.Some
        && destType.typeAspect(xx, compilerxyz.gen.GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
            .optIsSome()) {
      var sourceCode = cv.code().dynamic();
      if (sourceCode == null) {
        return EResult.err(
            loc,
            "cast " + typeDesc() + " → " + destType.typeDesc() + ": source has no runtime code");
      }
      var seq = new JavaBytecodeSequence();
      seq.add(sourceCode);
      seq.add(
          JavaBytecodeUtils.callMethod(
              loc,
              JavaInternalName.fromClass(TypeObjectDynamic.class),
              "getInterfaceData",
              compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromClass(InterfaceData.class), TList.of())));
      var call = new CodeDynamic(loc, seq);
      return EResult.ok(
          new CodeValue(Code.of(loc, call), Opt.some(ValueDynamic.stack(xx, destType))));
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (destType.typeAspect(xx, idAspect).optIsSome()) {
        return EResult.ok(cv);
      }
    }
    for (var rule : castRules()) {
      if (rule.matches(xx, destType)) {
        return rule.apply(xx, this, loc, cv, destType);
      }
    }
    return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, typeDesc() + "::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    if (!(ck instanceof ConstKey.Str ckStr)) {
      return EResult.err(loc, typeDesc() + ": unknown field " + ck.toCanonical());
    }
    var keyStr = ckStr.value();
    var dyn = self_.code().dynamic();
    if (dyn == null) {
      return EResult.err(loc, typeDesc() + ": self has no dynamic code");
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
                Opt.some(ValueDynamic.stack(xx, f.fieldType()))));
      }
    }
    for (var m : methods) {
      if (m.langName().equals(keyStr)) {
        return EResult.ok(
            new CodeValue(self_.code(), Opt.some(ValueDynamic.stack(xx, m.funcType()))));
      }
    }
    return EResult.err(loc, typeDesc() + ": unknown field '" + keyStr);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
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
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dynamicConsumeToDynamic(xx, loc, cv);
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return jvmClassDesc;
  }

  @Override
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
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
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, @Nullable Object externalHostValue) {
    var pairedConst = new TypeObjectConst(meta, hostClass, Opt.<Type>none());
    return EResult.ok(new ValueObjectConst(pairedConst, externalHostValue));
  }

  /**
   * Compile-time type of a value whose {@code data} is a TypeObjectDynamic
   * instance. Returned as a TypeObjectConst because TOD instances are only ever
   * wrapped in {@link ValueObjectConst}, which requires a const-side type.
   */
  public static Type constType() {
    return SelfConstType.INSTANCE;
  }

  private static final class SelfConstType {
    static final TypeObjectConst INSTANCE = build();

    private static TypeObjectConst build() {
      var trivialIdentityAspects = new TList<Unique>();
      trivialIdentityAspects.add(compilerxyz.gen.GenTrivialIdentityAspects.TYPE_IDENTITY);
      var castRules = new TList<TypeObjectCastRule>();
      castRules.add(new TypeObjectCastRule.RestampToIdentity(compilerxyz.gen.GenTrivialIdentityAspects.TYPE_IDENTITY));
      castRules.add(new TypeObjectCastRule.InvokeVirtualByDestIdentity(
          compilerxyz.gen.GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY,
          JavaInternalName.fromClass(TypeObjectDynamic.class),
          "getInterfaceData",
          compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
              JavaDataDescriptor.fromClass(InterfaceData.class),
              TList.of())));
      var meta = new TypeObjectMeta(
          compilerxyz.gen.GenTypeIds.TYPE_OBJECT_DYNAMIC_TYPE_ID,
          "type_object_dynamic",
          trivialIdentityAspects,
          castRules,
          new TList<TypeObjectCastFromRule>(),
          Opt.<ObjectAspectSource>none());
      return new TypeObjectConst(meta, Type.class, Opt.<Type>none());
    }
  }

}
