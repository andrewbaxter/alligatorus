package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportRun;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldBundle(TypeBundle type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        Opt.Some<Value> keySome;
        switch (key.value()) {
            case Opt.Some<Value> s -> keySome = s;
            case Opt.None<Value> n -> {
                return EResult.err(loc, "bundle: field key has no compile-time value");
            }
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, keySome.value()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        ConstKey.Str s;
        switch (ckOpt.optUnwrap()) {
            case ConstKey.Str str -> s = str;
            case ConstKey.Uint u -> {
                return EResult.err(loc, "bundle: field key must be a string");
            }
        }
        var subSpec =
                new SourceSpecAbsolute.Remote(type.base().baseUrl(), type.base().hash(), s.value() + ".at");
        var res = ImportRun.compileAbsolute(xx, loc, subSpec);
        if (res.hasErrors()) return res.reerr();
        var outcome = res.requireValue("importAbsolute");
        return switch (outcome) {
            case ImportRun.SpecOutcome.BundleOut b ->
                EResult.ok(new CodeValue(
                        Code.none(loc),
                        EvalOutcome.ofValue(
                                new ValueObjectConst(ValueObjectConst.plainTypeType(), new TypeBundle(b.base())))));
            case ImportRun.SpecOutcome.ModuleOut m -> {
                switch (m.abs()) {
                    case SourceSpecAbsolute.Local ignored -> xx.vx.ix.localDependencies.add(m.compileId());
                    case SourceSpecAbsolute.Virtual v -> {}
                    case SourceSpecAbsolute.Remote r -> {}
                }
                yield ImportRun.materialize(xx, loc, m.res());
            }
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return new TList<>();
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return Opt.none();
    }
}
