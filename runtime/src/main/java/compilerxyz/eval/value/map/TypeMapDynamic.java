package compilerxyz.eval.value.map;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectIterable;
import compilerxyz.gen.AspectStackValueDynamic;
import compilerxyz.eval.Code;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.any.ValueAnyConst;
import compilerxyz.eval.value.number.ValueNumberConst;
import compilerxyz.eval.value.record.Record;
import compilerxyz.eval.value.record.RecordField;
import compilerxyz.eval.value.string.ValueStringConst;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.Pair;
import compilerxyz.util.TList;
import compilerxyz.util.TMap;
import java.util.function.Function;

public record TypeMapDynamic(Type keyType, Type valueType)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JavaInternalName.of("java/util/HashMap"));
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.MapDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamicLight.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectIterable.ID)
        || key.equals(AspectStackValueDynamic.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
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
  }

  private TMap<ConstKey, FieldSpec> fieldSpecs() {
    return TMap.ofEntries(
        new Pair<>(
            new ConstKey.Str("new"),
            new FieldSpec.Factory(new TypeMapNewDynamic(keyType, valueType))),
        new Pair<>(
            new ConstKey.Str("get"),
            new FieldSpec.GetFn(new TypeMapGetFnDynamic(keyType, valueType))));
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
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
                new CodeValue(Code.none(loc), Opt.some(new ValueAnyConst(f.type()))));
        case FieldSpec.GetFn g -> {
          var selfDyn = self.code().dynamic();
          if (selfDyn == null) {
            yield EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
          }
          yield EResult.ok(
              new CodeValue(
                  Code.of(loc, selfDyn),
                  Opt.some(ValueDynamic.stack(g.type()))));
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
    var childSource = new ValueDynamic.Source.Field(selfVd.source(), keyStr.value());
    var fieldUse = DynUtil.renderSource(xx, loc, childSource);
    return EResult.ok(
        new CodeValue(
            Code.of(loc, fieldUse),
            Opt.some(new ValueDynamic(childSource, valueType))));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    return TList.from(fieldSpecs().keySet());
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    var spec = fieldSpecs().getOrNull(key);
    if (spec != null) return Opt.some(spec.fieldType());
    if (key instanceof ConstKey.Str) return Opt.some(valueType);
    return Opt.none();
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> bindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().dynamic();
    if (dyn == null) {
      throw new IllegalStateException(
          "TypeMapDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
    }
    return EResult.ok(dyn);
  }

  @Override
  public Binding initialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }

  @Override
  public Type elementType() {
    return new TypeMapEntryDynamic(keyType, valueType);
  }

  @Override
  public EResult<CodeValue> foreach(
      ExprContext xx, QExprId loc, CodeValue base, BindingId elementScopeId, CodeValue body) {
    var baseConsumeRes = AspectConsumeDynamic.get(xx, loc, this);
    if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
    var baseDynRes =
        baseConsumeRes.requireValue("AspectConsumeDynamic.get").toDynamic(xx, loc, base);
    if (baseDynRes.hasErrors()) return baseDynRes.reerr();
    var baseDyn = baseDynRes.requireValue("toDynamic(base)");

    var mapName = JavaInternalName.of("java/util/Map");
    var setName = JavaInternalName.of("java/util/Set");
    var iteratorName = JavaInternalName.of("java/util/Iterator");
    var descObject = JavaDataDescriptor.fromClass(Object.class);
    var descBool = JavaDataDescriptor.DESC_BOOL;
    var entrySetMethod =
        JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
    var iterMethod =
        JavaMethodDescriptor.fromParts(
            JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
    var hasNextMethod = JavaMethodDescriptor.fromParts(descBool, TList.of());
    var nextMethod = JavaMethodDescriptor.fromParts(descObject, TList.of());

    var loopLabel = new LabelNode();
    var doneLabel = new LabelNode();
    var seq = new JavaBytecodeSequence();
    seq.add(baseDyn);
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iteratorName, "hasNext", hasNextMethod));
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IFEQ, doneLabel))));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
    seq.add(JavaBytecodeUtils.callInterfaceMethod(loc, iteratorName, "next", nextMethod));
    if (!(elementScopeId instanceof BindingId.Local localBinding)) {
      throw new IllegalStateException(
          "TypeMapDynamic::foreach: element binding must be BindingId.Local");
    }
    // Iterator.next() returns Object; cast to Map.Entry (the element language
    // type) so the binding slot is typed as promised.
    seq.add(
        JavaBytecodeUtils.cast(loc,
            JavaDataDescriptor.fromInternalName(
                JavaInternalName.of("java/util/Map$Entry"))));
    seq.add(JavaBytecodeUtils.storeObj(loc, localBinding.key()));
    var bodyDyn = body.code().dynamic();
    if (bodyDyn != null) {
      seq.add(bodyDyn);
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    }
    seq.add(
        new CodeDynamic(
            loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, loopLabel))));
    seq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
    seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
    seq.add(JavaBytecodeUtils.literalNull(loc));

    var stackRes = AspectStackValueDynamic.get(xx, loc, new TypeNullDynamic());
    if (stackRes.hasErrors()) return stackRes.reerr();
    return EResult.ok(
        stackRes.requireValue("AspectStackValueDynamic.get").toValue(loc, new CodeDynamic(loc, seq)));
  }

  @Override
  public CodeValue toValue(QExprId loc, CodeDynamic value) {
    return DynUtil.dynamicStackToValue(loc, value, this);
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    if (sourceType instanceof TypeMapDynamic sourceMap) {
      return typesConvertible(xx, loc, sourceMap.keyType, keyType)
          && typesConvertible(xx, loc, sourceMap.valueType, valueType);
    }
    if (sourceType instanceof Record
        && AspectConsumeDynamic.getMaybe(xx, sourceType) instanceof Opt.Some<?>) {
      return true;
    }
    return false;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(loc, "TypeMapDynamic::typeCastFrom: CodeValue has no value");
    }
    var sourceType = some.value().valueTypeOf();
    if (sourceType instanceof TypeMapDynamic sourceMap) {
      if (!typesConvertible(xx, loc, sourceMap.keyType, keyType)) {
        return EResult.err(
            loc,
            "Incompatible map key types: "
                + sourceMap.keyType.typeDesc()
                + " cannot convert to "
                + keyType.typeDesc());
      }
      if (!typesConvertible(xx, loc, sourceMap.valueType, valueType)) {
        return EResult.err(
            loc,
            "Incompatible map value types: "
                + sourceMap.valueType.typeDesc()
                + " cannot convert to "
                + valueType.typeDesc());
      }
      return EResult.ok(
          new CodeValue(
              cv.code(),
              Opt.some(ValueDynamic.stack(this))));
    }
    if (!(some.value() instanceof Record record)) {
      return EResult.err(
          loc, "TypeMapDynamic::typeCastFrom: expected Record source, got " + some.value().getClass());
    }
    var erb = new Erb(loc);
    var valueToDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, valueType));
    if (valueToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var valueToDynamic = valueToDynamicOpt.get();
    var keyToDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, keyType));
    if (keyToDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var keyToDynamic = keyToDynamicOpt.get();
    var torderedMapIntname = JavaInternalName.of("compilerxyz/util/TOrderedMap");
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
            keyCv = new CodeValue(Code.none(loc), Opt.some(new ValueStringConst(s.value())));
        case ConstKey.Uint u ->
            keyCv =
                new CodeValue(Code.none(loc), Opt.some(new ValueNumberConst((double) u.value())));
      }
      var keyConvOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, keyType, keyCv));
      if (keyConvOpt.isEmpty()) return EResult.errs(erb.errors());
      var keyCodeOpt = erb.mustEr(keyToDynamic.toDynamic(xx, loc, keyConvOpt.get()));
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
      var jsValOpt = erb.mustEr(valueToDynamic.toDynamic(xx, loc, convertedOpt.get()));
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
            Opt.some(ValueDynamic.stack(this))));
  }

  private static boolean typesConvertible(
      ExprContext xx, QExprId loc, Type sourceType, Type destType) {
    return sourceType.typeCanCastTo(xx, loc, destType) || destType.typeCanCastFrom(xx, loc, sourceType);
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
