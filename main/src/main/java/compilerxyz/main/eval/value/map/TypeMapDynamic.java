package compilerxyz.main.eval.value.map;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactSupport;
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
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.long_.TypeLongConst;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.string.TypeStringConst;
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
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Pair;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Map;
import org.objectweb.asm.Opcodes;

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
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicMapDynamic<>(this));
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
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicMapDynamic<>(this));
        }
        if (key.equals(GenAspectIterable.AspectId)) {
            return Opt.some(new AspectIterableMapDynamic<>(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldMapDynamic<>(this));
        }
        if (key.equals(GenAspectDropDynamicLight.AspectId)) {
            return Opt.some(new AspectDropDynamicLightMapDynamic<>(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicMapDynamic<>(this));
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
                new Pair<>(new ConstKey.Str("put"), new FieldSpec.PutFn(new TypeMapPutFnDynamic(keyType, valueType))));
    }

    CodeDynamic entryDecode(QExprId loc) {
        var decodeSeq = new JavaBytecodeSequence();
        decodeSeq.add(JavaBytecodeUtils.cast(
                loc, JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Map.Entry.class))));
        return new CodeDynamic(loc, decodeSeq);
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
        return new AspectDynamicConstMapDynamic<>(this)
                .aspectDynamicConstConstForm()
                .artifactableExporter(xx, loc);
    }
}
