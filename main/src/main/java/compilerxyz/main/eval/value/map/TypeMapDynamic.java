package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.aspect.AspectSetDynamicDefault;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.string.TypeStringConst;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectBindDynamic;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDynamicConst;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectFieldUseDynamic;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectIterable;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectSet;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;

@BuiltinFull(uniqueId = 6122L)
public final class TypeMapDynamic<K extends Type, V extends Type> implements Aspect, Type {

    public final K keyType;
    public final V valueType;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public TypeMapDynamic(K keyType, V valueType) {
        this.keyType = keyType;
        this.valueType = valueType;
    }

    @Override
    public String typeDesc() {
        return "map(" + keyType.typeDesc() + ", " + valueType.typeDesc() + ")";
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBind.AspectId)) {
            return Opt.some(new AspectInitialBindMapDynamic<>(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TYPE_MAP_DYNAMIC_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return GenAspectDropDynamic.getMaybe(xx, keyType).optIsSome()
                            || GenAspectDropDynamic.getMaybe(xx, valueType).optIsSome()
                    ? Opt.some(new AspectDropDynamicMapDynamic<>(this))
                    : Opt.none();
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableMapDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldMapDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicMapDynamic<>(this));
        }
        if (key.equals(GenAspectSet.AspectId)) {
            return Opt.some(AspectSetDynamicDefault.INSTANCE);
        }
        if (key.equals(GenAspectBindDynamic.AspectId)) {
            return Opt.some(new AspectBindDynamicMapDynamic<>(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindMapDynamic<>(this));
        }
        if (key.equals(GenAspectDynamicConst.AspectId)) {
            return Opt.some(new AspectDynamicConstMapDynamic<>(this));
        }
        if (key.equals(GenAspectFieldUseDynamic.AspectId)) {
            return Opt.some(new AspectFieldUseDynamicMapDynamic<>(this));
        }
        if (key.equals(GenAspectLift.AspectId)) {
            var keyLift = GenAspectLift.getMaybe(xx, keyType);
            var valueLift = GenAspectLift.getMaybe(xx, valueType);
            if (keyLift.optIsNone() || valueLift.optIsNone()) return Opt.none();
            return Opt.some(new AspectLiftMapDynamic<>(this));
        }
        return Opt.none();
    }

    sealed interface FieldSpec {
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

        record HasFn(Type type) implements FieldSpec {
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

    TMap<ConstKey, FieldSpec> fieldSpecs() {
        return TMap.ofEntries(
                new Pair<>(new ConstKey.Str("new"), new FieldSpec.Factory(new TypeMapNewDynamic(keyType, valueType))),
                new Pair<>(new ConstKey.Str("get"), new FieldSpec.GetFn(new TypeMapGetFnDynamic(keyType, valueType))),
                new Pair<>(new ConstKey.Str("has"), new FieldSpec.HasFn(new TypeMapHasFnDynamic(keyType, valueType))),
                new Pair<>(new ConstKey.Str("put"), new FieldSpec.PutFn(new TypeMapPutFnDynamic(keyType, valueType))));
    }

    DynUtil.ElementDecode entryDecode() {
        return (b, raw) ->
                b.checkcast(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Map.Entry.class)), raw);
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
        var torderedMapIntname = JavaInternalName.fromClass(TOrderedMap.class);
        var descObject = JavaDataDescriptor.fromClass(Object.class);
        var putDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_VOID, TList.of(descObject, descObject));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var map = b.newInstance(torderedMapIntname, JavaMethodDescriptor.fromConstructorParts(TList.of()));
        for (var entry : record.fields()) {
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
            var keyRes = DynUtil.materialize(xx, loc, keyType, keyCv);
            if (keyRes.hasErrors()) return keyRes.reerr();
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
            var valRes = DynUtil.materialize(xx, loc, valueType, entryCv);
            if (valRes.hasErrors()) return valRes.reerr();
            var k = b.reg(keyRes.requireValue("map key"));
            var v = b.reg(valRes.requireValue("map value"));
            b.invokeVirtualVoid(torderedMapIntname, "put", putDesc, map, k, v);
        }
        return EResult.ok(DynUtil.regToValue(b.code(), map, this));
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
    public EResult<ArtifactExporter> typeExporter(ExprContext xx, QExprId loc) {
        return new AspectDynamicConstMapDynamic<>(this)
                .aspectDynamicConstConstForm()
                .typeExporter(xx, loc);
    }
}
