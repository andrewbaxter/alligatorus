package compilerxyz.eval.value.source;

import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectStringConst;
import compilerxyz.gen.GenAspectStringConst;
import compilerxyz.gen.Value;
import compilerxyz.util.Opt;

public final class SourceSpecs {
  private SourceSpecs() {}

  public static Value spec(SourceSpecValue value) {
    return new ValueObjectConst(new TypeSourceSpec(), value);
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

}
