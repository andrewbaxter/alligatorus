package compilerxyz.main.eval.value.source_object;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.External;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.BindingCode;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.CodeLoc;
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
    public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        var dynCast = CodeDynamic.castOpt(loc, cv.code());
        if (dynCast.hasErrors()) return dynCast.reerr();
        if (dynCast.requireValue("dyn").optIsSome())
            return EResult.ok(dynCast.requireValue("dyn").optUnwrap());
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
        var key = new BindingId();
        var id = key;
        xx.vx.externals.add(new EvalContext.ExternalEntry(key, type, new External(hostValue.getClass(), hostValue)));
        return EResult.ok(BindingCode.bindingUse(new CodeLoc.Some(loc), id));
    }
}
