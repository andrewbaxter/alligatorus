package compilerxyz.eval.value.map;

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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
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
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeMapEntryDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectInitialBind,
        AspectElementUseDynamicBind,
        ExporterAny {

  @Override
  public TypeId typeId() { return new TypeId.Composite(GenTypeIds.TYPE_MAP_ENTRY_DYNAMIC_TYPE_ID, TList.of(keyType.typeId(), valueType.typeId())); }


  private static final JavaInternalName MAP_ENTRY_INAME =
      JavaInternalName.of("java/util/Map$Entry");

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
    if (!(cv.value() instanceof compilerxyz.util.Opt.Some<compilerxyz.eval.Value> some)) {
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JavaInternalName.of("java/util/Map$Entry"));
  }

  @Override
  public String typeDesc() {
    return "map-entry(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.MapEntryDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectElementUseDynamicBind.ID)) {
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
    var fieldConsumeOpt = AspectConsumeDynamic.getMaybe(xx, spec.type());
    if (fieldConsumeOpt instanceof Opt.Some<AspectConsumeDynamic> fieldConsumeSome) {
      var fieldDesc = fieldConsumeSome.value().aspectConsumeDynamicConsumeDynamicJvmClass();
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
            Opt.some(ValueDynamic.stack(spec.type()))));
  }

  @Override
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
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
    return EResult.ok(this);
  }

  @Override
  public compilerxyz.gen.Binding aspectElementUseDynamicBindElementUseDynamicBind(compilerxyz.eval.BindingId dynamicKey) {
    return aspectInitialBindInitialBind(dynamicKey);
  }

}
