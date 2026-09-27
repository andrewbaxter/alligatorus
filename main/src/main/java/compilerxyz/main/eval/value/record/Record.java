package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.PostDeserializeIdentity;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.CompositeSlotSort;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseConst;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactRecordTypeCodec;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Field;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6134L)
public record Record(
        Opt<RecordBaseDynamic> base,
        IOrderedMap<ConstKey, RecordField> fields,
        ArtifactSupport.ArtifactIdHolder artifactIdCell)
        implements Aspect, Type, Value {

    public Record(Opt<RecordBaseDynamic> base, IOrderedMap<ConstKey, RecordField> fields) {
        this(base, fields, new ArtifactSupport.ArtifactIdHolder());
    }

    public EResult<CodeValue> accessOrDrop(ExprContext xx, QExprId loc, CodeValue selfCv, Opt<ConstKey> accessKey) {
        if (selfCv.value().optIsNone()) {
            throw new IllegalStateException("Record::accessOrDrop: CodeValue has no value");
        }
        var record = MustCast.mustCast(Record.class, selfCv.value().optUnwrap());
        return switch (record.base()) {
            case Opt.None<RecordBaseDynamic> n -> {
                var b = new JavaBytecodeBuilder(loc);
                var carryRes = b.addCode(loc, selfCv.code());
                if (carryRes.optIsSome()) yield EResult.errs(carryRes.optUnwrap());
                EvalOutcome accessedOutcome = null;
                var held = new TList<CodeValue>();
                for (var entry : record.fields()) {
                    var k = entry.getKey();
                    var f = entry.getValue();
                    boolean isAccessed =
                            accessKey.optIsSome() && accessKey.optUnwrap().equals(k);
                    RecordField.EvalRes er;
                    switch (f) {
                        case RecordField.EvalRes erVal -> er = erVal;
                        case RecordField.Dynamic d ->
                            throw new IllegalStateException("Record::accessOrDropSourceless: field '"
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
                    var cv = res.valueOpt();
                    if (!res.hasErrors()
                            && cv.optIsSome()
                            && cv.optUnwrap().value().optIsNone()) {
                        yield EResult.err(
                                loc,
                                "Field '" + k.constKeyToCanonical() + "' does not fall through (contains an unreachable"
                                        + " control jump like throw/panic); this expression is not valid in a"
                                        + " record field position");
                    }
                    var fieldCv = res.requireValue("Record::accessOrDropSourceless field");
                    Type fieldType = fieldCv.value().optUnwrap().valueType();
                    if (isAccessed) {
                        var useCv = fieldCv;
                        var useConstOpt = GenAspectFieldUseConst.getMaybe(xx, fieldType);
                        if (useConstOpt.optIsSome()) {
                            var useRes = useConstOpt.optUnwrap().aspectFieldUseConstFieldUseConst(xx, loc, fieldCv);
                            if (useRes.hasErrors()) yield useRes.reerr();
                            useCv = useRes.requireValue("Record::accessOrDropSourceless useConst");
                        }
                        var useAdd = b.addCode(loc, useCv.code());
                        if (useAdd.optIsSome()) yield EResult.errs(useAdd.optUnwrap());
                        accessedOutcome = useCv.outcome();
                        continue;
                    }
                    var fieldAdd = b.addCode(loc, fieldCv.code());
                    if (fieldAdd.optIsSome()) yield EResult.errs(fieldAdd.optUnwrap());
                    held.add(fieldCv);
                }
                for (int hi = held.size() - 1; hi >= 0; hi--) {
                    var heldCv = held.get(hi);
                    var fullDropOpt = GenAspectDropDynamic.getMaybe(
                            xx, heldCv.value().optUnwrap().valueType());
                    if (fullDropOpt.optIsSome()) {
                        var dropRes = fullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, heldCv);
                        if (dropRes.hasErrors()) yield dropRes.reerr();
                        var dropAdd = b.addCode(loc, dropRes.requireValue("Record::accessOrDropSourceless drop"));
                        if (dropAdd.optIsSome()) yield EResult.errs(dropAdd.optUnwrap());
                    }
                }
                if (accessKey.optIsSome()) {
                    if (accessedOutcome == null) {
                        yield EResult.err(
                                loc,
                                "Field '" + accessKey.optUnwrap().constKeyToCanonical()
                                        + "' not found on record value");
                    }
                    if (b.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), accessedOutcome));
                    yield EResult.ok(new CodeValue(Code.target(b.code()), accessedOutcome));
                }
                if (b.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.NO_VALUE));
                yield EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
            }
            case Opt.Some<RecordBaseDynamic> s -> {
                var baseSource = s.value().source();
                var dynamicFieldTypes = new TOrderedMap<ConstKey, Type>();
                for (var entry : record.fields()) {
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> dynamicFieldTypes.put(entry.getKey(), d.type());
                        case RecordField.EvalRes er -> {}
                    }
                }
                var slotInfo = computeSlotInfoFromTypes(xx, dynamicFieldTypes);
                var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes()));
                if (accessKey.optIsNone()) {
                    var out = new JavaBytecodeBuilder(loc);
                    var ecdKeys = new TList<ConstKey>(record.fields().size());
                    ecdKeys.addAll(record.fields().keys());
                    for (int ecdI = ecdKeys.size() - 1; ecdI >= 0; ecdI--) {
                        var k = ecdKeys.get(ecdI);
                        var tOpt = dynamicFieldTypes.getOpt(k);
                        if (tOpt.optIsNone()) {
                            var heldRes = ((RecordField.EvalRes)
                                            record.fields().getOpt(k).optUnwrap())
                                    .result();
                            if (heldRes.hasErrors()) yield heldRes.reerr();
                            var heldCv = heldRes.requireValue("Record::drop held field");
                            var heldDrop = Eval.dropValue(xx, loc, heldCv);
                            if (heldDrop.hasErrors()) yield heldDrop.reerr();
                            var heldCodeAdd = out.addCode(loc, heldCv.code());
                            if (heldCodeAdd.optIsSome()) yield EResult.errs(heldCodeAdd.optUnwrap());
                            var heldAdd = out.addCode(loc, heldDrop.requireValue("Record::drop held code"));
                            if (heldAdd.optIsSome()) yield EResult.errs(heldAdd.optUnwrap());
                            continue;
                        }
                        var t = tOpt.optUnwrap();
                        int slotIdx = slotInfo.slotOfKey().getOpt(k).optUnwrap();
                        var cfdcFullDropOpt = GenAspectDropDynamic.getMaybe(xx, t);
                        if (cfdcFullDropOpt.optIsSome()) {
                            var access = new JavaBytecodeBuilder(loc);
                            var child = access.getField(
                                    klass,
                                    CompositeClassEmitter.slotFieldName(slotIdx),
                                    CompositeClassEmitter.slotDescriptor(xx, t),
                                    DynUtil.sourceReg(access, baseSource));
                            var childCv = DynUtil.regToTypedValue(xx, loc, access.code(), child, t);
                            var cfdcDropRes = cfdcFullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, childCv);
                            if (cfdcDropRes.hasErrors()) yield cfdcDropRes.reerr();
                            out.add(access.code());
                            var dropAdd = out.addCode(loc, cfdcDropRes.requireValue("Record::childFullDropCode drop"));
                            if (dropAdd.optIsSome()) yield EResult.errs(dropAdd.optUnwrap());
                        }
                    }
                    if (out.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.NO_VALUE));
                    yield EResult.ok(new CodeValue(Code.target(out.code()), EvalOutcome.NO_VALUE));
                } else {
                    var accessedKey = accessKey.optUnwrap();
                    var fOpt = record.fields().getOpt(accessedKey);
                    if (fOpt.optIsNone()) {
                        yield EResult.err(loc, "Field '" + accessedKey.constKeyToCanonical() + "' not found on record");
                    }
                    var f = fOpt.optUnwrap();
                    switch (f) {
                        case RecordField.Dynamic d -> {}
                        case RecordField.EvalRes er ->
                            throw new IllegalStateException(
                                    "Record::accessSourced: accessed field '" + accessedKey.constKeyToCanonical()
                                            + "' is scope-bound (non-dynamic); access should be routed elsewhere");
                    }
                    var accessedType = recordFieldType(f);
                    int accessedSlot = slotInfo.slotOfKey().getOpt(accessedKey).optUnwrap();
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
                    var access = new JavaBytecodeBuilder(loc);
                    var selfAdd = access.addCode(loc, selfCv.code());
                    if (selfAdd.optIsSome()) yield EResult.errs(selfAdd.optUnwrap());
                    var fieldName = CompositeClassEmitter.slotFieldName(accessedSlot);
                    var fieldDesc = CompositeClassEmitter.slotDescriptor(xx, accessedType);
                    var childSource = new ValueDynamicSource.Field(baseSource, klass, fieldName, fieldDesc);
                    var useRes = useDynOpt.optUnwrap().aspectFieldUseDynamicFieldUseDynamic(xx, loc, childSource);
                    if (useRes.hasErrors()) yield useRes.reerr();
                    var useCv = useRes.requireValue("Record::accessSourced field use");
                    var combined = Eval.codeConcat(xx, loc, Code.target(access.code()), useCv.code());
                    if (combined.hasErrors()) yield combined.reerr();
                    yield EResult.ok(
                            new CodeValue(combined.requireValue("Record::accessSourced code"), useCv.outcome()));
                }
            }
        };
    }

    @Override
    public String typeDesc() {
        var parts = new TList<String>(fields.size());
        for (var e : fields) {
            var tDesc =
                    switch (e.getValue()) {
                        case RecordField.Dynamic d -> d.type().typeDesc();
                        case RecordField.EvalRes r -> "?";
                    };
            parts.add(e.getKey().constKeyToCanonical() + ": " + tDesc);
        }
        return "record(" + String.join(", ", parts) + ")";
    }

    public Type valueType() {

        var fieldTypes = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : fields) {
            var t = recordFieldType(entry.getValue());
            fieldTypes.put(entry.getKey(), new RecordField.Dynamic(t));
        }
        return new Record(base, IOrderedMap.from(fieldTypes));
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicRecord(this));
        }
        if (key.equals(GenTrivialIdentityAspects.RECORD_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            return recordFieldsAllTypes() ? Opt.some(AspectTypeRecord.INSTANCE) : Opt.none();
        }
        if (key.equals(GenAspectRecord.AspectId)) {
            return Opt.some(new AspectRecordRecord(this));
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicRecord(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldRecord(this));
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return Opt.some(new AspectDropDynamicRecord(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicRecord(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindRecord(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactRecord.INSTANCE);
        }
        if (key.equals(GenAspectRecord.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            var entryLifts = new TList<RecordLift.Entry>(fields.size());
            for (var entry : fields) {
                var k = entry.getKey();
                var f = entry.getValue();
                RecordField.Dynamic d;
                switch (f) {
                    case RecordField.Dynamic df -> d = df;
                    case RecordField.EvalRes er -> {
                        var inner = er.result();
                        if (inner.hasErrors() || inner.valueOpt().optIsNone()) {
                            return Opt.none();
                        }
                        var cv = inner.requireValue("Record::typeAspect lift field");
                        if (cv.value().optIsNone()) {
                            return Opt.none();
                        }
                        var fv = cv.value().optUnwrap();
                        if (!(fv instanceof ValueDynamic)) {
                            entryLifts.add(
                                    new RecordLift.Entry(k, (xx2, loc2, host) -> EResult.ok(fv), fv.valueType()));
                            continue;
                        }
                        d = new RecordField.Dynamic(fv.valueType());
                    }
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

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof Record record)) {
                return EResult.err(
                        loc, "Record::exporterRootDelegateSelfExport: expected Record, got " + self.getClass());
            }
            var cell = record.artifactIdCell();
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var erb = new Erb(loc);
                var fieldArts = new TOrderedMap<ConstKey, ArtifactRecordField>();
                for (var entry : record.fields()) {
                    var key = entry.getKey();
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> {
                            var artOpt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, d.type()));
                            if (artOpt.optIsSome())
                                fieldArts.put(key, new ArtifactRecordField(false, artOpt.optUnwrap()));
                        }
                        case RecordField.EvalRes r -> {
                            var cvOpt = erb.mustErMaybeUnreachable(r.result());
                            if (cvOpt.optIsNone()) continue;
                            var cv = cvOpt.optUnwrap();
                            if (cv.value().optIsNone()) {
                                erb.errPush("Record field has no value to serialize");
                                continue;
                            }
                            var artOpt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(
                                    xx, loc, cv.value().optUnwrap()));
                            if (artOpt.optIsSome())
                                fieldArts.put(key, new ArtifactRecordField(true, artOpt.optUnwrap()));
                        }
                    }
                }
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var body = new ArtifactRecordType(fieldArts);
                return EResult.ok(makeBody.apply(ArtifactRecordTypeCodec.toValue(body)));
            });
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
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
                        var deps = new TList<Artifact>();
                        for (var entry : body.fields().entrySet())
                            deps.add(entry.getValue().artifact());
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var fields = new TOrderedMap<ConstKey, RecordField>();
                            for (var entry : body.fields().entrySet()) {
                                if (entry.getValue().isValue()) {
                                    var eRes = ArtifactSupport.independentFinalizeImportArtifact(
                                            xx2, loc2, entry.getValue().artifact());
                                    var ea = eRes.requireValue("Record::finalizer field exporter");
                                    var v = ArtifactSupport.importedAsValue(ea);
                                    fields.put(
                                            entry.getKey(),
                                            new RecordField.EvalRes(EResult.ok(
                                                    new CodeValue(Code.none(loc2), EvalOutcome.ofValue(v)))));
                                } else {
                                    var tRes = ArtifactSupport.independentFinalizeImportTypeArtifact(
                                            xx2, loc2, entry.getValue().artifact());
                                    var t = tRes.requireValue("Record::finalizer field type");
                                    fields.put(entry.getKey(), new RecordField.Dynamic(t));
                                }
                            }
                            var out = new Record(Opt.none(), IOrderedMap.from(fields));
                            out.artifactIdCell().set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(Opt.none(), deps, fin));
                    });
        }
    };

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.RECORD_IDENTITY, Record.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (fields.size() != o.fields().size()) return false;
        for (var entry : fields) {
            var otherFieldOpt = o.fields().getOpt(entry.getKey());
            if (otherFieldOpt.optIsNone()) return false;
            var otherField = otherFieldOpt.optUnwrap();
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
        for (var k : fields.keys()) {
            if (srcTypes.getOpt(k).optIsNone()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        var jvmTargetRes = DynUtil.jvmTarget(xx, loc);
        if (jvmTargetRes.hasErrors()) return jvmTargetRes.reerr();
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Record::typeCastFrom: CodeValue has no value");
        }
        var srcValue = cv.value().optUnwrap();
        var srcType = srcValue.valueType();
        var srcAspOpt = GenAspectRecord.getMaybe(xx, srcType);
        if (srcAspOpt.optIsNone()) {
            throw new IllegalStateException("Record::typeCastFrom: source is not AspectRecord: " + srcType.typeDesc());
        }
        var srcAsp = srcAspOpt.optUnwrap();

        if (srcValue instanceof Record srcRec && srcRec.base().optIsNone()) {
            var srcTakenFields = srcAsp.aspectRecordTakeFields(xx, loc, srcValue);
            var erb = new Erb(loc);
            var convertedFields = new TOrderedMap<ConstKey, RecordField>();
            for (var destEntry : fields) {
                var key = destEntry.getKey();
                var destType = recordFieldType(destEntry.getValue());
                var srcFieldResOpt = srcTakenFields.getOpt(key);
                if (srcFieldResOpt.optIsNone()) {
                    return EResult.err(
                            loc, "Record::typeCastFrom: source missing field '" + key.constKeyToCanonical() + "'");
                }
                var srcFieldRes = srcFieldResOpt.optUnwrap();
                if (srcFieldRes.hasErrors()) {
                    convertedFields.put(
                            key,
                            new RecordField.EvalRes(
                                    EResult.errs(srcFieldRes.errorsOpt().optUnwrapOr(new TList<>()))));
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
            return EResult.ok(new CodeValue(
                    cv.code(), EvalOutcome.ofValue(new Record(Opt.none(), IOrderedMap.from(convertedFields)))));
        }

        ValueDynamicSource sourcedSource;
        if (srcValue instanceof Record srcRecSourced && srcRecSourced.base().optIsSome()) {
            sourcedSource = srcRecSourced.base().optUnwrap().source();
        } else if (srcValue instanceof ValueDynamic vd) {
            sourcedSource = vd.source();
        } else {
            throw new IllegalStateException(
                    "Record::typeCastFrom: unexpected source value class: " + srcValue.getClass());
        }

        var srcFieldTypes = srcAsp.aspectRecordFieldTypes();
        var srcSlotInfo = computeSlotInfoFromTypes(xx, srcFieldTypes);
        var destSlotInfo = computeSlotInfoFromTypes(xx, recordFieldTypesRaw(fields));

        var a = srcSlotInfo.sortedTypes();
        var stlB = destSlotInfo.sortedTypes();
        boolean types = a.size() == stlB.size();
        for (int stlI = 0; types && stlI < a.size(); stlI++) {
            if (!a.get(stlI).typeIsEqual(xx, stlB.get(stlI))) types = false;
        }
        if (types) {
            var sourcedResult = new Record(Opt.some(new RecordBaseDynamic(sourcedSource)), asDynamicFields(fields));
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue((Value) sourcedResult)));
        }

        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var srcReg = DynUtil.sourceReg(b, sourcedSource);
        var srcKlass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, srcSlotInfo.sortedTypes()));

        var destSortedKeys = new TList<ConstKey>(fields.size());
        for (var k : fields.keys()) destSortedKeys.add(k);
        for (var k : fields.keys()) {
            destSortedKeys.set(destSlotInfo.slotOfKey().getOpt(k).optUnwrap(), k);
        }

        var erb = new Erb(loc);
        var destFieldRegs = new TList<JavaBytecodeReg>(destSortedKeys.size());
        for (int i = 0; i < destSortedKeys.size(); i++) {
            var key = destSortedKeys.get(i);
            var destType = recordFieldType(fields.getOpt(key).optUnwrap());
            var srcFieldType = srcFieldTypes.getOpt(key).optUnwrap();
            int srcSlot = srcSlotInfo.slotOfKey().getOpt(key).optUnwrap();
            var load = b.getField(
                    srcKlass,
                    CompositeClassEmitter.slotFieldName(srcSlot),
                    CompositeClassEmitter.slotDescriptor(xx, srcFieldType),
                    srcReg);
            var srcFieldCv = DynUtil.regToValue(new CodeDynamic(loc, new JavaBytecodeSequence()), load, srcFieldType);
            var convOpt = erb.mustEr(DynUtil.materialize(xx, loc, destType, srcFieldCv));
            if (convOpt.optIsNone()) {
                return EResult.errs(erb.errors());
            }
            destFieldRegs.add(b.reg(convOpt.optUnwrap()));
        }
        if (!erb.errors().isEmpty()) {
            return EResult.errs(erb.errors());
        }
        var destKlass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, destSlotInfo.sortedTypes()));
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, destSlotInfo.sortedTypes());
        var newObj = b.newInstance(destKlass, ctorDesc, destFieldRegs);
        var newBase = Opt.some(new RecordBaseDynamic(new ValueDynamicSource.Reg(newObj)));
        var newRecord = new Record(newBase, asDynamicFields(fields));
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.ofValue((Value) newRecord)));
    }

    private static IOrderedMap<ConstKey, RecordField> asDynamicFields(IOrderedMap<ConstKey, RecordField> in) {
        var out = new TOrderedMap<ConstKey, RecordField>();
        for (var e : in) {
            var t =
                    switch (e.getValue()) {
                        case RecordField.Dynamic d -> d.type();
                        case RecordField.EvalRes er -> {
                            if (er.result().hasErrors()
                                    || er.result()
                                            .requireValue("asDynamicFields")
                                            .value()
                                            .optIsNone()) {
                                throw new IllegalStateException("Record::asDynamicFields: EvalRes with no value for '"
                                        + e.getKey().constKeyToCanonical()
                                        + "'");
                            }
                            yield er.result()
                                    .requireValue("asDynamicFields")
                                    .value()
                                    .optUnwrap()
                                    .valueType();
                        }
                    };
            out.put(e.getKey(), new RecordField.Dynamic(t));
        }
        return IOrderedMap.from(out);
    }

    static CompositeSlotSort.Result<ConstKey> computeSlotInfoFromTypes(
            ExprContext xx, TOrderedMap<ConstKey, Type> fieldTypes) {
        return CompositeSlotSort.sort(xx, fieldTypes, ConstKey.CANONICAL_ORDER);
    }

    static TOrderedMap<ConstKey, Type> recordFieldTypesRaw(IOrderedMap<ConstKey, RecordField> fields) {
        var out = new TOrderedMap<ConstKey, Type>();
        for (var e : fields) {
            out.put(e.getKey(), recordFieldType(e.getValue()));
        }
        return out;
    }

    public static final class RecordLift implements AspectLift {
        public record Entry(ConstKey key, AspectLift lift, Type type) {}

        private final TList<Entry> entryLifts;
        private volatile Opt<Field[]> cachedFields = Opt.none();

        public RecordLift(ExprContext xx, TList<Entry> entryLifts) {
            var sorted = new TList<Entry>(entryLifts.size());
            sorted.addAll(entryLifts);
            sorted.sort((a, b) -> {
                int c = CompositeClassEmitter.slotDescriptor(xx, a.type())
                        .value()
                        .compareTo(CompositeClassEmitter.slotDescriptor(xx, b.type())
                                .value());
                if (c != 0) return c;
                return ConstKey.CANONICAL_ORDER.compare(a.key(), b.key());
            });
            this.entryLifts = sorted;
        }

        @Override
        public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
            var sortedTypes = new TList<Type>(entryLifts.size());
            for (var entry : entryLifts) sortedTypes.add(entry.type());
            var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
            if (!expectedClass.isInstance(externalHostValue)) {
                return EResult.err(
                        loc,
                        "Record RecordLift::lift: expected "
                                + expectedClass.getName()
                                + ", got "
                                + externalHostValue.getClass().getName());
            }

            Field[] fields;
            if (cachedFields.optIsSome()) {
                fields = cachedFields.optUnwrap();
            } else {
                fields = new Field[entryLifts.size()];
                for (int i = 0; i < fields.length; i++) {
                    var fieldName = CompositeClassEmitter.slotFieldName(i);
                    try {
                        fields[i] = expectedClass.getField(fieldName);
                    } catch (NoSuchFieldException e) {
                        throw new AssertionError(
                                "Record RecordLift::lift: generated class missing entry field " + fieldName, e);
                    }
                }
                cachedFields = Opt.some(fields);
            }
            var out = new TOrderedMap<ConstKey, RecordField>();
            var errors = new TList<ExprErr>();
            for (int i = 0; i < entryLifts.size(); i++) {
                var entry = entryLifts.get(i);
                Object fieldValue;
                try {
                    fieldValue = fields[i].get(externalHostValue);
                } catch (IllegalAccessException e) {
                    throw new AssertionError(
                            "Record RecordLift::lift: cannot read entry field for '"
                                    + entry.key().constKeyToCanonical()
                                    + "'",
                            e);
                }
                var lifted = entry.lift().aspectLiftLift(xx, loc, fieldValue);
                if (lifted.hasErrors()) {
                    lifted.drainInto(errors);
                    continue;
                }
                var liftedValue = lifted.requireValue("Record RecordLift::lift entry value");
                out.put(
                        entry.key(),
                        new RecordField.EvalRes(
                                EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(liftedValue)))));
            }
            if (!errors.isEmpty()) {
                return EResult.errs(errors);
            }
            return EResult.ok(new Record(Opt.none(), IOrderedMap.from(out)));
        }
    }

    private boolean recordFieldsAllTypes() {
        for (var entry : fields) {
            switch (entry.getValue()) {
                case RecordField.Dynamic d -> {}
                case RecordField.EvalRes er -> {
                    var inner = er.result();
                    if (inner.hasErrors()) return false;
                    var innerCv = inner.requireValue("Record::recordFieldsAllTypes field");
                    if (innerCv.value().optIsNone()) return false;
                    var fieldValue = innerCv.value().optUnwrap();
                    if (fieldValue instanceof Record nested) {
                        if (!nested.recordFieldsAllTypes()) return false;
                    } else if (fieldValue instanceof ValueObjectConst voc) {
                        if (!(voc.data() instanceof Type)) return false;
                    } else if (!(fieldValue instanceof Type)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    static Type recordFieldType(RecordField f) {
        return switch (f) {
            case RecordField.Dynamic d -> d.type();
            case RecordField.EvalRes er -> {
                var innerOpt = er.result().valueOpt();
                if (innerOpt.optIsSome() && innerOpt.optUnwrap().value().optIsSome()) {
                    var v = innerOpt.optUnwrap().value().optUnwrap();
                    if (v instanceof Type asType) {
                        yield asType;
                    }
                    if (v instanceof ValueObjectConst voc && voc.data() instanceof Type asDataType) {
                        yield asDataType;
                    }
                    yield v.valueType();
                }
                throw new IllegalStateException("Record::recordFieldType: field has no value");
            }
        };
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome() && recordFieldsAllTypes();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, GenAspectStackValueDynamic.AspectId).optIsSome()) {
            if (cv.value().optIsNone()) {
                return EResult.err(loc, "Record::typeCastTo: CodeValue has no value");
            }
            if (!(cv.value().optUnwrap() instanceof Record record)) {
                return EResult.err(
                        loc,
                        "Record::typeCastTo: expected Record value, got "
                                + cv.value().optUnwrap().getClass());
            }
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var elems = new TList<JavaBytecodeReg>();
            for (var entry : record.fields()) {
                var key = entry.getKey();
                ConstKey.Str keyStr;
                switch (key) {
                    case ConstKey.Str ks -> keyStr = ks;
                    case ConstKey.Uint u -> {
                        return EResult.err(loc, "Record::typeCastTo: non-string key " + key);
                    }
                }
                RecordField.EvalRes er;
                switch (entry.getValue()) {
                    case RecordField.EvalRes erVal -> er = erVal;
                    case RecordField.Dynamic d -> {
                        return EResult.err(
                                loc, "Record::typeCastTo: field '" + key.constKeyToCanonical() + "' is dynamic-only");
                    }
                }
                var elemRes = er.result();
                if (elemRes.hasErrors()) return elemRes.reerr();
                var elemCv = elemRes.requireValue("Record::typeCastTo field");
                if (elemCv.value().optIsNone()) {
                    return EResult.err(
                            loc, "Record::typeCastTo: field '" + key.constKeyToCanonical() + "' has no value");
                }
                JavaBytecodeReg loweredReg;
                if (elemCv.value().optUnwrap() instanceof Record nestedRecord) {
                    var nestedRes = nestedRecord.typeCastTo(xx, loc, elemCv, destType);
                    if (nestedRes.hasErrors()) return nestedRes.reerr();
                    var nestedReg = DynUtil.valueReg(loc, nestedRes.requireValue("Record::typeCastTo nested record"));
                    if (nestedReg.hasErrors()) return nestedReg.reerr();
                    elems.add(b.constString(keyStr.value()));
                    loweredReg = b.reg(nestedReg.requireValue("nestedDyn"));
                } else {
                    var lowered = DynUtil.materialize(xx, loc, elemCv);
                    if (lowered.hasErrors()) return lowered.reerr();
                    elems.add(b.constString(keyStr.value()));
                    loweredReg = b.reg(lowered.requireValue("Record::typeCastTo field lowered"));
                }
                elems.add(loweredReg);
            }
            var array = b.newObjectArray(elems);
            var out = b.invokeStatic(GenCodegenDescriptors.RECORD_NEW_DYNAMIC, array);
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
        return EResult.ok(cv);
    }

    @CodegenDescriptor
    public static Record newDynamic(Object[] pairs) {
        var fields = new TOrderedMap<ConstKey, RecordField>();
        for (int i = 0; i < pairs.length; i += 2) {
            var key = new ConstKey.Str((String) pairs[i]);
            var raw = pairs[i + 1];
            if (!(raw instanceof Type t)) {
                throw new IllegalStateException(
                        "Record::newDynamic: field '" + pairs[i] + "' value is not a Type: " + raw.getClass());
            }
            fields.put(key, new RecordField.Dynamic(t));
        }
        return new Record(Opt.none(), IOrderedMap.from(fields));
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
