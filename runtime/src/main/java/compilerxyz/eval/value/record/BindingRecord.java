package compilerxyz.eval.value.record;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.EvalOutcome;
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
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.util.TOrderedMap;

public record BindingRecord(
    TOrderedMap<ConstKey, RecordScopeField> fields, Opt<BindingId> jsKey) implements Binding {

  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
    // Sourced-ness is exactly "is a composite stored?" — i.e. jsKey — not
    // whether some field happens to be dynamic (a lossy proxy that misses a
    // 0-field composite).
    Code useCode;
    Opt<RecordBaseDynamic> base;
    switch (jsKey) {
      case Opt.Some<BindingId> some -> {
        useCode = Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, some.value()));
        base = Opt.some(new RecordBaseDynamic(new ValueDynamicSource.Binding(some.value())));
      }
      case Opt.None<BindingId> none -> {
        useCode = Code.none(loc);
        base = Opt.none();
      }
    }
    var resultFields = new TOrderedMap<ConstKey, RecordField>();
    for (var entry : fields.entrySet()) {
      RecordField rf =
          switch (entry.getValue()) {
            case RecordScopeField.Scope s -> new RecordField.EvalRes(
                EResult.ok(s.binding().bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc))));
            case RecordScopeField.Dynamic d -> new RecordField.Dynamic(d.type());
            case RecordScopeField.Error e -> new RecordField.EvalRes(EResult.errs(e.errors()));
          };
      resultFields.put(entry.getKey(), rf);
    }
    return new CodeValue(useCode, EvalOutcome.ofValue(new Record(base, resultFields)));
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
    return new BindingRecord(forked, jsKey);
  }



  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    for (var other : others) {
      if (!(other instanceof BindingRecord o)) {
        return EResult.err(loc,
            "cannot merge BindingRecord with a differently-typed binding across branches");
      }
      if (!o.fields.keySet().equals(fields.keySet())) {
        return EResult.err(loc,
            "cannot merge BindingRecord with different field keys across branches");
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
            var os = switch (ov) {
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
            errors.add(new ExprErr(loc,
                TList.of(new ExprSubErr.Opaque(
                    "cannot merge BindingRecord: field " + key
                        + " has a different variant across branches"))));
            mergedFields.put(key, v);
          } else {
            var mres = s.binding().bindingMerge(xx, loc, otherSubs);
            mres.drainInto(errors);
            var mv = mres.valueOrNull();
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
            var isDynamic = switch (ov) {
              case RecordScopeField.Dynamic od -> true;
              case RecordScopeField.Scope os -> false;
              case RecordScopeField.Error oe -> false;
            };
            if (!isDynamic) {
              errors.add(new ExprErr(loc,
                  TList.of(new ExprSubErr.Opaque(
                      "cannot merge BindingRecord: field " + key
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

  @Override
  public Binding aspectElementUseDynamicBindElementUseDynamicBind(BindingId dynamicKey) {
    return Binding.elementUseDynamicBind(dynamicKey);
  }
}
