package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.ArtifactBody;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ArtifactIdSource;
import compilerxyz.main.artifact.ArtifactJson;
import compilerxyz.main.artifact.ArtifactSupport;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterRootDelegate;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
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
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.CompositeSlotSort;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectLift;
import compilerxyz.main.gen.AspectTuple;
import compilerxyz.main.gen.GenAspectArtifact;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.main.gen.GenAspectElementUseConst;
import compilerxyz.main.gen.GenAspectElementUseDynamic;
import compilerxyz.main.gen.GenAspectField;
import compilerxyz.main.gen.GenAspectInitialBindDynamic;
import compilerxyz.main.gen.GenAspectLift;
import compilerxyz.main.gen.GenAspectStackValueDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenAspectType;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.main.gen.Value;
import compilerxyz.main.safejson.JsonValue;
import compilerxyz.main.safejson.JsonValueArray;
import compilerxyz.main.safejson.JsonValueBoolean;
import compilerxyz.main.safejson.JsonValueNull;
import compilerxyz.main.safejson.JsonValueNumber;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.CodegenDescriptor;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.lang.reflect.Field;
import java.util.function.Function;

@BuiltinFull(uniqueId = 6147L)
public record Tuple(
        Opt<TupleBaseDynamic> base, IList<TupleElement> elements, ArtifactSupport.ArtifactIdHolder artifactIdCell)
        implements Aspect, Type, Value {

    public Tuple(Opt<TupleBaseDynamic> base, IList<TupleElement> elements) {
        this(base, elements, new ArtifactSupport.ArtifactIdHolder());
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
        return switch (tuple.base()) {
            case Opt.None<TupleBaseDynamic> n -> {
                var b = new JavaBytecodeBuilder(loc);
                EvalOutcome accessedOutcome = null;
                var held = new TList<CodeValue>();
                for (int i = 0; i < tuple.elements().size(); i++) {
                    boolean isAccessed = accessIndex.optIsSome() && accessIndex.optUnwrap() == i;
                    var e = tuple.elements().get(i);
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
                    var cv = res.valueOpt();
                    if (!res.hasErrors()
                            && cv.optIsSome()
                            && cv.optUnwrap().value().optIsNone()) {
                        yield EResult.err(
                                loc,
                                "Tuple element " + i + " does not fall through (contains an unreachable"
                                        + " control jump like throw/panic); this expression is not valid in a"
                                        + " tuple element position");
                    }
                    var elemCv = res.requireValue("Tuple::accessOrIndexSourceless element");
                    var elemSome = (Opt.Some<Value>) elemCv.value();
                    Type elemType = elemSome.value().valueType();
                    if (isAccessed) {
                        var useCv = elemCv;
                        var useConstOpt = GenAspectElementUseConst.getMaybe(xx, elemType);
                        if (useConstOpt.optIsSome()) {
                            var useRes = useConstOpt.optUnwrap().aspectElementUseConstElementUseConst(xx, loc, elemCv);
                            if (useRes.hasErrors()) yield useRes.reerr();
                            useCv = useRes.requireValue("Tuple::accessOrIndexSourceless useConst");
                        }
                        var useAdd = b.addCode(loc, useCv.code());
                        if (useAdd.optIsSome()) yield EResult.errs(useAdd.optUnwrap());
                        accessedOutcome = useCv.outcome();
                        continue;
                    }
                    var elemAdd = b.addCode(loc, elemCv.code());
                    if (elemAdd.optIsSome()) yield EResult.errs(elemAdd.optUnwrap());
                    held.add(elemCv);
                }
                for (int hi = held.size() - 1; hi >= 0; hi--) {
                    var heldCv = held.get(hi);
                    var fullDropOpt = GenAspectDropDynamic.getMaybe(
                            xx, heldCv.value().optUnwrap().valueType());
                    if (fullDropOpt.optIsSome()) {
                        var dropRes = fullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, heldCv);
                        if (dropRes.hasErrors()) yield dropRes.reerr();
                        var dropAdd = b.addCode(loc, dropRes.requireValue("Tuple::accessOrIndexSourceless drop"));
                        if (dropAdd.optIsSome()) yield EResult.errs(dropAdd.optUnwrap());
                    }
                }
                if (accessIndex.optIsSome()) {
                    if (accessedOutcome == null) {
                        yield EResult.err(loc, "Tuple element " + accessIndex.optUnwrap() + " not found");
                    }
                    if (b.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), accessedOutcome));
                    yield EResult.ok(new CodeValue(Code.target(b.code()), accessedOutcome));
                }
                if (b.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.NO_VALUE));
                yield EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
            }
            case Opt.Some<TupleBaseDynamic> s -> {
                var baseSource = s.value().source();
                var dynamicTypes = new TList<Type>();
                var dynamicIndexOfElement = new TList<Integer>(tuple.elements().size());
                for (int i = 0; i < tuple.elements().size(); i++) {
                    var e = tuple.elements().get(i);
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
                    var out = new JavaBytecodeBuilder(loc);
                    for (int i = tuple.elements().size() - 1; i >= 0; i--) {
                        int dynIdx = dynamicIndexOfElement.get(i);
                        if (dynIdx < 0) {
                            var heldRes =
                                    ((TupleElement.EvalRes) tuple.elements().get(i)).result();
                            if (heldRes.hasErrors()) yield heldRes.reerr();
                            var heldCv = heldRes.requireValue("Tuple::drop held element");
                            var heldDrop = Eval.dropValue(xx, loc, heldCv);
                            if (heldDrop.hasErrors()) yield heldDrop.reerr();
                            var heldCodeAdd = out.addCode(loc, heldCv.code());
                            if (heldCodeAdd.optIsSome()) yield EResult.errs(heldCodeAdd.optUnwrap());
                            var heldAdd = out.addCode(loc, heldDrop.requireValue("Tuple::drop held code"));
                            if (heldAdd.optIsSome()) yield EResult.errs(heldAdd.optUnwrap());
                            continue;
                        }
                        var t = dynamicTypes.get(dynIdx);
                        int slotIdx = slotInfo.slotOfKey().getOpt(dynIdx).optUnwrap();
                        var tcFullDropOpt = GenAspectDropDynamic.getMaybe(xx, t);
                        if (tcFullDropOpt.optIsSome()) {
                            var access = new JavaBytecodeBuilder(loc);
                            var child = access.getField(
                                    klass,
                                    CompositeClassEmitter.slotFieldName(slotIdx),
                                    CompositeClassEmitter.slotDescriptor(xx, t),
                                    DynUtil.sourceReg(access, baseSource));
                            var childCv = DynUtil.regToTypedValue(xx, loc, access.code(), child, t);
                            var tcDropRes = tcFullDropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, childCv);
                            if (tcDropRes.hasErrors()) yield tcDropRes.reerr();
                            out.add(access.code());
                            var dropAdd = out.addCode(loc, tcDropRes.requireValue("Tuple::tupleChildDropCode drop"));
                            if (dropAdd.optIsSome()) yield EResult.errs(dropAdd.optUnwrap());
                        }
                    }
                    if (out.isEmpty()) yield EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.NO_VALUE));
                    yield EResult.ok(new CodeValue(Code.target(out.code()), EvalOutcome.NO_VALUE));
                } else {
                    int accessedIdx = accessIndex.optUnwrap();
                    if (accessedIdx < 0 || accessedIdx >= tuple.elements().size()) {
                        yield EResult.err(loc, "Tuple element index " + accessedIdx + " out of range");
                    }
                    int isDynIdx = dynamicIndexOfElement.get(accessedIdx);
                    if (isDynIdx < 0) {
                        throw new IllegalStateException("Tuple::indexSourced: element index " + accessedIdx
                                + " is a scope-bound (non-dynamic) element; access should be routed elsewhere");
                    }
                    var accessedType = dynamicTypes.get(isDynIdx);
                    int accessedSlot = slotInfo.slotOfKey().getOpt(isDynIdx).optUnwrap();
                    var useDynOpt = GenAspectElementUseDynamic.getMaybe(xx, accessedType);
                    if (useDynOpt.optIsNone()) {
                        yield EResult.err(
                                loc,
                                "Tuple element " + accessedIdx + " type (" + accessedType.typeDesc()
                                        + ") does not implement AspectElementUseDynamic");
                    }
                    var access = new JavaBytecodeBuilder(loc);
                    var selfAdd = access.addCode(loc, selfCv.code());
                    if (selfAdd.optIsSome()) yield EResult.errs(selfAdd.optUnwrap());
                    var fieldName = CompositeClassEmitter.slotFieldName(accessedSlot);
                    var fieldDesc = CompositeClassEmitter.slotDescriptor(xx, accessedType);
                    var childSource = new ValueDynamicSource.Field(baseSource, klass, fieldName, fieldDesc);
                    yield useDynOpt
                            .optUnwrap()
                            .aspectElementUseDynamicElementUseDynamic(xx, loc, childSource, Code.target(access.code()));
                }
            }
        };
    }

    private static CompositeSlotSort.Result<Integer> sortTupleSlots(ExprContext xx, TList<Type> types) {
        var entries = new TOrderedMap<Integer, Type>();
        for (int i = 0; i < types.size(); i++) entries.put(i, types.get(i));
        return CompositeSlotSort.sort(xx, entries, Integer::compare);
    }

    static TList<Type> sortTupleTypes(ExprContext xx, TList<Type> types) {
        return sortTupleSlots(xx, types).sortedTypes();
    }

    static TList<Integer> slotOfIndex(ExprContext xx, TList<Type> types) {
        var slotOf = sortTupleSlots(xx, types).slotOfKey();
        var out = new TList<Integer>(types.size());
        for (int i = 0; i < types.size(); i++) out.add(slotOf.getOpt(i).optUnwrap());
        return out;
    }

    @Override
    public String typeDesc() {
        var parts = new TList<String>(elements.size());
        for (var e : elements) {
            parts.add(
                    switch (e) {
                        case TupleElement.Dynamic d -> d.type().typeDesc();
                        case TupleElement.EvalRes r -> "?";
                    });
        }
        return "tuple(" + String.join(", ", parts) + ")";
    }

    public Type valueType() {

        var slotTypes = new TList<TupleElement>(elements.size());
        for (var e : elements) {
            Type slot =
                    switch (e) {
                        case TupleElement.Dynamic d -> d.type();
                        case TupleElement.EvalRes r -> {
                            var elemOpt = r.result().valueOpt();
                            if (elemOpt.optIsSome()
                                    && elemOpt.optUnwrap().value().optIsSome()) {
                                yield elemOpt.optUnwrap().value().optUnwrap().valueType();
                            }
                            throw new IllegalStateException("Tuple::valueType: element has no value");
                        }
                    };
            slotTypes.add(new TupleElement.Dynamic(slot));
        }
        return new Tuple(base, IList.from(slotTypes));
    }

    private boolean tupleElementsAllTypes() {
        for (var element : elements) {
            switch (element) {
                case TupleElement.Dynamic d -> {}
                case TupleElement.EvalRes er -> {
                    var inner = er.result();
                    if (inner.hasErrors()) return false;
                    var innerCv = inner.requireValue("Tuple::tupleElementsAllTypes element");
                    if (innerCv.value().optIsNone()) return false;
                    var elementValue = innerCv.value().optUnwrap();
                    if (elementValue instanceof Tuple nested) {
                        if (!nested.tupleElementsAllTypes()) return false;
                    } else if (elementValue instanceof ValueObjectConst voc) {
                        if (!(voc.data() instanceof Type)) return false;
                    } else if (!(elementValue instanceof Type)) {
                        return false;
                    }
                }
            }
        }
        return true;
    }

    @CodegenDescriptor
    public static Tuple newDynamic(Object[] elems) {
        var out = new TList<TupleElement>(elems.length);
        for (var raw : elems) {
            if (!(raw instanceof Type t)) {
                throw new IllegalStateException("Tuple::newDynamic: element value is not a Type: " + raw.getClass());
            }
            out.add(new TupleElement.Dynamic(t));
        }
        return new Tuple(Opt.none(), IList.from(out));
    }

    @Override
    public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
        if (key.equals(GenAspectInitialBindDynamic.AspectId)) {
            return Opt.some(new AspectInitialBindDynamicTuple(this));
        }
        if (key.equals(GenTrivialIdentityAspects.TUPLE_IDENTITY)) {
            return Opt.some(this);
        }
        if (key.equals(GenAspectTuple.AspectId)) {
            return Opt.some(new AspectTupleTuple(this));
        }
        if (key.equals(GenAspectType.AspectId)) {
            return tupleElementsAllTypes() ? Opt.some(AspectTypeTuple.INSTANCE) : Opt.none();
        }
        if (key.equals(GenAspectStackValueDynamic.AspectId)) {
            return Opt.some(new AspectStackValueDynamicTuple(this));
        }
        if (key.equals(GenAspectField.AspectId)) {
            return Opt.some(new AspectFieldTuple(this));
        }
        if (key.equals(GenAspectDropDynamic.AspectId)) {
            return Opt.some(new AspectDropDynamicTuple(this));
        }
        if (key.equals(GenAspectConsumeDynamic.AspectId)) {
            return Opt.some(new AspectConsumeDynamicTuple(this));
        }
        if (key.equals(GenAspectBind.AspectId)) {
            return Opt.some(new AspectBindTuple(this));
        }
        if (key.equals(GenAspectArtifact.AspectId)) {
            return Opt.some(AspectArtifactTuple.INSTANCE);
        }
        if (key.equals(GenAspectField.AspectId)) {
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
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome() && tupleElementsAllTypes();
    }

    @Override
    public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        if (destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_IDENTITY).optIsSome()) {
            if (destType.typeAspect(xx, GenAspectStackValueDynamic.AspectId).optIsNone()) {
                return EResult.ok(cv);
            }
            if (cv.value().optIsNone()) {
                return EResult.err(loc, "Tuple::typeCastTo: CodeValue has no value");
            }
            var tuple = MustCast.mustCast(Tuple.class, cv.value().optUnwrap());
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var elems = new TList<JavaBytecodeReg>(tuple.elements().size());
            for (var element : tuple.elements()) {
                TupleElement.EvalRes er;
                switch (element) {
                    case TupleElement.EvalRes erVal -> er = erVal;
                    case TupleElement.Dynamic d -> {
                        return EResult.err(loc, "Tuple::typeCastTo: element is dynamic-only");
                    }
                }
                var elemRes = er.result();
                if (elemRes.hasErrors()) return elemRes.reerr();
                var elemCv = elemRes.requireValue("Tuple::typeCastTo element");
                if (elemCv.value().optIsNone()) {
                    return EResult.err(loc, "Tuple::typeCastTo: element has no value");
                }
                var elemValue = elemCv.value().optUnwrap();
                if (elemValue instanceof Tuple nested) {
                    var nestedRes = nested.typeCastTo(xx, loc, elemCv, destType);
                    if (nestedRes.hasErrors()) return nestedRes.reerr();
                    var nestedReg = DynUtil.valueReg(loc, nestedRes.requireValue("Tuple::typeCastTo nested"));
                    if (nestedReg.hasErrors()) return nestedReg.reerr();
                    elems.add(b.reg(nestedReg.requireValue("nestedDyn")));
                    continue;
                }
                var lowered = DynUtil.materialize(xx, loc, elemCv);
                if (lowered.hasErrors()) return lowered.reerr();
                elems.add(b.reg(lowered.requireValue("Tuple::typeCastTo element lowered")));
            }
            var array = b.newObjectArray(elems);
            var out = b.invokeStatic(GenCodegenDescriptors.TUPLE_NEW_DYNAMIC, array);
            return EResult.ok(DynUtil.regToValue(b.code(), out, destType));
        }
        var nb = new JavaBytecodeBuilder(loc);
        var nullCarry = nb.addCode(loc, cv.code());
        if (nullCarry.optIsSome()) return EResult.errs(nullCarry.optUnwrap());
        var nullReg = nb.constNull();
        return EResult.ok(DynUtil.regToValue(nb.code(), nullReg, destType));
    }

    @Override
    public boolean typeIsEqual(ExprContext xx, Type other) {
        var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TUPLE_IDENTITY, Tuple.class);
        if (oOpt.optIsNone()) return false;
        var o = oOpt.optUnwrap();
        if (elements.size() != o.elements().size()) return false;
        for (int i = 0; i < elements.size(); i++) {
            var aType = tupleElementType(elements.get(i));
            var bType = tupleElementType(o.elements().get(i));
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
            return EResult.ok(DynUtil.dynamicCastKeepingSource(cv, this));
        }
        if (!(cvValue instanceof Tuple tuple)) {
            throw new IllegalStateException("Tuple::typeCastFrom: expected Tuple value, got " + cvValue.getClass());
        }
        var erb = new Erb(loc);
        var convertedElements = new TList<TupleElement>(tuple.elements().size());
        for (int i = 0; i < tuple.elements().size(); i++) {
            var elem = tuple.elements().get(i);
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
        return EResult.ok(
                new CodeValue(cv.code(), EvalOutcome.ofValue(new Tuple(Opt.none(), IList.from(convertedElements)))));
    }

    public static final ExporterRootDelegate DELEGATE = new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
                ExprContext xx, QExprId loc, Artifactable self, Function<JsonValue, ArtifactBody> makeBody) {
            if (!(self instanceof Tuple tuple)) {
                return EResult.err(
                        loc, "Tuple::exporterRootDelegateSelfExport: expected Tuple self, got " + self.getClass());
            }
            var cell = tuple.artifactIdCell();
            return ArtifactSupport.implExportIndependentArtifact(xx, cell, () -> {
                var erb = new Erb(loc);
                var elementArtifacts = new TList<Artifact>(tuple.elements().size());
                for (var e : tuple.elements()) {
                    Type elemType;
                    switch (e) {
                        case TupleElement.Dynamic d -> elemType = d.type();
                        case TupleElement.EvalRes r -> {
                            var cvOpt = erb.mustErMaybeUnreachable(r.result());
                            if (cvOpt.optIsNone()) continue;
                            var cv = cvOpt.optUnwrap();
                            if (cv.value().optIsNone()) {
                                erb.errPush("Tuple element has no value to serialize");
                                continue;
                            }
                            elemType = cv.value().optUnwrap().valueType();
                        }
                    }
                    var artOpt = erb.mustErMaybeUnreachable(Artifactable.exportArtifact(xx, loc, elemType));
                    if (artOpt.optIsSome()) elementArtifacts.add(artOpt.optUnwrap());
                }
                if (!erb.errors().isEmpty()) return EResult.errs(erb.errors());
                var data = new TOrderedMap<String, JsonValue>();
                data.put("elements", ArtifactJson.encodeArtifactList(elementArtifacts));
                return EResult.ok(makeBody.apply(new JsonValueObject(data)));
            });
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
                        var elementsJsonOpt = obj.entries().getOpt("elements");
                        if (elementsJsonOpt.optIsNone()) {
                            return new Result.Err<>("Tuple data: missing 'elements'");
                        }
                        var elementsJson = elementsJsonOpt.optUnwrap();
                        TList<Artifact> decoded;
                        try {
                            decoded = ArtifactJson.decodeArtifactList(elementsJson);
                        } catch (RuntimeException e) {
                            return new Result.Err<>("Tuple data: malformed elements: " + e.getMessage());
                        }
                        return new Result.Ok<>(decoded);
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
                            var out = new Tuple(Opt.none(), IList.from(slots));
                            out.artifactIdCell().set(idValue);
                            return out;
                        };
                        return new Result.Ok<>(
                                new ArtifactSupport.ImportIndependentResult(Opt.none(), elementArts, fin));
                    });
        }
    };

    public static final class TupleLift implements AspectLift {
        private final TList<AspectLift> elementLifts;
        private final TList<Type> elementTypes;
        private volatile Opt<Field[]> cachedFields = Opt.none();

        public TupleLift(TList<AspectLift> elementLifts, TList<Type> elementTypes) {
            this.elementLifts = elementLifts;
            this.elementTypes = elementTypes;
        }

        @Override
        public EResult<Value> aspectLiftLift(ExprContext xx, QExprId loc, Object externalHostValue) {
            var expectedClass = CompositeClassEmitter.getOrEmit(xx, sortTupleTypes(xx, elementTypes));
            if (!expectedClass.isInstance(externalHostValue)) {
                return EResult.err(
                        loc,
                        "Tuple TupleLift::lift: expected "
                                + expectedClass.getName()
                                + ", got "
                                + externalHostValue.getClass().getName());
            }

            Field[] fields;
            if (cachedFields.optIsSome()) {
                fields = cachedFields.optUnwrap();
            } else {
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
            var errors = new TList<ExprErr>();
            for (int i = 0; i < elementLifts.size(); i++) {
                var elemLift = elementLifts.get(i);
                Object slotValue;
                try {
                    slotValue = fields[i].get(externalHostValue);
                } catch (IllegalAccessException e) {
                    throw new AssertionError("Tuple TupleLift::lift: cannot read slot field _" + i, e);
                }
                var lifted = elemLift.aspectLiftLift(xx, loc, slotValue);
                if (lifted.hasErrors()) {
                    lifted.drainInto(errors);
                    continue;
                }
                var liftedValue = lifted.requireValue("Tuple TupleLift::lift element value");
                out.add(new TupleElement.EvalRes(
                        EResult.ok(new CodeValue(Code.none(loc), EvalOutcome.ofValue(liftedValue)))));
            }
            if (!errors.isEmpty()) {
                return EResult.errs(errors);
            }
            return EResult.ok(new Tuple(Opt.none(), IList.from(out)));
        }
    }

    @Override
    public EResult<ArtifactExporter> artifactableExporter(ExprContext xx, QExprId loc) {
        return Artifactable.exporterOfType(xx, loc, valueType());
    }
}
