package compilerxyz.eval.value.record;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.dynamic.RecordClassEmitter;
import compilerxyz.eval.Binding;
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
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamic;
import compilerxyz.gen.AspectField;
import compilerxyz.gen.AspectFieldUseConst;
import compilerxyz.gen.AspectFieldUseDynamic;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectLift;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.Target;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
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
        ExporterAny {

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.Code> drop(
      compilerxyz.eval.ExprContext xx,
      compilerxyz.eval.QExprId loc,
      compilerxyz.eval.CodeValue cv) {
    // Record is a destructive stack-allocated composite: its runtime shape is a
    // RecordClassEmitter-generated class whose public Object fields hold the
    // per-key values. When the base sits on the JVM stack (Source.Expr) each
    // AspectDropDynamic-carrying child needs a DUP + GETFIELD + child.drop()
    // before the base is popped. Records with a dynamic base thread the source
    // through RecordBaseDynamic (Source.Field / Source.Binding), so the
    // stack-rooted case is only reachable for base==None records; in the
    // current test corpus these never appear as ValueDynamic with Source.Expr
    // (see feedback: records/tuples flow through Source.Binding). Implemented
    // for future-proofing per the drop policy.
    if (!(cv.value() instanceof compilerxyz.util.Opt.Some<Value> some)
        || !(some.value() instanceof compilerxyz.eval.value.ValueDynamic vd)
        || !(vd.source() instanceof compilerxyz.eval.value.ValueDynamic.Source.Expr)) {
      return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
    }
    if (base instanceof Opt.Some<RecordBaseDynamic>) {
      return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
    }
    var fieldKeys = new TList<compilerxyz.eval.dynamic.RecordClassEmitter.FieldKey>(fields.size());
    var sortedKeys = new TList<ConstKey>(fields.size());
    sortedKeys.addAll(fields.keySet());
    sortedKeys.sort(ConstKey.CANONICAL_ORDER);
    for (var k : sortedKeys) {
      var f = fields.getOrNull(k);
      var t = recordFieldType(f);
      fieldKeys.add(new compilerxyz.eval.dynamic.RecordClassEmitter.FieldKey(k.toCanonical(), t));
    }
    var klass =
        compilerxyz.eval.dynamic.JavaInternalName.of(
            compilerxyz.eval.dynamic.RecordClassEmitter.internalNameFor(fieldKeys));
    var children = new TList<DynUtil.DropChild>(fieldKeys.size());
    for (int i = 0; i < fieldKeys.size(); i++) {
      var fk = fieldKeys.get(i);
      var fieldName = compilerxyz.eval.dynamic.RecordClassEmitter.disambiguatedFieldName(fieldKeys, i);
      var access =
          compilerxyz.eval.dynamic.JavaBytecodeUtils.accessField(
              loc,
              klass,
              fieldName,
              compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT);
      children.add(new DynUtil.DropChild(fk.type(), access));
    }
    return DynUtil.dropDestructiveComposite(xx, loc, cv, children);
  }

  @Override
  public compilerxyz.eval.dynamic.JavaDataDescriptor consumeDynamicJvmClass() {
    return compilerxyz.eval.dynamic.JavaDataDescriptor.fromInternalName(compilerxyz.eval.dynamic.JvmConstants.INTNAME_OBJECT);
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
        || key.equals(AspectInitialBind.ID)) {
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
      var useDynOpt = AspectFieldUseDynamic.getMaybe(xx, entryType);
      if (useDynOpt instanceof Opt.Some<AspectFieldUseDynamic> useDynSome) {
        return useDynSome.value().fieldUseDynamic(
            xx, loc, baseSome.value().source(), ck.toCanonical());
      }
      return compilerxyz.eval.dynamic.DynUtil.fieldUseDefaultDynamic(
          xx, loc, entryType, baseSome.value().source(), ck.toCanonical());
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
  public ArtifactExporter exporter() {
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
    var targetFields = new TList<Target.RecordBindField>(bindResults.size());
    for (var br : bindResults) {
      if (br.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
        scopeFields.put(br.key(), new RecordScopeField.Dynamic(targetSome.value().type()));
        targetFields.add(new Target.RecordBindField(br.key(), targetSome.value()));
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
    var combined = xx.target.targetBindSubRecord(loc, carry, targetFields);
    return EResult.ok(new BindSub(scopeFields, Opt.some(combined)));
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
    var fieldKeys = new TList<RecordClassEmitter.FieldKey>(record.fields.size());
    var entryCodes = new TList<CodeDynamic>(record.fields.size());
    var sortedEntries =
        new TList<java.util.Map.Entry<ConstKey, RecordField>>(record.fields.size());
    sortedEntries.addAll(record.fields.entrySet());
    sortedEntries.sort((a, b) -> ConstKey.CANONICAL_ORDER.compare(a.getKey(), b.getKey()));
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
          fieldKeys.add(new RecordClassEmitter.FieldKey(key.toCanonical(), elemType));
          entryCodes.add(loweredCode);
        }
      }
    }
    if (errors != null && !errors.isEmpty()) {
      return EResult.errs(errors);
    }
    var klass =
        compilerxyz.eval.dynamic.JavaInternalName.of(RecordClassEmitter.internalNameFor(fieldKeys));
    return EResult.ok(
        DynUtil.wrapWithCarry(
            loc,
            carry,
            compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiateVarargs(loc, klass, entryCodes)));
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
      sorted.sort((a, b) -> ConstKey.CANONICAL_ORDER.compare(a.key(), b.key()));
      this.entryLifts = sorted;
    }

    @Override
  public EResult<Value> lift(
        ExprContext xx, QExprId loc, @org.jspecify.annotations.Nullable Object externalHostValue) {
      var fieldKeys = new TList<RecordClassEmitter.FieldKey>(entryLifts.size());
      for (var entry : entryLifts) {
        fieldKeys.add(
            new RecordClassEmitter.FieldKey(entry.key().toCanonical(), entry.type()));
      }
      var expectedClass = RecordClassEmitter.getOrEmit(fieldKeys);
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
          var fieldName = RecordClassEmitter.disambiguatedFieldName(fieldKeys, i);
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
        yield new Tuple(new TList<>());
      }
    };
  }
}
