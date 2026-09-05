package compilerxyz.main.eval.value.record;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.RecordBindField;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.GenAspectBind;
import compilerxyz.main.gen.TargetBindSub;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectBindRecord(Record type) implements AspectBind {

    @Override
    public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
        var jvmTargetRes = DynUtil.jvmTarget(xx, loc);
        if (jvmTargetRes.hasErrors()) return jvmTargetRes.reerr();
        var jvmTarget = jvmTargetRes.requireValue("jvm target");
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("Record::bindSub: CodeValue has no value");
        }
        if (!(cv.value().optUnwrap() instanceof Record record)) {
            throw new IllegalStateException("Record::bindSub: expected Record value, got "
                    + cv.value().optUnwrap().getClass());
        }
        var carry = cv.code();

        if (type.base.optIsSome()) {
            Type recordType = type;
            var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
            for (var entry : type.fields.entrySet()) {
                RecordField.Dynamic d;
                switch (entry.getValue()) {
                    case RecordField.Dynamic df -> d = df;
                    case RecordField.EvalRes er ->
                        throw new IllegalStateException("Record::bindSub: bind on non-type record");
                }
                scopeFields.put(entry.getKey(), new RecordScopeField.Dynamic(d.type()));
            }
            var subRes = jvmTarget.codeToBindSub(loc, carry, recordType);
            if (subRes.hasErrors()) return subRes.reerr();
            var sub = subRes.requireValue("Record::bindSub base.bindSub");
            var bound = jvmTarget.bindSlot(xx, loc, sub);
            return EResult.ok(
                    new BindResult(Code.target(bound.code), new BindingRecord(scopeFields, Opt.some(bound.id))));
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
                    errors.add(new ExprErr(
                            loc,
                            TList.of(new ExprSubErr.Opaque("Record entry '"
                                    + key.constKeyToCanonical()
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
                    var savedValue = elemCv.outcome();
                    var subRes = jvmTarget.maybeBindSub(xx, loc, elemCv);
                    if (subRes.hasErrors()) {
                        if (errors == null) errors = new TList<>();
                        subRes.drainInto(errors);
                        continue;
                    }
                    var subOpt = subRes.requireValue("Record::bindSub targetMaybeBindSub");
                    if (subOpt.optIsSome()) {
                        hasTarget = true;
                        bindResults.add(new FieldBindResult(key, Opt.some(subOpt.optUnwrap()), Opt.none()));
                    } else {
                        bindResults.add(new FieldBindResult(
                                key, Opt.none(), Opt.some(new CodeValue(Code.none(loc), savedValue))));
                    }
                }
            }
        }
        if (errors != null && !errors.isEmpty()) return EResult.errs(errors);
        if (!hasTarget) {
            var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
            for (var br : bindResults) {
                if (br.constCv().optIsNone()) {
                    throw new IllegalStateException(
                            "Record::bindSub: !has_target but bindResults contains a target sub");
                }
                var fieldCv = br.constCv().optUnwrap();
                if (fieldCv.value().optIsNone())
                    throw new IllegalStateException("Record::bindSub: const field CodeValue has no value");
                var fieldType = fieldCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, fieldType);
                if (bindRes.hasErrors()) {
                    scopeFields.put(
                            br.key(),
                            new RecordScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Record::bindSub AspectBind");
                var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
                if (fieldBindRes.hasErrors()) {
                    scopeFields.put(
                            br.key(),
                            new RecordScopeField.Error(fieldBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
                scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
            }
            return EResult.ok(new BindResult(Code.none(loc), new BindingRecord(scopeFields, Opt.none())));
        }
        var scopeFields = new TOrderedMap<ConstKey, RecordScopeField>();
        var targetFields = new TList<RecordBindField>(bindResults.size());
        for (var br : bindResults) {
            if (br.targetSub().optIsSome()) {
                scopeFields.put(
                        br.key(), new RecordScopeField.Dynamic(br.targetSub().optUnwrap().type));
                targetFields.add(new RecordBindField(br.key(), br.targetSub().optUnwrap()));
            } else if (br.constCv().optIsSome()) {
                var fieldCv = br.constCv().optUnwrap();
                if (fieldCv.value().optIsNone())
                    throw new IllegalStateException("Record::bindSub: const field CodeValue has no value");
                var fieldType = fieldCv.value().optUnwrap().valueType();
                var bindRes = GenAspectBind.get(xx, loc, fieldType);
                if (bindRes.hasErrors()) {
                    scopeFields.put(
                            br.key(),
                            new RecordScopeField.Error(bindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var bind = bindRes.requireValue("Record::bindSub AspectBind");
                var fieldBindRes = bind.aspectBindBind(xx, loc, fieldCv);
                if (fieldBindRes.hasErrors()) {
                    scopeFields.put(
                            br.key(),
                            new RecordScopeField.Error(fieldBindRes.errorsOpt().optUnwrap()));
                    continue;
                }
                var fieldBind = fieldBindRes.requireValue("Record::bindSub field AspectBind.bind");
                scopeFields.put(br.key(), new RecordScopeField.Scope(fieldBind.binding()));
            } else {
                throw new IllegalStateException("Record::bindSub: bind result has neither target nor const");
            }
        }
        var combinedRes = jvmTarget.bindSubRecord(xx, loc, carry, targetFields);
        if (combinedRes.hasErrors()) return combinedRes.reerr();
        var bound = jvmTarget.bindSlot(xx, loc, combinedRes.requireValue("record bind sub"));
        return EResult.ok(new BindResult(Code.target(bound.code), new BindingRecord(scopeFields, Opt.some(bound.id))));
    }
}
