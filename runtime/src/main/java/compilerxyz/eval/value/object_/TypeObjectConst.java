package compilerxyz.eval.value.object_;

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
import compilerxyz.eval.Value;
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
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

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
    TypeId typeId,
    String typeDesc,
    Class externalHostClass,
    Opt<Type> deferDynamic,
    TList<Unique> identityAspects,
    TList<TypeObjectCastRule> castRules,
    TList<TypeObjectCastFromRule> castFromRules,
    Opt<TypeObjectLift> lift,
    Opt<ObjectAspectSource> aspectSource)
    implements
        Type,
        compilerxyz.eval.TypeAspect,
        compilerxyz.gen.AspectArtifact,
        compilerxyz.artifact.ArtifactExporter,
        AspectAspect,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectFunction,
        AspectInitialBind,
        AspectType,
        compilerxyz.gen.AspectLift {

  @Override
  public boolean equals(Object other) {
    return other instanceof TypeObjectConst o && typeId.equals(o.typeId);
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
  public Unique id() {
    if (!(aspectSource instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(typeDesc + "::id: not an aspect metatype");
    }
    if (!(some.value() instanceof ObjectAspectSource.Static staticSrc)) {
      throw new IllegalStateException(typeDesc + "::id: aspect id is not statically known");
    }
    return staticSrc.id();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectBind.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectType.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectFunction.ID)
        || key.equals(compilerxyz.gen.AspectArtifact.ID)) {
      return Opt.some(this);
    }
    if (key.equals(compilerxyz.gen.AspectPostDeserialize.ID)) {
      return Opt.some(compilerxyz.eval.aspect.PostDeserializeIdentity.INSTANCE);
    }
    if (aspectSource instanceof Opt.Some && key.equals(AspectAspect.ID)) {
      return Opt.some(this);
    }
    if (lift instanceof Opt.Some && key.equals(compilerxyz.gen.AspectLift.ID)) {
      return Opt.some(this);
    }
    for (var idAspect : identityAspects) {
      if (key.equals(idAspect)) return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public Unique aspectAspectGetUnique(InterfaceData value) {
    if (!(aspectSource instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new IllegalStateException(typeDesc + "::getUnique: no aspect source");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static staticSrc -> staticSrc.id();
      case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
    };
  }

  @Override
  public InterfaceData aspectAspectGetIfaceData(InterfaceData value) {
    if (!(aspectSource instanceof Opt.Some<ObjectAspectSource> some)) {
      throw new UnsupportedOperationException(typeDesc + "::getIfaceData: no aspect source");
    }
    return switch (some.value()) {
      case ObjectAspectSource.Static ignored ->
          throw new UnsupportedOperationException(
              typeDesc + "::getIfaceData: static aspect has no InterfaceData");
      case ObjectAspectSource.FromInterfaceData ignored -> value;
    };
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    for (var rule : castRules) if (rule.matches(xx, destType)) return true;
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    for (var rule : castRules) {
      if (rule.matches(xx, destType)) {
        CodeValue materialized = cv;
        if (cv.code().dynamic() == null) {
          var dynRes = aspectConsumeDynamicToDynamic(xx, loc, cv);
          if (dynRes.hasErrors()) return dynRes.reerr();
          materialized =
              new CodeValue(
                  Code.of(loc, dynRes.requireValue(typeDesc + "::typeCastTo materialize")),
                  cv.value());
        }
        return rule.apply(xx, null, loc, materialized, destType);
      }
    }
    return EResult.err(loc, "Cannot cast " + typeDesc + " to " + destType.typeDesc());
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    for (var rule : castFromRules) if (rule.matches(xx, sourceType)) return true;
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc + "::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    for (var rule : castFromRules) {
      if (rule.matches(xx, sourceType)) {
        // castFromRules were designed for TypeObjectDynamic; we pass null dest
        // since it's not a TypeObjectDynamic. RestampFromIdentity ignores dest
        // for the value shape but does use it for the stack type — pass this
        // instead.
        return rule.apply(xx, null, loc, cv, sourceType);
      }
    }
    return EResult.err(loc, "Cannot cast " + sourceType.typeDesc() + " to " + typeDesc);
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return JavaDataDescriptor.fromClass(externalHostClass);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn != null) return EResult.ok(dyn);
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc + "::toDynamic: no compile-time value");
    }
    Object hostValue = some.value();
    if (hostValue instanceof ValueObjectConst wrap) {
      hostValue = wrap.inner();
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
    return DynUtil.bindViaTarget(xx, loc, cv);
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
    return this;
  }

  @Override
  public EResult<compilerxyz.artifact.Artifact> artifactExporterExport(
      ExprContext xx, QExprId loc, compilerxyz.artifact.ExporterAny self) {
    if (!(self instanceof compilerxyz.eval.value.object_.ValueObjectConst wrapper)) {
      return EResult.err(
          loc, typeDesc + "::artifactExporterExport: expected ValueObjectConst, got " + self.getClass());
    }
    return compilerxyz.artifact.ArtifactSupport.exportArtifact(xx, loc, wrapper.inner());
  }

  @Override
  public EResult<compilerxyz.artifact.ExporterAny> artifactExporterImportFromData(
      ExprContext xx,
      QExprId loc,
      Opt<compilerxyz.artifact.ArtifactIdSource> id,
      compilerxyz.safejson.JsonValue data) {
    return EResult.err(loc, typeDesc + "::artifactExporterImportFromData: should not be invoked");
  }

  @Override
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
    if (!(lift instanceof Opt.Some<TypeObjectLift> some)) {
      return EResult.err(loc, typeDesc + "::lift: no lift impl registered");
    }
    return some.value().typeObjectLift(xx, loc, externalHostValue);
  }

  // Field / Function / Type: toDynamic + delegate to deferDynamic.

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var innerRes = extractInnerType(loc, self, "field");
    if (innerRes.hasErrors()) return innerRes.reerr();
    var inner = innerRes.requireValue(typeDesc + "::field inner");
    var asp = AspectField.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc + "::field inner AspectField").aspectFieldField(xx, loc, self, key);
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, compilerxyz.eval.ConstKey key) {
    return Opt.none();
  }

  @Override
  public TList<compilerxyz.eval.ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return new TList<>();
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var innerRes = extractInnerType(loc, self, "call");
    if (innerRes.hasErrors()) return innerRes.reerr();
    var inner = innerRes.requireValue(typeDesc + "::call inner");
    var asp = AspectFunction.get(xx, loc, inner);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc + "::call inner AspectFunction")
        .aspectFunctionCall(xx, loc, self, argument);
  }

  private EResult<Type> extractInnerType(QExprId loc, CodeValue self, String op) {
    if (!(self.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, typeDesc + "::" + op + ": self has no compile-time value");
    }
    var v = some.value();
    if (v instanceof compilerxyz.eval.value.object_.ValueObjectConst wrap) {
      return EResult.ok(wrap.inner());
    }
    if (v instanceof Type t) {
      return EResult.ok(t);
    }
    return EResult.err(
        loc,
        typeDesc + "::" + op + ": expected Type or ValueObjectConst, got " + v.getClass());
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) return Opt.none();
    if (!(some.value() instanceof AspectFunction asp)) return Opt.none();
    return asp.aspectFunctionExceptionType();
  }

  @Override
  public Type type_(Value value) {
    if (value instanceof compilerxyz.eval.value.object_.ValueObjectConst v) {
      return v.inner();
    }
    if (value instanceof Type t) {
      return t;
    }
    throw new IllegalArgumentException(
        typeDesc + "::type_: expected Type or ValueObjectConst, got " + value.getClass());
  }

  private EResult<CodeValue> deferField(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) {
      return EResult.err(loc, typeDesc + "::field: no deferDynamic configured");
    }
    var target = some.value();
    var dyn = materializeAsDynamic(xx, loc, self);
    if (dyn.hasErrors()) return dyn.reerr();
    var asp = AspectField.get(xx, loc, target);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc + "::field target AspectField")
        .aspectFieldField(xx, loc, dyn.requireValue(typeDesc + "::field dyn self"), key);
  }

  private EResult<CodeValue> deferCall(
      ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    if (!(deferDynamic instanceof Opt.Some<Type> some)) {
      return EResult.err(loc, typeDesc + "::call: no deferDynamic configured");
    }
    var target = some.value();
    var dyn = materializeAsDynamic(xx, loc, self);
    if (dyn.hasErrors()) return dyn.reerr();
    var asp = AspectFunction.get(xx, loc, target);
    if (asp.hasErrors()) return asp.reerr();
    return asp.requireValue(typeDesc + "::call target AspectFunction")
        .aspectFunctionCall(xx, loc, dyn.requireValue(typeDesc + "::call dyn self"), argument);
  }

  private EResult<CodeValue> materializeAsDynamic(ExprContext xx, QExprId loc, CodeValue self) {
    var dynRes = aspectConsumeDynamicToDynamic(xx, loc, self);
    if (dynRes.hasErrors()) return dynRes.reerr();
    var dyn = dynRes.requireValue(typeDesc + "::materializeAsDynamic");
    var target =
        deferDynamic instanceof Opt.Some<Type> some
            ? some.value()
            : this;
    return EResult.ok(
        new CodeValue(Code.of(loc, dyn), Opt.some(ValueDynamic.stack(target))));
  }

}
