package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.Erb;
import compilerxyz.main.eval.Eval;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.ValueDynamic;
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
        var selfDynRes =
                CodeDynamic.castRequired(loc, self.code(), "TypeMapEntryDynamic::field: receiver has no runtime code");
        if (selfDynRes.hasErrors()) return selfDynRes.reerr();
        var selfDyn = selfDynRes.requireValue("selfDyn");
        var seq = new JavaBytecodeSequence();
        seq.add(selfDyn);
        seq.add(JavaBytecodeUtils.callInterfaceMethod(
                loc,
                TypeMapEntryDynamic.MAP_ENTRY_INAME,
                spec.jvmGetter(),
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
        var fieldDesc = JvmSort.descriptor(xx, spec.type());
        if (fieldDesc.isReference() && !fieldDesc.value().equals(JvmConstants.DESC_OBJECT.value())) {
            seq.add(JavaBytecodeUtils.cast(loc, fieldDesc));
        }
        return EResult.ok(new CodeValue(
                Code.target(new CodeDynamic(loc, seq)), EvalOutcome.ofValue(ValueDynamic.stack(xx, spec.type()))));
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
