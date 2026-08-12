package compilerxyz.eval.value.source;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.NumericKind;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.gen.AspectListConst;
import compilerxyz.gen.AspectNumericConst;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.gen.GenAspectByte;
import compilerxyz.gen.GenAspectListConst;
import compilerxyz.gen.GenAspectNumericConst;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.Value;
import compilerxyz.syntax.HashValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public final class SourceSpecs {
  private SourceSpecs() {}

  public static Value spec(SourceSpecValue value) {
    return new ValueObjectConst(new TypeSourceSpec(), value);
  }

  public static Value hash(HashValue value) {
    return new ValueObjectConst(new TypeHash(), value);
  }

  public static EResult<Value> argValue(QExprId loc, CodeValue argument, String context) {
    Opt.Some<Value> some;
    switch (argument.value()) {
      case Opt.Some<Value> s -> some = s;
      case Opt.None<Value> n -> { return EResult.err(loc, context + ": argument has no compile-time value"); }
    }
    return EResult.ok(some.value());
  }

  public static EResult<String> constString(
      ExprContext xx, QExprId loc, Value value, String context) {
    Opt.Some<AspectStringConst> some;
    switch (GenAspectStringConst.getMaybe(xx, value.valueType())) {
      case Opt.Some<AspectStringConst> s -> some = s;
      case Opt.None<AspectStringConst> n -> { return EResult.err(loc, context + ": expected a const string, got " + value.valueType().typeDesc()); }
    }
    return EResult.ok(some.value().aspectStringConstGet(value));
  }

  public static EResult<TList<Value>> constTuple(QExprId loc, Value value, String context) {
    if (!(value instanceof Tuple tuple) || tuple.base().optIsSome()) {
      return EResult.err(loc, context + ": expected a const tuple");
    }
    var out = new TList<Value>(tuple.elements().size());
    for (var element : tuple.elements()) {
      TupleElement.EvalRes er;
      switch (element) {
        case TupleElement.EvalRes e -> er = e;
        case TupleElement.Dynamic d -> { return EResult.err(loc, context + ": tuple element is not const"); }
      }
      var res = er.result();
      if (res.hasErrors()) return res.reerr();
      var cv = res.requireValue(context + " tuple element");
      Opt.Some<Value> some;
      switch (cv.value()) {
        case Opt.Some<Value> s -> some = s;
        case Opt.None<Value> n -> { return EResult.err(loc, context + ": tuple element has no value"); }
      }
      out.add(some.value());
    }
    return EResult.ok(out);
  }

  public static EResult<HashValue> hashValue(QExprId loc, Value value, String context) {
    if (value instanceof ValueObjectConst voc
        && voc.type() instanceof TypeHash
        && voc.data() instanceof HashValue hv) {
      return EResult.ok(hv);
    }
    return EResult.err(loc, context + ": expected a hash value");
  }

  public static EResult<byte[]> constBytes(
      ExprContext xx, QExprId loc, Value value, String context) {
    Opt.Some<AspectListConst> listSome;
    switch (GenAspectListConst.getMaybe(xx, value.valueType())) {
      case Opt.Some<AspectListConst> s -> listSome = s;
      case Opt.None<AspectListConst> n -> { return EResult.err(loc, context + ": expected a const list of bytes"); }
    }
    var elements = listSome.value().aspectListConstElements(value);
    var out = new byte[elements.size()];
    for (var i = 0; i < elements.size(); i++) {
      var element = elements.get(i);
      Opt.Some<AspectNumericConst> byteSome;
      switch (GenAspectNumericConst.getMaybe(xx, element.valueType())) {
        case Opt.Some<AspectNumericConst> s -> byteSome = s;
        case Opt.None<AspectNumericConst> n -> { return EResult.err(loc, context + ": list element is not a byte"); }
      }
      if (byteSome.value().aspectNumericConstKind() != NumericKind.BYTE) {
        return EResult.err(loc, context + ": list element is not a byte");
      }
      out[i] = byteSome.value().aspectNumericConstGet(element).byteValue();
    }
    return EResult.ok(out);
  }
}
