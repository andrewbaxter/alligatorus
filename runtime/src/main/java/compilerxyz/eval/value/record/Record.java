package compilerxyz.eval.value.record;




import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CompositeClassEmitter;
import compilerxyz.eval.dynamic.CompositeSlotSort;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeBindingKey;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectElementUseDynamicBind;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseConst;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectRecord;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.BindResult;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectElementUseDynamicBind;
import compilerxyz.gen.GenAspectField;
import compilerxyz.gen.GenAspectFieldUseConst;
import compilerxyz.gen.GenAspectFieldUseDynamic;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectRecord;
import compilerxyz.gen.GenAspectStackValueDynamic;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.RecordBindField;
import compilerxyz.gen.Target;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactRecordTypeCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.MustCast;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.objectweb.asm.Opcodes;

@BuiltinLight(uniqueId = 6134L)
public final class Record
    implements
        Type,
        Value,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectFieldUseDynamic,
        AspectElementUseDynamicBind,
        AspectInitialBind,
        AspectRecord,
        AspectType,
        ExporterAny {

  public final Opt<RecordBaseDynamic> base;
  public final TOrderedMap<ConstKey, RecordField> fields;
  public final ArtifactSupport.ArtifactIdHolder artifactIdCell =
      new ArtifactSupport.ArtifactIdHolder();

  public Record(Opt<RecordBaseDynamic> base, TOrderedMap<ConstKey, RecordField> fields) {
    this.base = base;
    this.fields = fields;
  }

  public TOrderedMap<ConstKey, RecordField> fields() {
    return fields;
  }


  @Override
  public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
    var res = accessOrDrop(xx, loc, cv, Opt.none());
    if (res.hasErrors()) return res.reerr();
    return EResult.ok(res.requireValue("Record::drop").code());
  }

  /**
   * Unified field-access + drop for records. See design spec.
   *
   * <p>{@code accessKey = None}: emits full-drop bytecode; returned CodeValue has no
   * value component.
   *
   * <p>{@code accessKey = Some(k)}: emits sibling-drop + accessed-field extraction;
   * returned CodeValue carries the accessed field's value on the stack.
   */
  public EResult<CodeValue> accessOrDrop(
      ExprContext xx, QExprId loc, CodeValue selfCv, Opt<ConstKey> accessKey) {
    if (selfCv.value().optIsNone()) {
      throw new IllegalStateException("Record::accessOrDrop: CodeValue has no value");
    }
    var record = MustCast.mustCast(Record.class, selfCv.value().optUnwrap());
    return switch (record.base) {
      case Opt.None<RecordBaseDynamic> n -> {
        var seq = new JavaBytecodeSequence();
        var carry = selfCv.code().codeDynamic();
        if (carry != null) seq.add(carry);
        CodeValue accessedCv = null;
        Type accessedType = null;
        CodeDynamic accessedPushDyn = null;
        var _keys = new TList<ConstKey>(record.fields.size());
        _keys.addAll(record.fields.keySet());
        for (int i = _keys.size() - 1; i >= 0; i--) {
          var k = _keys.get(i);
          var f = record.fields.getOrNull(k);
          boolean isAccessed = accessKey.optIsSome() && accessKey.optUnwrap().equals(k);
          RecordField.EvalRes er;
          switch (f) {
            case RecordField.EvalRes erVal -> er = erVal;
            case RecordField.Dynamic d ->
                throw new IllegalStateException(
                    "Record::accessOrDropSourceless: field '"
                        + k.constKeyToCanonical()
                        + "' is not EvalRes; sourceless value record cannot carry a"
                        + " RecordField.Dynamic entry");
          }
          var res = er.result();
          if (res.hasErrors()) {
            if (isAccessed) {
              yield EResult.err(loc, "Field '" + k.constKeyToCanonical() + "' evaluated to error");
            }
            continue;
          }
          var _erCv = res.valueOrNull();
          if (!res.hasErrors() && _erCv.optIsSome() && _erCv.optUnwrap().value().optIsNone()) {
            yield EResult.err(
                loc,
                "Field '" + k.constKeyToCanonical() + "' does not fall through (contains an unreachable"
                    + " control jump like throw/panic); this expression is not valid in a"
                    + " record field position");
          }
          var fieldCv = res.requireValue("Record::accessOrDropSourceless field");
          var fieldDyn = fieldCv.code().codeDynamic();
          Type fieldType = fieldCv.value().optUnwrap().valueType();
          if (isAccessed) {
            var useConstOpt = GenAspectFieldUseConst.getMaybe(xx, fieldType);
            if (useConstOpt.optIsSome()) {
              var useRes = useConstOpt.optUnwrap().aspectFieldUseConstFieldUseConst(xx, loc, fieldCv);
              if (useRes.hasErrors()) yield useRes.reerr();
              accessedCv = useRes.requireValue("Record::accessOrDropSourceless useConst");
              continue;
            }
            if (fieldDyn == null) { accessedCv = fieldCv; continue; }
            if (!(fieldCv.value().optUnwrap() instanceof ValueDynamic)) { accessedCv = fieldCv; continue; }
            accessedPushDyn = fieldDyn;
            accessedType = fieldType;
            continue;
          }
          if (fieldDyn != null) seq.add(fieldDyn);
          var fullDropOpt = GenAspectDropDynamic.getMaybe(xx, fieldType);
          if (fullDropOpt.optIsSome()) {
            var dropRes = fullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, fieldCv);
            if (dropRes.hasErrors()) yield dropRes.reerr();
            var dropDyn = dropRes.requireValue("Record::accessOrDropSourceless drop").codeDynamic();
            if (dropDyn != null) seq.add(dropDyn);
            continue;
          }
          var lightDropOpt = GenAspectDropDynamicLight.getMaybe(xx, fieldType);
          if (lightDropOpt.optIsSome()) {
            var dropRes = lightDropOpt.optUnwrap().aspectDropDynamicLightDropDynamicLight(xx, loc, fieldCv);
            if (dropRes.hasErrors()) yield dropRes.reerr();
            var dropDyn = dropRes.requireValue("Record::accessOrDropSourceless light drop").codeDynamic();
            if (dropDyn != null) seq.add(dropDyn);
            continue;
          }
          yield EResult.err(
              loc,
              "Sourceless record field '" + k.constKeyToCanonical() + "' type '"
                  + fieldType.typeDesc()
                  + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
        }
        if (accessKey.optIsSome()) {
          if (accessedCv != null) {
            var accessedDyn = accessedCv.code().codeDynamic();
            if (accessedDyn != null) seq.add(accessedDyn);
            if (seq.children.isEmpty()) {
              yield EResult.ok(new CodeValue(accessedCv.code(), accessedCv.outcome()));
            }
            yield EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), accessedCv.outcome()));
          }
          if (accessedPushDyn != null) {
            seq.add(accessedPushDyn);
            yield EResult.ok(
                new CodeValue(
                    Code.of(loc, new CodeDynamic(loc, seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(accessedType, 1))));
          }
          yield EResult.err(
              loc, "Field '" + accessKey.optUnwrap().constKeyToCanonical() + "' not found on record value");
        }
        yield EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
      }
      case Opt.Some<RecordBaseDynamic> s -> {
        var baseSource = s.value().source();
        var dynamicFieldTypes = new TOrderedMap<ConstKey, Type>();
        for (var entry : record.fields.entrySet()) {
          switch (entry.getValue()) {
            case RecordField.Dynamic d -> dynamicFieldTypes.put(entry.getKey(), d.type());
            case RecordField.EvalRes er -> {}
          }
        }
        var slotInfo = computeSlotInfoFromTypes(xx, dynamicFieldTypes);
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
        if (accessKey.optIsNone()) {
          var out = new JavaBytecodeSequence();
          out.add(baseSource.valueDynamicSourceRealize(xx, loc));
          var _ecdKeys = new TList<ConstKey>(dynamicFieldTypes.size());
          _ecdKeys.addAll(dynamicFieldTypes.keySet());
          for (int _ecdI = _ecdKeys.size() - 1; _ecdI >= 0; _ecdI--) {
            var k = _ecdKeys.get(_ecdI);
            var t = dynamicFieldTypes.getOrNull(k);
            int slotIdx = slotInfo.slotOfKey().get(k);
            var _cfdcFullDropOpt = GenAspectDropDynamic.getMaybe(xx, t);
            if (_cfdcFullDropOpt.optIsSome()) {
              var _cfdcSeq = new JavaBytecodeSequence();
              _cfdcSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
              _cfdcSeq.add(JavaBytecodeUtils.accessField(
                  loc, klass,
                  CompositeClassEmitter.slotFieldName(slotIdx),
                  CompositeClassEmitter.slotDescriptor(xx, t)));
              var _cfdcChildCv = new CodeValue(
                  Code.of(loc, new CodeDynamic(loc, new JavaBytecodeSequence())),
                  EvalOutcome.ofValue(ValueDynamic.stack(t, 1)));
              var _cfdcDropRes = _cfdcFullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, _cfdcChildCv);
              if (_cfdcDropRes.hasErrors()) yield _cfdcDropRes.reerr();
              var _cfdcD = _cfdcDropRes.requireValue("Record::childFullDropCode drop").codeDynamic();
              if (_cfdcD != null) _cfdcSeq.add(_cfdcD);
              out.add(new CodeDynamic(loc, _cfdcSeq));
            } else if (GenAspectDropDynamicLight.getMaybe(xx, t).optIsNone()) {
              yield EResult.err(
                  loc,
                  "Record field '" + k.constKeyToCanonical() + "' type '" + t.typeDesc()
                      + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
            }
          }
          out.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
          yield EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), EvalOutcome.NO_VALUE));
        } else {
          var accessedKey = accessKey.optUnwrap();
          var f = record.fields.getOrNull(accessedKey);
          if (f == null) {
            yield EResult.err(loc, "Field '" + accessedKey.constKeyToCanonical() + "' not found on record");
          }
          switch (f) {
            case RecordField.Dynamic d -> {}
            case RecordField.EvalRes er ->
                throw new IllegalStateException(
                    "Record::accessSourced: accessed field '" + accessedKey.constKeyToCanonical()
                        + "' is scope-bound (non-dynamic); access should be routed elsewhere");
          }
          var accessedType = recordFieldType(f);
          int accessedSlot = slotInfo.slotOfKey().get(accessedKey);
          var useDynOpt = GenAspectFieldUseDynamic.getMaybe(xx, accessedType);
          if (useDynOpt.optIsNone()) {
            yield EResult.err(
                loc,
                "Record field '"
                    + accessedKey.constKeyToCanonical()
                    + "' type ("
                    + accessedType.typeDesc()
                    + ") does not implement AspectFieldUseDynamic");
          }
          var childSource = new ValueDynamicSource.Field(
              baseSource, klass,
              CompositeClassEmitter.slotFieldName(accessedSlot),
              CompositeClassEmitter.slotDescriptor(xx, accessedType));
          yield useDynOpt.optUnwrap().aspectFieldUseDynamicFieldUseDynamic(xx, loc, childSource);
        }
      }
    };
  }

  @Override
  public TOrderedMap<ConstKey, Type> aspectRecordFieldTypes() {
    var out = new TOrderedMap<ConstKey, Type>();
    for (var entry : fields.entrySet()) {
      switch (entry.getValue()) {
        case RecordField.Dynamic d -> out.put(entry.getKey(), d.type());
        case RecordField.EvalRes er -> {
          var res = er.result();
          if (res.hasErrors()) {
            throw new IllegalStateException(
                "Record::recordFieldTypes: field '" + entry.getKey().constKeyToCanonical()
                    + "' EvalRes has errors");
          }
          var cv = res.requireValue("Record::recordFieldTypes field");
          if (cv.value().optIsNone()) {
            throw new IllegalStateException(
                "Record::recordFieldTypes: field '" + entry.getKey().constKeyToCanonical()
                    + "' EvalRes has no compile-time value");
          }
          var v = cv.value().optUnwrap();
          if (v instanceof Type asType) {
            out.put(entry.getKey(), asType);
          } else if (v instanceof ValueObjectConst voc && voc.data() instanceof Type asDataType) {
            out.put(entry.getKey(), asDataType);
          } else {
            throw new IllegalStateException(
                "Record::recordFieldTypes: field '" + entry.getKey().constKeyToCanonical()
                    + "' value is not a Type: " + v.getClass());
          }
        }
      }
    }
    return out;
  }

  @Override
  public TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordTakeFields(ExprContext xx, QExprId loc, Value value) {
    if (!(value instanceof Record r)) {
      throw new IllegalArgumentException(
          "Record::recordTakeFields: expected Record value, got " + value.getClass());
    }
    var out = new TOrderedMap<ConstKey, EResult<CodeValue>>();
    for (var entry : r.fields.entrySet()) {
      RecordField.EvalRes er;
      switch (entry.getValue()) {
        case RecordField.EvalRes erVal -> er = erVal;
        case RecordField.Dynamic d ->
            throw new IllegalStateException(
                "Record::recordTakeFields: field '" + entry.getKey().constKeyToCanonical()
                    + "' is not EvalRes-form");
      }
      out.put(entry.getKey(), er.result());
    }
    return out;
  }

  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    var slotInfo = computeSlotInfoFromTypes(xx, recordFieldTypesRaw(fields));
    var klass = CompositeClassEmitter.getOrEmit(xx, slotInfo.sortedTypes());
    return JavaDataDescriptor.fromInternalName(JavaInternalName.of(klass.getName().replace('.', '/')));
  }

  @Override
  public String typeDesc() {
    var inner =
        fields.entrySet().stream()
            .map(
                e -> {
                  var tDesc =
                      switch (e.getValue()) {
                        case RecordField.Dynamic d -> d.type().typeDesc();
                        case RecordField.EvalRes r -> "?";
                      };
                  return e.getKey().constKeyToCanonical() + ": " + tDesc;
                })
            .collect(Collectors.joining(", "));
    return "record(" + inner + ")";
  }

                public Type valueType() {

    var fieldTypes = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      var t = recordFieldType(entry.getValue());
      fieldTypes.put(entry.getKey(), new RecordField.Dynamic(t));
    }
    return new Record(base, fieldTypes);
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.RECORD_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamic.AspectId)
        || key.equals(GenAspectField.AspectId)
        || key.equals(GenAspectFieldUseDynamic.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectElementUseDynamicBind.AspectId)
        || key.equals(GenAspectRecord.AspectId)
        || key.equals(GenAspectType.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectLift.AspectId)) {
      var entryLifts = new TList<RecordLift.Entry>(fields.size());
      for (var entry : fields.entrySet()) {
        var k = entry.getKey();
        var f = entry.getValue();
        RecordField.Dynamic d;
        switch (f) {
          case RecordField.Dynamic df -> d = df;
          case RecordField.EvalRes er -> { return Opt.none(); }
        }
        var entryLiftOpt = GenAspectLift.getMaybe(xx, d.type());
        if (entryLiftOpt.optIsNone()) {
          return Opt.none();
        }
        entryLifts.add(new RecordLift.Entry(k, entryLiftOpt.optUnwrap(), d.type()));
      }
      return Opt.some(new RecordLift(xx, entryLifts));
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (key.value().optIsNone()) {
      return EResult.err(loc, "Record::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, key.value().optUnwrap()));
    if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
    return accessOrDrop(xx, loc, self_, Opt.some(ckOpt.optUnwrap()));
  }

  @Override
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
      ExprContext xx,
      QExprId loc,
      ValueDynamicSource source) {
    var fieldCode = Code.of(loc, DynUtil.renderSource(xx, loc, source));
    var newFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      var t =
          switch (entry.getValue()) {
            case RecordField.Dynamic d -> d.type();
            case RecordField.EvalRes er ->
                throw new IllegalStateException(
                    "Record::fieldUseDynamic: field-type Record has non-Dynamic entry '"
                        + entry.getKey().constKeyToCanonical() + "'");
          };
      newFields.put(entry.getKey(), new RecordField.Dynamic(t));
    }
    Value innerRecord = new Record(Opt.some(new RecordBaseDynamic(source)), newFields);
    return EResult.ok(new CodeValue(fieldCode, EvalOutcome.ofValue(innerRecord)));
  }

  @Override
  public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
    if (base.optIsNone()) {
      return new TList<>();
    }
    var out = new TList<ConstKey>(fields.size());
    for (var k : fields.keySet()) out.add(k);
    return out;
  }

  @Override
  public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
    var f = fields.getOrNull(key);
    if (f == null) return Opt.none();
    return Opt.some(recordFieldType(f));
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.RECORD;
  }

  private static Artifact nullArtifact() {
    return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof Record record)) {
            return EResult.err(
                loc,
                "Record::exporterRootDelegateSelfExport: expected Record, got " + self.getClass());
          }
          var cell = record.artifactIdCell;
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  cell,
                  () -> {
                    var fieldArts = new TOrderedMap<ConstKey, Artifact>();
                    for (var entry : record.fields.entrySet()) {
                      var key = entry.getKey();
                      var field = entry.getValue();
                      Type fieldType;
                      switch (field) {
                        case RecordField.Dynamic d -> fieldType = d.type();
                        case RecordField.EvalRes r -> {
                          var inner = r.result();
                          if (inner.hasErrors()) {
                            inner.drainInto(errors);
                          }
                          if (inner.valueOrNull() == null) {
                            errors.add(
                                new ExprErr(
                                    loc,
                                    TList.of(
                                        new ExprSubErr.Opaque(
                                            "Record field has no value to serialize"))));
                            fieldArts.put(key, nullArtifact());
                            continue;
                          }
                          var cv = inner.requireValue("Record::exporterExport field");
                          if (cv.value().optIsNone()) {
                            errors.add(
                                new ExprErr(
                                    loc,
                                    TList.of(
                                        new ExprSubErr.Opaque(
                                            "Record field has no value to serialize"))));
                            fieldArts.put(key, nullArtifact());
                            continue;
                          }
                          fieldType = cv.value().optUnwrap().valueType();
                        }
                        default -> throw new IllegalStateException();
                      }
                      var artRes = fieldType.artifactableExportArtifact(xx, loc);
                      artRes.drainInto(errors);
                      fieldArts.put(
                          key,
                          artRes.valueOrNull() != null
                              ? artRes.requireValue("Record::exporterExport field artifact")
                              : nullArtifact());
                    }
                    var body = new ArtifactRecordType(fieldArts);
                    return makeBody.apply(ArtifactRecordTypeCodec.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(loc, "Record requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactRecordType body;
                try {
                  body = ArtifactRecordTypeCodec.fromValue(raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "Record data: failed to parse ArtifactRecordType: " + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var holder = new ExporterAny[] {null};
                ExporterAny stub =
                    new ExporterAny() {
                      @Override
                      public EResult<Type> exporterAnyAsType() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "Record import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsType();
                      }

                      @Override
                      public EResult<Value> exporterAnyAsValue() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "Record import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsValue();
                      }
                    };
                var deps = new TList<Artifact>();
                for (var entry : body.fields().entrySet()) deps.add(entry.getValue());
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var fields = new TOrderedMap<ConstKey, RecordField>();
                      for (var entry : body.fields().entrySet()) {
                        var tRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, entry.getValue());
                        var t = tRes.requireValue("Record::finalizer field type");
                        fields.put(entry.getKey(), new RecordField.Dynamic(t));
                      }
                      var out = new Record(Opt.none(), fields);
                      out.artifactIdCell.set(idValue);
                      holder[0] = out;
                    };
                return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
              });
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      throw new IllegalStateException("Record::bindSub: CodeValue has no value");
    }
    if (!(cv.value().optUnwrap() instanceof Record record)) {
      throw new IllegalStateException(
          "Record::bindSub: expected Record value, got " + cv.value().optUnwrap().getClass());
    }
    var carry = cv.code();

    if (base.optIsSome()) {
      Type recordType = this;
      var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
      for (var entry : fields.entrySet()) {
        RecordField.Dynamic d;
        switch (entry.getValue()) {
          case RecordField.Dynamic df -> d = df;
          case RecordField.EvalRes er ->
              throw new IllegalStateException("Record::bindSub: bind on non-type record");
        }
        scopeFields.put(entry.getKey(), new RecordScopeField.Dynamic(d.type()));
      }
      var subRes = xx.target.targetCodeToBindSub(loc, carry, recordType);
      if (subRes.hasErrors()) return subRes.reerr();
      var sub = subRes.requireValue("Record::bindSub base.bindSub");
      var bound = xx.target.targetBind(xx, loc, sub);
      return EResult.ok(new BindResult(Code.of(loc, bound.code()), new BindingRecord(scopeFields, Opt.some(bound.id()))));
    }

    TList<ExprErr> errors = null;
    record FieldBindResult(ConstKey key, Opt<TargetBindSub> targetSub, Opt<CodeValue> constCv) {}
    var bindResults = new TList<FieldBindResult>(record.fields.size());
    boolean hasTarget = false;
    for (var entry : record.fields.entrySet()) {
      var key = entry.getKey();
      var field = entry.getValue();
      switch (field) {
        case RecordField.Dynamic d -> {
          if (errors == null) errors = new TList<>();
          errors.add(
              new ExprErr(
                  loc,
                  TList.of(
                      new ExprSubErr.Opaque(
                          "Record entry '"
                              + key.constKeyToCanonical()
                              + "' is dynamic-only, cannot bind directly"))));
        }
        case RecordField.EvalRes elemRes -> {
          var inner = elemRes.result();
          if (inner.hasErrors()) {
            if (errors == null) errors = new TList<>();
            inner.drainInto(errors);
            continue;
          }
          var elemCv = inner.requireValue("Record::bindSub field value");
          var savedValue = elemCv.outcome();
          var subRes = xx.target.targetMaybeBindSub(xx, loc, elemCv);
          if (subRes.hasErrors()) {
            if (errors == null) errors = new TList<>();
            subRes.drainInto(errors);
            continue;
          }
          var subOpt = subRes.requireValue("Record::bindSub targetMaybeBindSub");
          if (subOpt.optIsSome()) {
            hasTarget = true;
            bindResults.add(new FieldBindResult(key, Opt.some(subOpt.optUnwrap()), Opt.none()));
          } else {
            bindResults.add(new FieldBindResult(key, Opt.none(), Opt.some(new CodeValue(Code.none(loc), savedValue))));
          }
        }
      }
    }
    if (errors != null && !errors.isEmpty()) return EResult.errs(errors);
    if (!hasTarget) {
      var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
      for (var br : bindResults) {
        if (br.constCv().optIsNone()) {
          throw new IllegalStateException("Record::bindSub: !has_target but bindResults contains a target sub");
        }
        var fieldCv = br.constCv().optUnwrap();
        if (fieldCv.value().optIsNone()) throw new IllegalStateException("Record::bindSub: const field CodeValue has no value");
        var fieldType = fieldCv.value().optUnwrap().valueType();
        var bindRes = GenAspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) { scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull().optUnwrap())); continue; }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
        if (fieldBindRes.hasErrors()) { scopeFields.put(br.key(), new RecordScopeField.Error(fieldBindRes.errorsOrNull().optUnwrap())); continue; }
        var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
        scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
      }
      return EResult.ok(new BindResult(Code.none(loc), new BindingRecord(scopeFields, Opt.none())));
    }
    var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
    var targetFields = new TList<RecordBindField>(bindResults.size());
    for (var br : bindResults) {
      if (br.targetSub().optIsSome()) {
        scopeFields.put(br.key(), new RecordScopeField.Dynamic(br.targetSub().optUnwrap().type()));
        targetFields.add(new RecordBindField(br.key(), br.targetSub().optUnwrap()));
      } else if (br.constCv().optIsSome()) {
        var fieldCv = br.constCv().optUnwrap();
        if (fieldCv.value().optIsNone()) throw new IllegalStateException("Record::bindSub: const field CodeValue has no value");
        var fieldType = fieldCv.value().optUnwrap().valueType();
        var bindRes = GenAspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) { scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull().optUnwrap())); continue; }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
        if (fieldBindRes.hasErrors()) { scopeFields.put(br.key(), new RecordScopeField.Error(fieldBindRes.errorsOrNull().optUnwrap())); continue; }
        var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
        scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
      } else {
        throw new IllegalStateException("Record::bindSub: bind result has neither target nor const");
      }
    }
    var combined = xx.target.targetBindSubRecord(xx, loc, carry, targetFields);
    var bound = xx.target.targetBind(xx, loc, combined);
    return EResult.ok(new BindResult(Code.of(loc, bound.code()), new BindingRecord(scopeFields, Opt.some(bound.id()))));
  }

  @Override
  public Type aspectTypeType(Value value) {
    if (!(value instanceof Record r)) {
      throw new IllegalArgumentException(
          "Record::type_: expected Record value, got " + value.getClass());
    }
    return r.valueType();
  }

  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
    if (oOpt.optIsNone()) return false;
    var o = oOpt.optUnwrap();
    if (fields.size() != o.fields.size()) return false;
    for (var entry : fields.entrySet()) {
      var otherField = o.fields.getOrNull(entry.getKey());
      if (otherField == null) return false;
      var thisType = recordFieldType(entry.getValue());
      var otherType = recordFieldType(otherField);
      if (!thisType.typeIsEqual(xx, otherType)) return false;
    }
    return true;
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    var srcAsp = GenAspectRecord.getMaybe(xx, sourceType);
    if (srcAsp.optIsNone()) {
      return false;
    }
    var srcTypes = srcAsp.optUnwrap().aspectRecordFieldTypes();
    if (srcTypes.size() != fields.size()) {
      return false;
    }
    for (var k : fields.keySet()) {
      if (srcTypes.getOrNull(k) == null) {
        return false;
      }
    }
    return true;
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    if (cv.value().optIsNone()) {
      throw new IllegalStateException("Record::typeCastFrom: CodeValue has no value");
    }
    var srcValue = cv.value().optUnwrap();
    var srcType = srcValue.valueType();
    var srcAspOpt = GenAspectRecord.getMaybe(xx, srcType);
    if (srcAspOpt.optIsNone()) {
      throw new IllegalStateException(
          "Record::typeCastFrom: source is not AspectRecord: " + srcType.typeDesc());
    }
    var srcAsp = srcAspOpt.optUnwrap();

    if (srcValue instanceof Record srcRec && srcRec.base.optIsNone()) {
      var srcTakenFields = srcAsp.aspectRecordTakeFields(xx, loc, srcValue);
      var erb = new Erb(loc);
      var convertedFields = new TOrderedMap<ConstKey, RecordField>();
      for (var destEntry : fields.entrySet()) {
        var key = destEntry.getKey();
        var destType = recordFieldType(destEntry.getValue());
        var srcFieldRes = srcTakenFields.getOrNull(key);
        if (srcFieldRes == null) {
          return EResult.err(
              loc, "Record::typeCastFrom: source missing field '" + key.constKeyToCanonical() + "'");
        }
        if (srcFieldRes.hasErrors()) {
          convertedFields.put(
              key, new RecordField.EvalRes(EResult.errs(srcFieldRes.errorsOrNull().optUnwrapOr(new TList<>()))));
          continue;
        }
        var srcFieldCv = srcFieldRes.requireValue("Record::typeCastFrom source field");
        var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destType, srcFieldCv));
        if (convOpt.optIsNone()) {
          return EResult.errs(erb.errors());
        }
        convertedFields.put(key, new RecordField.EvalRes(EResult.ok(convOpt.optUnwrap())));
      }
      if (!erb.errors().isEmpty()) {
        return EResult.errs(erb.errors());
      }
      return EResult.ok(
          new CodeValue(cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), convertedFields))));
    }

    ValueDynamicSource sourcedSource;
    if (srcValue instanceof Record srcRecSourced
        && srcRecSourced.base.optIsSome()) {
      sourcedSource = srcRecSourced.base.optUnwrap().source();
    } else if (srcValue instanceof ValueDynamic vd) {
      sourcedSource = vd.source();
    } else {
      throw new IllegalStateException(
          "Record::typeCastFrom: unexpected source value class: " + srcValue.getClass());
    }

    var srcFieldTypes = srcAsp.aspectRecordFieldTypes();
    var srcSlotInfo = computeSlotInfoFromTypes(xx, srcFieldTypes);
    var destSlotInfo = computeSlotInfoFromTypes(xx, recordFieldTypesRaw(fields));

    var _stlA = srcSlotInfo.sortedTypes();
    var _stlB = destSlotInfo.sortedTypes();
    boolean _sameTypes = _stlA.size() == _stlB.size();
    for (int _stlI = 0; _sameTypes && _stlI < _stlA.size(); _stlI++) {
      if (!_stlA.get(_stlI).typeIsEqual(xx, _stlB.get(_stlI))) _sameTypes = false;
    }
    if (_sameTypes) {
      var sourcedResult =
          new Record(Opt.some(new RecordBaseDynamic(sourcedSource)), asDynamicFields(fields));
      return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue((Value) sourcedResult)));
    }

    CodeDynamic carryDyn = cv.code().codeDynamic();
    CodeDynamic sourceLoadCarry;
    ValueDynamicSource srcAccessSource;
    if (carryDyn != null) {
      var bindResult = xx.target.targetBind(xx, loc, new TargetBindSub(carryDyn, srcType));
      sourceLoadCarry = bindResult.code();
      srcAccessSource = new ValueDynamicSource.Binding(bindResult.id());
    } else {
      switch (sourcedSource) {
        case ValueDynamicSource.Expr e -> {
          // The source composite sits on the operand stack; each per-field access below
          // realizes the source once, so it must live in a reloadable slot.
          var srcKey = new JavaBytecodeBindingKey();
          sourceLoadCarry = new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, srcKey));
          srcAccessSource = new ValueDynamicSource.Binding(new BindingId.Local(srcKey));
        }
        case ValueDynamicSource.Binding b -> {
          sourceLoadCarry = null;
          srcAccessSource = sourcedSource;
        }
        case ValueDynamicSource.Field f -> {
          sourceLoadCarry = null;
          srcAccessSource = sourcedSource;
        }
      }
    }
    var srcKlass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, srcSlotInfo.sortedTypes()));

    var destSortedKeys = new TList<ConstKey>(fields.size());
    for (var k : fields.keySet()) destSortedKeys.add(k);
    for (var k : fields.keySet()) {
      destSortedKeys.set(destSlotInfo.slotOfKey().get(k), k);
    }

    var erb = new Erb(loc);
    var destFieldCodes = new TList<CodeDynamic>(destSortedKeys.size());
    for (int i = 0; i < destSortedKeys.size(); i++) {
      var key = destSortedKeys.get(i);
      var destType = recordFieldType(fields.getOrNull(key));
      var srcFieldType = srcFieldTypes.getOrNull(key);
      int srcSlot = srcSlotInfo.slotOfKey().get(key);
      var loadSeq = new JavaBytecodeSequence();
      loadSeq.add(srcAccessSource.valueDynamicSourceRealize(xx, loc));
      loadSeq.add(
          JavaBytecodeUtils.accessField(
              loc,
              srcKlass,
              CompositeClassEmitter.slotFieldName(srcSlot),
              CompositeClassEmitter.slotDescriptor(xx, srcFieldType)));
      var loadDyn = new CodeDynamic(loc, loadSeq);
      var srcFieldCv =
          new CodeValue(
              Code.of(loc, loadDyn),
              EvalOutcome.ofValue(ValueDynamic.stack(srcFieldType, 1)));
      var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destType, srcFieldCv));
      if (convOpt.optIsNone()) {
        return EResult.errs(erb.errors());
      }
      var convCv = convOpt.optUnwrap();
      var convDyn = convCv.code().codeDynamic();
      if (convDyn == null) {
        return EResult.err(
            loc, "Record::typeCastFrom: converted field '" + key.constKeyToCanonical() + "' has no dynamic code");
      }
      destFieldCodes.add(convDyn);
    }
    if (!erb.errors().isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var destKlass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, destSlotInfo.sortedTypes()));
    var argSeq = new JavaBytecodeSequence();
    for (var c : destFieldCodes) argSeq.add(c);
    var ctorDesc = CompositeClassEmitter.constructorDesc(xx, destSlotInfo.sortedTypes());
    var instantiate =
        JavaBytecodeUtils.instantiate(
            loc, destKlass, ctorDesc, new CodeDynamic(loc, argSeq));
    CodeDynamic finalDyn;
    if (sourceLoadCarry != null) {
      var finalSeq = new JavaBytecodeSequence();
      finalSeq.add(sourceLoadCarry);
      finalSeq.add(instantiate);
      finalDyn = new CodeDynamic(loc, finalSeq);
    } else {
      finalDyn = instantiate;
    }
    var newBase = Opt.some(
        new RecordBaseDynamic(new ValueDynamicSource.Expr(1)));
    var newRecord = new Record(newBase, asDynamicFields(fields));
    return EResult.ok(
        new CodeValue(Code.of(loc, finalDyn), EvalOutcome.ofValue((Value) newRecord)));
  }

  private static TOrderedMap<ConstKey, RecordField> asDynamicFields(
      TOrderedMap<ConstKey, RecordField> in) {
    var out = new TOrderedMap<ConstKey, RecordField>();
    for (var e : in.entrySet()) {
      var t = switch (e.getValue()) {
        case RecordField.Dynamic d -> d.type();
        case RecordField.EvalRes er -> {
          if (er.result().hasErrors()
              || er.result().requireValue("asDynamicFields").value().optIsNone()) {
            throw new IllegalStateException(
                "Record::asDynamicFields: EvalRes with no value for '"
                    + e.getKey().constKeyToCanonical()
                    + "'");
          }
          yield er.result().requireValue("asDynamicFields").value().optUnwrap().valueType();
        }
      };
      out.put(e.getKey(), new RecordField.Dynamic(t));
    }
    return out;
  }

  private static CompositeSlotSort.Result<ConstKey> computeSlotInfoFromTypes(
      ExprContext xx, TOrderedMap<ConstKey, Type> fieldTypes) {
    return CompositeSlotSort.sort(xx, fieldTypes, ConstKey.CANONICAL_ORDER);
  }

  private static TOrderedMap<ConstKey, Type> recordFieldTypesRaw(
      TOrderedMap<ConstKey, RecordField> fields) {
    var out = new TOrderedMap<ConstKey, Type>();
    for (var e : fields.entrySet()) {
      out.put(e.getKey(), recordFieldType(e.getValue()));
    }
    return out;
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var carry = cv.code();
    if (cv.value().optIsNone()) {
      throw new IllegalStateException("Record::toDynamic: CodeValue has no value");
    }
    if (!(cv.value().optUnwrap() instanceof Record record)) {
      throw new IllegalStateException(
          "Record::toDynamic: expected Record value, got " + cv.value().optUnwrap().getClass());
    }
    if (record.base.optIsSome()) {
      var _rbDyn = carry.codeDynamic();
      if (_rbDyn == null) return EResult.err(loc, "RecordBaseDynamic::toDynamic: carry has no dynamic code");
      return EResult.ok(_rbDyn);
    }
    TList<ExprErr> errors = null;
    record LoweredField(ConstKey key, String desc, Type type, CodeDynamic code) {}
    var loweredFields = new TList<LoweredField>(record.fields.size());
    for (var entry : record.fields.entrySet()) {
      var key = entry.getKey();
      var field = entry.getValue();
      var elemRes =
          switch (field) {
            case RecordField.Dynamic d ->
                EResult.<CodeValue>err(
                    loc,
                    "Record entry '"
                        + key.constKeyToCanonical()
                        + "' is dynamic-only, cannot consume directly to dynamic");
            case RecordField.EvalRes er -> er.result();
          };
      {
          if (elemRes.hasErrors()) {
            if (errors == null) errors = new TList<>();
            elemRes.drainInto(errors);
            continue;
          }
          var elemCv = elemRes.requireValue("Record::toDynamic field value");
          if (elemCv.value().optIsNone()) {
            throw new IllegalStateException(
                "Record::toDynamic: field '" + key.constKeyToCanonical() + "' CodeValue has no value");
          }
          var elemType = elemCv.value().optUnwrap().valueType();
          var toDynOpt = GenAspectConsumeDynamic.getMaybe(xx, elemType);
          if (toDynOpt.optIsNone()) {
            if (errors == null) errors = new TList<>();
            errors.add(
                new ExprErr(
                    loc,
                    TList.of(
                        new ExprSubErr.Opaque(
                            "Record entry '"
                                + key.constKeyToCanonical()
                                + "' type ("
                                + elemType.typeDesc()
                                + ") is missing aspect AspectConsumeDynamic"))));
            continue;
          }
          var toDyn = toDynOpt.optUnwrap();
          var lowered = toDyn.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
          if (lowered.hasErrors()) {
            if (errors == null) errors = new TList<>();
            lowered.drainInto(errors);
            continue;
          }
          var loweredCode = lowered.requireValue("Record::toDynamic field lowered");
          loweredFields.add(
              new LoweredField(
                  key,
                  toDyn.aspectConsumeDynamicJvmClass(xx).value(),
                  elemType,
                  loweredCode));
        }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    loweredFields.sort(
        (a, b) -> {
          int c = a.desc().compareTo(b.desc());
          if (c != 0) return c;
          return ConstKey.CANONICAL_ORDER.compare(a.key(), b.key());
        });
    var sortedTypes = new TList<Type>(loweredFields.size());
    var entryCodes = new TList<CodeDynamic>(loweredFields.size());
    for (var f : loweredFields) {
      sortedTypes.add(f.type());
      entryCodes.add(f.code());
    }
    var klass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, sortedTypes));
    var argSeq = new JavaBytecodeSequence();
    for (var c : entryCodes) argSeq.add(c);
    var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
    return EResult.ok(
        DynUtil.wrapWithCarry(
            loc,
            carry,
            JavaBytecodeUtils.instantiate(
                loc, klass, ctorDesc, new CodeDynamic(loc, argSeq))));
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    // An initial-bind record is the whole composite in one local: every field
    // reads out of it (Dynamic) and the composite is the stored value (jsKey).
    // Same shape as any other bound record — one BindingRecord.
    var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
    for (var entry : fields.entrySet()) {
      scopeFields.put(entry.getKey(), new RecordScopeField.Dynamic(recordFieldType(entry.getValue())));
    }
    return new BindingRecord(scopeFields, Opt.some(dynamicKey));
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  public static final class RecordLift implements AspectLift {
    public record Entry(ConstKey key, AspectLift lift, Type type) {}

    private final TList<Entry> entryLifts;
    private volatile Opt<Field[]> cachedFields = Opt.none();

    public RecordLift(ExprContext xx, TList<Entry> entryLifts) {
      var sorted = new TList<Entry>(entryLifts.size());
      sorted.addAll(entryLifts);
      sorted.sort(
          (a, b) -> {
            int c =
                CompositeClassEmitter.slotDescriptor(xx, a.type())
                    .value()
                    .compareTo(CompositeClassEmitter.slotDescriptor(xx, b.type()).value());
            if (c != 0) return c;
            return ConstKey.CANONICAL_ORDER.compare(a.key(), b.key());
          });
      this.entryLifts = sorted;
    }

    @Override
  public EResult<Value> aspectLiftLift(
        ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {
      var sortedTypes = new TList<Type>(entryLifts.size());
      for (var entry : entryLifts) sortedTypes.add(entry.type());
      var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
      if (externalHostValue.optIsNone()
          || !expectedClass.isInstance(externalHostValue.optUnwrap())) {
        return EResult.err(
            loc,
            "Record RecordLift::lift: expected "
                + expectedClass.getName()
                + ", got "
                + (externalHostValue.optIsNone()
                    ? "null"
                    : externalHostValue.optUnwrap().getClass().getName()));
      }
      var rawHostValue = externalHostValue.optUnwrap();
      var fields = cachedFields.optUnwrapOr(null);
      if (fields == null) {
        fields = new Field[entryLifts.size()];
        for (int i = 0; i < fields.length; i++) {
          var fieldName = CompositeClassEmitter.slotFieldName(i);
          try {
            fields[i] = expectedClass.getField(fieldName);
          } catch (NoSuchFieldException e) {
            throw new AssertionError(
                "Record RecordLift::lift: generated class missing entry field "
                    + fieldName,
                e);
          }
        }
        cachedFields = Opt.some(fields);
      }
      var out = new TOrderedMap<ConstKey, RecordField>();
      TList<ExprErr> errors = null;
      for (int i = 0; i < entryLifts.size(); i++) {
        var entry = entryLifts.get(i);
        Object fieldValue;
        try {
          fieldValue = fields[i].get(rawHostValue);
        } catch (IllegalAccessException e) {
          throw new AssertionError(
              "Record RecordLift::lift: cannot read entry field for '"
                  + entry.key().constKeyToCanonical()
                  + "'",
              e);
        }
        var lifted = entry.lift().aspectLiftLift(xx, loc, Opt.ofNullable(fieldValue));
        if (lifted.hasErrors()) {
          if (errors == null) errors = new TList<>();
          lifted.drainInto(errors);
          continue;
        }
        var liftedValue = lifted.requireValue("Record RecordLift::lift entry value");
        out.put(
            entry.key(),
            new RecordField.EvalRes(
                EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(liftedValue)))));
      }
      if (errors != null && !errors.isEmpty()) {
        return EResult.errs(errors);
      }
      return EResult.ok(new Record(Opt.none(), out));
    }
  }

  private static Type recordFieldType(RecordField f) {
    return switch (f) {
      case RecordField.Dynamic d -> d.type();
      case RecordField.EvalRes er -> {
        var inner = er.result();
        if (!inner.hasErrors()
            && inner.requireValue("Record::recordFieldType inner").value().optIsSome()) {
          var v = inner.requireValue("Record::recordFieldType inner").value().optUnwrap();
          if (v instanceof Type asType) {
            yield asType;
          }
          if (v instanceof ValueObjectConst voc && voc.data() instanceof Type asDataType) {
            yield asDataType;
          }
          yield v.valueType();
        }
        yield new Tuple(Opt.none(), new TList<>());
      }
    };
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome();
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    if (destType.typeAspect(xx, GenAspectStackValueDynamic.AspectId).optIsSome()) {
      if (cv.value().optIsNone()) {
        return EResult.err(loc, "Record::typeCastTo: CodeValue has no value");
      }
      if (!(cv.value().optUnwrap() instanceof Record record)) {
        return EResult.err(
            loc, "Record::typeCastTo: expected Record value, got " + cv.value().optUnwrap().getClass());
      }
      var elems = new TList<CodeDynamic>();
      for (var entry : record.fields.entrySet()) {
        var key = entry.getKey();
        ConstKey.Str keyStr;
        switch (key) {
          case ConstKey.Str ks -> keyStr = ks;
          case ConstKey.Uint u ->
              { return EResult.err(loc, "Record::typeCastTo: non-string key " + key); }
        }
        RecordField.EvalRes er;
        switch (entry.getValue()) {
          case RecordField.EvalRes erVal -> er = erVal;
          case RecordField.Dynamic d ->
              { return EResult.err(loc, "Record::typeCastTo: field '" + key.constKeyToCanonical() + "' is dynamic-only"); }
        }
        var elemRes = er.result();
        if (elemRes.hasErrors()) return elemRes.reerr();
        var elemCv = elemRes.requireValue("Record::typeCastTo field");
        if (elemCv.value().optIsNone()) {
          return EResult.err(
              loc, "Record::typeCastTo: field '" + key.constKeyToCanonical() + "' has no value");
        }
        var elemType = elemCv.value().optUnwrap().valueType();
        CodeDynamic loweredCode;
        if (elemCv.value().optUnwrap() instanceof Record nestedRecord) {
          var nestedRes = nestedRecord.typeCastTo(xx, loc, elemCv, destType);
          if (nestedRes.hasErrors()) return nestedRes.reerr();
          var nestedCv = nestedRes.requireValue("Record::typeCastTo nested record");
          var nestedDyn = nestedCv.code().codeDynamic();
          if (nestedDyn == null) {
            return EResult.err(
                loc,
                "Record::typeCastTo: nested record field '"
                    + key.constKeyToCanonical()
                    + "' cast produced no dynamic code");
          }
          loweredCode = nestedDyn;
        } else {
          var toDynOpt = GenAspectConsumeDynamic.getMaybe(xx, elemType);
          if (toDynOpt.optIsNone()) {
            return EResult.err(
                loc,
                "Record::typeCastTo: field '"
                    + key.constKeyToCanonical()
                    + "' type ("
                    + elemType.typeDesc()
                    + ") missing AspectConsumeDynamic");
          }
          var lowered = toDynOpt.optUnwrap().aspectConsumeDynamicToDynamic(xx, loc, elemCv);
          if (lowered.hasErrors()) return lowered.reerr();
          loweredCode = lowered.requireValue("Record::typeCastTo field lowered");
        }
        elems.add(JavaBytecodeUtils.literalString(loc, keyStr.value()));
        elems.add(loweredCode);
      }
      var callSeq = new JavaBytecodeSequence();
      callSeq.add(JavaBytecodeUtils.buildObjectArray(loc, elems));
      CodeDynamic _csm;
      csmBlock: {
        for (var _m1 : Record.class.getMethods()) {
          if (!"newDynamic".equals(_m1.getName())) continue;
          var _ret = JavaDataDescriptor.fromClass(_m1.getReturnType());
          var _args = new TList<JavaDataDescriptor>();
          for (var _p : _m1.getParameters()) _args.add(JavaDataDescriptor.fromClass(_p.getType()));
          _csm = JavaBytecodeUtils.callStaticMethod(
              loc,
              JavaInternalName.of(Record.class.getName().replace('.', '/')),
              "newDynamic",
              JavaMethodDescriptor.fromParts(_ret, _args));
          break csmBlock;
        }
        throw new AssertionError("No such method: " + Record.class.getName() + ".newDynamic");
      }
      callSeq.add(_csm);
      var stackValue = ValueDynamic.stack(destType, 1);
      var withCarry =
          DynUtil.wrapWithCarry(loc, cv.code(), new CodeDynamic(loc, callSeq));
      return EResult.ok(
          new CodeValue(Code.of(loc, withCarry), EvalOutcome.ofValue(stackValue)));
    }
    return EResult.ok(cv);
  }

  public static Record newDynamic(Object[] pairs) {
    var fields = new TOrderedMap<ConstKey, RecordField>();
    for (int i = 0; i < pairs.length; i += 2) {
      var key = new ConstKey.Str((String) pairs[i]);
      var raw = pairs[i + 1];
      if (!(raw instanceof Type t)) {
        throw new IllegalStateException(
            "Record::newDynamic: field '" + pairs[i] + "' value is not a Type: "
                + raw.getClass());
      }
      fields.put(key, new RecordField.Dynamic(t));
    }
    return new Record(Opt.none(), fields);
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(
      ExprContext xx, QExprId loc) {
    return Value.exportArtifact(xx, loc, this);
  }

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, this);
  }
}
