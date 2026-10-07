package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.eval.value.tuple.TupleElement;
import compilerxyz.main.gen.AspectFunction;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.RemoteOrigin;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFunctionSourceRemoteGit(TypeSourceRemoteGit type) implements AspectFunction {

    @Override
    public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
        var argRes = SourceSpecs.argValue(loc, argument, "source.remote.git");
        if (argRes.hasErrors()) return argRes.reerr();
        var val = argRes.requireValue("source.remote.git arg");
        if (!(val instanceof Tuple tuple)) {
            return EResult.err(loc, "source.remote.git: expected a const tuple");
        }
        var elements = new TList<Value>(tuple.elements().size());
        for (var elem : tuple.elements()) {
            TupleElement.EvalRes er;
            switch (elem) {
                case TupleElement.EvalRes e -> er = e;
                case TupleElement.Dynamic d -> {
                    return EResult.err(loc, "source.remote.git: tuple element is not const");
                }
            }
            var res = er.result();
            if (res.hasErrors()) return res.reerr();
            var cv = res.requireValue("source.remote.git tuple element");
            Opt.Some<Value> some;
            switch (cv.value()) {
                case Opt.Some<Value> s -> some = s;
                case Opt.None<Value> n -> {
                    return EResult.err(loc, "source.remote.git: tuple element has no value");
                }
            }
            elements.add(some.value());
        }
        if (elements.size() != 2) {
            return EResult.err(loc, "source.remote.git: expected (url, commit), got " + elements.size() + " arguments");
        }
        var urlRes = SourceSpecs.constString(xx, loc, elements.get(0), "source.remote.git url");
        if (urlRes.hasErrors()) return urlRes.reerr();
        var commitRes = SourceSpecs.constString(xx, loc, elements.get(1), "source.remote.git commit");
        if (commitRes.hasErrors()) return commitRes.reerr();
        var commit = commitRes.requireValue("source.remote.git commit");
        if (!RemoteOrigin.Git.COMMIT.matcher(commit).matches()) {
            return EResult.err(loc, "source.remote.git: commit must be a full 40-digit hex object id, got " + commit);
        }
        var remote = new RemoteOrigin.Git(urlRes.requireValue("source.remote.git url"), commit);
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
