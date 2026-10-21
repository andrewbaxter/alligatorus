package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.expr.StagedExprCell;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.eresult.TypeEResultDynamic;
import compilerxyz.main.eval.value.record.Record;
import compilerxyz.main.eval.value.record.RecordField;
import compilerxyz.main.eval.value.source_function.TypeSourceFunctionConst;
import compilerxyz.main.eval.value.source_macro.TypeSourceMacroConst;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.util.IOrderedMap;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TOrderedMap;

@BuiltinFull(uniqueId = 6345L)
public final class SourceMacroResult {
    private final TypeSourceMacroConst macro;

    public final Value func;
    public final Value funcImpl;

    public SourceMacroResult(TypeSourceMacroConst func) {
        this.macro = func;
        this.func = new ValueEmpty(func);
        this.funcImpl = new ValueEmpty(new TypeSourceFunctionConst(func.data().function));
    }

    public void impl(StagedExprCell implBody, Opt<Type> exceptionType, Opt<String> description) {
        var data = this.macro.data();
        var function = data.function;
        if (function.argType.optIsSome()) {
            throw new IllegalStateException("Const function already has an implementation");
        }
        var arg = new TOrderedMap<ConstKey, RecordField>();
        arg.put(new ConstKey.Str("xx"), new RecordField.Dynamic(GenExprContext.dynamicType()));
        arg.put(new ConstKey.Str("loc"), new RecordField.Dynamic(GenQExprId.dynamicType()));
        arg.put(new ConstKey.Str("argument"), new RecordField.Dynamic(GenCodeValue.dynamicType()));
        function.argType = Opt.some(new Record(IOrderedMap.from(arg)));
        function.retType = Opt.some(new TypeEResultDynamic<>(GenCodeValue.dynamicType()));
        function.implExpr = Opt.some((Value) implBody.value());
        data.exceptionType = exceptionType;
        data.description = description;
    }
}
