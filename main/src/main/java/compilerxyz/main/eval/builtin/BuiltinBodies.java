package compilerxyz.main.eval.builtin;

import com.google.gson.stream.JsonWriter;
import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.BindingId;
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
import compilerxyz.main.eval.ScopeLevel;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.binding.BindingSetterConsumed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynPanic;
import compilerxyz.main.eval.dynamic.DynRuntime;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.expr.ExprRecordEntry;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.object.ArtifactExporterObjectConstPayload;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectBind;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.BindingSetter;
import compilerxyz.main.gen.DefinitionSetBuilder;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.gen.ListStoreByte;
import compilerxyz.main.gen.ListStoreInt;
import compilerxyz.main.gen.Target;
import compilerxyz.main.gen.Value;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.main.syntax.RemoteOrigin;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;

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

    @BuiltinPath(value = "eval.exporter_object_const", scopeId = 5041L)
    public static ArtifactExporter exporterObjectConst(Type payloadType) {
        return new ArtifactExporterObjectConstPayload(payloadType);
    }

    @BuiltinPath(value = "eval.source_object_exporter", scopeId = 5059L)
    public static ArtifactExporter sourceObjectExporter(Type type) {
        return MustCast.mustCast(TypeSourceObjectDynamic.class, type).instanceExporter();
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
        return new DefinitionSetBuilder(new DefinitionSet(defSetId));
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

    @BuiltinPath(value = "eval.scope_bindings", scopeId = 5061L)
    public static TOrderedMap<ConstKey, Binding> scopeBindings(Scope scope) {
        var out = new TOrderedMap<ConstKey, Binding>();
        for (var level : scope.levels) {
            if (level instanceof ScopeLevel.Entry entry && entry.name.optIsSome()) {
                out.put(entry.name.optUnwrap(), entry.binding);
            }
        }
        return out;
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

    @BuiltinPath(value = "eval.import_file", scopeId = 5046L)
    @BuiltinEvalContext
    public static FileHandle importFile(EvalContext vx, String specKey) {
        var ix = vx.ix;
        SourceSpecAbsolute abs;
        if (specKey.startsWith("local:")) {
            abs = new SourceSpecAbsolute.Local(specKey.substring("local:".length()));
        } else if (specKey.startsWith("remote:")) {
            var rest = specKey.substring("remote:".length());
            var subAt = rest.lastIndexOf('!');
            if (subAt < 0) throw new IllegalArgumentException("malformed remote spec key: " + specKey);
            var originKey = rest.substring(0, subAt);
            RemoteOrigin origin;
            if (originKey.startsWith("http:")) {
                var originRest = originKey.substring("http:".length());
                var hashAt = originRest.lastIndexOf('#');
                if (hashAt < 0) throw new IllegalArgumentException("malformed http origin key: " + originKey);
                var hashKey = originRest.substring(hashAt + 1);
                if (!hashKey.startsWith("sha256:")) throw new IllegalArgumentException("unknown hash key: " + hashKey);
                origin = new RemoteOrigin.Http(
                        originRest.substring(0, hashAt),
                        new HashValue.Sha256(HexFormat.of().parseHex(hashKey.substring("sha256:".length()))));
            } else if (originKey.startsWith("git:")) {
                var originRest = originKey.substring("git:".length());
                var commitAt = originRest.lastIndexOf('@');
                if (commitAt < 0) throw new IllegalArgumentException("malformed git origin key: " + originKey);
                origin = new RemoteOrigin.Git(originRest.substring(0, commitAt), originRest.substring(commitAt + 1));
            } else {
                throw new IllegalArgumentException("unknown remote origin key: " + originKey);
            }
            abs = new SourceSpecAbsolute.Remote(origin, rest.substring(subAt + 1));
        } else {
            throw new IllegalArgumentException("not a local or remote spec key: " + specKey);
        }
        Sources.SourceData data;
        switch (ix.gx.sources.sourcesGetData(abs)) {
            case Result.Ok<Sources.SourceData, String> ok -> data = ok.value();
            case Result.Err<Sources.SourceData, String> err -> throw new DynPanic("import: " + err.error());
        }
        if (data.local().optIsSome()) ix.localData.add(data.local().optUnwrap());
        return new FileHandle(data.bytes());
    }

    @BuiltinPath(value = "exec", scopeId = 5047L)
    public static ExecRun exec(
            String command,
            TList<String> args,
            TOrderedMap<String, String> env,
            TOrderedMap<String, byte[]> files,
            Opt<ListStoreInt> successCodes) {
        int[] success;
        if (successCodes.optIsSome()) {
            var store = successCodes.optUnwrap();
            success = new int[store.size()];
            for (int i = 0; i < success.length; i++) success[i] = store.get(i);
        } else {
            success = new int[] {0};
        }
        Path root;
        try {
            root = Files.createTempDirectory("allig-exec");
        } catch (IOException e) {
            throw new DynPanic("exec: cannot create sandbox directory: " + e.getMessage());
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            if (!Files.exists(root)) return;
            try (var walk = Files.walk(root)) {
                for (var p : walk.sorted(Comparator.reverseOrder()).toList()) {
                    Files.deleteIfExists(p);
                }
            } catch (IOException e) {
                throw new IllegalStateException("exec: cannot delete sandbox directory " + root, e);
            }
        }));
        var fs = root.resolve("fs");
        var stdoutFile = root.resolve("stdout");
        var stderrFile = root.resolve("stderr");
        try {
            Files.createDirectory(fs);
            for (var entry : files.entrySet()) {
                var target = ExecRun.sandboxPath(fs, entry.getKey(), "exec");
                var parent = target.getParent();
                if (parent != null) Files.createDirectories(parent);
                Files.write(target, entry.getValue());
            }
        } catch (IOException e) {
            throw new DynPanic("exec: cannot populate sandbox directory: " + e.getMessage());
        }
        var argv = new ArrayList<String>(args.size() + 1);
        argv.add(command);
        for (var arg : args) argv.add(arg);
        var pb = new ProcessBuilder(argv)
                .directory(fs.toFile())
                .redirectInput(ProcessBuilder.Redirect.from(Path.of("/dev/null").toFile()))
                .redirectOutput(stdoutFile.toFile())
                .redirectError(stderrFile.toFile());
        for (var entry : env.entrySet()) pb.environment().put(entry.getKey(), entry.getValue());
        Process process;
        try {
            process = pb.start();
        } catch (IOException e) {
            throw new DynPanic("exec: cannot start " + command + ": " + e.getMessage());
        }
        return new ExecRun(fs, stdoutFile, stderrFile, process, command, success);
    }

    @BuiltinPath(value = "app", scopeId = 5048L)
    public static App app(Opt<String> executor, byte[] bytes) {
        return new App(executor, bytes);
    }

    @BuiltinPath(value = "dir", scopeId = 5052L)
    public static Dir dir(TOrderedMap<String, byte[]> files) {
        return new Dir(files);
    }

    @BuiltinPath(value = "eval.str_utf8", scopeId = 5044L)
    public static byte[] strUtf8(String s) {
        return s.getBytes(StandardCharsets.UTF_8);
    }

    @BuiltinPath(value = "eval.str_from_utf8", scopeId = 5050L)
    public static Opt<String> strFromUtf8(ListStoreByte bytes) {
        var raw = new byte[bytes.size()];
        for (int i = 0; i < raw.length; i++) raw[i] = bytes.get(i);
        try {
            return Opt.some(StandardCharsets.UTF_8
                    .newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(raw))
                    .toString());
        } catch (CharacterCodingException e) {
            return Opt.none();
        }
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

    @BuiltinPath(value = "eval.value_empty", scopeId = 5060L)
    public static Value valueEmpty(Type type) {
        return new ValueEmpty(type);
    }

    @BuiltinPath(value = "eval.binding_dynamic", scopeId = 5045L)
    public static Binding bindingDynamic(BindingId dynamicKey, Type type) {
        return new BindingDynamicBinding(dynamicKey, type);
    }

    @BuiltinPath(value = "eval.exit_value", scopeId = 5053L)
    public static Target.ExitValue exitValue(boolean carries, CodeValue cv) {
        return new Target.ExitValue(carries, cv);
    }

    @BuiltinPath(value = "eval.aspect_union_variant", scopeId = 5054L)
    public static AspectUnion.Variant aspectUnionVariant(ConstKey key, Type type) {
        return new AspectUnion.Variant(key, type);
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

    private static Expr exprOf(long scopeId, StagedExprCell staged) {
        Object exprValue = staged.value();
        if (!(exprValue instanceof Expr exprAst)) {
            throw new AssertionError(
                    "builtin " + scopeId + ": staged payload is not Expr, got " + exprValue.getClass());
        }
        return exprAst;
    }

    private static TList<Expr> exprsOf(long scopeId, TList<StagedExprCell> staged) {
        var out = new TList<Expr>(staged.size());
        for (var cell : staged) out.add(exprOf(scopeId, cell));
        return out;
    }

    @BuiltinPath(value = "eval.expr_record_entry", scopeId = 5055L)
    public static ExprRecordEntry exprRecordEntry(StagedExprCell key, StagedExprCell value) {
        return new ExprRecordEntry(key, value);
    }

    @BuiltinPath(value = "eval.expr_record", scopeId = 5056L)
    public static StagedExprCell exprRecord(QExprId loc, TList<ExprRecordEntry> entries) {
        var out = new TList<Expr.RecordEntry>(entries.size());
        for (var entry : entries) {
            out.add(new Expr.RecordEntry(exprOf(5056L, entry.key()), exprOf(5056L, entry.value())));
        }
        return new StagedExprCell(new Expr(loc, new Expr.Record(out)));
    }

    @BuiltinPath(value = "eval.expr_tuple", scopeId = 5057L)
    public static StagedExprCell exprTuple(QExprId loc, TList<StagedExprCell> elements) {
        return new StagedExprCell(new Expr(loc, new Expr.Tuple(exprsOf(5057L, elements))));
    }

    @BuiltinPath(value = "eval.expr_seq", scopeId = 5058L)
    public static StagedExprCell exprSeq(QExprId loc, TList<StagedExprCell> exprs) {
        return new StagedExprCell(new Expr(loc, new Expr.Seq(exprsOf(5058L, exprs))));
    }
}
