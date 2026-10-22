package compilerxyz.main.eval;

import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectStringConst;
import compilerxyz.main.gen.Value;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public final class Eval {

    public static final String KEY_BUILTINS = "builtins";

    private Eval() {}

    public static Result<ConstKey, String> extractConstKey(ExprContext xx, Value v) {
        var typeRes = v.valueType();
        var stringConst = GenAspectStringConst.getMaybe(xx, typeRes);
        if (stringConst.optIsSome()) {
            return Result.ok(new ConstKey.Str(stringConst.optUnwrap().aspectStringConstGet(v)));
        }
        return Result.err("TODO value type [X] must expose const key aspect [LIST] but doesn't");
    }

    public static EResult<Code> codeConcat(ExprContext xx, QExprId loc, Code first, Code second) {
        if (first.codeIsNone()) return EResult.ok(second);
        if (second.codeIsNone()) return EResult.ok(first);
        return xx.target.targetSeq(loc, TList.of(first, second));
    }

    public static EResult<Code> dropValue(ExprContext xx, QExprId loc, CodeValue cv) {
        return xx.target.targetDrop(dropContext(xx), loc, cv);
    }

    public static ExprContext dropContext(ExprContext xx) {
        var scope = new Scope(xx.vx);
        scope.pushAll(bodyBuiltinScope(xx));
        return new ExprContext(scope, xx.target, xx.vx, xx.importState, Opt.none(), xx.site);
    }

    public static EResult<CodeValue> typecheckConvert(ExprContext xx, QExprId loc, Type destType, CodeValue source) {
        if (source.value().optIsNone()) {
            return EResult.ok(source);
        }
        var erb = new Erb(loc);
        var sourceType = source.value().optUnwrap().valueType();
        CodeValue dest;
        if (sourceType.typeCanCastTo(xx, loc, destType)) {
            var castOpt = erb.mustEr(sourceType.typeCastTo(xx, loc, source, destType));
            if (castOpt.optIsNone()) {
                return EResult.errs(erb.errors());
            }
            dest = castOpt.optUnwrap();
        } else if (destType.typeCanCastFrom(xx, loc, sourceType)) {
            var castOpt = erb.mustEr(destType.typeCastFrom(xx, loc, source));
            if (castOpt.optIsNone()) {
                return EResult.errs(erb.errors());
            }
            dest = castOpt.optUnwrap();
        } else {
            return EResult.err(
                    loc,
                    "Incompatible types: source "
                            + sourceType.typeDesc()
                            + " cannot convert to dest "
                            + destType.typeDesc());
        }
        return EResult.ok(dest);
    }

    public static TOrderedMap<ConstKey, Binding> bodyBuiltinScope(ExprContext xx) {
        if (xx.vx.ix.builtinScopeRootBinding.optIsNone()) {
            throw new IllegalStateException("bodyBuiltinScope called before initBuiltinScope");
        }
        var rootBinding = xx.vx.ix.builtinScopeRootBinding.optUnwrap();
        var scopeEntries = new TOrderedMap<ConstKey, Binding>();
        scopeEntries.put(new ConstKey.Str(KEY_BUILTINS), rootBinding);
        return scopeEntries;
    }
}
