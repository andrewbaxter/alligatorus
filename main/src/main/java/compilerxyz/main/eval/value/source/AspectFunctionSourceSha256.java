package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.NumericKind;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.AspectListConst;
import compilerxyz.main.gen.AspectNumericConst;
import compilerxyz.main.gen.GenAspectListConst;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.prelude.util.Opt;

public final class AspectFunctionSourceSha256 implements AspectFunction {
    public static final AspectFunctionSourceSha256 INSTANCE = new AspectFunctionSourceSha256();

    private AspectFunctionSourceSha256() {}

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.sha256");
        if (argRes.hasErrors()) return argRes.reerr();
        var val = argRes.requireValue("source.sha256 arg");
        Opt.Some<AspectListConst> listSome;
        switch (GenAspectListConst.getMaybe(xx, val.valueType())) {
            case Opt.Some<AspectListConst> s -> listSome = s;
            case Opt.None<AspectListConst> n -> {
                return EResult.err(loc, "source.sha256: expected a const list of bytes");
            }
        }
        var elements = listSome.value().aspectListConstElements(val);
        var bytes = new byte[elements.size()];
        for (var i = 0; i < elements.size(); i++) {
            var elem = elements.get(i);
            Opt.Some<AspectNumericConst> byteSome;
            switch (GenAspectNumericConst.getMaybe(xx, elem.valueType())) {
                case Opt.Some<AspectNumericConst> s -> byteSome = s;
                case Opt.None<AspectNumericConst> n -> {
                    return EResult.err(loc, "source.sha256: list element is not a byte");
                }
            }
            if (byteSome.value().aspectNumericConstKind() != NumericKind.BYTE) {
                return EResult.err(loc, "source.sha256: list element is not a byte");
            }
            bytes[i] = byteSome.value().aspectNumericConstGet(elem).byteValue();
        }
        if (bytes.length != 32) {
            return EResult.err(loc, "source.sha256: expected 32 bytes, got " + bytes.length);
        }
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(new ValueObjectConst(new TypeHash(), new HashValue.Sha256(bytes)))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
