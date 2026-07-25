package compilerxyz.eval.value.map;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;

import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.Map;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;

@BuiltinIdentity(uniqueId = 6123L)
public record TypeMapEntryDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectInitialBind,
        AspectElementUseDynamicBind,
        ExporterAny {


  private static final JavaInternalName MAP_ENTRY_INAME =
      JavaInternalName.fromClass(Map.Entry.class);

  private record FieldSpec(Type type, String jvmGetter) {}

  private TMap<ConstKey, FieldSpec> fieldSpecs() {
    return TMap.ofEntries(
        new Pair<>(new ConstKey.Str("key"), new FieldSpec(keyType, "getKey")),
        new Pair<>(new ConstKey.Str("value"), new FieldSpec(valueType, "getValue")));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> aspectDropDynamicDrop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    // Map entry has exactly two child slots (key, value) accessed through
    // Map.Entry.getKey / getValue. In practice both slots are just GC-managed
    // Object references, so the base POP suffices, but the destructuring
    // policy is emitted for correctness when a child type introduces its own
    // drop semantics.
    if (!(cv.value() instanceof compilerxyz.util.Opt.Some<compilerxyz.gen.Value> some)) {
      throw new IllegalStateException("TypeMapEntryDynamic::drop: called on absent CodeValue");
    }
    if (!(some.value() instanceof compilerxyz.eval.value.ValueDynamic vd)) {
      throw new IllegalStateException(
          "TypeMapEntryDynamic::drop: expected ValueDynamic, got " + some.value().getClass());
    }
    var descObjNoArgs =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromClass(Object.class), TList.of());
    var children =
        new compilerxyz.util.TList<compilerxyz.eval.dynamic.DynUtil.DropChild>(2);
    children.add(
        new compilerxyz.eval.dynamic.DynUtil.DropChild(
            keyType,
            JavaBytecodeUtils.callInterfaceMethod(loc, MAP_ENTRY_INAME, "getKey", descObjNoArgs)));
    children.add(
        new compilerxyz.eval.dynamic.DynUtil.DropChild(
            valueType,
            JavaBytecodeUtils.callInterfaceMethod(loc, MAP_ENTRY_INAME, "getValue", descObjNoArgs)));
    return compilerxyz.eval.dynamic.DynUtil.dropDestructiveComposite(xx, loc, vd.source(), children);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JavaInternalName.fromClass(Map.Entry.class));
  }

  @Override
  public String typeDesc() {
    return "map-entry(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeMapEntryDynamic.class, "map_entry_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_ENTRY_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamic.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeMapEntryDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var spec = fieldSpecs().getOrNull(ck);
    if (spec == null) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on map entry");
    }
    var selfDyn = self.code().dynamic();
    if (selfDyn == null) {
      return EResult.err(loc, "TypeMapEntryDynamic::field: receiver has no runtime code");
    }
    var seq = new JavaBytecodeSequence();
    seq.add(selfDyn);
    seq.add(
        JavaBytecodeUtils.callInterfaceMethod(
            loc,
            MAP_ENTRY_INAME,
            spec.jvmGetter(),
            JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.fromClass(Object.class), TList.of())));
    // Iterator.next() and Map.Entry.getKey/getValue return Object; cast to the
    // field type's declared JVM class so the stack shape matches the language
    // type. Container element types are already box-substituted at construction
    // time, so the target descriptor is always object-shaped here.
    var fieldConsumeOpt = GenAspectConsumeDynamic.getMaybe(xx, spec.type());
    if (fieldConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> fieldConsumeSome) {
      var fieldDesc = fieldConsumeSome.value().aspectConsumeDynamicJvmClass(xx);
      var d = fieldDesc.value();
      if (!d.equals(compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT.value())) {
        char c = d.charAt(0);
        if (c == 'L' || c == '[') {
          seq.add(JavaBytecodeUtils.cast(loc, fieldDesc));
        }
      }
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, spec.type()))));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs().getOrNull(key);
    return spec == null ? Opt.none() : Opt.some(spec.type());
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeMapEntryDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
  }

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return aspectInitialBindInitialBind(dynamicKey);
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_MAP_ENTRY_DYNAMIC_IDENTITY, TypeMapEntryDynamic.class) instanceof Opt.Some<TypeMapEntryDynamic> oSome
        && keyType.typeIsEqual(xx, oSome.value().keyType) && valueType.typeIsEqual(xx, oSome.value().valueType);
  }

  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
