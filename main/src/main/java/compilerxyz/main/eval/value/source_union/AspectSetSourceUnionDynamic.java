package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.eval.value.ValueDynamicSource;
import compilerxyz.main.eval.value.binding.BindingDynamicBinding;
import compilerxyz.main.gen.AspectSet;
import compilerxyz.prelude.util.MustCast;

public record AspectSetSourceUnionDynamic(TypeSourceUnionDynamic type) implements AspectSet {

    @Override
    public EResult<CodeValue> aspectSetSet(ExprContext xx, QExprId loc, CodeValue base, CodeValue value) {
        if (value.value().optIsNone()) {
            return EResult.err(loc, "set: assigned union value has no compile-time value");
        }
        var newOpt =
                TypeSourceUnionDynamic.unionOf(xx, value.value().optUnwrap().valueType());
        var out = DynUtil.dynamicSet(xx, loc, base, value);
        if (out.hasErrors()) return out;
        if (newOpt.optIsNone() || !newOpt.optUnwrap().identity.equals(type.identity)) {
            return EResult.err(loc, "set: assigned value is not a value of this union");
        }
        var selfDyn = MustCast.mustCast(ValueDynamic.class, base.value().optUnwrap());
        return switch (selfDyn.source()) {
            case ValueDynamicSource.Slot slot -> {
                var replacement =
                        new BindingDynamicBinding(slot.slot(), type.withActiveVariants(newOpt.optUnwrap().active));
                if (!slot.setter().bindingSetterSet(xx, loc, replacement, false)) {
                    yield EResult.err(loc, "set: union binding is no longer in scope");
                }
                yield out;
            }
            case ValueDynamicSource.Stack e -> out;
            case ValueDynamicSource.Field f -> out;
        };
    }
}
