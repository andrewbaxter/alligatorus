package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.HashValue;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionSourceRemote(TypeSourceRemote type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.remote");
        if (argRes.hasErrors()) return argRes.reerr();
        var val = argRes.requireValue("source.remote arg");
        if (!(val instanceof Tuple tuple)) {
            return EResult.err(loc, "source.remote: expected a const tuple");
        }
        var elements = new TList<Value>(tuple.elements().size());
        for (var elem : tuple.elements()) {
            TupleElement.EvalRes er;
            switch (elem) {
                case TupleElement.EvalRes e -> er = e;
                case TupleElement.Dynamic d -> {
                    return EResult.err(loc, "source.remote: tuple element is not const");
                }
            }
            var res = er.result();
            if (res.hasErrors()) return res.reerr();
            var cv = res.requireValue("source.remote tuple element");
            Opt.Some<Value> some;
            switch (cv.value()) {
                case Opt.Some<Value> s -> some = s;
                case Opt.None<Value> n -> {
                    return EResult.err(loc, "source.remote: tuple element has no value");
                }
            }
            elements.add(some.value());
        }
        if (elements.size() != 2) {
            return EResult.err(loc, "source.remote: expected (url, hash), got " + elements.size() + " arguments");
        }
        var urlRes = SourceSpecs.constString(xx, loc, elements.get(0), "source.remote url");
        if (urlRes.hasErrors()) return urlRes.reerr();
        var hashRes = (elements.get(1) instanceof ValueObjectConst voc
                        && voc.type() instanceof TypeHash
                        && voc.data() instanceof HashValue hv)
                ? EResult.ok(hv)
                : EResult.<HashValue>err(loc, "source.remote hash: expected a hash value");
        if (hashRes.hasErrors()) return hashRes.reerr();
        var remote = new SourceSpecValue.SourceSpecRemote(
                urlRes.requireValue("source.remote url"), hashRes.requireValue("source.remote hash"));
        var concatRes = Eval.codeConcat(xx, loc, self.code(), argument.code());
        if (concatRes.hasErrors()) return concatRes.reerr();
        return EResult.ok(new CodeValue(
                concatRes.requireValue("concat"),
                EvalOutcome.ofValue(SourceSpecs.spec(new SourceSpecValue.Remote(remote)))));
    }

    @Override
    public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
        return Opt.none();
    }
}
