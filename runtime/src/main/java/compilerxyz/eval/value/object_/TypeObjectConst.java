package compilerxyz.eval.value.object_;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
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
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.source_interface.InterfaceData;
import compilerxyz.gen.AspectAspect;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectAspect;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;

/**
 * Compile-time-only sibling to {@link TypeObjectDynamic}. Values of a
 * TypeObjectConst are Java compile-time {@link Value}s. Materialization to
 * runtime happens via {@link #toDynamic}: it registers the value as an
 * external and emits a binding-use load; the resulting stack value is typed
 * as {@code deferDynamic} (if present).
 *
 * <p>Field / Function / Type aspects, when exposed, work by first
 * materializing self via toDynamic then dispatching to the corresponding
 * aspect on {@code deferDynamic}.
 */
@SuppressWarnings("rawtypes")
public record TypeObjectConst(
    TypeObjectMeta meta,
    Class externalHostClass,
    Opt<Type> deferDynamic)
    implements
        Type,
        compilerxyz.eval.TypeAspect,
        compilerxyz.gen.AspectArtifact,
        compilerxyz.artifact.ArtifactExporter,
        AspectAspect,
        AspectBind,
        compilerxyz.gen.AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectFunction,
        AspectInitialBind,
        compilerxyz.gen.AspectLift,
        AspectType {

  private static final java.util.concurrent.ConcurrentHashMap<TypeId, compilerxyz.artifact.ExporterRoot> META_EXPORTERS = new java.util.concurrent.ConcurrentHashMap<>();

  public static TypeObjectConst metaOf(Class<?> hostClass, TypeId hostBaseTypeId, String descSuffix, compilerxyz.artifact.ExporterRoot exporter) {
    var toc = metaOf(hostClass, hostBaseTypeId, descSuffix);
    META_EXPORTERS.put(toc.meta().typeId(), exporter);
    return toc;
  }

  public static TypeObjectConst metaOf(Class<?> hostClass, TypeId hostBaseTypeId, String descSuffix) {
    var trivialIdentityAspects = new TList<Unique>();
    trivialIdentityAspects.add(compilerxyz.gen.GenTrivialIdentityAspects.TYPE_IDENTITY);
    var castRules = new TList<TypeObjectCastRule>();
    castRules.add(new TypeObjectCastRule.RestampToIdentity(compilerxyz.gen.GenTrivialIdentityAspects.TYPE_IDENTITY));
    var meta = new TypeObjectMeta(hostBaseTypeId, "type<" + descSuffix + ">", trivialIdentityAspects, castRules, new TList<TypeObjectCastFromRule>(), Opt.<ObjectAspectSource>none());
    return new TypeObjectConst(
        meta,
        hostClass,
        Opt.<Type>none());
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TypeObjectConst o && meta.typeId().equals(o.meta.typeId());
  }

  @Override
  public int hashCode() {
    return meta.typeId().hashCode();
  }

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
  public TypeId hasTypeIdTypeId() {
    return meta.typeId();
  }

                public Type valueType() {
    return this;
  }

  @Override
  public Unique id() {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(typeDesc() + "::id: not an aspect metatype");
    }
    if (!(some.value() instanceof ObjectAspectSource.Static staticSrc)) {
      throw new IllegalStateException(typeDesc() + "::id: aspect id is not statically known");
    }
    return staticSrc.id();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenAspectBind.AspectId)
        || key.equals(compilerxyz.gen.GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectType.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectLift.AspectId)
        || key.equals(compilerxyz.gen.GenAspectArtifact.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.GenAspectPostDeserialize.AspectId)) {
      return Opt.some(compilerxyz.eval.aspect.PostDeserializeIdentity.INSTANCE);
    }
    if (aspectSource() instanceof Opt.Some && key.equals(GenAspectAspect.AspectId)) {
      return Opt.some(this);
    }
    for (var idAspect : trivialIdentityAspects()) {
      if (key.equals(idAspect)) return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public Unique aspectAspectGetUnique(InterfaceData value) {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(typeDesc() + "::getUnique: no aspect source");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static staticSrc -> staticSrc.id();
      case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
    };
  }

  @Override
  public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
    if (!(aspectSource() instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new UnsupportedOperationException(typeDesc() + "::getIfaceData: no aspect source");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static ignored ->
          throw new UnsupportedOperationException(
              typeDesc() + "::getIfaceData: static aspect has no InterfaceData");
      case ObjectAspectSource.FromInterfaceData ignored -> value;
    };
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    for (var rule : castRules()) if (rule.matches(xx, destType)) return true;
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    for (var rule : castRules()) {
      if (rule.matches(xx, destType)) {
        CodeValue materialized = cv;
        if (cv.code().dynamic() == null) {
          var dynRes = aspectConsumeDynamicToDynamic(xx, loc, cv);
          if (dynRes.hasErrors()) return dynRes.reerr();
          materialized =
              new CodeValue(
                  Code.of(loc, dynRes.requireValue(typeDesc() + "::typeCastTo materialize")),
                  cv.outcome());
        }
        return rule.apply(xx, null, loc, materialized, destType);
      }
    }
    return EResult.err(loc, "Cannot cast " + typeDesc() + " to " + destType.typeDesc());
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    for (var rule : castFromRules()) if (rule.matches(xx, sourceType)) return true;
    return false;
  }

  @Override
  public boolean typeIsEqual(compilerxyz.eval.Type other) {
    return other instanceof TypeObjectConst o && meta.typeId().equals(o.meta.typeId());
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc() + "::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    for (var rule : castFromRules()) {
      if (rule.matches(xx, sourceType)) {
        // castFromRules() were designed for TypeObjectDynamic; we pass null dest
        // since it's not a TypeObjectDynamic. RestampFromIdentity ignores dest
        // for the value shape but does use it for the stack type — pass this
        // instead.
        return rule.apply(xx, null, loc, cv, sourceType);
      }
    }
    return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc());
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return JavaDataDescriptor.fromClass(externalHostClass);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc() + "::toDynamic: no compile-time value");
    }
    Object hostValue = some.value();
    if (hostValue instanceof ValueObjectConst wrap) {
      hostValue = wrap.data();
    }
    var key = new JavaBytecodeBindingKey();
    var id = new BindingId.Local(key);
    xx.vx.externals.add(
        new EvalContext.ExternalEntry(
            key, this, new External(externalHostClass, hostValue)));
    return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, id));
  }

  @Override
  public EResult<AspectBind.BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value> some) {
      return EResult.ok(new AspectBind.BindResult(
          Code.none(loc), new compilerxyz.eval.binding.BindingValue(some.value())));
    }
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<compilerxyz.eval.dynamic.target.TargetBindSub>> aspectBindDynamicBindDynamic(
      ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value() instanceof Opt.Some<Value>) {
      return EResult.ok(Opt.none());
    }
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
  public compilerxyz.artifact.ArtifactExporter aspectArtifactExporter() {
    var metaExporter = META_EXPORTERS.get(meta.typeId());
    return metaExporter != null ? metaExporter : this;
  }

  @Override
  public EResult<compilerxyz.artifact.Artifact> artifactExporterExport(
      ExprContext xx, QExprId loc, compilerxyz.artifact.ExporterAny self) {
    // If self is a ValueObjectConst wrapping a Type (from ofType), delegate to the wrapped
    // Type's own exportArtifact — which routes to its specific exporter (e.g.
    // SOURCE_OBJECT_CONST for TypeTypeSourceObjectConst).
    if (self instanceof compilerxyz.eval.value.object_.ValueObjectConst wrapper
        && wrapper.data() instanceof Type innerType) {
      return innerType.exportArtifact(xx, loc);
    }
    if (self instanceof compilerxyz.gen.AspectArtifact concrete && concrete != this) {
      return concrete.aspectArtifactExporter().artifactExporterExport(xx, loc, self);
    }
    return EResult.err(
        loc, typeDesc() + "::artifactExporterExport: expected ValueObjectConst-of-Type or AspectArtifact, got " + self.getClass());
  }

  @Override
  public EResult<compilerxyz.artifact.ExporterAny> artifactExporterImportFromData(
      ExprContext xx,
      QExprId loc,
      Opt<compilerxyz.artifact.ArtifactIdSource> id,
      compilerxyz.safejson.JsonValue data) {
    return EResult.err(loc, typeDesc() + "::artifactExporterImportFromData: should not be invoked");
  }


  // Field / Function / Type: toDynamic + delegate to deferDynamic.

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var innerRes = extractInnerType(loc, self, "field");
    if (innerRes.hasErrors()) return innerRes.reerr();
    var inner = innerRes.requireValue(typeDesc() + "::field inner");
    if (key.value() instanceof Opt.Some<Value> keySome) {
      var ckRes = compilerxyz.eval.Eval.extractConstKey(xx, loc, keySome.value());
      if (ckRes instanceof compilerxyz.util.Result.Ok<compilerxyz.eval.ConstKey, String> ok
          && compilerxyz.gen.GenType.Holder.INSTANCE.aspectFieldTypeOpt(xx, ok.value()).optIsSome()) {
        CodeValue selfLifted = self;
        if (self.code().isNone()) {
          var liftRes = aspectConsumeDynamicToDynamic(xx, loc, self);
          if (liftRes.hasErrors()) return liftRes.reerr();
          selfLifted =
              new CodeValue(
                  Code.of(loc, liftRes.requireValue(typeDesc() + "::field lift")),
                  self.outcome());
        }
        return compilerxyz.gen.GenType.Holder.INSTANCE.aspectFieldField(xx, loc, selfLifted, key);
      }
    }
    var asp = GenAspectField.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc() + "::field inner AspectField").aspectFieldField(xx, loc, self, key);
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, compilerxyz.eval.ConstKey key) {
    return Opt.none();
  }

  @Override
  public TList<compilerxyz.eval.ConstKey> aspectFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var innerRes = extractInnerType(loc, self, "call");
    if (innerRes.hasErrors()) return innerRes.reerr();
    var inner = innerRes.requireValue(typeDesc() + "::call inner");
    var asp = GenAspectFunction.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc() + "::call inner AspectFunction")
        .aspectFunctionCall(xx, loc, self, argument);
  }

  private EResult<Type> extractInnerType(QExprId loc, CodeValue self, String op) {
    if (!(self.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc() + "::" + op + ": self has no compile-time value");
    }
    var v = some.value();
    if (v instanceof compilerxyz.eval.value.object_.ValueObjectConst wrap) {
      return EResult.ok((Type) wrap.data());
    }
    if (v instanceof Type t) {
      return EResult.ok(t);
    }
    return EResult.err(
        loc,
        typeDesc() + "::" + op + ": expected Type or ValueObjectConst, got " + v.getClass());
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) return Opt.none();
    if (!(some.value() instanceof AspectFunction asp)) return Opt.none();
    return asp.aspectFunctionExceptionType();
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst v) {
      return (Type) v.data();
    }
    if (value instanceof Type t) {
      return t;
    }
    throw new IllegalArgumentException(
        typeDesc() + "::type_: expected Type or ValueObjectConst, got " + value.getClass());
  }

  private EResult<CodeValue> deferField(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) {
      return EResult.err(loc, typeDesc() + "::field: no deferDynamic configured");
    }
    var target = some.value();
    var dyn = materializeAsDynamic(xx, loc, self);
    if (dyn.hasErrors()) return dyn.reerr();
    var asp = GenAspectField.get(xx, loc, target);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc() + "::field target AspectField")
        .aspectFieldField(xx, loc, dyn.requireValue(typeDesc() + "::field dyn self"), key);
  }

  private EResult<CodeValue> deferCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) {
      return EResult.err(loc, typeDesc() + "::call: no deferDynamic configured");
    }
    var target = some.value();
    var dyn = materializeAsDynamic(xx, loc, self);
    if (dyn.hasErrors()) return dyn.reerr();
    var asp = GenAspectFunction.get(xx, loc, target);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc() + "::call target AspectFunction")
        .aspectFunctionCall(xx, loc, dyn.requireValue(typeDesc() + "::call dyn self"), argument);
  }

  private EResult<CodeValue> materializeAsDynamic(ExprContext xx, QExprId loc, CodeValue self) {
    var dynRes = aspectConsumeDynamicToDynamic(xx, loc, self);
    if (dynRes.hasErrors()) return dynRes.reerr();
    var dyn = dynRes.requireValue(typeDesc() + "::materializeAsDynamic");
    var target =
        deferDynamic instanceof Opt.Some<Type> some
            ? some.value()
            : this;
    return EResult.ok(
        new CodeValue(Code.of(loc, dyn), EvalOutcome.ofValue(ValueDynamic.stack(xx, target))));
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    return EResult.ok(new ValueObjectConst(this, externalHostValue));
  }

}
