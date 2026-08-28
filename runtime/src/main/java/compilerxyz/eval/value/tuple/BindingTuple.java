package compilerxyz.eval.value.tuple;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.EvalOutcome;
import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.eval.value.ValueDynamicSource;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.eval.ExprSubErr.Opaque;

public record BindingTuple(TList<TupleScopeField> elements, Opt<BindingId> jsKey)
    implements Binding {

  @Override
  public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
    // Sourced-ness is exactly "is a composite stored?" — i.e. jsKey. Inferring
    // it from whether any element is dynamic is a lossy proxy: a 0-field
    // composite (empty tuple) is stored yet has no dynamic element, so it must
    // still load from its local on use.
    Code useCode;
    Opt<TupleBaseDynamic> base;
    if (jsKey.optIsSome()) {
      useCode = Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, jsKey.optUnwrap()));
      base = Opt.some(new TupleBaseDynamic(new ValueDynamicSource.Binding(jsKey.optUnwrap())));
    } else {
      useCode = Code.none(loc);
      base = Opt.none();
    }
    var resultElements = new TList<TupleElement>(elements.size());
    for (var e : elements) {
      TupleElement te =
          switch (e) {
            case TupleScopeField.Scope s -> new TupleElement.EvalRes(
                EResult.ok(s.binding().bindingUse(xx, loc, Opt.<BindingSetter>none(), Code.none(loc))));
            case TupleScopeField.Dynamic d -> new TupleElement.Dynamic(d.type());
            case TupleScopeField.Error er -> new TupleElement.EvalRes(EResult.errs(er.errors()));
          };
      resultElements.add(te);
    }
    return new CodeValue(useCode, EvalOutcome.ofValue(new Tuple(base, resultElements)));
  }

  @Override
  public Binding bindingFork() {
    var forked = new TList<TupleScopeField>(elements.size());
    for (var e : elements) {
      switch (e) {
        case TupleScopeField.Scope s -> forked.add(new TupleScopeField.Scope(s.binding().bindingFork()));
        case TupleScopeField.Dynamic d -> forked.add(d);
        case TupleScopeField.Error er -> forked.add(er);
      }
    }
    return new BindingTuple(forked, jsKey);
  }



  @Override
  public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
    for (var other : others) {
      if (!(other instanceof BindingTuple o)) {
        return EResult.err(loc,
            "cannot merge BindingTuple with a differently-typed binding across branches");
      }
      if (o.elements.size() != elements.size()) {
        return EResult.err(loc,
            "cannot merge BindingTuple with different element count across branches");
      }
    }
    var mergedElements = new TList<TupleScopeField>(elements.size());
    var errors = new TList<ExprErr>();
    for (int i = 0; i < elements.size(); i++) {
      var e = elements.get(i);
      switch (e) {
        case TupleScopeField.Scope s -> {
          var otherSubs = new TList<Binding>(others.size());
          boolean shapeOk = true;
          for (var other : others) {
            var oe = ((BindingTuple) other).elements.get(i);
            switch (oe) {
              case TupleScopeField.Scope os -> otherSubs.add(os.binding());
              case TupleScopeField.Dynamic d -> shapeOk = false;
              case TupleScopeField.Error er -> shapeOk = false;
            }
            if (!shapeOk) break;
          }
          if (!shapeOk) {
            errors.add(new ExprErr(loc,
                TList.of(new Opaque(
                    "cannot merge BindingTuple: element " + i
                        + " has a different variant across branches"))));
            mergedElements.add(e);
          } else {
            var mres = s.binding().bindingMerge(xx, loc, otherSubs);
            mres.drainInto(errors);
            var mv = mres.valueOrNull();
            if (mv.optIsSome()) {
              mergedElements.add(new TupleScopeField.Scope(mv.optUnwrap()));
            } else {
              mergedElements.add(e);
            }
          }
        }
        case TupleScopeField.Dynamic d -> {
          mergeOthers:
          for (var other : others) {
            var oe = ((BindingTuple) other).elements.get(i);
            switch (oe) {
              case TupleScopeField.Dynamic ignored -> {}
              case TupleScopeField.Scope s -> {
                errors.add(new ExprErr(loc,
                    TList.of(new Opaque(
                        "cannot merge BindingTuple: element " + i
                            + " has a different variant across branches"))));
                break mergeOthers;
              }
              case TupleScopeField.Error er -> {
                errors.add(new ExprErr(loc,
                    TList.of(new Opaque(
                        "cannot merge BindingTuple: element " + i
                            + " has a different variant across branches"))));
                break mergeOthers;
              }
            }
          }
          mergedElements.add(d);
        }
        case TupleScopeField.Error er -> mergedElements.add(er);
      }
    }
    if (!errors.isEmpty()) {
      return EResult.errs(errors);
    }
    return EResult.ok(new BindingTuple(mergedElements, jsKey));
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
