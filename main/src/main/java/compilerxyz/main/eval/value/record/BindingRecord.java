package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.BindingDynamicSlot;
import compilerxyz.main.eval.dynamic.CompositeClassEmitter;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record BindingRecord(TOrderedMap<ConstKey, RecordScopeField> fields, Opt<BindingId> jsKey)
        implements Binding, BindingDynamicSlot {
    @Override
    public BindingId bindingDynamicSlotKey() {
        if (jsKey.optIsNone()) {
            throw new IllegalStateException("BindingRecord: binding has no dynamic slot");
        }
        return jsKey.optUnwrap();
    }

    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, BindingSetter setter, Code code) {
        Opt<RecordBaseDynamic> base;
        Code baseCode = Code.none(loc);
        switch (jsKey) {
            case Opt.Some<BindingId> some -> {
                var dynamicTypes = new TOrderedMap<ConstKey, Type>();
                for (var entry : fields.entrySet()) {
                    switch (entry.getValue()) {
                        case RecordScopeField.Dynamic d -> dynamicTypes.put(entry.getKey(), d.type());
                        default -> {}
                    }
                }
                var slotInfo = Record.computeSlotInfoFromTypes(xx, dynamicTypes);
                var desc = JavaDataDescriptor.fromInternalName(
                        JavaInternalName.of(CompositeClassEmitter.internalNameFor(xx, slotInfo.sortedTypes())));
                base = Opt.some(new RecordBaseDynamic(new ValueDynamicSource.Slot(some.value(), desc, setter)));
            }
            case Opt.None<BindingId> none -> base = Opt.none();
        }
        var resultFields = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : fields.entrySet()) {
            RecordField rf =
                    switch (entry.getValue()) {
                        case RecordScopeField.Scope s ->
                            new RecordField.EvalRes(EResult.ok(s.binding()
                                    .bindingUse(
                                            xx,
                                            loc,
                                            new BindingSetterRecordField(fields, entry.getKey(), jsKey, setter),
                                            Code.none(loc))));
                        case RecordScopeField.Dynamic d -> new RecordField.Dynamic(d.type());
                        case RecordScopeField.Error e -> new RecordField.EvalRes(EResult.errs(e.errors()));
                    };
            resultFields.put(entry.getKey(), rf);
        }
        var combined = Eval.codeConcat(xx, loc, code, baseCode);
        return new CodeValue(
                combined.requireValue("BindingRecord use"), EvalOutcome.ofValue(new Record(base, resultFields)));
    }

    @Override
    public Binding bindingFork() {
        var forked = new TOrderedMap<ConstKey, RecordScopeField>();
        for (var entry : fields.entrySet()) {
            var v = entry.getValue();
            RecordScopeField nf =
                    switch (v) {
                        case RecordScopeField.Scope s ->
                            new RecordScopeField.Scope(s.binding().bindingFork());
                        case RecordScopeField.Dynamic d -> d;
                        case RecordScopeField.Error e -> e;
                    };
            forked.put(entry.getKey(), nf);
        }
        return new BindingRecord(forked, jsKey);
    }

    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
        for (var other : others) {
            if (!(other instanceof BindingRecord o)) {
                return EResult.err(loc, "cannot merge BindingRecord with a differently-typed binding across branches");
            }
            if (!o.fields.keySet().equals(fields.keySet())) {
                return EResult.err(loc, "cannot merge BindingRecord with different field keys across branches");
            }
        }
        var mergedFields = new TOrderedMap<ConstKey, RecordScopeField>();
        var errors = new TList<ExprErr>();
        for (var entry : fields.entrySet()) {
            var key = entry.getKey();
            var v = entry.getValue();
            switch (v) {
                case RecordScopeField.Scope s -> {
                    var otherSubs = new TList<Binding>(others.size());
                    boolean shapeOk = true;
                    for (var other : others) {
                        var ov = ((BindingRecord) other).fields.get(key);
                        var os =
                                switch (ov) {
                                    case RecordScopeField.Scope scopeField -> Opt.some(scopeField);
                                    case RecordScopeField.Dynamic dynField -> Opt.<RecordScopeField.Scope>none();
                                    case RecordScopeField.Error errField -> Opt.<RecordScopeField.Scope>none();
                                };
                        if (os.optIsNone()) {
                            shapeOk = false;
                            break;
                        }
                        otherSubs.add(os.optUnwrap().binding());
                    }
                    if (!shapeOk) {
                        errors.add(new ExprErr(
                                loc,
                                TList.of(new ExprSubErr.Opaque("cannot merge BindingRecord: field " + key
                                        + " has a different variant across branches"))));
                        mergedFields.put(key, v);
                    } else {
                        var mres = s.binding().bindingMerge(xx, loc, otherSubs);
                        mres.drainInto(errors);
                        var mv = mres.valueOpt();
                        if (mv.optIsSome()) {
                            mergedFields.put(key, new RecordScopeField.Scope(mv.optUnwrap()));
                        } else {
                            mergedFields.put(key, v);
                        }
                    }
                }
                case RecordScopeField.Dynamic d -> {
                    for (var other : others) {
                        var ov = ((BindingRecord) other).fields.get(key);
                        var isDynamic =
                                switch (ov) {
                                    case RecordScopeField.Dynamic od -> true;
                                    case RecordScopeField.Scope os -> false;
                                    case RecordScopeField.Error oe -> false;
                                };
                        if (!isDynamic) {
                            errors.add(new ExprErr(
                                    loc,
                                    TList.of(new ExprSubErr.Opaque("cannot merge BindingRecord: field " + key
                                            + " has a different variant across branches"))));
                            break;
                        }
                    }
                    mergedFields.put(key, d);
                }
                case RecordScopeField.Error e -> mergedFields.put(key, e);
            }
        }
        if (!errors.isEmpty()) {
            return EResult.errs(errors);
        }
        return EResult.ok(new BindingRecord(mergedFields, jsKey));
    }

    @Override
    public EResult<Code> bindingDrop(ExprContext xx, QExprId loc) {
        return Binding.bindingDrop(xx, loc, this);
    }

    private record BindingSetterRecordField(
            TOrderedMap<ConstKey, RecordScopeField> fields, ConstKey key, Opt<BindingId> jsKey, BindingSetter parent)
            implements BindingSetter {
        @Override
        public boolean bindingSetterSet(ExprContext xx, QExprId loc, Binding newBinding, boolean dropModified) {
            var replaced = new TOrderedMap<ConstKey, RecordScopeField>();
            for (var entry : fields.entrySet()) {
                replaced.put(
                        entry.getKey(),
                        entry.getKey().equals(key) ? new RecordScopeField.Scope(newBinding) : entry.getValue());
            }
            return parent.bindingSetterSet(xx, loc, new BindingRecord(replaced, jsKey), dropModified);
        }
    }
}
