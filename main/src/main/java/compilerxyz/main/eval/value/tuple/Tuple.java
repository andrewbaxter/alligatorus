package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactBodyExporter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactJson;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.CompositeSlotSort;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.AspectElementUseDynamicBind;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.AspectInitialBind;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectStackValueDynamic;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.BindResult;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamicLight;
import compilerxyz.main.gen.GenAspectElementUseConst;
import compilerxyz.main.gen.GenAspectElementUseDynamic;
import compilerxyz.main.gen.GenAspectElementUseDynamicBind;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBind;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectNull;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.main.gen.TupleBindSlot;
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6147L)
public final class Tuple
        implements Type,
                Value,
                AspectArtifact,
                AspectBind,
                AspectConsumeDynamic,
                AspectDropDynamic,
                AspectField,
                AspectElementUseDynamicBind,
                AspectInitialBind,
                AspectStackValueDynamic,
                AspectTuple {

    public final Opt<TupleBaseDynamic> base;
    public final TList<TupleElement> elements;
    public final ArtifactSupport.ArtifactIdHolder artifactIdCell = new ArtifactSupport.ArtifactIdHolder();

    public Tuple(Opt<TupleBaseDynamic> base, TList<TupleElement> elements) {
        this.base = base;
        this.elements = elements;
    }

    public TList<TupleElement> elements() {
        return elements;
    }

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var res = accessOrIndex(xx, loc, cv, Opt.none());
        if (res.hasErrors()) return res.reerr();
        return EResult.ok(res.requireValue("Tuple::drop").code());
    }

    public EResult<CodeValue> accessOrIndex(ExprContext xx, QExprId loc, CodeValue selfCv, Opt<Integer> accessIndex) {
        var selfValue = selfCv.value();
        if (selfValue.optIsNone()) {
            throw new IllegalStateException("Tuple::accessOrIndex: CodeValue has no value");
        }
        if (!(selfValue.optUnwrap() instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::accessOrIndex: value is not a Tuple, got "
                    + selfValue.optUnwrap().getClass());
        }
        return switch (tuple.base) {
            case Opt.None<TupleBaseDynamic> n -> {
                var seq = new JavaBytecodeSequence();
                CodeValue accessedCv = null;
                Type accessedType = null;
                CodeDynamic accessedPushDyn = null;
                for (int i = tuple.elements.size() - 1; i >= 0; i--) {
                    boolean isAccessed = accessIndex.optIsSome() && accessIndex.optUnwrap() == i;
                    var e = tuple.elements.get(i);
                    TupleElement.EvalRes er;
                    switch (e) {
                        case TupleElement.EvalRes r -> er = r;
                        case TupleElement.Dynamic d ->
                            throw new IllegalStateException("Tuple::accessOrIndexSourceless: element " + i
                                    + " is not EvalRes; sourceless value tuple cannot carry a"
                                    + " TupleElement.Dynamic entry");
                    }
                    var res = er.result();
                    if (res.hasErrors()) {
                        if (isAccessed) yield EResult.err(loc, "Tuple element " + i + " evaluated to error");
                        continue;
                    }
                    var _erCv = res.valueOpt();
                    if (!res.hasErrors()
                            && _erCv.optIsSome()
                            && _erCv.optUnwrap().value().optIsNone()) {
                        yield EResult.err(
                                loc,
                                "Tuple element " + i + " does not fall through (contains an unreachable"
                                        + " control jump like throw/panic); this expression is not valid in a"
                                        + " tuple element position");
                    }
                    var elemCv = res.requireValue("Tuple::accessOrIndexSourceless element");
                    var elemDyn = elemCv.code().codeDynamic();
                    var elemSome = (Opt.Some<Value>) elemCv.value();
                    Type elemType = elemSome.value().valueType();
                    if (isAccessed) {
                        var useConstOpt = GenAspectElementUseConst.getMaybe(xx, elemType);
                        if (useConstOpt.optIsSome()) {
                            var useRes = useConstOpt.optUnwrap().aspectElementUseConstElementUseConst(xx, loc, elemCv);
                            if (useRes.hasErrors()) yield useRes.reerr();
                            accessedCv = useRes.requireValue("Tuple::accessOrIndexSourceless useConst");
                            continue;
                        }
                        if (elemDyn == null) {
                            accessedCv = elemCv;
                            continue;
                        }
                        if (!(elemSome.value() instanceof ValueDynamic)) {
                            accessedCv = elemCv;
                            continue;
                        }
                        accessedPushDyn = elemDyn;
                        accessedType = elemType;
                        continue;
                    }
                    if (elemDyn != null) seq.add(elemDyn);
                    var fullDropOpt = GenAspectDropDynamic.getMaybe(xx, elemType);
                    if (fullDropOpt.optIsSome()) {
                        var dropRes = fullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, elemCv);
                        if (dropRes.hasErrors()) yield dropRes.reerr();
                        var dropDyn = dropRes.requireValue("Tuple::accessOrIndexSourceless drop")
                                .codeDynamic();
                        if (dropDyn != null) seq.add(dropDyn);
                        continue;
                    }
                    var lightDropOpt = GenAspectDropDynamicLight.getMaybe(xx, elemType);
                    if (lightDropOpt.optIsSome()) {
                        var dropRes = lightDropOpt.optUnwrap().aspectDropDynamicLightDropDynamicLight(xx, loc, elemCv);
                        if (dropRes.hasErrors()) yield dropRes.reerr();
                        var dropDyn = dropRes.requireValue("Tuple::accessOrIndexSourceless light drop")
                                .codeDynamic();
                        if (dropDyn != null) seq.add(dropDyn);
                        continue;
                    }
                    yield EResult.err(
                            loc,
                            "Sourceless tuple element " + i + " type '" + elemType.typeDesc()
                                    + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
                }
                if (accessIndex.optIsSome()) {
                    if (accessedCv != null) {
                        var accessedDyn = accessedCv.code().codeDynamic();
                        if (accessedDyn != null) seq.add(accessedDyn);
                        if (seq.children.isEmpty())
                            yield EResult.ok(new CodeValue(accessedCv.code(), accessedCv.outcome()));
                        yield EResult.ok(new CodeValue(Code.of(new CodeDynamic(loc, seq)), accessedCv.outcome()));
                    }
                    if (accessedPushDyn != null) {
                        seq.add(accessedPushDyn);
                        yield EResult.ok(new CodeValue(
                                Code.of(new CodeDynamic(loc, seq)),
                                EvalOutcome.ofValue(ValueDynamic.stack(xx, accessedType))));
                    }
                    yield EResult.err(loc, "Tuple element " + accessIndex.optUnwrap() + " not found");
                }
                yield EResult.ok(new CodeValue(Code.of(new CodeDynamic(loc, seq)), EvalOutcome.NO_VALUE));
            }
            case Opt.Some<TupleBaseDynamic> s -> {
                var baseSource = s.value().source();
                var dynamicTypes = new TList<Type>();
                var dynamicIndexOfElement = new TList<Integer>(tuple.elements.size());
                for (int i = 0; i < tuple.elements.size(); i++) {
                    var e = tuple.elements.get(i);
                    switch (e) {
                        case TupleElement.Dynamic d -> {
                            dynamicIndexOfElement.add(dynamicTypes.size());
                            dynamicTypes.add(d.type());
                        }
                        case TupleElement.EvalRes r -> dynamicIndexOfElement.add(-1);
                    }
                }
                var slotInfo = sortTupleSlots(xx, dynamicTypes);
                var sortedTypes = slotInfo.sortedTypes();
                var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
                if (accessIndex.optIsNone()) {
                    var out = new JavaBytecodeSequence();
                    out.add(baseSource.valueDynamicSourceRealize(xx, loc));
                    for (int i = tuple.elements.size() - 1; i >= 0; i--) {
                        int dynIdx = dynamicIndexOfElement.get(i);
                        if (dynIdx < 0) continue;
                        var t = dynamicTypes.get(dynIdx);
                        int slotIdx = slotInfo.slotOfKey().get(dynIdx);
                        var _tcFullDropOpt = GenAspectDropDynamic.getMaybe(xx, t);
                        if (_tcFullDropOpt.optIsSome()) {
                            var _tcSeq = new JavaBytecodeSequence();
                            _tcSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
                            _tcSeq.add(JavaBytecodeUtils.accessField(
                                    loc,
                                    klass,
                                    CompositeClassEmitter.slotFieldName(slotIdx),
                                    CompositeClassEmitter.slotDescriptor(xx, t)));
                            var _tcChildCv = new CodeValue(
                                    Code.of(new CodeDynamic(loc, new JavaBytecodeSequence())),
                                    EvalOutcome.ofValue(ValueDynamic.stack(xx, t)));
                            var _tcDropRes = _tcFullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, _tcChildCv);
                            if (_tcDropRes.hasErrors()) yield _tcDropRes.reerr();
                            var _tcD = _tcDropRes
                                    .requireValue("Tuple::tupleChildDropCode drop")
                                    .codeDynamic();
                            if (_tcD != null) _tcSeq.add(_tcD);
                            out.add(new CodeDynamic(loc, _tcSeq));
                        } else if (GenAspectDropDynamicLight.getMaybe(xx, t).optIsNone()) {
                            yield EResult.err(
                                    loc,
                                    "Tuple element " + i + " type '" + t.typeDesc()
                                            + "' has neither AspectDropDynamic nor AspectDropDynamicLight");
                        }
                    }
                    out.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
                    yield EResult.ok(new CodeValue(Code.of(new CodeDynamic(loc, out)), EvalOutcome.NO_VALUE));
                } else {
                    int accessedIdx = accessIndex.optUnwrap();
                    if (accessedIdx < 0 || accessedIdx >= tuple.elements.size()) {
                        yield EResult.err(loc, "Tuple element index " + accessedIdx + " out of range");
                    }
                    int _isDynIdx = dynamicIndexOfElement.get(accessedIdx);
                    if (_isDynIdx < 0) {
                        throw new IllegalStateException("Tuple::indexSourced: element index " + accessedIdx
                                + " is a scope-bound (non-dynamic) element; access should be routed elsewhere");
                    }
                    var accessedType = dynamicTypes.get(_isDynIdx);
                    int accessedSlot = slotInfo.slotOfKey().get(_isDynIdx);
                    var useDynOpt = GenAspectElementUseDynamic.getMaybe(xx, accessedType);
                    if (useDynOpt.optIsNone()) {
                        yield EResult.err(
                                loc,
                                "Tuple element " + accessedIdx + " type (" + accessedType.typeDesc()
                                        + ") does not implement AspectElementUseDynamic");
                    }
                    var childSource = new ValueDynamicSource.Field(
                            baseSource,
                            klass,
                            CompositeClassEmitter.slotFieldName(accessedSlot),
                            CompositeClassEmitter.slotDescriptor(xx, accessedType));
                    yield useDynOpt
                            .optUnwrap()
                            .aspectElementUseDynamicElementUseDynamic(xx, loc, childSource, Code.none(loc));
                }
            }
        };
    }

    private static CompositeSlotSort.Result<Integer> sortTupleSlots(ExprContext xx, TList<Type> types) {
        var entries = new TOrderedMap<Integer, Type>();
        for (int i = 0; i < types.size(); i++) entries.put(i, types.get(i));
        return CompositeSlotSort.sort(xx, entries, Integer::compare);
    }

    private static TList<Type> sortTupleTypes(ExprContext xx, TList<Type> types) {
        return sortTupleSlots(xx, types).sortedTypes();
    }

    private static TList<Integer> slotOfIndex(ExprContext xx, TList<Type> types) {
        var slotOf = sortTupleSlots(xx, types).slotOfKey();
        var out = new TList<Integer>(types.size());
        for (int i = 0; i < types.size(); i++) out.add(slotOf.get(i));
        return out;
    }

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        var types = new TList<Type>(elements.size());
        for (var e : elements) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::jvmClass: self has non-Dynamic element");
            }
            types.add(d.type());
        }
        var sortedTypes = sortTupleTypes(xx, types);
        var klass = CompositeClassEmitter.getOrEmit(xx, sortedTypes);
        return JavaDataDescriptor.fromInternalName(
                JavaInternalName.of(klass.getName().replace('.', '/')));
    }

    @Override
    public String typeDesc() {
        var inner = elements.stream()
                .map(e -> switch (e) {
                    case TupleElement.Dynamic d -> d.type().typeDesc();
                    case TupleElement.EvalRes r -> "?";
                })
                .collect(Collectors.joining(", "));
        return "tuple(" + inner + ")";
    }

    public Type valueType() {

        var slotTypes = new TList<TupleElement>(elements.size());
        for (var e : elements) {
            Type slot =
                    switch (e) {
                        case TupleElement.Dynamic d -> d.type();
                        case TupleElement.EvalRes r -> {
                            var er = r.result();
                            if (!er.hasErrors()) {
                                var elemCv = er.requireValue("Tuple::valueType element");
                                if (elemCv.value().optIsSome()) {
                                    yield elemCv.value().optUnwrap().valueType();
                                }
                            }
                            yield new Tuple(Opt.none(), new TList<>());
                        }
                    };
            slotTypes.add(new TupleElement.Dynamic(slot));
        }
        return new Tuple(base, slotTypes);
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenTrivialIdentityAspects.TUPLE_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectTuple.AspectId)
                || key.equals(GenAspectArtifact.AspectId)
                || key.equals(GenAspectBind.AspectId)
                || key.equals(GenAspectConsumeDynamic.AspectId)
                || key.equals(GenAspectDropDynamic.AspectId)
                || key.equals(GenAspectField.AspectId)
                || key.equals(GenAspectInitialBind.AspectId)
                || key.equals(GenAspectElementUseDynamicBind.AspectId)
                || key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectLift.AspectId)) {
            var elementLifts = new TList<AspectLift>(elements.size());
            var elementTypes = new TList<Type>(elements.size());
            for (var e : elements) {
                TupleElement.Dynamic d;
                switch (e) {
                    case TupleElement.Dynamic dyn -> d = dyn;
                    case TupleElement.EvalRes r -> {
                        return Opt.none();
                    }
                }
                var elemLiftOpt = GenAspectLift.getMaybe(xx, d.type());
                if (elemLiftOpt.optIsNone()) {
                    return Opt.none();
                }
                elementLifts.add(elemLiftOpt.optUnwrap());
                elementTypes.add(d.type());
            }
            return Opt.some(new TupleLift(elementLifts, elementTypes));
        }
        return Opt.none();
    }

    @Override
    public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
        return elements.isEmpty() && GenAspectNull.getMaybe(xx, destType).optIsSome();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        return EResult.ok(new CodeValue(
                Code.concat(loc, cv.code(), Code.of(JavaBytecodeUtils.literalNull(loc))),
                EvalOutcome.ofValue(ValueDynamic.stack(xx, destType))));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TUPLE_IDENTITY, Tuple.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (elements.size() != o.elements.size()) return false;
        for (int i = 0; i < elements.size(); i++) {
            var aType = tupleElementType(elements.get(i));
            var bType = tupleElementType(o.elements.get(i));
            if (aType.optIsNone() || bType.optIsNone()) return false;
            if (!aType.optUnwrap().typeIsEqual(xx, bType.optUnwrap())) return false;
        }
        return true;
    }

    @Override
    public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
        var sourceTuple = GenAspectTuple.getMaybe(xx, sourceType);
        if (!sourceTuple.optIsSome()) {
            return false;
        }
        return ((Opt.Some<AspectTuple>) sourceTuple)
                        .value()
                        .aspectTupleElementTypes()
                        .size()
                == elements.size();
    }

    private static Opt<Type> tupleElementType(TupleElement e) {
        return switch (e) {
            case TupleElement.Dynamic d -> Opt.some(d.type());
            case TupleElement.EvalRes er -> {
                if (er.result().hasErrors()) yield Opt.none();
                var cvOpt = er.result().valueOpt();
                if (cvOpt.optIsNone() || cvOpt.optUnwrap().value().optIsNone()) yield Opt.none();
                yield Opt.some(cvOpt.optUnwrap().value().optUnwrap().valueType());
            }
        };
    }

    @Override
    public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::typeCastFrom: CodeValue has no value");
        }
        var cvValue = cv.value().optUnwrap();
        if (cvValue instanceof ValueDynamic) {
            return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(ValueDynamic.stack(xx, this))));
        }
        if (!(cvValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::typeCastFrom: expected Tuple value, got " + cvValue.getClass());
        }
        var erb = new Erb(loc);
        var convertedElements = new TList<TupleElement>(tuple.elements.size());
        for (int i = 0; i < tuple.elements.size(); i++) {
            var elem = tuple.elements.get(i);
            var destSlot = elements.get(i);
            Type destElemType;
            switch (destSlot) {
                case TupleElement.Dynamic d -> destElemType = d.type();
                case TupleElement.EvalRes er -> {
                    var destRes = er.result();
                    if (destRes.hasErrors()) {
                        return EResult.err(loc, "Tuple::typeCastFrom: dest element " + i + " missing");
                    }
                    var destCv = destRes.requireValue("Tuple::typeCastFrom dest element");
                    if (destCv.value().optIsNone()) {
                        return EResult.err(loc, "Tuple::typeCastFrom: dest element " + i + " missing");
                    }
                    destElemType = destCv.value().optUnwrap().valueType();
                }
                default -> throw new IllegalStateException();
            }
            TupleElement converted;
            switch (elem) {
                case TupleElement.EvalRes elemRes -> {
                    var innerRes = elemRes.result();
                    if (innerRes.hasErrors()) {
                        converted = new TupleElement.EvalRes(
                                EResult.errs(innerRes.errorsOpt().optUnwrapOr(new TList<>())));
                    } else {
                        var elemCv = innerRes.requireValue("Tuple::typeCastFrom source element");
                        var convOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, destElemType, elemCv));
                        if (convOpt.optIsNone()) {
                            return EResult.errs(erb.errors());
                        }
                        converted = new TupleElement.EvalRes(EResult.ok(convOpt.optUnwrap()));
                    }
                }
                case TupleElement.Dynamic d -> converted = new TupleElement.Dynamic(destElemType);
                default -> throw new IllegalStateException();
            }
            convertedElements.add(converted);
        }
        if (!erb.errors().isEmpty()) {
            return EResult.errs(erb.errors());
        }
        return EResult.ok(new CodeValue(cv.code(), EvalOutcome.ofValue(new Tuple(Opt.none(), convertedElements))));
    }

    @Override
    public TList<Type> aspectTupleElementTypes() {
        var out = new TList<Type>(elements.size());
        for (var e : elements) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::elementTypes: slot is not Dynamic-only");
            }
            out.add(d.type());
        }
        return out;
    }

    @Override
    public TList<EResult<CodeValue>> aspectTupleTakeElements(ExprContext xx, QExprId loc, Value value) {
        if (!(value instanceof Tuple t)) {
            throw new IllegalArgumentException("Tuple::takeElements: expected Tuple value, got " + value.getClass());
        }
        var out = new TList<EResult<CodeValue>>(t.elements.size());
        for (var e : t.elements) {
            TupleElement.EvalRes r;
            switch (e) {
                case TupleElement.EvalRes res -> r = res;
                case TupleElement.Dynamic d ->
                    throw new IllegalStateException("Tuple::takeElements: slot is not EvalRes-form");
            }
            out.add(r.result());
        }
        return out;
    }

    @Override
    public ArtifactExporter aspectArtifactExporter() {
        return ExporterRoot.TUPLE;
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof Tuple tuple)) {
                return EResult.err(
                        loc, "Tuple::exporterRootDelegateSelfExport: expected Tuple self, got " + self.getClass());
            }
            var cell = tuple.artifactIdCell;
            TList<ExprErr> errors = new TList<>();
            var artifact = ArtifactSupport.implExportIndependentArtifact(xx.vx.ix.gx.artifacts, cell, () -> {
                var elementArtifacts = new TList<Artifact>(tuple.elements.size());
                for (var e : tuple.elements) {
                    Type elemType;
                    switch (e) {
                        case TupleElement.Dynamic d -> elemType = d.type();
                        case TupleElement.EvalRes r -> {
                            var innerRes = r.result();
                            if (innerRes.hasErrors()) {
                                innerRes.drainInto(errors);
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new ExprSubErr.Opaque("Tuple element has no value to serialize"))));
                                elementArtifacts.add(new Artifact.Body(
                                        new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root())));
                                continue;
                            }
                            var cv = innerRes.requireValue("Tuple::exporterExport element");
                            if (cv.value().optIsNone()) {
                                errors.add(new ExprErr(
                                        loc,
                                        TList.of(new ExprSubErr.Opaque("Tuple element has no value to serialize"))));
                                elementArtifacts.add(new Artifact.Body(
                                        new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root())));
                                continue;
                            }
                            elemType = cv.value().optUnwrap().valueType();
                        }
                        default -> throw new IllegalStateException();
                    }
                    var artRes = Artifactable.exportArtifact(xx, loc, elemType);
                    artRes.drainInto(errors);
                    if (artRes.valueOpt().optIsSome()) {
                        elementArtifacts.add(artRes.valueOpt().optUnwrap());
                    } else {
                        elementArtifacts.add(new Artifact.Body(
                                new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root())));
                    }
                }
                var data = new TOrderedMap<String, JsonValue>();
                data.put("elements", ArtifactJson.encodeArtifactList(elementArtifacts));
                return makeBody.apply(new JsonValueObject(data));
            });
            if (!errors.isEmpty()) {
                return EResult.mixed(artifact, errors);
            }
            return EResult.ok(artifact);
        }

        @Override
        public EResult<Artifactable> exporterRootDelegateSelfFromArtifactData(
                ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
            if (id.optIsNone()) {
                return EResult.err(loc, "Tuple requires an artifact id to deserialize");
            }
            var idValue = id.optUnwrap();
            var elementArtifactsHolder = new TList<TList<Artifact>>();
            return ArtifactSupport.implImportIndependentArtifact(
                    xx,
                    loc,
                    idValue,
                    data,
                    (raw) -> {
                        JsonValueObject obj;
                        switch (raw) {
                            case JsonValueObject o -> obj = o;
                            case JsonValueNull n -> {
                                return new Result.Err<>("Tuple data: expected object");
                            }
                            case JsonValueBoolean b -> {
                                return new Result.Err<>("Tuple data: expected object");
                            }
                            case JsonValueNumber n -> {
                                return new Result.Err<>("Tuple data: expected object");
                            }
                            case JsonValueString s -> {
                                return new Result.Err<>("Tuple data: expected object");
                            }
                            case JsonValueArray a -> {
                                return new Result.Err<>("Tuple data: expected object");
                            }
                        }
                        var elementsJson = obj.entries().get("elements");
                        if (elementsJson == null) {
                            return new Result.Err<>("Tuple data: missing 'elements'");
                        }
                        var decoded = ArtifactJson.decodeArtifactList(loc, elementsJson);
                        if (decoded.hasErrors()) {
                            return new Result.Err<>("Tuple data: malformed elements");
                        }
                        return new Result.Ok<>(decoded.requireValue("Tuple::exporterFromArtifactData elements"));
                    },
                    (elementArts) -> {
                        elementArtifactsHolder.add(elementArts);
                        ArtifactSupport.ImportIndependentResult.Finalizer fin = (xx2, loc2) -> {
                            var slots = new TList<TupleElement>(elementArts.size());
                            for (var ea : elementArts) {
                                var t = ArtifactSupport.independentFinalizeImportTypeArtifact(xx2, loc2, ea);
                                if (t.hasErrors()) {
                                    throw new IllegalStateException("Failed to deserialize tuple element type");
                                }
                                slots.add(new TupleElement.Dynamic(t.requireValue("Tuple::finalizer element type")));
                            }
                            var out = new Tuple(Opt.none(), slots);
                            out.artifactIdCell.set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(
                                new ArtifactSupport.ImportIndependentResult(Opt.none(), elementArts, fin));
                    });
        }
    };

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::bindSub: CodeValue has no value");
        }
        var cvValue = cv.value().optUnwrap();
        if (!(cvValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::bindSub: expected Tuple value, got " + cvValue.getClass());
        }
        var carry = cv.code();

        if (base.optIsSome()) {
            Type tupleType = this;
            var scopeFields = new TList<TupleScopeField>(elements.size());
            for (var e : elements) {
                TupleElement.Dynamic d;
                switch (e) {
                    case TupleElement.Dynamic dyn -> d = dyn;
                    case TupleElement.EvalRes r ->
                        throw new IllegalStateException("Tuple::bindSub: bind on non-type tuple");
                }
                scopeFields.add(new TupleScopeField.Dynamic(d.type()));
            }
            var subRes = xx.target.targetCodeToBindSub(loc, carry, tupleType);
            if (subRes.hasErrors()) return subRes.reerr();
            var sub = subRes.requireValue("Tuple::bindSub base.bindSub");
            var bound = xx.target.targetBind(xx, loc, sub);
            return EResult.ok(new BindResult(Code.of(bound.code), new BindingTuple(scopeFields, Opt.some(bound.id))));
        }

        TList<ExprErr> errors = null;
        record ElemBindResult(Opt<TargetBindSub> targetSub, Opt<CodeValue> constCv) {}
        var bindResults = new TList<ElemBindResult>(tuple.elements.size());
        boolean hasTarget = false;
        for (int i = 0; i < tuple.elements.size(); i++) {
            var elem = tuple.elements.get(i);
            switch (elem) {
                case TupleElement.Dynamic d -> {
                    if (errors == null) errors = new TList<>();
                    errors.add(new ExprErr(
                            loc,
                            TList.of(new ExprSubErr.Opaque(
                                    "Tuple element " + i + " is dynamic-only, cannot bind directly"))));
                }
                case TupleElement.EvalRes elemRes -> {
                    var inner = elemRes.result();
                    if (inner.hasErrors()) {
                        if (errors == null) errors = new TList<>();
                        inner.drainInto(errors);
                        continue;
                    }
                    var elemCv = inner.requireValue("Tuple::bindSub element value");
                    if (elemCv.value().optIsSome()) {
                        var elemType = elemCv.value().optUnwrap().valueType();
                        var boxedType = DynUtil.substituteBoxed(xx, elemType);
                        if (!boxedType.typeIsEqual(xx, elemType)) {
                            var boxedRes = boxedType.typeCastFrom(xx, loc, elemCv);
                            if (boxedRes.hasErrors()) {
                                if (errors == null) errors = new TList<>();
                                boxedRes.drainInto(errors);
                                continue;
                            }
                            elemCv = boxedRes.requireValue("Tuple::bindSub boxed element");
                        }
                    }
                    var savedValue = elemCv.outcome();
                    var subRes = xx.target.targetMaybeBindSub(xx, loc, elemCv);
                    if (subRes.hasErrors()) {
                        if (errors == null) errors = new TList<>();
                        subRes.drainInto(errors);
                        continue;
                    }
                    var subOpt = subRes.requireValue("Tuple::bindSub targetMaybeBindSub");
                    if (subOpt.optIsSome()) {
                        hasTarget = true;
                        bindResults.add(new ElemBindResult(Opt.some(subOpt.optUnwrap()), Opt.none()));
                    } else {
                        bindResults.add(
                                new ElemBindResult(Opt.none(), Opt.some(new CodeValue(Code.none(loc), savedValue))));
                    }
                }
            }
        }
        if (errors != null && !errors.isEmpty()) return EResult.errs(errors);
        if (!hasTarget) {
            var scopeFields = new TList<TupleScopeField>(bindResults.size());
            for (var br : bindResults) {
                if (br.constCv().optIsNone())
                    throw new IllegalStateException(
                            "Tuple::bindSub: !has_target but bindResults contains a target sub");
                var elemCv = br.constCv().optUnwrap();
                if (elemCv.value().optIsNone())
                    throw new IllegalStateException("Tuple::bindSub: const element CodeValue has no value");
                var elemType = elemCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, elemType);
                if (bindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
                var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
                if (elemBindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(elemBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
                scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
            }
            return EResult.ok(new BindResult(Code.none(loc), new BindingTuple(scopeFields, Opt.none())));
        }
        var scopeFields = new TList<TupleScopeField>(bindResults.size());
        var targetSlots = new TList<TupleBindSlot>(bindResults.size());
        for (var br : bindResults) {
            if (br.targetSub().optIsSome()) {
                var targetSome = br.targetSub().optUnwrap();
                scopeFields.add(new TupleScopeField.Dynamic(targetSome.type));
                targetSlots.add(new TupleBindSlot(targetSome));
            } else if (br.constCv().optIsSome()) {
                var elemCv = br.constCv().optUnwrap();
                if (elemCv.value().optIsNone())
                    throw new IllegalStateException("Tuple::bindSub: const element CodeValue has no value");
                var elemType = elemCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, elemType);
                if (bindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Tuple::bindSub AspectBind");
                var elemBindRes = bind.aspectBindBind(xx, loc, elemCv);
                if (elemBindRes.hasErrors()) {
                    scopeFields.add(
                            new TupleScopeField.Error(elemBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var elemBind = elemBindRes.requireValue("Tuple::bindSub element AspectBind.bind");
                scopeFields.add(new TupleScopeField.Scope(elemBind.binding()));
            } else {
                throw new IllegalStateException("Tuple::bindSub: bind result has neither target nor const");
            }
        }
        var combined = xx.target.targetBindSubTuple(xx, loc, carry, targetSlots);
        var bound = xx.target.targetBind(xx, loc, combined);
        return EResult.ok(new BindResult(Code.of(bound.code), new BindingTuple(scopeFields, Opt.some(bound.id))));
    }

    @Override
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var carry = cv.code();
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Tuple::toDynamic: CodeValue has no value");
        }
        var someValue = cv.value().optUnwrap();
        if (someValue instanceof ValueDynamic) {
            return DynUtil.dynamicConsumeToDynamic(loc, cv);
        }
        if (!(someValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::toDynamic: expected Tuple value, got " + someValue.getClass());
        }
        if (tuple.base.optIsSome()) {
            var _tbDyn = carry.codeDynamic();
            if (_tbDyn == null) return EResult.err(loc, "TupleBaseDynamic::toDynamic: carry has no dynamic code");
            return EResult.ok(_tbDyn);
        }
        var types = new TList<Type>(elements.size());
        for (var e : elements) {
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::toDynamic: self has non-Dynamic element");
            }
            types.add(d.type());
        }
        if (tuple.elements.size() != types.size()) {
            throw new IllegalStateException("Tuple::toDynamic: arity mismatch between value and type");
        }
        TList<ExprErr> errors = null;
        var dynamicElements = new TList<CodeDynamic>(tuple.elements.size());
        for (int i = 0; i < tuple.elements.size(); i++) {
            var elem = tuple.elements.get(i);
            var elemType = types.get(i);
            TupleElement.EvalRes er;
            switch (elem) {
                case TupleElement.EvalRes r -> er = r;
                case TupleElement.Dynamic d ->
                    throw new IllegalStateException("Tuple::toDynamic: value tuple has Dynamic-only element");
            }
            var elemRes = er.result();
            if (elemRes.hasErrors()) {
                if (errors == null) errors = new TList<>();
                elemRes.drainInto(errors);
                continue;
            }
            var elemCv = elemRes.requireValue("Tuple::toDynamic element value");
            var lookup = GenAspectConsumeDynamic.get(xx, loc, elemType);
            if (lookup.hasErrors()) {
                if (errors == null) errors = new TList<>();
                lookup.drainInto(errors);
                continue;
            }
            var toDyn = lookup.requireValue("Tuple::toDynamic consume aspect");
            var lowered = toDyn.aspectConsumeDynamicToDynamic(xx, loc, elemCv);
            if (lowered.hasErrors()) {
                if (errors == null) errors = new TList<>();
                lowered.drainInto(errors);
                continue;
            }
            var loweredCode = lowered.requireValue("Tuple::toDynamic element lowered");
            dynamicElements.add(loweredCode);
        }
        if (errors != null && !errors.isEmpty()) {
            return EResult.errs(errors);
        }
        var slotOf = slotOfIndex(xx, types);
        var sortedTypes = sortTupleTypes(xx, types);
        @SuppressWarnings("unchecked")
        CodeDynamic[] sortedCodesArr = new CodeDynamic[types.size()];
        for (int i = 0; i < types.size(); i++) {
            sortedCodesArr[slotOf.get(i)] = dynamicElements.get(i);
        }
        var sortedCodes = new TList<CodeDynamic>(types.size());
        for (var c : sortedCodesArr) sortedCodes.add(c);
        var klass = JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, sortedTypes));
        var argSeq = new JavaBytecodeSequence();
        for (var c : sortedCodes) argSeq.add(c);
        var ctorDesc = CompositeClassEmitter.constructorDesc(xx, sortedTypes);
        return EResult.ok(DynUtil.wrapWithCarry(
                loc, carry, JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, new CodeDynamic(loc, argSeq))));
    }

    @Override
    public CodeValue aspectStackValueDynamicToValue(ExprContext xx, QExprId loc, CodeDynamic value) {
        var source =
                new ValueDynamicSource.Expr(aspectConsumeDynamicJvmClass(xx).slotWidth());
        return new CodeValue(
                Code.of(value), EvalOutcome.ofValue(new Tuple(Opt.some(new TupleBaseDynamic(source)), elements)));
    }

    @Override
    public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
        var scopeFields = new TList<TupleScopeField>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            var e = elements.get(i);
            TupleElement.Dynamic d;
            switch (e) {
                case TupleElement.Dynamic dyn -> d = dyn;
                case TupleElement.EvalRes r ->
                    throw new IllegalStateException("Tuple::initialBind: element " + i + " is not Dynamic-only");
            }
            scopeFields.add(new TupleScopeField.Dynamic(d.type()));
        }
        return new BindingTuple(scopeFields, Opt.some(dynamicKey));
    }

    public static final class TupleLift implements AspectLift {
        private final TList<AspectLift> elementLifts;
        private final TList<Type> elementTypes;
        private volatile Opt<Field[]> cachedFields = Opt.none();

        public TupleLift(TList<AspectLift> elementLifts, TList<Type> elementTypes) {
            this.elementLifts = elementLifts;
            this.elementTypes = elementTypes;
        }

        @Override
        public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Opt<Object> externalHostValue) {

            if (externalHostValue.optIsNone() && elementLifts.isEmpty()) {
                return EResult.ok(new Tuple(Opt.none(), new TList<>()));
            }
            var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortTupleTypes(xx, elementTypes));
            if (externalHostValue.optIsNone() || !expectedClass.isInstance(externalHostValue.optUnwrap())) {
                return EResult.err(
                        loc,
                        "Tuple TupleLift::lift: expected "
                                + expectedClass.getName()
                                + ", got "
                                + (externalHostValue.optIsNone()
                                        ? "null"
                                        : externalHostValue
                                                .optUnwrap()
                                                .getClass()
                                                .getName()));
            }
            var rawHostValue = externalHostValue.optUnwrap();
            var fields = cachedFields.optUnwrapOr(null);
            if (fields == null) {
                fields = new Field[elementLifts.size()];
                var slotOf = slotOfIndex(xx, elementTypes);
                for (int i = 0; i < fields.length; i++) {
                    int slot = slotOf.get(i);
                    try {
                        fields[i] = expectedClass.getField(CompositeClassEmitter.slotFieldName(slot));
                    } catch (NoSuchFieldException e) {
                        throw new AssertionError(
                                "Tuple TupleLift::lift: generated class missing slot field _" + slot, e);
                    }
                }
                cachedFields = Opt.some(fields);
            }
            var out = new TList<TupleElement>(elementLifts.size());
            TList<ExprErr> errors = null;
            for (int i = 0; i < elementLifts.size(); i++) {
                var elemLift = elementLifts.get(i);
                Object slotValue;
                try {
                    slotValue = fields[i].get(rawHostValue);
                } catch (IllegalAccessException e) {
                    throw new AssertionError("Tuple TupleLift::lift: cannot read slot field _" + i, e);
                }
                var lifted = elemLift.aspectLiftLift(xx, loc, Opt.ofNullable(slotValue));
                if (lifted.hasErrors()) {
                    if (errors == null) errors = new TList<>();
                    lifted.drainInto(errors);
                    continue;
                }
                var liftedValue = lifted.requireValue("Tuple TupleLift::lift element value");
                out.add(new TupleElement.EvalRes(
                        EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(liftedValue)))));
            }
            if (errors != null && !errors.isEmpty()) {
                return EResult.errs(errors);
            }
            return EResult.ok(new Tuple(Opt.none(), out));
        }
    }

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self_, CodeValue key) {
        if (key.value().optIsNone()) {
            return EResult.err(loc, "Tuple::field: key has no compile-time value");
        }
        var keyVal = key.value().optUnwrap();
        var numConst = GenAspectNumericConst.getMaybe(xx, keyVal.valueType());
        if (numConst.optIsNone()) {
            return EResult.err(loc, "Tuple::field: key must be a compile-time number");
        }
        long d = numConst.optUnwrap().aspectNumericConstGet(keyVal).longValue();
        int idx = (int) d;
        if ((long) idx != d) {
            return EResult.err(loc, "Tuple::field: index out of int range, got " + d);
        }
        return accessOrIndex(xx, loc, self_, Opt.some(idx));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        if (base.optIsNone()) {
            return new TList<>();
        }
        var out = new TList<ConstKey>(elements.size());
        for (int i = 0; i < elements.size(); i++) {
            out.add(new ConstKey.Uint(i));
        }
        return out;
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        ConstKey.Uint u;
        switch (key) {
            case ConstKey.Uint uint -> u = uint;
            case ConstKey.Str s -> {
                return Opt.none();
            }
        }
        long idx = u.value();
        if (idx < 0 || idx >= elements.size()) return Opt.none();
        var e = elements.get((int) idx);
        Type t =
                switch (e) {
                    case TupleElement.Dynamic d -> d.type();
                    case TupleElement.EvalRes r -> {
                        var inner = r.result();
                        if (!inner.hasErrors()) {
                            var innerCv = inner.requireValue("Tuple::fieldTypeOpt inner");
                            if (innerCv.value().optIsSome()) {
                                yield innerCv.value().optUnwrap().valueType();
                            }
                        }
                        yield (Type) new Tuple(Opt.none(), new TList<>());
                    }
                };
        return Opt.some(t);
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }

    @Override
    public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
        return new BindingDynamicBinding(dynamicKey, this);
    }
}
