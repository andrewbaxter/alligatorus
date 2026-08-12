package compilerxyz.eval.value.object_;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.AspectArtifact;
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
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.gen.AspectAspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
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
import compilerxyz.gen.GenAspectArtifact;
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
import compilerxyz.eval.TypeAspect;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.function.TypeMethodDynamic;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.source_interface.InterfaceMethodData;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueArray;
import compilerxyz.safejson.JsonValueBoolean;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValueNumber;
import compilerxyz.safejson.JsonValueObject;
import compilerxyz.safejson.JsonValueString;

@SuppressWarnings("rawtypes")
@BuiltinLight(uniqueId = 6130L)
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
        AspectFieldUseDynamic,
        TypeAspect,
        AspectArtifact,
        AspectAspect,
        AspectField,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectDropDynamicLight,
        AspectBind,
        AspectBindDynamic,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectLift,
        AspectStackValueDynamic,
        ExporterAny {

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
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY, TypeObjectDynamic.class);
    return oOpt.optIsSome()
        && hostClass == oOpt.optUnwrap().hostClass
        && meta.typeDesc().equals(oOpt.optUnwrap().meta.typeDesc());
  }

  @Override
  public Unique typeAspectId() {
    var aspSrcOpt = aspectSource();
    if (aspSrcOpt.optIsNone()) {
      throw new IllegalStateException(
          typeDesc() + "::id: TypeObjectDynamic is not an aspect metatype");
    }
    ObjectAspectSource.Static staticSrc;
    switch (aspSrcOpt.optUnwrap()) {
      case ObjectAspectSource.Static s -> staticSrc = s;
      case ObjectAspectSource.FromInterfaceData ignored -> throw new IllegalStateException(
          typeDesc() + "::id: aspect id is not statically known");
    }
    return staticSrc.id();
  }

  public InterfaceData getInterfaceData() {
    if (interfaceRepresented.optIsNone()) {
      throw new IllegalStateException(
          typeDesc() + "::getInterfaceData: does not represent an interface");
    }
    var irValue = interfaceRepresented.optUnwrap();
    var aspSrcOpt2 = aspectSource();
    if (aspSrcOpt2.optIsNone()) {
      throw new IllegalStateException(
          typeDesc() + "::getInterfaceData: missing static aspect source");
    }
    ObjectAspectSource.Static staticSrc2;
    switch (aspSrcOpt2.optUnwrap()) {
      case ObjectAspectSource.Static s -> staticSrc2 = s;
      case ObjectAspectSource.FromInterfaceData ignored -> throw new IllegalStateException(
          typeDesc() + "::getInterfaceData: missing static aspect source");
    }
    var kind =
        compilerxyz.eval.value.source_interface.BuiltinInterfaceRegistry.kindForAspectId(
            staticSrc2.id());
    var data = new InterfaceData(null, staticSrc2.identity(), kind);
    data.aspectId = Opt.some(staticSrc2.id());
    data.isAspect = !staticSrc2.identity().equals(staticSrc2.id());
    data.description = Opt.some(typeDesc());
    data.compiledBinaryName = irValue.value().replace('/', '.');
    for (var m : methods) {
      if (!(m.funcType() instanceof TypeMethodDynamic mt)) {
        throw new IllegalStateException(
            typeDesc() + "::getInterfaceData: method " + m.langName()
                + " funcType is not TypeMethodDynamic");
      }
      var md = new InterfaceMethodData(m.langName());
      md.argType = mt.arg();
      md.retType = mt.ret();
      md.hostMethodName = compilerxyz.util.Opt.some(mt.methodName());
      md.hostMethodDescriptor = compilerxyz.util.Opt.some(mt.descriptor());
      if (mt.arg() instanceof Record rec) {
        for (var key : rec.fields().keySet()) {
          switch (key) {
            case ConstKey.Str s -> md.dataFieldNames.add(s.value());
            case ConstKey.Uint u -> md.dataFieldNames.add(key.constKeyToCanonical());
          }
        }
      }
      data.methods.add(md);
    }
    return data;
  }


  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.OBJECT_DYNAMIC;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            java.util.function.Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeObjectDynamic obj)) {
            return EResult.err(
                loc,
                "TypeObjectDynamic::exporterRootDelegateSelfExport: expected TypeObjectDynamic,"
                    + " got "
                    + self.getClass());
          }
          return EResult.ok(
              new Artifact.Body(
                  makeBody.apply(
                      new JsonValueString(obj.hostClass.getName()))));
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx,
            QExprId loc,
            Opt<ArtifactIdSource> id,
            JsonValue data) {
          JsonValueString s;
          switch (data) {
            case JsonValueString js -> s = js;
            case JsonValueNull ignored -> { return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data); }
            case JsonValueBoolean ignored -> { return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data); }
            case JsonValueNumber ignored -> { return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data); }
            case JsonValueArray ignored -> { return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data); }
            case JsonValueObject ignored -> { return EResult.err(loc, "TypeObjectDynamic::import: expected host class name string, got " + data); }
          }
          var found = compilerxyz.gen.GenDynamicTypes.byClassName(s.value());
          if (found.optIsNone()) {
            return EResult.err(
                loc,
                "TypeObjectDynamic::import: no registered dynamic type for host class "
                    + s.value());
          }
          return EResult.ok(compilerxyz.util.MustCast.mustCast(ExporterAny.class, found.optUnwrap()));
        }
      };

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_OBJECT_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)
        || key.equals(compilerxyz.gen.GenAspectFieldUseDynamic.AspectId)) {
      return Opt.some(this);
    }
    if ((!dataFields.isEmpty() || !methods.isEmpty()) && key.equals(GenAspectField.AspectId)) {
      return Opt.some(this);
    }
    if (aspectSource().optIsSome() && key.equals(GenAspectAspect.AspectId)) {
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
  public compilerxyz.eval.EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx,
      QExprId loc,
      ValueDynamicSource source) {
    return compilerxyz.eval.EResult.ok(
        new CodeValue(
            compilerxyz.eval.Code.of(
                loc, compilerxyz.eval.dynamic.DynUtil.renderSource(xx, loc, source)),
            compilerxyz.eval.EvalOutcome.ofValue(
                new ValueDynamic(source, this))));
  }

  @Override
  public Unique aspectAspectGetUnique(InterfaceData value) {
    var aspSrcOpt3 = aspectSource();
    if (aspSrcOpt3.optIsNone()) {
      throw new IllegalStateException(
          typeDesc() + "::getUnique: TypeObjectDynamic does not represent an aspect");
    }
    return switch (aspSrcOpt3.optUnwrap()) {
      case ObjectAspectSource.Static staticSrc -> staticSrc.id();
      case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
    };
  }

  @Override
  public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
    var aspSrcOpt4 = aspectSource();
    if (aspSrcOpt4.optIsNone()) {
      throw new UnsupportedOperationException(
          typeDesc() + "::getIfaceData: TypeObjectDynamic has no aspect source");
    }
    return switch (aspSrcOpt4.optUnwrap()) {
      case ObjectAspectSource.Static ignored ->
          throw new UnsupportedOperationException(
              typeDesc() + "::getIfaceData: static aspect has no InterfaceData");
      case ObjectAspectSource.FromInterfaceData ignored -> value;
    };
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    for (var rule : castFromRules()) {
      if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
    }
    var sourceType = cv.value().optUnwrap().valueType();
    for (var rule : castFromRules()) {
      if (rule.typeObjectCastFromRuleMatches(xx, sourceType)) {
        return rule.typeObjectCastFromRuleApply(xx, this, loc, cv, sourceType);
      }
    }
    return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    if (interfaceRepresented.optIsSome()
        && destType.typeAspect(xx, compilerxyz.gen.GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
            .optIsSome()) {
      return true;
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (destType.typeAspect(xx, idAspect).optIsSome()) return true;
    }
    for (var rule : castRules()) {
      if (rule.typeObjectCastRuleMatches(xx, destType)) return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (interfaceRepresented.optIsSome()
        && destType.typeAspect(xx, compilerxyz.gen.GenTrivialIdentityAspects.INTERFACE_DATA_IDENTITY)
            .optIsSome()) {
      var sourceCode = cv.code().codeDynamic();
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
          new CodeValue(Code.of(loc, call), EvalOutcome.ofValue(ValueDynamic.stack(destType, 1))));
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (destType.typeAspect(xx, idAspect).optIsSome()) {
        return EResult.ok(cv);
      }
    }
    for (var rule : castRules()) {
      if (rule.typeObjectCastRuleMatches(xx, destType)) {
        return rule.typeObjectCastRuleApply(xx, this, loc, cv, destType);
      }
    }
    return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, typeDesc() + "::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    var ck = ckOpt.optUnwrap();
    ConstKey.Str ckStr;
    switch (ck) {
      case ConstKey.Str s -> ckStr = s;
      case ConstKey.Uint u -> { return EResult.err(loc, typeDesc() + ": unknown field " + ck.constKeyToCanonical()); }
    }
    var keyStr = ckStr.value();
    var dyn = self_.code().codeDynamic();
    if (dyn == null) {
      return EResult.err(loc, typeDesc() + ": self has no dynamic code");
    }
    for (var f : dataFields) {
      if (f.langName().equals(keyStr)) {
        var loadSeq = new JavaBytecodeSequence();
        loadSeq.add(dyn);
        switch (f.access()) {
          case ObjectDataField.Access.FieldGet fg ->
              loadSeq.add(
                  JavaBytecodeUtils.accessField(loc, jvmClass, fg.jvmFieldName(), fg.jvmFieldDesc()));
          case ObjectDataField.Access.MethodCall mc ->
              loadSeq.add(
                  JavaBytecodeUtils.callMethod(loc, jvmClass, mc.jvmMethodName(), mc.jvmMethodDesc()));
        }
        return EResult.ok(
            new CodeValue(
                Code.of(loc, new CodeDynamic(loc, loadSeq)),
                EvalOutcome.ofValue(ValueDynamic.stack(f.fieldType(), 1))));
      }
    }
    for (var m : methods) {
      if (m.langName().equals(keyStr)) {
        return EResult.ok(
            new CodeValue(self_.code(), EvalOutcome.ofValue(ValueDynamic.stack(m.funcType(), 1))));
      }
    }
    return EResult.err(loc, typeDesc() + ": unknown field '" + keyStr);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    ConstKey.Str keyStr;
    switch (key) {
      case ConstKey.Str s -> keyStr = s;
      case ConstKey.Uint u -> { return Opt.none(); }
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
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
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
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this, 1);
  }

  @Override
  public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
    var pairedConst = new TypeObjectConst(meta, hostClass, Opt.<Type>none(), Opt.<ExporterRoot>none());
    return EResult.ok(new ValueObjectConst(pairedConst, externalHostValue.optUnwrap()));
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
      var meta = new TypeObjectMeta("type_object_dynamic",
          trivialIdentityAspects,
          castRules,
          new TList<TypeObjectCastFromRule>(),
          Opt.<ObjectAspectSource>none());
      return new TypeObjectConst(meta, Type.class, Opt.<Type>none(), Opt.<ExporterRoot>none());
    }
  }

}
