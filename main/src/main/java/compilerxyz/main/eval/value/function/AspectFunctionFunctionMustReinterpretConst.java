package compilerxyz.main.eval.value.function;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.Opt;

public record AspectFunctionFunctionMustReinterpretConst(TypeFunctionMustReinterpretConst type)
        implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var elementsRes = ReinterpretArgs.elements(xx, loc, "must_reinterpret", "(value, type)", argument);
        if (elementsRes.hasErrors()) return elementsRes.reerr();
        var elements = elementsRes.requireValue("must_reinterpret elements");
        var argsRes = ReinterpretArgs.parse(xx, loc, "must_reinterpret", elements.get(0), elements.get(1));
        if (argsRes.hasErrors()) return argsRes.reerr();
        var args = argsRes.requireValue("must_reinterpret args");
        var b = new JavaBytecodeBuilder(loc);
        var argumentAdd = b.addCode(loc, argument.code());
        if (argumentAdd.optIsSome()) return EResult.errs(argumentAdd.optUnwrap());
        var out = b.checkcast(args.targetJvmClass(), b.reg(args.value()));
        return EResult.ok(DynUtil.regToValue(b.code(), out, args.targetType()));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
