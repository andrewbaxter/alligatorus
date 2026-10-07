package compilerxyz.main.eval.value.source;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ImportRun;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.Value;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import java.nio.file.Path;

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
        EResult<ImportRun.SpecOutcome> res = EResult.err(loc, "bundle: no entry " + s.value());
        for (var name : new String[] {s.value(), s.value() + ".at", s.value() + ".atz"}) {
            res = ImportRun.compileAbsolute(xx, loc, entrySpec(type.base(), name));
            if (!res.hasErrors()) break;
        }
        if (res.hasErrors()) return res.reerr();
        return ImportRun.outcomeValue(xx, loc, res.requireValue("bundle entry"));
    }

    private static SourceSpecAbsolute entrySpec(SourceSpecAbsolute base, String name) {
        return switch (base) {
            case SourceSpecAbsolute.Local l ->
                new SourceSpecAbsolute.Local(Path.of(l.path(), name).toString());
            case SourceSpecAbsolute.Remote r ->
                new SourceSpecAbsolute.Remote(r.origin(), r.subpath().isEmpty() ? name : r.subpath() + "/" + name);
            case SourceSpecAbsolute.Virtual v -> throw new IllegalStateException("virtual bundle");
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
