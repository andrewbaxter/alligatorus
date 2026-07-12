package compilerxyz.eval.value.record;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Result;
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
import compilerxyz.gen.Value;
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
import org.objectweb.asm.Opcodes;

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
    if (!(selfCv.value() instanceof Opt.Some<Value> some)) {
      throw new IllegalStateException("Record::accessOrDrop: CodeValue has no value");
    }
    var v = some.value();
    ValueDynamic.Source baseSource;
    Record record;
    if (v instanceof ValueDynamic vd) {
      baseSource = vd.source();
      record = this;
    } else if (v instanceof Record r) {
      record = r;
      if (r.base instanceof Opt.Some<RecordBaseDynamic> baseSome) {
        baseSource = baseSome.value().source();
      } else {
        return accessOrDropSourceless(xx, loc, selfCv, record, accessKey);
      }
    } else {
      throw new IllegalStateException(
          "Record::accessOrDrop: expected Record or ValueDynamic, got " + v.getClass());
    }
    return accessOrDropSourced(xx, loc, selfCv, record, baseSource, accessKey);
  }

  private EResult<CodeValue> accessOrDropSourceless(
      ExprContext xx, QExprId loc, CodeValue selfCv, Record record, Opt<ConstKey> accessKey) {
    // Sourceless: no composite heap object. Each field's value code hasn't been
    // emitted yet — it lives in the RecordField.EvalRes's inner CV until drop
    // (or field access / consumption / object-construction) consumes it.
    //
    // For DROP: iterate reverse canonical order (matching the ordering used
    // elsewhere), for each field emit fieldCv.code (pushes value; may be Code.none
    // for pure-const fields) then the drop code (pops it). Net stack-neutral per
    // field.
    //
    // For ACCESS(key=k): siblings same as above; for the accessed field emit
    // fieldCv.code + STORE to a local. LOAD at the end.
    var seq = new JavaBytecodeSequence();
    var carry = selfCv.code().dynamic();
    if (carry != null) seq.add(carry);
    CodeValue accessedCv = null;
    Type accessedType = null;
    CodeDynamic accessedPushDyn = null;
    ConstKey accessedKeyResolved = accessKey instanceof Opt.Some<ConstKey> aks ? aks.value() : null;
    var sortedKeys = new TList<ConstKey>(record.fields.size());
    sortedKeys.addAll(record.fields.keySet());
    sortedKeys.sort(ConstKey.CANONICAL_ORDER);
    for (int i = sortedKeys.size() - 1; i >= 0; i--) {
      var k = sortedKeys.get(i);
      var f = record.fields.getOrNull(k);
      boolean isAccessed = accessedKeyResolved != null && accessedKeyResolved.equals(k);
      if (!(f instanceof RecordField.EvalRes er)) {
        // RecordField.Dynamic entries appear only in type-level records (schemas);
        // a value record never contains one under current construction paths.
        throw new IllegalStateException(
            "Record::accessOrDropSourceless: field '"
                + k.toCanonical()
                + "' is not EvalRes; sourceless value record cannot carry a"
                + " RecordField.Dynamic entry");
      }
      var res = er.result();
      if (res.hasErrors()) {
        if (isAccessed) {
          return EResult.err(loc, "Field '" + k.toCanonical() + "' evaluated to error");
        }
        continue;
      }
      if (er.isUnreachable()) {
        return EResult.err(
            loc,
            "Field '" + k.toCanonical() + "' does not fall through (contains an unreachable"
                + " control jump like throw/panic); this expression is not valid in a"
                + " record field position");
      }
      var fieldCv = res.requireValue("Record::accessOrDropSourceless field");
      var fieldDyn = fieldCv.code().dynamic();
      var fieldSome = (Opt.Some<Value>) fieldCv.value();
      Type fieldType = fieldSome.value().valueType();
      if (isAccessed) {
        var useConstOpt = AspectFieldUseConst.getMaybe(xx, fieldType);
        if (useConstOpt instanceof Opt.Some<AspectFieldUseConst> useConstSome) {
          var useRes = useConstSome.value().aspectFieldUseConstFieldUseConst(xx, loc, fieldCv);
          if (useRes.hasErrors()) return useRes.reerr();
          accessedCv = useRes.requireValue("Record::accessOrDropSourceless useConst");
          continue;
        }
        if (fieldDyn == null) {
          accessedCv = fieldCv;
          continue;
        }
        accessedPushDyn = fieldDyn;
        accessedType = fieldType;
        continue;
      }
      // Sibling: emit push code + drop code. Try full drop first, then light.
      if (fieldDyn != null) seq.add(fieldDyn);
      var fullDropOpt = AspectDropDynamic.getMaybe(xx, fieldType);
      if (fullDropOpt instanceof Opt.Some<AspectDropDynamic> fullDropSome) {
        var dropRes = fullDropSome.value().aspectDropDynamicDrop(xx, loc, fieldCv);
        if (dropRes.hasErrors()) return dropRes.reerr();
        var dropDyn = dropRes.requireValue("Record::accessOrDropSourceless drop").dynamic();
        if (dropDyn != null) seq.add(dropDyn);
        continue;
      }
      var lightDropOpt = compilerxyz.gen.AspectDropDynamicLight.getMaybe(xx, fieldType);
      if (lightDropOpt instanceof Opt.Some<compilerxyz.gen.AspectDropDynamicLight> lightSome) {
        var dropRes = lightSome.value().aspectDropDynamicLightDropDynamicLight(xx, loc, fieldCv);
        if (dropRes.hasErrors()) return dropRes.reerr();
        var dropDyn = dropRes.requireValue("Record::accessOrDropSourceless light drop").dynamic();
        if (dropDyn != null) seq.add(dropDyn);
        continue;
      }
      return EResult.err(
          loc,
          "Sourceless record field '" + k.toCanonical() + "' type '"
              + fieldType.typeDesc()
              + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
    }
    if (accessKey instanceof Opt.Some<ConstKey> aksReq) {
      if (accessedCv != null) {
        var accessedDyn = accessedCv.code().dynamic();
        if (accessedDyn != null) seq.add(accessedDyn);
        return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), accessedCv.value()));
      }
      if (accessedPushDyn != null) {
        seq.add(accessedPushDyn);
        return EResult.ok(
            new CodeValue(
                Code.of(loc, new CodeDynamic(loc, seq)),
                Opt.some(ValueDynamic.stack(xx, accessedType))));
      }
      return EResult.err(
          loc, "Field '" + aksReq.value().toCanonical() + "' not found on record value");
    }
    return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, seq)), Opt.none()));
  }

  private EResult<CodeValue> accessOrDropSourced(
      ExprContext xx,
      QExprId loc,
      CodeValue selfCv,
      Record record,
      ValueDynamic.Source baseSource,
      Opt<ConstKey> accessKey) {
    var slotInfo = computeSlotInfo(fields);
    var klass =
        JavaInternalName.of(
            CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
    boolean stackRooted = DynUtil.isStackRooted(baseSource);

    ConstKey accessedKey = accessKey instanceof Opt.Some<ConstKey> aks ? aks.value() : null;
    Type accessedType = null;
    int accessedSlot = -1;
    if (accessedKey != null) {
      var f = fields.getOrNull(accessedKey);
      if (f == null) {
        return EResult.err(loc, "Field '" + accessedKey.toCanonical() + "' not found on record");
      }
      accessedType = recordFieldType(f);
      accessedSlot = slotInfo.slotOfKey().get(accessedKey);
    }

    // Fast path: pure field-access (no sibling drops needed).
    if (accessedKey != null && !stackRooted) {
      // Non-stack source: no sibling drops, just base.realize + GETFIELD.
      // Do NOT prepend selfCv.code() carry — for non-stack sources the source
      // already reaches its base (e.g. ALOAD binding, GETFIELD chain); any
      // carry produced by upstream is a side channel that should not repeat.
      var loadSeq = new JavaBytecodeSequence();
      loadSeq.add(baseSource.realize(xx, loc));
      loadSeq.add(
          JavaBytecodeUtils.accessField(
              loc,
              klass,
              CompositeClassEmitter.slotFieldName(accessedSlot),
              CompositeClassEmitter.slotDescriptor(xx, accessedType)));
      return EResult.ok(
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, loadSeq)),
              Opt.some(ValueDynamic.stack(xx, accessedType))));
    }

    // Stack-rooted or drop-only path.
    var out = new JavaBytecodeSequence();
    // Do not include selfCv.code() carry (upstream carry is a side channel that
    // aspectDropDynamicDrop / aspectFieldField ignore per legacy behavior).

    if (accessedKey == null) {
      // Full drop.
      if (!stackRooted) {
        // Non-stack source: base isn't on the stack; defer to enclosing owner.
        // Emit only the source's own drop code (may be Code.none for Binding).
        var srcDrop = baseSource.drop(loc).dynamic();
        if (srcDrop != null) out.add(srcDrop);
        return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), Opt.none()));
      }
      // Stack-rooted: base ref is on top of stack; realize is a no-op for Expr.
      // Emit sibling drops (skipping light-only fields, which GC handles) then POP.
      out.add(baseSource.realize(xx, loc));
      var dropRes = emitSiblingDrops(xx, loc, klass, slotInfo, /* skipKey */ null, out);
      if (dropRes.hasErrors()) return dropRes.reerr();
      out.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
      return EResult.ok(new CodeValue(Code.of(loc, new CodeDynamic(loc, out)), Opt.none()));
    }

    // Field access with stack-rooted base: emit sibling drops (each DUP+GETFIELD+
    // drop, base preserved), then GETFIELD the accessed slot — no DUP needed
    // because we consume the base here (the accessed field replaces it on stack).
    var sibRes = emitSiblingDrops(xx, loc, klass, slotInfo, accessedKey, out);
    if (sibRes.hasErrors()) return sibRes.reerr();
    out.add(
        JavaBytecodeUtils.accessField(
            loc,
            klass,
            CompositeClassEmitter.slotFieldName(accessedSlot),
            CompositeClassEmitter.slotDescriptor(xx, accessedType)));
    return EResult.ok(
        new CodeValue(
            Code.of(loc, new CodeDynamic(loc, out)),
            Opt.some(ValueDynamic.stack(xx, accessedType))));
  }

  private EResult<Void> emitSiblingDrops(
      ExprContext xx,
      QExprId loc,
      JavaInternalName klass,
      compilerxyz.eval.dynamic.CompositeSlotSort.Result<ConstKey> slotInfo,
      @org.jspecify.annotations.Nullable ConstKey skipKey,
      JavaBytecodeSequence out) {
    var sortedKeys = new TList<ConstKey>(fields.size());
    sortedKeys.addAll(fields.keySet());
    sortedKeys.sort(ConstKey.CANONICAL_ORDER);
    for (int i = sortedKeys.size() - 1; i >= 0; i--) {
      var k = sortedKeys.get(i);
      if (skipKey != null && skipKey.equals(k)) continue;
      var t = recordFieldType(fields.getOrNull(k));
      int slotIdx = slotInfo.slotOfKey().get(k);
      var dropRes = childFullDropCode(xx, loc, klass, slotIdx, t, k);
      if (dropRes.hasErrors()) return dropRes.reerr();
      var dropCode = dropRes.requireValue("Record::emitSiblingDrops child");
      if (dropCode != null) out.add(dropCode);
    }
    return EResult.ok(null);
  }

  /**
   * Emit sibling-drop bytecode for one field. Returns:
   * <ul>
   *   <li>{@code Ok(some(code))} — field has a non-light drop; emit {@code DUP; GETFIELD; drop_side_effect}.
   *   <li>{@code Ok(none())} — field is light-drop only; skip entirely (GC handles it when the
   *       composite is dropped).
   *   <li>{@code Err} — field type has neither drop aspect.
   * </ul>
   */
  private EResult<@org.jspecify.annotations.Nullable CodeDynamic> childFullDropCode(
      ExprContext xx, QExprId loc, JavaInternalName klass, int slotIdx, Type childType,
      ConstKey key) {
    var fullDropOpt = AspectDropDynamic.getMaybe(xx, childType);
    if (fullDropOpt instanceof Opt.Some<AspectDropDynamic> fullSome) {
      var seq = new JavaBytecodeSequence();
      seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
      seq.add(
          JavaBytecodeUtils.accessField(
              loc,
              klass,
              CompositeClassEmitter.slotFieldName(slotIdx),
              CompositeClassEmitter.slotDescriptor(xx, childType)));
      var childCv =
          new CodeValue(
              Code.of(loc, new CodeDynamic(loc, new JavaBytecodeSequence())),
              Opt.some(ValueDynamic.stack(xx, childType)));
      var res = fullSome.value().aspectDropDynamicDrop(xx, loc, childCv);
      if (res.hasErrors()) return res.reerr();
      var d = res.requireValue("Record::childFullDropCode drop").dynamic();
      if (d != null) seq.add(d);
      return EResult.ok(new CodeDynamic(loc, seq));
    }
    if (compilerxyz.gen.AspectDropDynamicLight.getMaybe(xx, childType)
        instanceof Opt.Some<compilerxyz.gen.AspectDropDynamicLight>) {
      // Light-drop only: no per-field side effect, GC handles it when the composite
      // is dropped. Skip emitting DUP+GETFIELD+POP for this sibling entirely.
      return EResult.ok(null);
    }
    return EResult.err(
        loc,
        "Record field '" + key.toCanonical() + "' type '" + childType.typeDesc()
            + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
  }

  @Override
  public TOrderedMap<ConstKey, Type> aspectRecordRecordFieldTypes() {
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
  public TOrderedMap<ConstKey, EResult<CodeValue>> aspectRecordRecordTakeFields(ExprContext xx, QExprId loc, Value value) {
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
  public compilerxyz.eval.dynamic.JavaDataDescriptor aspectConsumeDynamicConsumeDynamicJvmClass(compilerxyz.eval.ExprContext xx) {
    var slotInfo = computeSlotInfo(fields);
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
                  return e.getKey().toCanonical() + ": " + tDesc;
                })
            .collect(Collectors.joining(", "));
    return "record(" + inner + ")";
  }

  @Override
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
          return Opt.none();
        }
        var entryLiftOpt = AspectLift.getMaybe(xx, d.type());
        if (!(entryLiftOpt instanceof Opt.Some<AspectLift> entryLiftSome)) {
          return Opt.none();
        }
        entryLifts.add(new RecordLift.Entry(k, entryLiftSome.value(), d.type()));
      }
      return Opt.some(new RecordLift(entryLifts));
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
    var erb = new Erb(loc);
    if (!(key.value() instanceof Opt.Some<Value> keySome)) {
      return EResult.err(loc, "Record::field: key has no compile-time value");
    }
    var ckOpt = erb.mustRes(Eval.extractConstKey(xx, loc, keySome.value()));
    if (ckOpt.isEmpty()) return EResult.errs(erb.errors());
    return accessOrDrop(xx, loc, self_, Opt.some(ckOpt.get()));
  }

  @Override
  public EResult<CodeValue> aspectFieldUseDynamicFieldUseDynamic(
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
  public TList<ConstKey> aspectFieldFieldKeys(ExprContext xx) {
    if (!(base instanceof Opt.Some<RecordBaseDynamic>)) {
      return new TList<>();
    }
    var out = new TList<ConstKey>(fields.size());
    for (var k : fields.keySet()) out.add(k);
    return out;
  }

  @Override
  public Opt<Type> aspectFieldFieldTypeOpt(ExprContext xx, ConstKey key) {
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
          var cell = new ArtifactSupport.ArtifactIdHolder();
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
                          if (!(cv.value() instanceof Opt.Some<Value> some)) {
                            errors.add(
                                new ExprErr(
                                    loc,
                                    TList.of(
                                        new ExprSubErr.Opaque(
                                            "Record field has no value to serialize"))));
                            fieldArts.put(key, nullArtifact());
                            continue;
                          }
                          fieldType = some.value().valueType();
                        }
                        default -> throw new IllegalStateException();
                      }
                      var artRes = ArtifactSupport.exportArtifact(xx, loc, fieldType);
                      artRes.drainInto(errors);
                      fieldArts.put(
                          key,
                          artRes.valueOrNull() != null
                              ? artRes.requireValue("Record::exporterExport field artifact")
                              : nullArtifact());
                    }
                    var body = new ArtifactRecordType(fieldArts);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(loc, "Record requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactRecordType body;
                try {
                  body = JsonValues.fromValue(ArtifactRecordType.class, raw);
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
                      holder[0] = new Record(Opt.none(), fields);
                    };
                return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
              });
        }
      };

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    var subRes = bindSub(xx, loc, cv);
    if (subRes.hasErrors()) {
      return subRes.reerr();
    }
    var sub = subRes.requireValue("Record::bind bindSub");
    if (sub.targetSub() instanceof Opt.Some<TargetBindSub> targetSome) {
      var bound = xx.target.targetBind(xx, loc, targetSome.value());
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
        var fieldType = fieldSome.value().valueType();
        var bindRes = AspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
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
        var fieldType = fieldSome.value().valueType();
        var bindRes = AspectBind.get(xx, loc, fieldType);
        if (bindRes.hasErrors()) {
          scopeFields.put(br.key(), new RecordScopeField.Error(bindRes.errorsOrNull()));
          continue;
        }
        var bind = bindRes.requireValue("Record::bindSub AspectBind");
        var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
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
  public Type aspectTypeType(Value value) {
    if (!(value instanceof Record r)) {
      throw new IllegalArgumentException(
          "Record::type_: expected Record value, got " + value.getClass());
    }
    return r.valueType();
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    var srcAsp = AspectRecord.getMaybe(xx, sourceType);
    if (!(srcAsp instanceof Opt.Some<AspectRecord> srcSome)) {
      return false;
    }
    var srcTypes = srcSome.value().aspectRecordRecordFieldTypes();
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
    var srcType = srcValue.valueType();
    var srcAspOpt = AspectRecord.getMaybe(xx, srcType);
    if (!(srcAspOpt instanceof Opt.Some<AspectRecord> srcAspSome)) {
      throw new IllegalStateException(
          "Record::typeCastFrom: source is not AspectRecord: " + srcType.typeDesc());
    }
    var srcAsp = srcAspSome.value();

    if (srcValue instanceof Record srcRec && srcRec.base instanceof Opt.None<RecordBaseDynamic>) {
      var srcTakenFields = srcAsp.aspectRecordRecordTakeFields(xx, loc, srcValue);
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

    var srcFieldTypes = srcAsp.aspectRecordRecordFieldTypes();
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
      var bindResult = xx.target.targetBind(xx, loc, new TargetBindSub(carryDyn, srcType));
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
              Opt.some(compilerxyz.eval.value.ValueDynamic.stack(xx, srcFieldType)));
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
        new RecordBaseDynamic(ValueDynamic.Source.Expr.stackFor(xx, this)));
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
          yield vs.value().valueType();
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
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
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
          var elemType = elemValueSome.value().valueType();
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
          var lowered = toDynSome.value().aspectConsumeDynamicToDynamic(xx, loc, elemCv);
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
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
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
  public EResult<Value> aspectLiftLift(
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
        var lifted = entry.lift().aspectLiftLift(xx, loc, fieldValue);
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
          yield some.value().valueType();
        }
        yield new Tuple(Opt.none(), new TList<>());
      }
    };
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
