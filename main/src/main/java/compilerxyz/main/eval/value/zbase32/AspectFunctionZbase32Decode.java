package compilerxyz.main.eval.value.zbase32;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.builtin.Zbase32;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.value.byte_.TypeByteConst;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.eval.value.list.TypeListConst;
import compilerxyz.main.eval.value.list.ValueListConst;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.GenAspectStringDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenLists;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public final class AspectFunctionZbase32Decode implements AspectFunction {
    public static final AspectFunctionZbase32Decode INSTANCE = new AspectFunctionZbase32Decode();

    private AspectFunctionZbase32Decode() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        if (argument.value().optIsNone()) {
            return EResult.err(loc, "zbase32.decode: argument must have a value");
        }
        var argValue = argument.value().optUnwrap();
        var argType = argValue.valueType();
        var strConstOpt = GenAspectStringConst.getMaybe(xx, argType);
        if (strConstOpt.optIsSome()) {
            var s = strConstOpt.optUnwrap().aspectStringConstGet(argValue);
            byte[] bytes;
            try {
                bytes = Zbase32.decode(s);
            } catch (IllegalArgumentException e) {
                return EResult.err(loc, "zbase32.decode: " + e.getMessage());
            }
            var elements = new TList<Value>(bytes.length);
            for (var b : bytes) {
                elements.add(new ValueObjectConst(new TypeByteConst(), b));
            }
            var listType = new TypeListConst<>(new TypeByteDynamic());
            var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
            if (concatRes.hasErrors()) return concatRes.reerr();
            return EResult.ok(new CodeValue(
                    concatRes.requireValue("concat"), EvalOutcome.ofValue(new ValueListConst<>(listType, elements))));
        }
        if (GenAspectStringDynamic.getMaybe(xx, argType).optIsSome()) {
            var argRes = DynUtil.materialize(xx, loc, argument);
            if (argRes.hasErrors()) return argRes.reerr();
            var b = new JavaBytecodeBuilder(loc);
            var out = b.invokeStatic(
                    GenCodegenDescriptors.ZBASE32_DECODE_TO_STORE, b.reg(argRes.requireValue("zbase32.decode arg")));
            var listType = GenLists.listDynamic(xx, new TypeByteDynamic());
            return EResult.ok(DynUtil.regToValue(b.code(), out, listType));
        }
        return EResult.err(loc, "zbase32.decode: argument must be a string, got " + argType.typeDesc());
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
