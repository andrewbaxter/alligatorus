package compilerxyz.eval.value.record;

import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.ExprSubErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaBytecode;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public record BindingRecordBound(
    TOrderedMap<ConstKey, RecordScopeField> fields, Opt<BindingId> jsKey) implements Binding {

  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
    boolean hasDynamic = fields.values().stream().anyMatch(f -> f instanceof RecordScopeField.Dynamic);
    Code useCode;
    Opt<RecordBaseDynamic> base;
    if (hasDynamic) {
      if (!(jsKey instanceof Opt.Some<BindingId> some)) {
        throw new IllegalStateException(
            "BindingRecordBound: record has dynamic fields but no jsKey");
      }
      useCode = Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, some.value()));
      base = Opt.some(new RecordBaseDynamic(new ValueDynamic.Source.Binding(some.value())));
    } else {
      useCode = Code.none(loc);
      base = Opt.none();
    }
    var resultFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      RecordField rf =
          switch (entry.getValue()) {
            case RecordScopeField.Scope s -> new RecordField.EvalRes(
                EResult.ok(s.binding().bindingUse(xx, loc, Code.none(loc))));
            case RecordScopeField.Dynamic d -> new RecordField.Dynamic(d.type());
            case RecordScopeField.Error e -> new RecordField.EvalRes(EResult.errs(e.errors()));
          };
      resultFields.put(entry.getKey(), rf);
    }
    return new CodeValue(useCode, Opt.some(new Record(base, resultFields)));
  }

  @Override
  public Binding bindingFork() {
    var forked = new TOrderedMap<ConstKey, RecordScopeField>();
    for (var entry : fields.entrySet()) {
      var v = entry.getValue();
      RecordScopeField nf =
          switch (v) {
            case RecordScopeField.Scope s -> new RecordScopeField.Scope(s.binding().bindingFork());
            case RecordScopeField.Dynamic d -> d;
            case RecordScopeField.Error e -> e;
          };
      forked.put(entry.getKey(), nf);
    }
    return new BindingRecordBound(forked, jsKey);
  }

  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    for (var other : others) {
      if (!(other instanceof BindingRecordBound o)) {
        return EResult.err(loc,
            "cannot merge BindingRecordBound with a differently-typed binding across branches");
      }
      if (!o.fields.keySet().equals(fields.keySet())) {
        return EResult.err(loc,
            "cannot merge BindingRecordBound with different field keys across branches");
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
            var ov = ((BindingRecordBound) other).fields.get(key);
            if (!(ov instanceof RecordScopeField.Scope os)) {
              shapeOk = false;
              break;
            }
            otherSubs.add(os.binding());
          }
          if (!shapeOk) {
            errors.add(new ExprErr(loc,
                TList.of(new ExprSubErr.Opaque(
                    "cannot merge BindingRecordBound: field " + key
                        + " has a different variant across branches"))));
            mergedFields.put(key, v);
          } else {
            var mres = s.binding().bindingMerge(xx, loc, otherSubs);
            mres.drainInto(errors);
            var mv = mres.valueOrNull();
            if (mv != null) {
              mergedFields.put(key, new RecordScopeField.Scope(mv));
            } else {
              mergedFields.put(key, v);
            }
          }
        }
        case RecordScopeField.Dynamic d -> {
          for (var other : others) {
            var ov = ((BindingRecordBound) other).fields.get(key);
            if (!(ov instanceof RecordScopeField.Dynamic)) {
              errors.add(new ExprErr(loc,
                  TList.of(new ExprSubErr.Opaque(
                      "cannot merge BindingRecordBound: field " + key
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
    return EResult.ok(new BindingRecordBound(mergedFields, jsKey));
  }
}
