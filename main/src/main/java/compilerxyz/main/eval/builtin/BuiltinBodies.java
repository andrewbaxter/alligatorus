package compilerxyz.main.eval.builtin;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.artifact.ExporterRoot;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ConstEval;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.DefSetId;
import compilerxyz.main.eval.DefinitionSet;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.Expr;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.LogEntry;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Scope;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.DefinitionSetBuilder;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.Target;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;

public final class BuiltinBodies {
    private BuiltinBodies() {}

    @BuiltinPath(value = "eval.codevalue_dynamic", scopeId = 5039L)
    public static EResult<CodeValue> codevalueDynamic(QExprId loc, Code code, Type type) {
        var castRes = CodeDynamic.castOpt(loc, code);
        if (castRes.hasErrors()) return castRes.reerr();
        var dyn = castRes.requireValue("codevalue_dynamic code");
        var out = dyn.optIsSome() ? JavaBytecodeUtils.lastOut(dyn.optUnwrap()) : Opt.<JavaBytecodeReg>none();
        if (out.optIsNone()) {
            return EResult.err(loc, "codevalue_dynamic: code defines no value");
        }
        return EResult.ok(DynUtil.regToValue(dyn.optUnwrap(), out.optUnwrap(), type));
    }

    @BuiltinPath(value = "eval.value_dynamic", scopeId = 5040L)
    public static ValueDynamic valueDynamic(ValueDynamicSource source, Type type) {
        return new ValueDynamic(source, type);
    }

    @BuiltinPath(value = "eval.exporter_source_object_type_instance", scopeId = 5041L)
    public static ArtifactExporter exporterSourceObjectTypeInstance() {
        return ExporterRoot.SOURCE_OBJECT_TYPE_INSTANCE;
    }

    @BuiltinPath(value = "log", scopeId = 5001L)
    @BuiltinEvalContext
    public static void log(EvalContext vx, String message) {
        vx.logs.add(new LogEntry(System.currentTimeMillis(), message));
    }

    @BuiltinPath(value = "define", scopeId = 5002L)
    @BuiltinEvalContext
    public static DefinitionSetBuilder define(EvalContext vx) {
        var compileId = new CompileId(vx.ix.importState, vx.ix.sourceId);
        var defSetId = new DefSetId(compileId, vx.ix.defSetCounter.next());
        var inner = new DefinitionSet(defSetId);
        vx.definitionSets.put(defSetId, inner);
        return new DefinitionSetBuilder(inner);
    }

    @BuiltinPath(value = "types.record", scopeId = 5042L)
    public static Type record(TOrderedMap<ConstKey, Type> fields) {
        var out = new TOrderedMap<ConstKey, RecordField>();
        for (var entry : fields.entrySet()) {
            out.put(entry.getKey(), new RecordField.Dynamic(entry.getValue()));
        }
        return new Record(IOrderedMap.from(out));
    }

    @BuiltinPath(value = "types.tuple", scopeId = 5043L)
    public static Type tuple(TList<Type> elements) {
        var out = new TList<TupleElement>(elements.size());
        for (var element : elements) {
            out.add(new TupleElement.Dynamic(element));
        }
        return new Tuple(IList.from(out));
    }

    @BuiltinPath(value = "eval.scope", scopeId = 5006L)
    public static Scope scope(EvalContext vx, TOrderedMap<ConstKey, Binding> entries) {
        var scope = new Scope(vx);
        var named = new TMap<ConstKey, Binding>();
        for (var entry : entries.entrySet()) {
            named.put(entry.getKey(), entry.getValue());
        }
        scope.pushAll(named);
        return scope;
    }

    @BuiltinPath(value = "eval.eval_context", scopeId = 5009L)
    @BuiltinEvalContext
    public static EvalContext evalContext(EvalContext vx) {
        return new EvalContext(vx.ix);
    }

    @BuiltinPath(value = "eval.fresh_unique", scopeId = 5016L)
    @BuiltinEvalContext
    public static Unique freshUnique(EvalContext vx) {
        return DynRuntime.freshUnique(vx);
    }

    @BuiltinPath(value = "eval.str_to_json", scopeId = 5003L)
    public static String strToJson(String s) {
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

    @BuiltinPath(value = "eval.str_utf8", scopeId = 5044L)
    public static byte[] strUtf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @BuiltinPath(value = "eval.constkey_to_str", scopeId = 5004L)
    public static String constkeyToStr(ConstKey key) {
        return switch (key) {
            case ConstKey.Str s -> s.value();
            case ConstKey.Uint u -> Long.toUnsignedString(u.value());
        };
    }

    @BuiltinPath(value = "eval.codevalue", scopeId = 5005L)
    public static CodeValue codevalue(Code code, Value value) {
        return new CodeValue(code, EvalOutcome.ofValue(value));
    }

    @BuiltinPath(value = "eval.const_eval", scopeId = 5008L)
    public static EResult<CodeValue> constEval(ExprContext xx, StagedExprCell staged) {
        Object exprValue = staged.value();
        if (!(exprValue instanceof Expr exprAst)) {
            throw new AssertionError(
                    "builtin 5008 (const_eval): staged payload is not Expr, got " + exprValue.getClass());
        }
        var evalResult = ConstEval.constEval(xx, exprAst);
        if (evalResult.valueOpt().optIsNone()) return evalResult.reerr();
        var cv = evalResult.requireValue("const_eval map").cv();
        var errs = evalResult.errorsOpt();
        if (errs.optIsNone() || errs.optUnwrap().isEmpty()) return EResult.ok(cv);
        return EResult.mixed(cv, errs.optUnwrap());
    }

    @BuiltinPath(value = "eval.expr_context", scopeId = 5017L)
    public static ExprContext exprContext(Scope scope, Target target, EvalContext vx, Type exceptionType) {
        return new ExprContext(scope, target, vx, vx.ix.importState, Opt.some(exceptionType));
    }

    @BuiltinPath(value = "eval.code_none", scopeId = 5018L)
    public static Code codeNone(QExprId loc) {
        return Code.none(loc);
    }

    @BuiltinPath(value = "eval.const_numeric_to_str", scopeId = 5031L)
    public static String constNumericToStr(ExprContext xx, Value value) {
        var aspOpt = GenAspectNumericConst.getMaybe(xx, value.valueType());
        if (aspOpt.optIsNone()) {
            throw new IllegalStateException("const_numeric_to_str: value type "
                    + value.valueType().typeDesc() + " has no const numeric aspect");
        }
        return aspOpt.optUnwrap().aspectNumericConstGet(value).toString();
    }

    @BuiltinPath(value = "eval.extract_constkey", scopeId = 5019L)
    public static ConstKey extractConstkey(ExprContext xx, Value value) {
        var res = Eval.extractConstKey(xx, value);
        return switch (res) {
            case Result.Err<ConstKey, String> err ->
                throw new IllegalStateException("builtin 5019 (extract_constkey): " + err.error());
            case Result.Ok<ConstKey, String> ok -> ok.value();
        };
    }

    @BuiltinPath(value = "eval.binding_setter_consumed", scopeId = 5032L)
    public static BindingSetter bindingSetterConsumed() {
        return new BindingSetterConsumed();
    }

    @BuiltinPath(value = "eval.value_object_const", scopeId = 5038L)
    public static ValueObjectConst valueObjectConst(Type type, Object data) {
        return new ValueObjectConst(type, data);
    }

    @BuiltinPath(value = "eval.bind_result", scopeId = 5015L)
    public static AspectBind.BindResult bindResult(Code code, Binding binding) {
        return new AspectBind.BindResult(code, binding);
    }

    @BuiltinPath(value = "eval.drop_value", scopeId = 5020L)
    public static EResult<Code> dropValue(ExprContext xx, QExprId loc, CodeValue cv) {
        return Eval.dropValue(xx, loc, cv);
    }

    @BuiltinPath(value = "eval.cast_value", scopeId = 5033L)
    public static EResult<CodeValue> castValue(ExprContext xx, QExprId loc, Type type, CodeValue cv) {
        return Eval.typecheckConvert(xx, loc, type, cv);
    }

    @BuiltinPath(value = "eval.codevalue_no_value", scopeId = 5034L)
    public static CodeValue codevalueNoValue(Code code) {
        return new CodeValue(code, EvalOutcome.NO_VALUE);
    }

    @BuiltinPath(value = "eval.codevalue_control_ended", scopeId = 5035L)
    public static CodeValue codevalueControlEnded(Code code) {
        return new CodeValue(code, EvalOutcome.CONTROL_ENDED);
    }

    @BuiltinPath(value = "eval.const_numeric_to_double", scopeId = 5036L)
    public static double constNumericToDouble(ExprContext xx, Value value) {
        var aspOpt = GenAspectNumericConst.getMaybe(xx, value.valueType());
        if (aspOpt.optIsNone()) {
            throw new IllegalStateException("const_numeric_to_double: value type "
                    + value.valueType().typeDesc() + " has no const numeric aspect");
        }
        return aspOpt.optUnwrap().aspectNumericConstGet(value).doubleValue();
    }

    @BuiltinPath(value = "eval.expr_loc", scopeId = 5037L)
    public static QExprId exprLoc(StagedExprCell staged) {
        Object exprValue = staged.value();
        if (!(exprValue instanceof Expr exprAst)) {
            throw new AssertionError(
                    "builtin 5037 (expr_loc): staged payload is not Expr, got " + exprValue.getClass());
        }
        return exprAst.loc();
    }
}
