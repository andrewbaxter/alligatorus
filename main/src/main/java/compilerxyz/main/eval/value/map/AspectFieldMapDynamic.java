package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.map.TypeMapDynamic.FieldSpec;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeMapDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var specOpt = type.fieldSpecs().getOpt(ck);
        if (specOpt.optIsSome()) {
            var spec = specOpt.optUnwrap();
            return switch (spec) {
                case FieldSpec.Factory f ->
                    EResult.ok(new CodeValue(
                            self.code(),
                            EvalOutcome.ofValue(new ValueObjectConst(ValueObjectConst.plainTypeType(), f.type()))));
                case FieldSpec.GetFn g -> {
                    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic fnSelfVdg)) {
                        yield EResult.err(loc, "TypeMapDynamic::field: fn receiver has no dynamic source");
                    }
                    yield EResult.ok(DynUtil.methodRefOn(self, g.type()));
                }
                case FieldSpec.HasFn h -> {
                    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic fnSelfVdh)) {
                        yield EResult.err(loc, "TypeMapDynamic::field: fn receiver has no dynamic source");
                    }
                    yield EResult.ok(DynUtil.methodRefOn(self, h.type()));
                }
                case FieldSpec.PutFn p -> {
                    if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic fnSelfVdp)) {
                        yield EResult.err(loc, "TypeMapDynamic::field: fn receiver has no dynamic source");
                    }
                    yield EResult.ok(DynUtil.methodRefOn(self, p.type()));
                }
            };
        }
        switch (ck) {
            case ConstKey.Uint u -> {
                return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on map");
            }
            case ConstKey.Str s -> {}
        }
        if (self.value().optIsNone() || !(self.value().optUnwrap() instanceof ValueDynamic selfVd)) {
            return EResult.err(loc, "TypeMapDynamic::field: receiver is not a ValueDynamic");
        }
        return EResult.err(
                loc,
                "TypeMapDynamic::field: map key access via source-tracked value no longer supported;"
                        + " use map.get(key) explicitly");
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(type.fieldSpecs().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var specOpt = type.fieldSpecs().getOpt(key);
        if (specOpt.optIsSome()) {
            var spec = specOpt.optUnwrap();
            return Opt.some(spec.fieldSpecFieldType());
        }
        return switch (key) {
            case ConstKey.Str s -> Opt.some(type.valueType);
            case ConstKey.Uint u -> Opt.none();
        };
    }
}
