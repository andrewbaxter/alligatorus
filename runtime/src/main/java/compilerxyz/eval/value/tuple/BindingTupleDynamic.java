package compilerxyz.eval.value.tuple;

import compilerxyz.gen.Binding;
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.BindingCode;
import compilerxyz.eval.dynamic.CodeLoc;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record BindingTupleDynamic(BindingId dynamicKey, TList<Type> elements)
    implements Binding {

  @Override
  public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
    var resultElements = new TList<TupleElement>(elements.size());
    for (var t : elements) {
      resultElements.add(new TupleElement.Dynamic(t));
    }
    return new CodeValue(
        Code.of(loc, BindingCode.bindingUse(new CodeLoc.Some(loc), xx.vx, dynamicKey)),
        Opt.some(
            new Tuple(
                Opt.some(new TupleBaseDynamic(new ValueDynamic.Source.Binding(dynamicKey))),
                resultElements)));
  }
}
