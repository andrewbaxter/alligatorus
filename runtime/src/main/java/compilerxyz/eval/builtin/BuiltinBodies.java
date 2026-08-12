package compilerxyz.eval.builtin;


import com.google.gson.stream.JsonWriter;
import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.Eval;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EvalContext;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.LogEntry;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Scope;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.expr.StagedExprCell;
import compilerxyz.eval.value.ValueDynamic;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.Binding;
import compilerxyz.gen.DefinitionSetBuilder;
import compilerxyz.gen.Target;
import compilerxyz.gen.Value;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.util.Opt;
import compilerxyz.util.TMap;
import compilerxyz.util.TOrderedMap;
import java.io.IOException;
import java.io.StringWriter;

public final class BuiltinBodies {
  private BuiltinBodies() {}

  @BuiltinPath(value = "log", scopeId = 5001L)
  public static Object log(ExprContext xx, String message) {
    long timestampMs = System.currentTimeMillis();
    xx.vx.logs.add(new LogEntry(timestampMs, message));
    return null;
  }

  @BuiltinPath(value = "define", scopeId = 5002L)
  public static DefinitionSetBuilder define(ExprContext xx) {
    var ex = xx.vx;
    var compileId = new CompileId(ex.ix.importState, ex.ix.sourceId);
    var defSetId = new DefSetId(compileId, ex.ix.defSetCounter.next());
    var inner = new DefinitionSet(defSetId);
    ex.definitionSets.put(defSetId, inner);
    return new DefinitionSetBuilder(inner);
  }

  @BuiltinPath(value = "eval.str_to_json", scopeId = 5003L)
  public static String strToJson(ExprContext xx, String s) {
    try {
      var stringWriter = new StringWriter();
      var writer = new JsonWriter(stringWriter);
      writer.value(s);
      writer.close();
      return stringWriter.toString();
    } catch (IOException e) {
      throw new AssertionError("In-memory IO failed", e);
    }
  }

  @BuiltinPath(value = "eval.constkey_to_str", scopeId = 5004L)
  public static String constkeyToStr(ExprContext xx, ConstKey key) {
    return switch (key) {
      case ConstKey.Str s -> s.value();
      case ConstKey.Uint u -> Long.toUnsignedString(u.value());
    };
  }

  @BuiltinPath(value = "eval.codevalue", scopeId = 5005L)
  public static CodeValue codevalue(ExprContext xx, Code code, Value value) {
    return new CodeValue(code, EvalOutcome.ofValue(value));
  }




  @BuiltinPath(value = "eval.scope", scopeId = 5006L)
  public static Scope scope(ExprContext xx, TOrderedMap<ConstKey, Binding> entries) {
    var scope = new Scope(xx.vx);
    var named = new TMap<ConstKey, Binding>();
    for (var entry : entries.entrySet()) {
      named.put(entry.getKey(), entry.getValue());
    }
    scope.pushAll(named);
    return scope;
  }

  @BuiltinPath(value = "eval.const_eval", scopeId = 5008L)
  public static EResult<CodeValue> constEval(ExprContext xx, ExprContext xx_, StagedExprCell staged) {
    ExprContext targetXx;
    if (xx_ instanceof ExprContext raw) {
      targetXx = raw;
    } else {
      throw new AssertionError(
          "builtin 5008 (const_eval): xx is not ExprContext, got "
              + (xx_ == null ? "null" : xx_.getClass()));
    }
    Object exprValue = staged.value();
    if (!(exprValue instanceof Expr exprAst)) {
      throw new AssertionError(
          "builtin 5008 (const_eval): staged payload is not Expr, got " + exprValue.getClass());
    }
    // Errors from the user-staged code ride out through the return: the caller
    // gets an eresult and propagates it (e.g. a drop via match).
    var _constEvalResult = ConstEval.constEval(targetXx, exprAst);
    if (_constEvalResult.valueOrNull().optIsNone()) return _constEvalResult.reerr();
    var _cv = _constEvalResult.requireValue("const_eval map").cv();
    var _errs = _constEvalResult.errorsOrNull();
    if (_errs.optIsNone() || _errs.optUnwrap().isEmpty()) return EResult.ok(_cv);
    return EResult.mixed(_cv, _errs.optUnwrap());
  }

  @BuiltinPath(value = "eval.eval_context", scopeId = 5009L)
  public static EvalContext evalContext(ExprContext xx) {
    return new EvalContext(xx.vx.ix);
  }

  @BuiltinPath(value = "eval.expr_context", scopeId = 5017L)
  public static ExprContext exprContext(
      ExprContext xx, Scope scope, Target target, EvalContext vx, Type exception_type) {
    return new ExprContext(scope, target, vx, vx.ix.importState, Opt.some(exception_type));
  }

  @BuiltinPath(value = "eval.code_none", scopeId = 5018L)
  public static Code codeNone(ExprContext xx, QExprId loc) {
    return Code.none(loc);
  }

  @BuiltinPath(value = "eval.extract_constkey", scopeId = 5019L)
  public static ConstKey extractConstkey(ExprContext xx, QExprId loc, Value value) {
    var res = Eval.extractConstKey(xx, loc, value);
    return switch (res) {
      case compilerxyz.util.Result.Err<ConstKey, String> err ->
          throw new IllegalStateException("builtin 5019 (extract_constkey): " + err.error());
      case compilerxyz.util.Result.Ok<ConstKey, String> ok -> ok.value();
    };
  }

  @BuiltinPath(value = "eval.bind_result", scopeId = 5015L)
  public static AspectBind.BindResult bindResult(ExprContext xx, Code code, Binding binding) {
    return new AspectBind.BindResult(code, binding);
  }

  @BuiltinPath(value = "eval.fresh_unique", scopeId = 5016L)
  public static Unique freshUnique(ExprContext xx) {
    return compilerxyz.eval.dynamic.DynRuntime.freshUnique(xx);
  }

}
