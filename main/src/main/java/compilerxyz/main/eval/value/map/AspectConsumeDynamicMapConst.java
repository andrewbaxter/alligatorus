package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmConstants;
import compilerxyz.main.gen.AspectConsumeDynamic;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;

public record AspectConsumeDynamicMapConst<K extends Type, V extends Type>(TypeMapConst<K, V> type)
        implements AspectConsumeDynamic {

    @Override
    public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
        return JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TOrderedMap.class));
    }

    @Override
    public EResult<CodeValue> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, "TypeMapConst::toDynamic: CodeValue has no compile-time value");
        }
        var mapName = JavaInternalName.fromClass(TOrderedMap.class);
        var putDesc = JavaMethodDescriptor.fromParts(
                JavaDataDescriptor.DESC_VOID, TList.of(JvmConstants.DESC_OBJECT, JvmConstants.DESC_OBJECT));
        var b = new JavaBytecodeBuilder(loc);
        var carryRes = b.addCode(loc, cv.code());
        if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
        var map = b.newInstance(mapName, JavaMethodDescriptor.fromConstructorParts(TList.of()));
        for (var entry : TypeMapConst.entriesOf(cv.value().optUnwrap())) {
            var keyRes = DynUtil.materialize(
                    xx, loc, type.keyType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(entry.key())));
            if (keyRes.hasErrors()) return keyRes.reerr();
            var valueRes = DynUtil.materialize(
                    xx, loc, type.valueType, new CodeValue(Code.none(loc), EvalOutcome.ofValue(entry.value())));
            if (valueRes.hasErrors()) return valueRes.reerr();
            var key = b.reg(keyRes.requireValue("TypeMapConst::toDynamic key"));
            var value = b.reg(valueRes.requireValue("TypeMapConst::toDynamic value"));
            b.invokeVirtualVoid(mapName, "put", putDesc, map, key, value);
        }
        return EResult.ok(DynUtil.regToValue(b.code(), map, new TypeMapDynamic<>(type.keyType, type.valueType)));
    }
}
