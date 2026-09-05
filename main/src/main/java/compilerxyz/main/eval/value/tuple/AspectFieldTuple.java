package compilerxyz.main.eval.value.tuple;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectField;
import compilerxyz.main.gen.GenAspectNumericConst;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldTuple(Tuple type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        if (key.value().optIsNone()) {
            return EResult.err(loc, "Tuple::field: key has no compile-time value");
        }
        var keyVal = key.value().optUnwrap();
        var numConst = GenAspectNumericConst.getMaybe(xx, keyVal.valueType());
        if (numConst.optIsNone()) {
            return EResult.err(loc, "Tuple::field: key must be a compile-time number");
        }
        long d = numConst.optUnwrap().aspectNumericConstGet(keyVal).longValue();
        int idx = (int) d;
        if ((long) idx != d) {
            return EResult.err(loc, "Tuple::field: index out of int range, got " + d);
        }
        return type.accessOrIndex(xx, loc, self, Opt.some(idx));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        if (type.base.optIsNone()) {
            return new TList<>();
        }
        var out = new TList<ConstKey>(type.elements.size());
        for (int i = 0; i < type.elements.size(); i++) {
            out.add(new ConstKey.Uint(i));
        }
        return out;
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        ConstKey.Uint u;
        switch (key) {
            case ConstKey.Uint uint -> u = uint;
            case ConstKey.Str s -> {
                return Opt.none();
            }
        }
        long idx = u.value();
        if (idx < 0 || idx >= type.elements.size()) return Opt.none();
        var e = type.elements.get((int) idx);
        Type t =
                switch (e) {
                    case TupleElement.Dynamic d -> d.type();
                    case TupleElement.EvalRes r -> {
                        var inner = r.result();
                        if (!inner.hasErrors()) {
                            var innerCv = inner.requireValue("Tuple::fieldTypeOpt inner");
                            if (innerCv.value().optIsSome()) {
                                yield innerCv.value().optUnwrap().valueType();
                            }
                        }
                        yield (Type) new Tuple(Opt.none(), new TList<>());
                    }
                };
        return Opt.some(t);
    }
}
