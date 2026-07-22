package compilerxyz.eval.builtin;

import compilerxyz.eval.EvalOutcome;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.CompileId;
import compilerxyz.eval.ConstEval;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.DefSetId;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.Expr;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.LogEntry;
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
    throw new UnsupportedOperationException(
        "builtin scope id 5003 (str_to_json): port of builtin_tree.rs body pending — serde_json::to_string equivalent on JVM (com.fasterxml.jackson or similar) not yet selected for the runtime");
  }

  @BuiltinPath(value = "eval.constkey_to_str", scopeId = 5004L)
  public static String constkeyToStr(ExprContext xx, ConstKey key) {
    throw new UnsupportedOperationException(
        "builtin scope id 5004 (constkey_to_str): port of builtin_tree.rs body pending — ConstKey.toStr() bridge from dynamic invocation not yet wired");
  }

  @BuiltinPath(value = "eval.codevalue", scopeId = 5005L)
  public static CodeValue codevalue(ExprContext xx, Code code, Value value) {
    return new CodeValue(code, EvalOutcome.ofValue(value));
  }

  @BuiltinPath(value = "eval.gen_binding_name", scopeId = 5006L)
  public static String genBindingName(ExprContext xx, ExprContext _xx) {
    throw new UnsupportedOperationException(
        "builtin scope id 5006 (gen_binding_name): port of builtin_tree.rs body pending — requires next_local_id allocation from xx.vx — pending Push decode parity");
  }

  @BuiltinPath(value = "eval.const_eval", scopeId = 5008L)
  public static CodeValue constEval(ExprContext xx, ExprContext xx_, StagedExprCell staged) {
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
    var res = ConstEval.constEval(targetXx, exprAst);
    if (res.hasErrors()) {
      throw new IllegalStateException(
          "builtin 5008 (const_eval): const_eval produced no value; errors: " + res.errorsOrNull());
    }
    var resCv = res.requireValue("BuiltinTreeGen builtin 5008 const_eval result");
    return resCv;
  }

  @BuiltinPath(value = "eval.new_expr_context", scopeId = 5009L)
  public static ExprContext newExprContext(
      ExprContext xx, Target target_arg, TMap<ConstKey, Binding> scope_arg) {
    throw new UnsupportedOperationException(
        "builtin scope id 5009 (new_expr_context): port of builtin_tree.rs body pending — requires RcBindingWrap + Scope construction — pending Push decode parity");
  }

  @BuiltinPath(value = "eval.stack", scopeId = 5013L)
  public static Value stack(ExprContext xx, Type type) {
    return ValueDynamic.stack(xx, type);
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
