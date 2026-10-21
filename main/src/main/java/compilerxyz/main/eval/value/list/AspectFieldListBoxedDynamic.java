package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic.FieldSpec;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldListBoxedDynamic<T extends Type>(TypeListBoxedDynamic<T> type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeListBoxedDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var specOpt = type.fieldSpecs(xx).getOpt(ck);
        if (specOpt.optIsNone()) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on list");
        }
        var spec = specOpt.optUnwrap();
        return switch (spec) {
            case FieldSpec.Method m -> EResult.ok(DynUtil.methodRefOn(self, m.type()));
            case FieldSpec.Length l -> {
                var selfRes = DynUtil.valueReg(xx, loc, self);
                if (selfRes.hasErrors()) yield selfRes.reerr();
                var b = new JavaBytecodeBuilder(loc);
                var sizeDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());
                var out = b.invokeVirtual(
                        JavaInternalName.fromClass(TList.class),
                        "size",
                        sizeDesc,
                        b.reg(selfRes.requireValue("selfDyn")));
                yield EResult.ok(DynUtil.regToValue(b.code(), out, l.fieldSpecFieldType()));
            }
        };
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(type.fieldSpecs(xx).keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        return type.fieldSpecs(xx).getOpt(key).optMap(spec -> spec.fieldSpecFieldType());
    }
}
