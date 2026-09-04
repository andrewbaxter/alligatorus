package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.binding.BindingIteratorStack;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.object_.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectBindDynamic;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.AspectDropDynamicLight;
import compilerxyz.main.gen.AspectDynamicConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBindDynamic;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6122L)
public final class TypeMapDynamic<K extends Type, V extends Type>
        implements Type,
                AspectInitialBindDynamic,
                AspectDynamicConst,
                AspectBind,
                AspectBindDynamic,
                AspectConsumeDynamic,
                AspectDropDynamic,
                AspectDropDynamicLight,
                AspectField,
                AspectIterable,
                AspectStackValueDynamic {

    public final K keyType;
    public final V valueType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeMapDynamic(K keyType, V valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public Binding aspectInitialBindDynamicInitialBindDynamic(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }

    @Override
    public String typeDesc() {
        return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromClass(compilerxyz.prelude.util.TOrderedMap.class);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, keyType).optIsSome()
                            || GenAspectDropDynamic.getMaybe(xx, valueType).optIsSome()
                    ? Opt.some(this)
                    : Opt.none();
        }
        if (key.equals(GenAspectDynamicConst.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectBindDynamic.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamicLight.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectIterable.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(this);
        }
        return Opt.none();
    }

    @Override
    public Type aspectDynamicConstConstForm() {
        return new TypeMapConst(keyType, valueType);
    }

    private sealed interface FieldSpec {
        Type fieldSpecFieldType();

        record Factory(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }

        record GetFn(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }

        record PutFn(Type type) implements FieldSpec {
            @Override
            public Type fieldSpecFieldType() {
                return type;
            }
        }
    }

    private TMap<ConstKey, FieldSpec> fieldSpecs() {
        return TMap.ofEntries(
                new Pair<>(new ConstKey.Str("new"), new FieldSpec.Factory(new TypeMapNewDynamic(keyType, valueType))),
                new Pair<>(new ConstKey.Str("get"), new FieldSpec.GetFn(new TypeMapGetFnDynamic(keyType, valueType))),
                new Pair<>(new ConstKey.Str("put"), new FieldSpec.PutFn(new TypeMapPutFnDynamic(keyType, valueType))));
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeMapDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var spec = fieldSpecs().getOrNull(ck);
        if (spec != null) {
            return switch (spec) {
                case FieldSpec.Factory f ->
                    EResult.ok(new CodeValue(
                            self.code(),
                            EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
                case FieldSpec.GetFn g -> {
                    var selfDynCast = CodeDynamic.castOpt(loc, self.code());
                    if (selfDynCast.hasErrors()) yield selfDynCast.reerr();
                    if (selfDynCast.requireValue("selfDyn").optIsNone()) {
                        yield EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
                    }
                    var selfDyn = selfDynCast.requireValue("selfDyn").optUnwrap();
                    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic fnSelfVdg)) {
                        yield EResult.err(loc, "TypeMapDynamic::field: fn receiver has no dynamic source");
                    }
                    yield EResult.ok(new CodeValue(
                            Code.target(selfDyn), EvalOutcome.ofValue(new ValueDynamic(fnSelfVdg.source(), g.type()))));
                }
                case FieldSpec.PutFn p -> {
                    var selfDynCast = CodeDynamic.castOpt(loc, self.code());
                    if (selfDynCast.hasErrors()) yield selfDynCast.reerr();
                    if (selfDynCast.requireValue("selfDyn").optIsNone()) {
                        yield EResult.err(loc, "TypeMapDynamic::field: receiver has no runtime code");
                    }
                    var selfDyn = selfDynCast.requireValue("selfDyn").optUnwrap();
                    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic fnSelfVdp)) {
                        yield EResult.err(loc, "TypeMapDynamic::field: fn receiver has no dynamic source");
                    }
                    yield EResult.ok(new CodeValue(
                            Code.target(selfDyn), EvalOutcome.ofValue(new ValueDynamic(fnSelfVdp.source(), p.type()))));
                }
            };
        }
        switch (ck) {
            case ConstKey.Uint u -> {
                return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on map");
            }
            case ConstKey.Str s -> {}
        }
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
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
        if (spec != null) return Opt.some(spec.fieldSpecFieldType());
        return switch (key) {
            case ConstKey.Str s -> Opt.some(valueType);
            case ConstKey.Uint u -> Opt.none();
        };
    }

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindViaTarget(xx, loc, cv);
    }

    @Override
    public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.bindDynamicDefault(loc, cv);
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsNone()) {
            throw new IllegalStateException(
                    "TypeMapDynamic::toDynamic: dynamic type must carry dynamic code (single-representation invariant)");
        }
        var dyn = dynCast.requireValue("dyn").optUnwrap();
        return EResult.ok(dyn);
    }

    @Override
    public Type aspectIterableElementType() {
        return new TypeMapEntryDynamic(keyType, valueType);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorStack();
    }

    private CodeDynamic entryDecode(QExprId loc) {
        var decodeSeq = new JavaBytecodeSequence();
        decodeSeq.add(JavaBytecodeUtils.cast(
                loc, JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Map.Entry.class))));
        return new CodeDynamic(loc, decodeSeq);
    }

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return EResult.ok(DynUtil.dynamicStackToValue(
                xx, DynUtil.iteratorNext(loc, entryDecode(loc)), aspectIterableElementType()));
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseConsumeRes = GenAspectConsumeDynamic.get(xx, loc, this);
        if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
        var baseDynRes =
                baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDynRes.hasErrors()) return baseDynRes.reerr();
        var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

        var mapName = JavaInternalName.fromClass(Map.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(baseDyn);
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }

        Code bodyCode;
        if (body.value().optIsSome()) {
            var dropRes = xx.target.targetDrop(xx, loc, body);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var dropCode = dropRes.requireValue("TypeMapDynamic::foreach body drop");
            var bodyDynCast = CodeDynamic.castOpt(loc, body.code());
            if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
            var bodyDyn = bodyDynCast.requireValue("bodyDyn");
            var dropDynCast = CodeDynamic.castOpt(loc, dropCode);
            if (dropDynCast.hasErrors()) return dropDynCast.reerr();
            var dropDyn = dropDynCast.requireValue("dropDyn");
            if (bodyDyn.optIsNone() && dropDyn.optIsNone()) {
                bodyCode = Code.none(loc);
            } else {
                var combinedSeq = new JavaBytecodeSequence();
                if (bodyDyn.optIsSome()) combinedSeq.add(bodyDyn.optUnwrap());
                if (dropDyn.optIsSome()) combinedSeq.add(dropDyn.optUnwrap());
                bodyCode = Code.target(new CodeDynamic(loc, combinedSeq));
            }
        } else {
            bodyCode = body.code();
        }
        return DynUtil.foreachSkeleton(loc, iteratorSetup, elementBindDyn.optUnwrap(), bodyCode);
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        return DynUtil.dynamicStackToValue(xx, value, this);
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var mapOpt = AspectLookup.getMaybe(
                xx, other, GenTrivialIdentityAspects.TYPE_MAP_DYNAMIC_IDENTITY, TypeMapDynamic.class);
        return mapOpt.optIsSome()
                && keyType.typeIsEqual(xx, mapOpt.optUnwrap().keyType)
                && valueType.typeIsEqual(xx, mapOpt.optUnwrap().valueType);
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        if (typeIsEqual(xx, sourceType)) {
            return true;
        }
        if (sourceType instanceof Record
                && GenAspectConsumeDynamic.getMaybe(xx, sourceType).optIsSome()) {
            return true;
        }
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeMapDynamic::typeCastFrom: CodeValue has no value");
        }
        var cvVal = cv.value().optUnwrap();
        var sourceType = cvVal.valueType();
        if (typeIsEqual(xx, sourceType)) {
            return EResult.ok(cv);
        }
        if (!(cvVal instanceof Record record)) {
            return EResult.err(loc, "TypeMapDynamic::typeCastFrom: expected Record source, got " + cvVal.getClass());
        }
        var erb = new Erb(loc);
        var valueToDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, valueType));
        if (valueToDynamicOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var valueToDynamic = valueToDynamicOpt.optUnwrap();
        var keyToDynamicOpt = erb.mustEr(GenAspectConsumeDynamic.get(xx, loc, keyType));
        if (keyToDynamicOpt.optIsNone()) {
            return EResult.errs(erb.errors());
        }
        var keyToDynamic = keyToDynamicOpt.optUnwrap();
        var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var putDesc = JavaMethodDescriptor.fromParts(descObject, TList.of(descObject, descObject));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.instantiate(
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
                    keyCv = new CodeValue(
                            Code.none(loc),
                            EvalOutcome.ofValue(new ValueObjectConst(new TypeStringConst(), s.value())));
                case ConstKey.Uint u ->
                    keyCv = new CodeValue(
                            Code.none(loc),
                            EvalOutcome.ofValue(new ValueObjectConst(new TypeLongConst(), (long) u.value())));
            }
            var keyConvOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, keyType, keyCv));
            if (keyConvOpt.optIsNone()) return EResult.errs(erb.errors());
            var keyCodeOpt = erb.mustEr(keyToDynamic.aspectConsumeDynamicToDynamic(xx, loc, keyConvOpt.optUnwrap()));
            if (keyCodeOpt.optIsNone()) return EResult.errs(erb.errors());
            CodeValue entryCv;
            switch (fieldVal) {
                case RecordField.EvalRes er -> {
                    var inner = er.result();
                    if (inner.hasErrors()) {
                        inner.drainInto(erb.errors());
                        erb.errPush("Record entry '" + fieldKey.constKeyToCanonical() + "' failed to evaluate");
                        return EResult.errs(erb.errors());
                    }
                    entryCv = inner.requireValue("TypeMapDynamic::construct record entry");
                }
                case RecordField.Dynamic d -> {
                    erb.errPush("Record entry '"
                            + fieldKey.constKeyToCanonical()
                            + "' is dynamic-only, cannot consume directly to dynamic");
                    return EResult.errs(erb.errors());
                }
            }
            var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, valueType, entryCv));
            if (convertedOpt.optIsNone()) return EResult.errs(erb.errors());
            var jsValOpt = erb.mustEr(valueToDynamic.aspectConsumeDynamicToDynamic(xx, loc, convertedOpt.optUnwrap()));
            if (jsValOpt.optIsNone()) return EResult.errs(erb.errors());
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            seq.add(keyCodeOpt.optUnwrap());
            seq.add(jsValOpt.optUnwrap());
            seq.add(JavaBytecodeUtils.callMethod(loc, torderedMapIntname, "put", putDesc));
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        }
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
    }

    @Override
    public EResult<Code> aspectDropDynamicLightDropDynamicLight(ExprContext xx, QExprId loc, CodeValue cv) {
        return DynUtil.dropDefault(loc, cv);
    }

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var keyDropOpt = GenAspectDropDynamic.getMaybe(xx, keyType);
        var valDropOpt = GenAspectDropDynamic.getMaybe(xx, valueType);
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var entrySlot = new BindingId();
        var entryClassName = JavaInternalName.fromClass(Map.Entry.class);

        var bodySeq = new JavaBytecodeSequence();
        if (keyDropOpt.optIsSome()) {
            var keyDropSome = keyDropOpt.optUnwrap();
            var loadKeySeq = new JavaBytecodeSequence();
            loadKeySeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot)));
            loadKeySeq.add(JavaBytecodeUtils.callInterfaceMethod(
                    loc,
                    entryClassName,
                    "getKey",
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
            var keyCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, loadKeySeq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, keyType)));
            var keyDropRes = keyDropSome.aspectDropDynamicDrop(xx, loc, keyCv);
            if (keyDropRes.hasErrors()) return keyDropRes.reerr();
            var keyDropDynCast = CodeDynamic.castOpt(loc, keyDropRes.requireValue("map key drop"));
            if (keyDropDynCast.hasErrors()) return keyDropDynCast.reerr();
            var keyDropDyn = keyDropDynCast.requireValue("keyDropDyn");
            if (keyDropDyn.optIsSome()) bodySeq.add(keyDropDyn.optUnwrap());
        }
        if (valDropOpt.optIsSome()) {
            var valDropSome = valDropOpt.optUnwrap();
            var loadValSeq = new JavaBytecodeSequence();
            loadValSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot)));
            loadValSeq.add(JavaBytecodeUtils.callInterfaceMethod(
                    loc,
                    entryClassName,
                    "getValue",
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
            var valCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, loadValSeq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, valueType)));
            var valDropRes = valDropSome.aspectDropDynamicDrop(xx, loc, valCv);
            if (valDropRes.hasErrors()) return valDropRes.reerr();
            var valDropDynCast = CodeDynamic.castOpt(loc, valDropRes.requireValue("map value drop"));
            if (valDropDynCast.hasErrors()) return valDropDynCast.reerr();
            var valDropDyn = valDropDynCast.requireValue("valDropDyn");
            if (valDropDyn.optIsSome()) bodySeq.add(valDropDyn.optUnwrap());
        }

        var mapName = JavaInternalName.fromClass(Map.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(vd.source().valueDynamicSourceRealize(xx, loc));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var entryStoreSeq = new JavaBytecodeSequence();
        entryStoreSeq.add(DynUtil.iteratorNext(loc, entryDecode(loc)));
        entryStoreSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entrySlot)));
        var loopRes = DynUtil.foreachSkeleton(
                loc, iteratorSetup, new CodeDynamic(loc, entryStoreSeq), Code.target(new CodeDynamic(loc, bodySeq)));
        if (loopRes.hasErrors()) return loopRes.reerr();
        var loopCodeCast = CodeDynamic.castOpt(loc, loopRes.requireValue("loop").code());
        if (loopCodeCast.hasErrors()) return loopCodeCast.reerr();
        var loopCode = loopCodeCast.requireValue("loopCode");
        var finalSeq = new JavaBytecodeSequence();
        if (loopCode.optIsSome()) finalSeq.add(loopCode.optUnwrap());
        finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, finalSeq)));
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return false;
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        throw new UnsupportedOperationException(
                getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return aspectDynamicConstConstForm().artifactableExporter(xx, loc);
    }
}
