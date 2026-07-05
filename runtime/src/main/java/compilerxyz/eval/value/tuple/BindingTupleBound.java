package compilerxyz.eval.value.tuple;

import compilerxyz.eval.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record BindingTupleBound(TList<TupleScopeField> elements, Opt<BindingId> jsKey)
    implements Binding {

  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    boolean hasDynamic =
        elements.stream().anyMatch(f -> f instanceof TupleScopeField.Dynamic);
    Code useCode;
    Opt<TupleBaseDynamic> base;
    if (hasDynamic) {
      if (!(jsKey instanceof Opt.Some<BindingId> some)) {
        throw new IllegalStateException(
            "BindingTupleBound: tuple has dynamic elements but no jsKey");
      }
      useCode = Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, some.value()));
      base = Opt.some(new TupleBaseDynamic(new ValueDynamic.Source.Binding(some.value())));
    } else {
      useCode = Code.none(loc);
      base = Opt.none();
    }
    var resultElements = new TList<TupleElement>(elements.size());
    for (var e : elements) {
      TupleElement te =
          switch (e) {
            case TupleScopeField.Scope s -> new TupleElement.EvalRes(
                EResult.ok(s.binding().bindingUse_(xx, loc, Code.none(loc))));
            case TupleScopeField.Dynamic d -> new TupleElement.Dynamic(d.type());
            case TupleScopeField.Error er -> new TupleElement.EvalRes(EResult.errs(er.errors()));
          };
      resultElements.add(te);
    }
    return new CodeValue(useCode, Opt.some(new Tuple(base, resultElements)));
  }
}
