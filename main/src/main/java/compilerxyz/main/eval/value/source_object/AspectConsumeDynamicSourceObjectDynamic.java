package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.eval.value.object.ValueObjectConst;
import compilerxyz.main.gen.AspectConsumeDynamic;

public record AspectConsumeDynamicSourceObjectDynamic(TypeSourceObjectDynamic type) implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        if (type.set == null) {
            return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
        }
        xx.vx.definitionSets.putIfAbsent(type.set.defSetId, type.set);
        return JavaDataDescriptor.fromInternalName(JavaInternalName.of(type.internalName()));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsSome() && DynUtil.sourceOf(cv.value().optUnwrap()).optIsSome()) {
            return EResult.ok(cv);
        }
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeSourceObjectDynamic::toDynamic: no compile-time value");
        }
        if (!(cv.value().optUnwrap() instanceof ValueObjectConst voc)) {
            return EResult.err(
                    loc,
                    "TypeSourceObjectDynamic::toDynamic: expected ValueObjectConst, got "
                            + cv.value().optUnwrap().getClass().getName());
        }
        Object hostValue = voc.data();
        if (type.set != null) {
            xx.vx.definitionSets.putIfAbsent(type.set.defSetId, type.set);
        }
        var entry = new EvalContext.ExternalEntry(new BindingId(), type, new External(hostValue.getClass(), hostValue));
        xx.vx.externals.add(entry);
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var loaded = b.external(entry);
        return EResult.ok(DynUtil.regToValue(b.code(), loaded, type));
    }
}
