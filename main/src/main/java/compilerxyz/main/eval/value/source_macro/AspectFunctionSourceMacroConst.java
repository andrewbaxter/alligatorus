package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalRoot;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.prelude.util.MustCast;
import compilerxyz.prelude.util.Opt;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public record AspectFunctionSourceMacroConst(TypeSourceMacroConst type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var function = type.data().function;
        var set = function.set;
        var name = function.name.optUnwrapOr("(unnamed)");
        if (!set.finished) {
            return EResult.err(
                    loc,
                    "Const function '" + name
                            + "' cannot be called: its definition set has not been finished (d.finish()); from"
                            + " inside the same set, call its func_impl with (xx, loc, argument) instead");
        }
        var loaderRes = set.install(xx, loc);
        if (loaderRes.hasErrors()) return loaderRes.reerr();
        var loader = loaderRes.requireValue("TypeSourceMacroConst::call install");
        var binaryName = function.compiledClassName.optUnwrap();
        Method method;
        try {
            method = Class.forName(binaryName, true, loader)
                    .getDeclaredMethod(function.jvmMethodName, ExprContext.class, QExprId.class, CodeValue.class);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Const function '" + name + "' not found in " + binaryName, e);
        }
        Object result;
        try {
            result = method.invoke(null, xx, loc, argument);
        } catch (InvocationTargetException e) {
            return EvalRoot.thrownErrors(loader, loc, e, "Const function '" + name + "'");
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Const function '" + name + "' invoke failed", e);
        }
        EResult<?> host = MustCast.mustCast(EResult.class, result);
        if (host.hasErrors()) return host.reerr();
        return EResult.ok(MustCast.mustCast(CodeValue.class, host.requireValue("TypeSourceMacroConst::call result")));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return type.data().exceptionType;
    }
}
