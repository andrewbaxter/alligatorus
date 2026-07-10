package compilerxyz.eval.value.record;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.dynamic.CompositeSlotSort;
import compilerxyz.eval.dynamic.CompositeClassEmitter;
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
import compilerxyz.gen.GenTypeIds;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseConst;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.gen.AspectRecord;
import compilerxyz.gen.AspectType;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.gen.Target;
import compilerxyz.eval.dynamic.target.BindResult;
import compilerxyz.eval.dynamic.target.RecordBindField;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;
import java.util.function.Function;
import java.util.stream.Collectors;

public record Record(Opt<RecordBaseDynamic> base, TOrderedMap<ConstKey, RecordField> fields)
    implements
        Type,
        Value,
        AspectArtifact,
        AspectBind,
        AspectConsumeDynamic,
        AspectDropDynamic,
        AspectField,
        AspectFieldUseDynamic,
        AspectInitialBind,
        AspectRecord,
        AspectType,
        ExporterAny {

  @Override
  public EResult<Code> drop(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Record::drop: called on absent CodeValue");
    }
    var v = some.value();
    if (v instanceof ValueDynamic vd) {
      return dropSourced(xx, loc, vd.source());
    }
    if (!(v instanceof Record record)) {
      throw new IllegalStateException(
          "Record::drop: expected Record or ValueDynamic value, got " + v.getClass());
    }
    return switch (record.base) {
      case Opt.None<RecordBaseDynamic> ignored -> dropSourceless(xx, loc, record);
      case Opt.Some<RecordBaseDynamic> baseSome -> dropSourced(xx, loc, baseSome.value().source());
    };
  }

  private EResult<Code> dropSourceless(ExprContext xx, QExprId loc, Record record) {
    var sortedKeys = new TList<ConstKey>(record.fields.size());
    sortedKeys.addAll(record.fields.keySet());
    sortedKeys.sort(ConstKey.CANONICAL_ORDER);
    var seq = new JavaBytecodeSequence();
    for (int i = sortedKeys.size() - 1; i >= 0; i--) {
      var f = record.fields.getOrNull(sortedKeys.get(i));
      if (!(f instanceof RecordField.EvalRes er)) {
        throw new IllegalStateException(
            "Record::dropSourceless: expected EvalRes field, got " + f.getClass());
      }
      var res = er.result();
      if (res.hasErrors()) continue;
      var fieldCv = res.requireValue("Record::dropSourceless field");
      if (!(fieldCv.value() instanceof Opt.Some<Value> fieldSome)) continue;
      var fieldType = fieldSome.value().valueTypeOf();
      var fieldDropOpt = AspectDropDynamic.getMaybe(xx, fieldType);
      if (!(fieldDropOpt instanceof Opt.Some<AspectDropDynamic> fieldDropSome)) continue;
      var fieldDropRes = fieldDropSome.value().drop(xx, loc, fieldCv);
      if (fieldDropRes.hasErrors()) return fieldDropRes.reerr();
      var fieldDrop = fieldDropRes.requireValue("Record::dropSourceless field drop").dynamic();
      if (fieldDrop != null) seq.add(fieldDrop);
    }
    return EResult.ok(Code.of(loc, new CodeDynamic(loc, seq)));
  }

  private EResult<Code> dropSourced(ExprContext xx, QExprId loc, ValueDynamic.Source source) {
    var slotInfo = computeSlotInfo(fields);
    var klass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
    var children = new TList<DynUtil.DropChild>(slotInfo.sortedTypes().size());
    for (int i = slotInfo.sortedTypes().size() - 1; i >= 0; i--) {
      var slotType = slotInfo.sortedTypes().get(i);
      var access =
          JavaBytecodeUtils.accessField(
              loc,
              klass,
              CompositeClassEmitter.slotFieldName(i),
              CompositeClassEmitter.slotDescriptor(xx, slotType));
      children.add(new DynUtil.DropChild(slotType, access));
    }
    return DynUtil.dropDestructiveComposite(xx, loc, source, children);
  }

  @Override
  public TOrderedMap<ConstKey, Type> recordFieldTypes() {
    var out = new TOrderedMap<ConstKey, Type>();
    for (var entry : fields.entrySet()) {
      if (!(entry.getValue() instanceof RecordField.Dynamic d)) {
        throw new IllegalStateException(
            "Record::recordFieldTypes: field '" + entry.getKey().toCanonical()
                + "' is not Dynamic-only");
      }
      out.put(entry.getKey(), d.type());
    }
    return out;
  }

  @Override
  public TOrderedMap<ConstKey, EResult<CodeValue>> recordTakeFields(ExprContext xx, QExprId loc, Value value) {
    if (!(value instanceof Record r)) {
      throw new IllegalArgumentException(
          "Record::recordTakeFields: expected Record value, got " + value.getClass());
    }
    var out = new TOrderedMap<ConstKey, EResult<CodeValue>>();
    for (var entry : r.fields.entrySet()) {
      if (!(entry.getValue() instanceof RecordField.EvalRes er)) {
        throw new IllegalStateException(
            "Record::recordTakeFields: field '" + entry.getKey().toCanonical()
                + "' is not EvalRes-form");
      }
      out.put(entry.getKey(), er.result());
    }
    return out;
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    var slotInfo = computeSlotInfo(fields);
    var klass = CompositeClassEmitter.getOrEmit(slotInfo.sortedTypes());
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
                  return e.getKey().toCanonical() + ": " + tDesc;
                })
            .collect(Collectors.joining(", "));
    return "record(" + inner + ")";
  }

  @Override
  public Type valueTypeOf() {

    var fieldTypes = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      var t = recordFieldType(entry.getValue());
      fieldTypes.put(entry.getKey(), new RecordField.Dynamic(t));
    }
    return new Record(base, fieldTypes);
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectConsumeDynamic.ID)
        || key.equals(AspectDropDynamic.ID)
        || key.equals(AspectField.ID)
        || key.equals(AspectFieldUseDynamic.ID)
        || key.equals(AspectInitialBind.ID)
        || key.equals(AspectRecord.ID)
        || key.equals(AspectType.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectLift.ID)) {

      var entryLifts = new TList<RecordLift.Entry>(fields.size());
      for (var entry : fields.entrySet()) {
        var k = entry.getKey();
        var f = entry.getValue();
        if (!(f instanceof RecordField.Dynamic d)) {
          throw new IllegalStateException(
              "Record::aspect(LIFT): non-type record entry " + k.toCanonical());
        }
        var entryLiftOpt = AspectLift.getMaybe(xx, d.type());
        if (!(entryLiftOpt instanceof Opt.Some<AspectLift> entryLiftSome)) {
          throw new IllegalStateException(
              "Record entry '" + k.toCanonical() + "' type (" + d.type().typeDesc()
                  + ") missing AspectLift");
        }
        entryLifts.add(new RecordLift.Entry(k, entryLiftSome.value(), d.type()));
      }
      return Opt.some(new RecordLift(entryLifts));
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> field(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "Record::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    var ck = ckOpt.get();
    var typeField = fields.getOrNull(ck);
    if (typeField == null) {
      return EResult.err(loc, "Field '" + ck.toCanonical() + "' not found on record");
    }
    var entryType = recordFieldType(typeField);
    if (base instanceof Opt.Some<RecordBaseDynamic> baseSome) {
      var slotInfo = computeSlotInfo(fields);
      var recordInternal = CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes());
      var recordKlass = JavaInternalName.of(recordInternal);
      int slotIndex = slotInfo.slotOfKey().get(ck);
      var loadSeq = new JavaBytecodeSequence();
      loadSeq.add(baseSome.value().source().realize(xx, loc));
      loadSeq.add(
          JavaBytecodeUtils.accessField(
              loc,
              recordKlass,
              CompositeClassEmitter.slotFieldName(slotIndex),
              CompositeClassEmitter.slotDescriptor(xx, entryType)));
      var fieldCode = Code.of(loc, new CodeDynamic(loc, loadSeq));
      return EResult.ok(
          new CodeValue(fieldCode, Opt.some(ValueDynamic.stack(entryType))));
    }
    if (!(self_.value() instanceof Opt.Some<Value> selfSome)
        || !(selfSome.value() instanceof Record recordValue)) {
      throw new IllegalStateException(
          "Record::field: baseless record self_ does not carry a Record value");
    }
    var valueField = recordValue.fields.getOrNull(ck);
    if (valueField == null) {
      return EResult.err(loc, "Field '" + ck.toCanonical() + "' not found on record value");
    }
    return switch (valueField) {
      case RecordField.EvalRes er -> {
        var inner = er.result();
        if (inner.hasErrors()) {
          yield EResult.err(loc, "Field '" + ck.toCanonical() + "' evaluated to error");
        }
        var childCv = inner.requireValue("Record::field EvalRes");
        var childType = childCv.value() instanceof Opt.Some<Value> cvSome
            ? cvSome.value().valueTypeOf()
            : entryType;
        var useConstOpt = AspectFieldUseConst.getMaybe(xx, childType);
        if (useConstOpt instanceof Opt.Some<AspectFieldUseConst> useConstSome) {
          yield useConstSome.value().fieldUseConst(xx, loc, childCv);
        }
        yield EResult.ok(childCv);
      }
      case RecordField.Dynamic d ->
          throw new IllegalStateException("Record::field: baseless record value has Dynamic field");
    };
  }

  @Override
  public EResult<CodeValue> fieldUseDynamic(
      ExprContext xx,
      QExprId loc,
      compilerxyz.eval.value.ValueDynamic.Source baseSource,
      String javaFieldName) {
    var childSource = new compilerxyz.eval.value.ValueDynamic.Source.Field(baseSource, javaFieldName);
    var fieldCode = Code.of(loc, DynUtil.renderSource(xx, loc, childSource));
    var newFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      var t =
          switch (entry.getValue()) {
            case RecordField.Dynamic d -> d.type();
            case RecordField.EvalRes er ->
                throw new IllegalStateException(
                    "Record::fieldUseDynamic: field-type Record has non-Dynamic entry '"
                        + entry.getKey().toCanonical() + "'");
          };
      newFields.put(entry.getKey(), new RecordField.Dynamic(t));
    }
    Value innerRecord = new Record(Opt.some(new RecordBaseDynamic(childSource)), newFields);
    return EResult.ok(new CodeValue(fieldCode, Opt.some(innerRecord)));
  }

  @Override
  public TList<ConstKey> fieldKeys(ExprContext xx) {
    if (!(base instanceof Opt.Some<RecordBaseDynamic>)) {
      return new TList<>();
    }
    var out = new TList<ConstKey>(fields.size());
    for (var k : fields.keySet()) out.add(k);
    return out;
  }

  @Override
  public Opt<Type> fieldTypeOpt(ExprContext xx, ConstKey key) {
    var f = fields.getOrNull(key);
    if (f == null) return Opt.none();
    return Opt.some(recordFieldType(f));
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.RECORD;
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
              "Record::exporterRootDelegateSelfExport: recursive export_artifact (eval/artifact.rs) port deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "Record::exporterRootDelegateSelfFromArtifactData: recursive import_type_artifact (eval/artifact.rs)"
                  + " port deferred");
        }
      };

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    var subRes = bindSub(xx, loc, cv);
    if (subRes.hasErrors()) {
      return subRes.reerr();
    }
    var sub = subRes.requireValue("Record::bind bindSub");
    if (sub.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
      var bound = xx.target.targetBind(loc, targetSome.value());
      return EResult.ok(
          new BindResult(
              Code.of(loc, bound.code()),
              new BindingRecordBound(sub.scopeFields(), Opt.some(bound.id()))));
    }
    return EResult.ok(
        new BindResult(
            Code.none(loc), new BindingRecordBound(sub.scopeFields(), Opt.none())));
  }

  private record BindSub(
      TOrderedMap<ConstKey, RecordScopeField> scopeFields, Opt<TargetBindSub> targetSub) {}

  private EResult<BindSub> bindSub(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Record::bindSub: CodeValue has no value");
    }
    if (!(some.value() instanceof Record record)) {
      throw new IllegalStateException(
          "Record::bindSub: expected Record value, got " + some.value().getClass());
    }
    var carry = cv.code();

    if (base instanceof Opt.Some<RecordBaseDynamic> baseSome) {
      Type recordType = this;
      var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
      for (var entry : fields.entrySet()) {
        if (!(entry.getValue() instanceof RecordField.Dynamic d)) {
          throw new IllegalStateException("Record::bindSub: bind on non-type record");
        }
        scopeFields.put(entry.getKey(), new RecordScopeField.Dynamic(d.type()));
      }
      var subRes = baseSome.value().recordBaseBindSub(xx, loc, carry, recordType);
      if (subRes.hasErrors()) {
        return subRes.reerr();
      }
      var sub = subRes.requireValue("Record::bindSub base.bindSub");
      return EResult.ok(new BindSub(scopeFields, Opt.some(sub)));
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
                              + key.toCanonical()
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
          var savedValue = elemCv.value();
          var subRes = xx.target.targetMaybeBindSub(xx, loc, elemCv);
          if (subRes.hasErrors()) {
            if (errors == null) errors = new TList<>();
            subRes.drainInto(errors);
            continue;
          }
          var subOpt = subRes.requireValue("Record::bindSub targetMaybeBindSub");
          if (subOpt instanceof Opt.Some<TargetBindSub> subSome) {
            hasTarget = true;
            bindResults.add(new FieldBindResult(key, Opt.some(subSome.value()), Opt.none()));
          } else {
            bindResults.add(
                new FieldBindResult(
                    key,
                    Opt.none(),
                    Opt.some(new CodeValue(Code.none(loc), savedValue))));
          }
        }
      }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    if (!hasTarget) {
      var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
      for (var br : bindResults) {
        if (!(br.constCv() instanceof Opt.Some<CodeValue> constSome)) {
          throw new IllegalStateException(
              "Record::bindSub: !has_target but bindResults contains a target sub");
        }
        var fieldCv = constSome.value();
        if (!(fieldCv.value() instanceof Opt.Some<Value> fieldSome)) {
          throw new IllegalStateException(
              "Record::bindSub: const field CodeValue has no value");
        }
        var fieldType = fieldSome.value().valueTypeOf();
        var bindRes = AspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.bind(xx, loc, fieldCv);
        if (fieldBindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(fieldBindRes.errorsOrNull()));
          continue;
        }
        var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
        scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
      }
      return EResult.ok(new BindSub(scopeFields, Opt.none()));
    }
    var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
    var targetFields = new TList<RecordBindField>(bindResults.size());
    for (var br : bindResults) {
      if (br.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
        scopeFields.put(br.key(), new RecordScopeField.Dynamic(targetSome.value().type()));
        targetFields.add(new RecordBindField(br.key(), targetSome.value()));
      } else if (br.constCv() instanceof Opt.Some<CodeValue> constSome) {
        var fieldCv = constSome.value();
        if (!(fieldCv.value() instanceof Opt.Some<Value> fieldSome)) {
          throw new IllegalStateException(
              "Record::bindSub: const field CodeValue has no value");
        }
        var fieldType = fieldSome.value().valueTypeOf();
        var bindRes = AspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.bind(xx, loc, fieldCv);
        if (fieldBindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(fieldBindRes.errorsOrNull()));
          continue;
        }
        var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
        scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
      } else {
        throw new IllegalStateException("Record::bindSub: bind result has neither target nor const");
      }
    }
    var combined = xx.target.targetBindSubRecord(xx, loc, carry, targetFields);
    return EResult.ok(new BindSub(scopeFields, Opt.some(combined)));
  }

  @Override
  public Type type_(Value value) {
    if (!(value instanceof Record r)) {
      throw new IllegalArgumentException(
          "Record::type_: expected Record value, got " + value.getClass());
    }
    return r.valueTypeOf();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    var srcAsp = AspectRecord.getMaybe(xx, sourceType);
    if (!(srcAsp instanceof Opt.Some<AspectRecord> srcSome)) {
      return false;
    }
    var srcTypes = srcSome.value().recordFieldTypes();
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
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Record::typeCastFrom: CodeValue has no value");
    }
    var srcValue = some.value();
    var srcType = srcValue.valueTypeOf();
    var srcAspOpt = AspectRecord.getMaybe(xx, srcType);
    if (!(srcAspOpt instanceof Opt.Some<AspectRecord> srcAspSome)) {
      throw new IllegalStateException(
          "Record::typeCastFrom: source is not AspectRecord: " + srcType.typeDesc());
    }
    var srcAsp = srcAspSome.value();

    if (srcValue instanceof Record srcRec && srcRec.base instanceof Opt.None<RecordBaseDynamic>) {
      var srcTakenFields = srcAsp.recordTakeFields(xx, loc, srcValue);
      var erb = new Erb(loc);
      var convertedFields = new TOrderedMap<ConstKey, RecordField>();
      for (var destEntry : fields.entrySet()) {
        var key = destEntry.getKey();
        var destType = recordFieldType(destEntry.getValue());
        var srcFieldRes = srcTakenFields.getOrNull(key);
        if (srcFieldRes == null) {
          return EResult.err(
              loc, "Record::typeCastFrom: source missing field '" + key.toCanonical() + "'");
        }
        if (srcFieldRes.hasErrors()) {
          var errs = srcFieldRes.errorsOrNull();
          convertedFields.put(
              key, new RecordField.EvalRes(EResult.errs(errs != null ? errs : new TList<>())));
          continue;
        }
        var srcFieldCv = srcFieldRes.requireValue("Record::typeCastFrom source field");
        var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destType, srcFieldCv));
        if (convOpt.isEmpty()) {
          return EResult.errs(erb.errors());
        }
        convertedFields.put(key, new RecordField.EvalRes(EResult.ok(convOpt.get())));
      }
      if (!erb.errors().isEmpty()) {
        return EResult.errs(erb.errors());
      }
      return EResult.ok(
          new CodeValue(cv.code(), Opt.some(new Record(Opt.none(), convertedFields))));
    }

    ValueDynamic.Source sourcedSource;
    if (srcValue instanceof Record srcRecSourced
        && srcRecSourced.base instanceof Opt.Some<RecordBaseDynamic> srcBaseSome) {
      sourcedSource = srcBaseSome.value().source();
    } else if (srcValue instanceof ValueDynamic vd) {
      sourcedSource = vd.source();
    } else {
      throw new IllegalStateException(
          "Record::typeCastFrom: unexpected source value class: " + srcValue.getClass());
    }

    var srcFieldTypes = srcAsp.recordFieldTypes();
    var srcSlotInfo = computeSlotInfoFromTypes(srcFieldTypes);
    var destSlotInfo = computeSlotInfoFromTypes(recordFieldTypesRaw(fields));

    if (sameTypeIdList(srcSlotInfo.sortedTypes(), destSlotInfo.sortedTypes())) {
      var sourcedResult =
          new Record(Opt.some(new RecordBaseDynamic(sourcedSource)), asDynamicFields(fields));
      return EResult.ok(new CodeValue(cv.code(), Opt.some((Value) sourcedResult)));
    }

    CodeDynamic carryDyn = cv.code().dynamic();
    CodeDynamic sourceLoadCarry;
    ValueDynamic.Source srcAccessSource;
    if (carryDyn != null) {
      var bindResult = xx.target.targetBind(loc, new TargetBindSub(carryDyn, srcType));
      sourceLoadCarry = bindResult.code();
      srcAccessSource = new ValueDynamic.Source.Binding(bindResult.id());
    } else {
      sourceLoadCarry = null;
      srcAccessSource = sourcedSource;
    }
    var srcKlass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, srcSlotInfo.sortedTypes()));

    var destSortedKeys = new TList<ConstKey>(fields.size());
    destSortedKeys.addAll(fields.keySet());
    destSortedKeys.sort(
        (a, b) -> {
          int c =
              TypeId.CANONICAL_ORDER.compare(
                  recordFieldType(fields.getOrNull(a)).typeId(),
                  recordFieldType(fields.getOrNull(b)).typeId());
          if (c != 0) return c;
          return ConstKey.CANONICAL_ORDER.compare(a, b);
        });

    var erb = new Erb(loc);
    var destFieldCodes = new TList<CodeDynamic>(destSortedKeys.size());
    for (int i = 0; i < destSortedKeys.size(); i++) {
      var key = destSortedKeys.get(i);
      var destType = recordFieldType(fields.getOrNull(key));
      var srcFieldType = srcFieldTypes.getOrNull(key);
      int srcSlot = srcSlotInfo.slotOfKey().get(key);
      var loadSeq = new JavaBytecodeSequence();
      loadSeq.add(srcAccessSource.realize(xx, loc));
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
              Opt.some(compilerxyz.eval.value.ValueDynamic.stack(srcFieldType)));
      var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destType, srcFieldCv));
      if (convOpt.isEmpty()) {
        return EResult.errs(erb.errors());
      }
      var convCv = convOpt.get();
      var convDyn = convCv.code().dynamic();
      if (convDyn == null) {
        return EResult.err(
            loc, "Record::typeCastFrom: converted field '" + key.toCanonical() + "' has no dynamic code");
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
        new RecordBaseDynamic(ValueDynamic.Source.Expr.stackFor(this)));
    var newRecord = new Record(newBase, asDynamicFields(fields));
    return EResult.ok(
        new CodeValue(Code.of(loc, finalDyn), Opt.some((Value) newRecord)));
  }

  private static TOrderedMap<ConstKey, RecordField> asDynamicFields(
      TOrderedMap<ConstKey, RecordField> in) {
    var out = new TOrderedMap<ConstKey, RecordField>();
    for (var e : in.entrySet()) {
      var t = switch (e.getValue()) {
        case RecordField.Dynamic d -> d.type();
        case RecordField.EvalRes er -> {
          if (er.result().hasErrors()
              || !(er.result().requireValue("asDynamicFields").value()
                  instanceof Opt.Some<Value> vs)) {
            throw new IllegalStateException(
                "Record::asDynamicFields: EvalRes with no value for '"
                    + e.getKey().toCanonical()
                    + "'");
          }
          yield vs.value().valueTypeOf();
        }
      };
      out.put(e.getKey(), new RecordField.Dynamic(t));
    }
    return out;
  }

  private static CompositeSlotSort.Result<ConstKey> computeSlotInfo(
      TOrderedMap<ConstKey, RecordField> fields) {
    return computeSlotInfoFromTypes(recordFieldTypesRaw(fields));
  }

  private static CompositeSlotSort.Result<ConstKey> computeSlotInfoFromTypes(
      TOrderedMap<ConstKey, Type> fieldTypes) {
    return CompositeSlotSort.sort(fieldTypes, ConstKey.CANONICAL_ORDER);
  }

  private static TOrderedMap<ConstKey, Type> recordFieldTypesRaw(
      TOrderedMap<ConstKey, RecordField> fields) {
    var out = new TOrderedMap<ConstKey, Type>();
    for (var e : fields.entrySet()) {
      out.put(e.getKey(), recordFieldType(e.getValue()));
    }
    return out;
  }

  private static boolean sameTypeIdList(TList<Type> a, TList<Type> b) {
    if (a.size() != b.size()) return false;
    for (int i = 0; i < a.size(); i++) {
      if (!a.get(i).typeId().equals(b.get(i).typeId())) return false;
    }
    return true;
  }

  @Override
  public EResult<CodeDynamic> toDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var carry = cv.code();
    if (base instanceof Opt.Some<RecordBaseDynamic> some) {
      return some.value().recordBaseToDynamic(loc, carry);
    }
    if (!(cv.value() instanceof Opt.Some<Value> valueSome)) {
      throw new IllegalStateException("Record::toDynamic: CodeValue has no value");
    }
    if (!(valueSome.value() instanceof Record record)) {
      throw new IllegalStateException(
          "Record::toDynamic: expected Record value, got " + valueSome.value().getClass());
    }
    TList<ExprErr> errors = null;
    var sortedTypes = new TList<Type>(record.fields.size());
    var entryCodes = new TList<CodeDynamic>(record.fields.size());
    var sortedEntries =
        new TList<java.util.Map.Entry<ConstKey, RecordField>>(record.fields.size());
    sortedEntries.addAll(record.fields.entrySet());
    sortedEntries.sort(
        (a, b) -> {
          int c =
              TypeId.CANONICAL_ORDER.compare(
                  recordFieldType(a.getValue()).typeId(),
                  recordFieldType(b.getValue()).typeId());
          if (c != 0) return c;
          return ConstKey.CANONICAL_ORDER.compare(a.getKey(), b.getKey());
        });
    for (var entry : sortedEntries) {
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
                              + key.toCanonical()
                              + "' is dynamic-only, cannot consume directly to dynamic"))));
        }
        case RecordField.EvalRes er -> {
          var elemRes = er.result();
          if (elemRes.hasErrors()) {
            if (errors == null) errors = new TList<>();
            elemRes.drainInto(errors);
            continue;
          }
          var elemCv = elemRes.requireValue("Record::toDynamic field value");
          if (!(elemCv.value() instanceof Opt.Some<Value> elemValueSome)) {
            throw new IllegalStateException(
                "Record::toDynamic: field '" + key.toCanonical() + "' CodeValue has no value");
          }
          var elemType = elemValueSome.value().valueTypeOf();
          var toDynOpt = AspectConsumeDynamic.getMaybe(xx, elemType);
          if (!(toDynOpt instanceof Opt.Some<AspectConsumeDynamic> toDynSome)) {
            if (errors == null) errors = new TList<>();
            errors.add(
                new ExprErr(
                    loc,
                    TList.of(
                        new ExprSubErr.Opaque(
                            "Record entry '"
                                + key.toCanonical()
                                + "' type ("
                                + elemType.typeDesc()
                                + ") is missing aspect AspectConsumeDynamic"))));
            continue;
          }
          var lowered = toDynSome.value().toDynamic(xx, loc, elemCv);
          if (lowered.hasErrors()) {
            if (errors == null) errors = new TList<>();
            lowered.drainInto(errors);
            continue;
          }
          var loweredCode = lowered.requireValue("Record::toDynamic field lowered");
          sortedTypes.add(elemType);
          entryCodes.add(loweredCode);
        }
      }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
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
  public Binding initialBind(BindingId dynamicKey) {
    var typeFields = new TOrderedMap<ConstKey, Type>();
    for (var entry : fields.entrySet()) {
      if (!(entry.getValue() instanceof RecordField.Dynamic d)) {
        throw new IllegalStateException(
            "Record::initialBind: field " + entry.getKey() + " is not Dynamic-only");
      }
      typeFields.put(entry.getKey(), d.type());
    }
    return new BindingRecordDynamic(dynamicKey, typeFields);
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }

  public static final class RecordLift implements AspectLift {
    public record Entry(ConstKey key, AspectLift lift, Type type) {}

    private final TList<Entry> entryLifts;
    private volatile java.lang.reflect.Field @org.jspecify.annotations.Nullable [] cachedFields;

    public RecordLift(TList<Entry> entryLifts) {
      var sorted = new TList<Entry>(entryLifts.size());
      sorted.addAll(entryLifts);
      sorted.sort(
          (a, b) -> {
            int c = TypeId.CANONICAL_ORDER.compare(a.type().typeId(), b.type().typeId());
            if (c != 0) return c;
            return ConstKey.CANONICAL_ORDER.compare(a.key(), b.key());
          });
      this.entryLifts = sorted;
    }

    @Override
  public EResult<Value> lift(
        ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
      var sortedTypes = new TList<Type>(entryLifts.size());
      for (var entry : entryLifts) sortedTypes.add(entry.type());
      var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
      if (externalHostValue == null || externalHostValue.getClass() != expectedClass) {
        return EResult.err(
            loc,
            "Record RecordLift::lift: expected "
                + expectedClass.getName()
                + ", got "
                + (externalHostValue == null ? "null" : externalHostValue.getClass().getName()));
      }
      var fields = cachedFields;
      if (fields == null) {
        fields = new java.lang.reflect.Field[entryLifts.size()];
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
        cachedFields = fields;
      }
      var out = new TOrderedMap<ConstKey, RecordField>();
      TList<ExprErr> errors = null;
      for (int i = 0; i < entryLifts.size(); i++) {
        var entry = entryLifts.get(i);
        Object fieldValue;
        try {
          fieldValue = fields[i].get(externalHostValue);
        } catch (IllegalAccessException e) {
          throw new AssertionError(
              "Record RecordLift::lift: cannot read entry field for '"
                  + entry.key().toCanonical()
                  + "'",
              e);
        }
        var lifted = entry.lift().lift(xx, loc, fieldValue);
        if (lifted.hasErrors()) {
          if (errors == null) errors = new TList<>();
          lifted.drainInto(errors);
          continue;
        }
        var liftedValue = lifted.requireValue("Record RecordLift::lift entry value");
        out.put(
            entry.key(),
            new RecordField.EvalRes(
                EResult.ok(new CodeValue(Code.none(loc), Opt.some(liftedValue)))));
      }
      if (errors != null && !errors.isEmpty()) {
        return EResult.errs(errors);
      }
      return EResult.ok(new Record(Opt.none(), out));
    }
  }

  @Override
  public TypeId typeId() {
    var ids = new TList<TypeId>(fields.size());
    for (var entry : fields.entrySet()) {
      ids.add(recordFieldType(entry.getValue()).typeId());
    }
    ids.sort(TypeId.CANONICAL_ORDER);
    return new TypeId.Composite(GenTypeIds.RECORD_TYPE_ID, ids);
  }

  private static Type recordFieldType(RecordField f) {
    return switch (f) {
      case RecordField.Dynamic d -> d.type();
      case RecordField.EvalRes er -> {
        var inner = er.result();
        if (!inner.hasErrors()
            && inner.requireValue("Record::recordFieldType inner").value()
                instanceof Opt.Some<Value> some) {
          yield some.value().valueTypeOf();
        }
        yield new Tuple(Opt.none(), new TList<>());
      }
    };
  }
}
