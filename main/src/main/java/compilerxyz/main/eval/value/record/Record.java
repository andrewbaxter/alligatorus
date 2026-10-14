package compilerxyz.main.eval.value.record;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.artifact.Exporters;
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
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseConst;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectPostDeserialize;
import compilerxyz.main.gen.GenAspectRecord;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.gen.codec.ArtifactRecordDataCodec;
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
public record Record(IOrderedMap<ConstKey, RecordField> fields, ArtifactSupport.ArtifactIdHolder artifactIdCell)
        implements Aspect, Type, Value {

    public Record(IOrderedMap<ConstKey, RecordField> fields) {
        this(fields, new ArtifactSupport.ArtifactIdHolder());
    }

    public EResult<CodeValue> accessOrDrop(ExprContext xx, QExprId loc, CodeValue selfCv, Opt<ConstKey> accessKey) {
        if (selfCv.value().optIsNone()) {
            throw new IllegalStateException("Record::accessOrDrop: CodeValue has no value");
        }
        return switch (selfCv.value().optUnwrap()) {
            case Record record -> {
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
            case ValueDynamic vd -> {
                var record = MustCast.mustCast(Record.class, vd.type());
                var baseSource = vd.source();
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
                            var childCv = DynUtil.regToValue(access.code(), child, t);
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
                        case RecordField.EvalRes held -> {
                            if (held.result().hasErrors()) yield held.result().reerr();
                            var heldCv = held.result().requireValue("Record::accessSourced held field");
                            var heldCombined = Eval.codeConcat(xx, loc, selfCv.code(), heldCv.code());
                            if (heldCombined.hasErrors()) yield heldCombined.reerr();
                            yield EResult.ok(new CodeValue(
                                    heldCombined.requireValue("Record::accessSourced held code"), heldCv.outcome()));
                        }
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
            default ->
                throw new IllegalStateException("Record::accessOrDrop: expected Record or ValueDynamic value, got "
                        + selfCv.value().optUnwrap().getClass());
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
            switch (entry.getValue()) {
                case RecordField.EvalRes er -> {
                    if (er.result().hasErrors()) {
                        throw new IllegalStateException("Record::valueType: field '"
                                + entry.getKey().constKeyToCanonical() + "' holds errors: "
                                + er.result().errorsOpt().optUnwrap());
                    }
                }
                case RecordField.Dynamic d -> {}
            }
            var t = recordFieldType(entry.getValue());
            fieldTypes.put(entry.getKey(), new RecordField.Dynamic(t));
        }
        return new Record(IOrderedMap.from(fieldTypes));
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindRecord(this));
        }
        if (key.equals(GenTrivialIdentityAspects.RECORD_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectType.AspectId)) {
            var fieldAspects = new TOrderedMap<ConstKey, AspectType>();
            for (var entry : fields) {
                var fieldAspect = GenAspectType.getMaybe(xx, recordFieldType(entry.getValue()));
                if (fieldAspect.optIsNone()) return Opt.none();
                fieldAspects.put(entry.getKey(), fieldAspect.optUnwrap());
            }
            return Opt.some(new AspectTypeRecord(IOrderedMap.from(fieldAspects)));
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
            return Opt.some(new AspectArtifactRecord(this));
        }
        if (key.equals(GenAspectRecord.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectPostDeserialize.AspectId)) {
            return Opt.some(PostDeserializeIdentity.INSTANCE);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            var entryLifts = new TList<RecordLift.Entry>(fields.size());
            var held = new TOrderedMap<ConstKey, Value>();
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
                            held.put(k, fv);
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
            return Opt.some(new RecordLift(xx, entryLifts, held));
        }
        return Opt.none();
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof Record record)) {
                return EResult.err(
                        loc,
                        "Record::exporterRootDelegateSelfExport: expected Record, got "
                                + self.getClass().getName());
            }
            var cell = record.artifactIdCell();
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var erb = new Erb(loc);
                var fieldArts = new TOrderedMap<ConstKey, ArtifactRecordField>();
                for (var entry : record.fields()) {
                    var key = entry.getKey();
                    switch (entry.getValue()) {
                        case RecordField.Dynamic d -> {
                            var artOpt = erb.mustErMaybeUnreachable(Exporters.exportType(xx, loc, d.type()));
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
                            var artOpt = erb.mustErMaybeUnreachable(
                                    Exporters.exportValue(xx, loc, cv.value().optUnwrap()));
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
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
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
                                    var v = MustCast.mustCast(Value.class, ea);
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
                            var out = new Record(IOrderedMap.from(fields));
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
            if (isSlotField(entry.getValue()) != isSlotField(otherField)) return false;
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

        if (srcValue instanceof Record) {
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
            return EResult.ok(
                    new CodeValue(cv.code(), EvalOutcome.ofValue(new Record(IOrderedMap.from(convertedFields)))));
        }

        var srcDyn = MustCast.mustCast(ValueDynamic.class, srcValue);
        var srcRecordType = MustCast.mustCast(Record.class, srcDyn.type());
        var srcSlotTypes = slotFieldTypes(srcRecordType.fields());
        var srcSlotInfo = computeSlotInfoFromTypes(xx, srcSlotTypes);
        var destFields = MustCast.mustCast(Record.class, valueType()).fields();
        var destSlotInfo = computeSlotInfoFromTypes(xx, slotFieldTypes(destFields));

        var a = srcSlotInfo.sortedTypes();
        var stlB = destSlotInfo.sortedTypes();
        boolean types = srcSlotTypes.size() == fields.size() && a.size() == stlB.size();
        for (int stlI = 0; types && stlI < a.size(); stlI++) {
            if (!a.get(stlI).typeIsEqual(xx, stlB.get(stlI))) types = false;
        }
        if (types) {
            return EResult.ok(new CodeValue(
                    cv.code(), EvalOutcome.ofValue(new ValueDynamic(srcDyn.source(), new Record(destFields)))));
        }

        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var srcReg = DynUtil.sourceReg(b, srcDyn.source());
        var srcKlass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, srcSlotInfo.sortedTypes()));

        var destSlotKeys = slotFieldTypes(destFields).keySet();
        var destSortedKeys = new TList<ConstKey>(destSlotKeys.size());
        for (var k : destSlotKeys) destSortedKeys.add(k);
        for (var k : destSlotKeys) {
            destSortedKeys.set(destSlotInfo.slotOfKey().getOpt(k).optUnwrap(), k);
        }
        for (var destEntry : destFields) {
            if (isSlotField(destEntry.getValue())) continue;
            var key = destEntry.getKey();
            CodeValue srcFieldCv;
            switch (srcRecordType.fields().getOpt(key).optUnwrap()) {
                case RecordField.Dynamic d -> {
                    int srcSlot = srcSlotInfo.slotOfKey().getOpt(key).optUnwrap();
                    var load = b.getField(
                            srcKlass,
                            CompositeClassEmitter.slotFieldName(srcSlot),
                            CompositeClassEmitter.slotDescriptor(xx, d.type()),
                            srcReg);
                    srcFieldCv = DynUtil.regToValue(new CodeDynamic(loc, new JavaBytecodeSequence()), load, d.type());
                }
                case RecordField.EvalRes held -> {
                    if (held.result().hasErrors()) return held.result().reerr();
                    srcFieldCv = held.result().requireValue("Record::typeCastFrom source held field");
                }
            }
            var heldConv = Eval.typecheckConvert(xx, loc, recordFieldType(destEntry.getValue()), srcFieldCv);
            if (heldConv.hasErrors()) return heldConv.reerr();
        }

        var erb = new Erb(loc);
        var destFieldRegs = new TList<JavaBytecodeReg>(destSortedKeys.size());
        for (int i = 0; i < destSortedKeys.size(); i++) {
            var key = destSortedKeys.get(i);
            var destType = recordFieldType(destFields.getOpt(key).optUnwrap());
            CodeValue srcFieldCv;
            switch (srcRecordType.fields().getOpt(key).optUnwrap()) {
                case RecordField.Dynamic d -> {
                    int srcSlot = srcSlotInfo.slotOfKey().getOpt(key).optUnwrap();
                    var load = b.getField(
                            srcKlass,
                            CompositeClassEmitter.slotFieldName(srcSlot),
                            CompositeClassEmitter.slotDescriptor(xx, d.type()),
                            srcReg);
                    srcFieldCv = DynUtil.regToValue(new CodeDynamic(loc, new JavaBytecodeSequence()), load, d.type());
                }
                case RecordField.EvalRes held -> {
                    if (held.result().hasErrors()) return held.result().reerr();
                    srcFieldCv = held.result().requireValue("Record::typeCastFrom held field");
                }
            }
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
        return EResult.ok(new CodeValue(
                Code.target(b.code()),
                EvalOutcome.ofValue(new ValueDynamic(new ValueDynamicSource.Reg(newObj), new Record(destFields)))));
    }

    public static Object hostOf(ExprContext xx, Type declared, Value value) {
        return switch (value) {
            case ValueObjectConst voc -> voc.data();
            case Record r -> compositeOf(xx, MustCast.mustCast(Record.class, declared), r);
            case Tuple t -> Tuple.compositeOf(xx, MustCast.mustCast(Tuple.class, declared), t);
            default ->
                throw new IllegalStateException(
                        "imported " + value.getClass().getName() + " has no host form for " + declared.typeDesc());
        };
    }

    public static Object compositeOf(ExprContext xx, Record declared, Record value) {
        var slotTypes = slotFieldTypes(declared.fields());
        var slotInfo = computeSlotInfoFromTypes(xx, slotTypes);
        var args = new Object[slotInfo.sortedTypes().size()];
        for (var entry : slotInfo.slotOfKey().entrySet()) {
            var key = entry.getKey();
            var field = value.fields().getOpt(key);
            if (field.optIsNone()) {
                throw new IllegalStateException(
                        "Record::compositeOf: imported record has no field '" + key.constKeyToCanonical() + "'");
            }
            var fieldValue =
                    switch (field.optUnwrap()) {
                        case RecordField.EvalRes held ->
                            held.result()
                                    .requireValue("Record::compositeOf field result")
                                    .value()
                                    .optUnwrap();
                        case RecordField.Dynamic d ->
                            throw new IllegalStateException("Record::compositeOf: imported record field '"
                                    + key.constKeyToCanonical() + "' has no value");
                    };
            args[entry.getValue()] = hostOf(xx, slotTypes.getOpt(key).optUnwrap(), fieldValue);
        }
        return CompositeClassEmitter.construct(xx, slotInfo.sortedTypes(), args);
    }

    public static final ExporterRootDelegate DATA_DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Object self, Function<JsonValue, ArtifactBody> makeBody) {
            return EResult.err(loc, "record data is exported through its type");
        }

        @Override
        public EResult<Object> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            ArtifactRecordData body;
            try {
                body = ArtifactRecordDataCodec.fromValue(data);
            } catch (RuntimeException e) {
                return EResult.err(loc, "record data: failed to parse ArtifactRecordData: " + e.getMessage());
            }
            var erb = new Erb(loc);
            var declared = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.type()));
            var value = erb.mustErMaybeUnreachable(ArtifactSupport.importArtifact(xx, loc, body.value()));
            if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
            return EResult.ok(compositeOf(
                    xx,
                    MustCast.mustCast(Record.class, declared.optUnwrap()),
                    MustCast.mustCast(Record.class, value.optUnwrap())));
        }
    };

    static TOrderedMap<ConstKey, Type> slotFieldTypes(IOrderedMap<ConstKey, RecordField> fields) {
        var out = new TOrderedMap<ConstKey, Type>();
        for (var e : fields) {
            switch (e.getValue()) {
                case RecordField.Dynamic d -> out.put(e.getKey(), d.type());
                case RecordField.EvalRes held -> {}
            }
        }
        return out;
    }

    private static boolean isSlotField(RecordField f) {
        return switch (f) {
            case RecordField.Dynamic d -> true;
            case RecordField.EvalRes held -> false;
        };
    }

    static CompositeSlotSort.Result<ConstKey> computeSlotInfoFromTypes(
            ExprContext xx, TOrderedMap<ConstKey, Type> fieldTypes) {
        return CompositeSlotSort.sort(xx, fieldTypes, ConstKey.CANONICAL_ORDER);
    }

    public static final class RecordLift implements AspectLift {
        public record Entry(ConstKey key, AspectLift lift, Type type) {}

        private final TList<Entry> entryLifts;

        private final TOrderedMap<ConstKey, Value> held;

        private volatile Opt<Field[]> cachedFields = Opt.none();

        public RecordLift(ExprContext xx, TList<Entry> entryLifts, TOrderedMap<ConstKey, Value> held) {
            this.held = held;
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
            for (var entry : held.entrySet()) {
                out.put(
                        entry.getKey(),
                        new RecordField.EvalRes(
                                EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(entry.getValue())))));
            }
            return EResult.ok(new Record(IOrderedMap.from(out)));
        }
    }

    static Type recordFieldType(RecordField f) {
        return switch (f) {
            case RecordField.Dynamic d -> d.type();
            case RecordField.EvalRes er -> {
                var innerOpt = er.result().valueOpt();
                if (innerOpt.optIsSome() && innerOpt.optUnwrap().value().optIsSome()) {
                    yield innerOpt.optUnwrap().value().optUnwrap().valueType();
                }
                throw new IllegalStateException("Record::recordFieldType: field has no value");
            }
        };
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsNone()) return false;
        for (var entry : fields) {
            if (!recordFieldType(entry.getValue()).typeCanCastTo(xx, loc, destType)) return false;
        }
        return true;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "Record::typeCastTo: CodeValue has no value");
        }
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var fieldResults = new TOrderedMap<ConstKey, EResult<CodeValue>>();
        switch (cv.value().optUnwrap()) {
            case Record record -> {
                for (var entry : record.fields()) {
                    switch (entry.getValue()) {
                        case RecordField.EvalRes er -> fieldResults.put(entry.getKey(), er.result());
                        case RecordField.Dynamic d ->
                            throw new IllegalStateException("Record::typeCastTo: record value has a Dynamic field");
                    }
                }
            }
            case ValueDynamic vd -> {
                var runtimeRecord = MustCast.mustCast(Record.class, vd.type());
                var baseCv = new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(new ValueDynamic(
                                new ValueDynamicSource.Reg(DynUtil.sourceReg(b, vd.source())), vd.type())));
                for (var key : runtimeRecord.fields().keys()) {
                    fieldResults.put(key, runtimeRecord.accessOrDrop(xx, loc, baseCv, Opt.some(key)));
                }
            }
            default ->
                throw new IllegalStateException("Record::typeCastTo: expected Record or ValueDynamic value, got "
                        + cv.value().optUnwrap().getClass());
        }
        var elems = new TList<JavaBytecodeReg>();
        for (var entry : fieldResults.entrySet()) {
            var key = entry.getKey();
            ConstKey.Str keyStr;
            switch (key) {
                case ConstKey.Str ks -> keyStr = ks;
                case ConstKey.Uint u -> {
                    return EResult.err(loc, "Record::typeCastTo: non-string key " + key);
                }
            }
            var elemRes = entry.getValue();
            if (elemRes.hasErrors()) return elemRes.reerr();
            var elemCv = elemRes.requireValue("Record::typeCastTo field");
            if (elemCv.value().optIsNone()) {
                return EResult.err(loc, "Record::typeCastTo: field '" + key.constKeyToCanonical() + "' has no value");
            }
            var fieldCast = elemCv.value().optUnwrap().valueType().typeCastTo(xx, loc, elemCv, destType);
            if (fieldCast.hasErrors()) return fieldCast.reerr();
            var fieldReg = DynUtil.valueReg(loc, fieldCast.requireValue("Record::typeCastTo field cast"));
            if (fieldReg.hasErrors()) return fieldReg.reerr();
            elems.add(b.constString(keyStr.value()));
            elems.add(b.reg(fieldReg.requireValue("Record::typeCastTo field reg")));
        }
        var array = b.newObjectArray(elems);
        var out = b.invokeStatic(GenCodegenDescriptors.RECORD_NEW_DYNAMIC, array);
        return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
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
        return new Record(IOrderedMap.from(fields));
    }

    @Override
    public Opt<ArtifactExporter> typeExporter() {
        return Opt.some(ExporterRoot.RECORD);
    }

    @Override
    public EResult<ArtifactExporter> valueExporter(ExprContext xx, QExprId loc) {
        return Exporters.typeExporter(loc, this);
    }
}
