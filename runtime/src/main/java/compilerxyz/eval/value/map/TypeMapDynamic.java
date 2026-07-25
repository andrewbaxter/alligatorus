package compilerxyz.eval.value.map;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
import compilerxyz.eval.EvalOutcome;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import org.objectweb.asm.Opcodes;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.int_.TypeIntConst;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.string.TypeStringConst;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectIterable;
import compilerxyz.gen.GenAspectStackValueDynamic;

@BuiltinIdentity(uniqueId = 6122L)
public record TypeMapDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectDropDynamicLight,
        AspectField,
        AspectInitialBind,
        AspectIterable,
        AspectStackValueDynamic,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(compilerxyz.util.TOrderedMap.class);
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeMapDynamic.class, "map_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectDropDynamic.AspectId)) {
      return GenAspectDropDynamic.getMaybe(xx, keyType).optIsSome()
              || GenAspectDropDynamic.getMaybe(xx, valueType).optIsSome()
          ? Opt.some(this)
          : Opt.none();
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectIterable.AspectId)
        || key.equals(GenAspectStackValueDynamic.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.MAP_DYNAMIC;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          return EResult.err(
              loc,
              "TypeMapDynamic::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port"
                  + " deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeMapDynamic::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact"
                  + " (eval/artifact.rs) port deferred");
        }
      };

  private sealed interface FieldSpec {
    Type fieldType();

    record Factory(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }

    record GetFn(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }

    record PutFn(Type type) implements FieldSpec {
      @Override
      public Type fieldType() {
        return type;
      }
    }
  }

  private TMap<ConstKey, FieldSpec> fieldSpecs() {
    return TMap.ofEntries(
        new Pair<>(
            new ConstKey.Str("new"),
            new FieldSpec.Factory(new TypeMapNewDynamic(keyType, valueType))),
        new Pair<>(
            new ConstKey.Str("get"),
            new FieldSpec.GetFn(new TypeMapGetFnDynamic(keyType, valueType))),
        new Pair<>(
            new ConstKey.Str("put"),
            new FieldSpec.PutFn(new TypeMapPutFnDynamic(keyType, valueType))));
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "TypeMapDynamic::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var spec = fieldSpecs().getOrNull(ck);
    if (spec != null) {
      return switch (spec) {
        case FieldSpec.Factory f ->
            EResult.ok(
                new CodeValue(Code.none(loc), EvalOutcome.ofValue(compilerxyz.eval.value.object_.ValueObjectConst.ofType(f.type()))));
        case FieldSpec.GetFn g -> {
          var selfDyn = self.code().dynamic();
          if (selfDyn == null) {
            yield EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
          }
          yield EResult.ok(
              new CodeValue(
                  Code.of(loc, selfDyn),
                  EvalOutcome.ofValue(ValueDynamic.stack(xx, g.type()))));
        }
        case FieldSpec.PutFn p -> {
          var selfDyn = self.code().dynamic();
          if (selfDyn == null) {
            yield EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
          }
          yield EResult.ok(
              new CodeValue(
                  Code.of(loc, selfDyn),
                  EvalOutcome.ofValue(ValueDynamic.stack(xx, p.type()))));
        }
      };
    }
    if (!(ck instanceof ConstKey.Str keyStr)) {
      return EResult.err(loc, "Field " + ck.toCanonical() + " not found on map");
    }
    if (!(self.value() instanceof Opt.Some<Value> selfSome)
        || !(selfSome.value() instanceof ValueDynamic selfVd)) {
      return EResult.err(loc, "TypeMapDynamic::field: receiver is not a ValueDynamic");
    }
    return EResult.err(
        loc,
        "TypeMapDynamic::field: map key access via source-tracked value no longer supported;"
            + " use map.get(key) explicitly");
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs().getOrNull(key);
    if (spec != null) return Opt.some(spec.fieldType());
    if (key instanceof ConstKey.Str) return Opt.some(valueType);
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeMapDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type aspectIterableElementType() {
    return new TypeMapEntryDynamic(keyType, valueType);
  }

  @Override
  public EResult<CodeValue> aspectIterableForeach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = GenAspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
    var baseDynRes =
        baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) return baseDynRes.reerr();
    var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

    var mapName = JavaInternalName.fromClass(Map.class);
    var setName = JavaInternalName.fromClass(Set.class);
    var iteratorName = JavaInternalName.fromClass(Iterator.class);
    var entrySetMethod =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(baseDyn);
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    var localBinding = compilerxyz.util.MustCast.mustCast(BindingId.Local.class, elementScopeId);
    // Iterator.next() returns Object; cast to Map.Entry (the element language
    // type) so the binding slot is typed as promised.
    var decodeSeq = new JavaBytecodeSequence();
    decodeSeq.add(
        JavaBytecodeUtils.cast(loc,
            JavaDataDescriptor.fromInternalName(
                JavaInternalName.fromClass(Map.Entry.class))));
    var elementDecode = new CodeDynamic(loc, decodeSeq);

    compilerxyz.eval.Code bodyCode;
    if (body.value().optIsSome()) {
      var dropRes = xx.target.targetDrop(xx, loc, body);
      if (dropRes.hasErrors()) return dropRes.reerr();
      var dropCode = dropRes.requireValue("TypeMapDynamic::foreach body drop");
      var bodyDyn = body.code().dynamic();
      var dropDyn = dropCode.dynamic();
      if (bodyDyn == null && dropDyn == null) {
        bodyCode = compilerxyz.eval.Code.none(loc);
      } else {
        var combinedSeq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
        if (bodyDyn != null) combinedSeq.add(bodyDyn);
        if (dropDyn != null) combinedSeq.add(dropDyn);
        bodyCode = compilerxyz.eval.Code.of(loc, new CodeDynamic(loc, combinedSeq));
      }
    } else {
      bodyCode = body.code();
    }
    return EResult.ok(
        compilerxyz.eval.dynamic.DynUtil.foreachSkeleton(xx,
            loc, iteratorSetup, elementDecode, localBinding, Opcodes.ASTORE, bodyCode));
  }

  @Override
  public CodeValue aspectStackValueDynamicToValue(compilerxyz.eval.ExprContext xx, QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(xx, loc, value, this);
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_MAP_DYNAMIC_IDENTITY, TypeMapDynamic.class) instanceof Opt.Some<TypeMapDynamic> oSome
        && keyType.typeIsEqual(xx, oSome.value().keyType)
        && valueType.typeIsEqual(xx, oSome.value().valueType);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    if (typeIsEqual(xx, sourceType)) {
      return true;
    }
    if (sourceType instanceof Record
        && GenAspectConsumeDynamic.getMaybe(xx, sourceType) instanceof Opt.Some<?>) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeMapDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueType();
    if (typeIsEqual(xx, sourceType)) {
      return EResult.ok(cv);
    }
    if (!(some.value() instanceof Record record)) {
      return EResult.err(
          loc, "TypeMapDynamic::typeCastFrom: expected Record source, got " + some.value().getClass());
    }
    var erb = new Erb(loc);
    var valueToDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, valueType));
    if (valueToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var valueToDynamic = valueToDynamicOpt.get();
    var keyToDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, keyType));
    if (keyToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var keyToDynamic = keyToDynamicOpt.get();
    var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
    var descObject = JavaDataDescriptor.fromClass(Object.class);
    var putDesc = JavaMethodDescriptor.fromParts(descObject, TList.of(descObject, descObject));
    var seq = new JavaBytecodeSequence();
    seq.add(
        JavaBytecodeUtils.instantiate(
            loc,
            torderedMapIntname,
            JavaMethodDescriptor.fromConstructorParts(TList.of()),
            new CodeDynamic(loc, new JavaBytecodeSequence())));
    for (var entry : record.fields().entrySet()) {
      var fieldKey = entry.getKey();
      var fieldVal = entry.getValue();
      CodeValue keyCv;
      switch (fieldKey) {
        case ConstKey.Str s ->
            keyCv = new CodeValue(Code.none(loc), EvalOutcome.ofValue(
                new ValueObjectConst(new TypeStringConst(), s.value())));
        case ConstKey.Uint u ->
            keyCv =
                new CodeValue(Code.none(loc), EvalOutcome.ofValue(
                    new ValueObjectConst(new TypeIntConst(), (long) u.value())));
      }
      var keyConvOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, keyType, keyCv));
      if (keyConvOpt.isEmpty()) return EResult.errs(erb.errors());
      var keyCodeOpt = erb.mustEr(keyToDynamic.aspectConsumeDynamicToDynamic(xx, loc, keyConvOpt.get()));
      if (keyCodeOpt.isEmpty()) return EResult.errs(erb.errors());
      CodeValue entryCv;
      switch (fieldVal) {
        case RecordField.EvalRes er -> {
          var inner = er.result();
          if (inner.hasErrors()) {
            inner.drainInto(erb.errors());
            erb.errPush("Record entry '" + fieldKey.toCanonical() + "' failed to evaluate");
            return EResult.errs(erb.errors());
          }
          entryCv = inner.requireValue("TypeMapDynamic::construct record entry");
        }
        case RecordField.Dynamic d -> {
          erb.errPush(
              "Record entry '"
                  + fieldKey.toCanonical()
                  + "' is dynamic-only, cannot consume directly to dynamic");
          return EResult.errs(erb.errors());
        }
      }
      var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, valueType, entryCv));
      if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
      var jsValOpt = erb.mustEr(valueToDynamic.aspectConsumeDynamicToDynamic(xx, loc, convertedOpt.get()));
      if (jsValOpt.isEmpty()) return EResult.errs(erb.errors());
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(keyCodeOpt.get());
      seq.add(jsValOpt.get());
      seq.add(JavaBytecodeUtils.callMethod(loc, torderedMapIntname, "put", putDesc));
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    }
    return EResult.ok(
        new CodeValue(
            Code.of(new CodeDynamic(loc, seq)),
            EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
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
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    var keyDropOpt = GenAspectDropDynamic.getMaybe(xx, keyType);
    var valDropOpt = GenAspectDropDynamic.getMaybe(xx, valueType);
    if (!(cv.value() instanceof Opt.Some<Value> some)
        || !(some.value() instanceof compilerxyz.eval.value.ValueDynamic vd)) {
      return EResult.ok(Code.none(loc));
    }
    var entrySlot =
        new BindingId.Local(new compilerxyz.eval.dynamic.JavaBytecodeBindingKey());
    var entryClassName = JavaInternalName.fromClass(Map.Entry.class);

    var bodySeq = new JavaBytecodeSequence();
    if (keyDropOpt instanceof Opt.Some<AspectDropDynamic> keyDropSome) {
      var loadKeySeq = new JavaBytecodeSequence();
      loadKeySeq.add(
          new CodeDynamic(
              loc,
              new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot.key())));
      loadKeySeq.add(
          JavaBytecodeUtils.callInterfaceMethod(
              loc,
              entryClassName,
              "getKey",
              JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromClass(Object.class), TList.of())));
      var keyCv =
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, loadKeySeq)),
              EvalOutcome.ofValue(compilerxyz.eval.value.ValueDynamic.stack(xx, keyType)));
      var keyDropRes = keyDropSome.value().aspectDropDynamicDrop(xx, loc, keyCv);
      if (keyDropRes.hasErrors()) return keyDropRes.reerr();
      var keyDropDyn = keyDropRes.requireValue("map key drop").dynamic();
      if (keyDropDyn != null) bodySeq.add(keyDropDyn);
    }
    if (valDropOpt instanceof Opt.Some<AspectDropDynamic> valDropSome) {
      var loadValSeq = new JavaBytecodeSequence();
      loadValSeq.add(
          new CodeDynamic(
              loc,
              new compilerxyz.eval.dynamic.JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot.key())));
      loadValSeq.add(
          JavaBytecodeUtils.callInterfaceMethod(
              loc,
              entryClassName,
              "getValue",
              JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromClass(Object.class), TList.of())));
      var valCv =
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, loadValSeq)),
              EvalOutcome.ofValue(compilerxyz.eval.value.ValueDynamic.stack(xx, valueType)));
      var valDropRes = valDropSome.value().aspectDropDynamicDrop(xx, loc, valCv);
      if (valDropRes.hasErrors()) return valDropRes.reerr();
      var valDropDyn = valDropRes.requireValue("map value drop").dynamic();
      if (valDropDyn != null) bodySeq.add(valDropDyn);
    }

    var mapName = JavaInternalName.fromClass(Map.class);
    var setName = JavaInternalName.fromClass(Set.class);
    var iteratorName = JavaInternalName.fromClass(Iterator.class);
    var entrySetMethod =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var setupSeq = new JavaBytecodeSequence();
    setupSeq.add(vd.source().realize(xx, loc));
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
    setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
    var iteratorSetup = new CodeDynamic(loc, setupSeq);

    var decodeSeq = new JavaBytecodeSequence();
    decodeSeq.add(
        JavaBytecodeUtils.cast(loc, JavaDataDescriptor.fromInternalName(entryClassName)));
    var elementDecode = new CodeDynamic(loc, decodeSeq);

    var loop =
        DynUtil.foreachSkeleton(xx,
            loc,
            iteratorSetup,
            elementDecode,
            entrySlot,
            Opcodes.ASTORE,
            Code.of(loc, new CodeDynamic(loc, bodySeq)));
    var loopCode = loop.code().dynamic();
    var finalSeq = new JavaBytecodeSequence();
    if (loopCode != null) finalSeq.add(loopCode);
    finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, finalSeq)));
  }

  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
}
