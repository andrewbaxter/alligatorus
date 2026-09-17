package compilerxyz.main.eval.value.map;

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
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.gen.AspectField;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectFieldMapEntryDynamic(TypeMapEntryDynamic type) implements AspectField {

    @Override
    public EResult<CodeValue> aspectFieldField(ExprContext xx, QExprId loc, CodeValue self, CodeValue key) {
        var erb = new Erb(loc);
        if (key.value().optIsNone()) {
            return EResult.err(loc, "TypeMapEntryDynamic::field: key has no compile-time value");
        }
        var ckOpt = erb.mustRes(Eval.extractConstKey(xx, key.value().optUnwrap()));
        if (ckOpt.optIsNone()) return EResult.errs(erb.errors());
        var ck = ckOpt.optUnwrap();
        var spec = type.fieldSpecs().getOrNull(ck);
        if (spec == null) {
            return EResult.err(loc, "Field " + ck.constKeyToCanonical() + " not found on map entry");
        }
        var selfRes = DynUtil.valueReg(loc, self);
        if (selfRes.hasErrors()) return selfRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var out = b.invokeInterface(
                TypeMapEntryDynamic.MAP_ENTRY_INAME,
                spec.jvmGetter(),
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of()),
                b.reg(selfRes.requireValue("selfDyn")));
        var fieldDesc = JvmSort.descriptor(xx, spec.type());
        if (fieldDesc.isReference() && !fieldDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            out = b.checkcast(fieldDesc, out);
        }
        return EResult.ok(DynUtil.regToValue(b.code(), out, spec.type()));
    }

    @Override
    public TList<ConstKey> aspectFieldKeys(ExprContext xx) {
        return TList.from(type.fieldSpecs().keySet());
    }

    @Override
    public Opt<Type> aspectFieldTypeOpt(ExprContext xx, ConstKey key) {
        var spec = type.fieldSpecs().getOrNull(key);
        return spec == null ? Opt.none() : Opt.some(spec.type());
    }
}
