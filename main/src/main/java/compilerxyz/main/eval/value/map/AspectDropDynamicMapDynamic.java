package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public record AspectDropDynamicMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectDropDynamic {

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        var keyDropOpt = GenAspectDropDynamic.getMaybe(xx, type.keyType);
        var valDropOpt = GenAspectDropDynamic.getMaybe(xx, type.valueType);
        if (cv.value().optIsNone() || !(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            return EResult.ok(Code.none(loc));
        }
        var entrySlot = new BindingId();
        var iteratorSlot = new BindingId();
        var entryClassName = JavaInternalName.fromClass(Map.Entry.class);
        var entryDesc = JavaDataDescriptor.fromInternalName(entryClassName);
        var objNoArgs = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of());

        var body = new JavaBytecodeBuilder(loc);
        if (keyDropOpt.optIsSome()) {
            var access = new JavaBytecodeBuilder(loc);
            var key = access.invokeInterface(entryClassName, "getKey", objNoArgs, access.load(entrySlot, entryDesc));
            var keyDropRes = keyDropOpt
                    .optUnwrap()
                    .aspectDropDynamicDrop(xx, loc, DynUtil.regToValue(access.code(), key, type.keyType));
            if (keyDropRes.hasErrors()) return keyDropRes.reerr();
            body.add(access.code());
            var r = body.addCode(loc, keyDropRes.requireValue("map key drop"));
            if (r.optIsSome()) return EResult.errs(r.optUnwrap());
        }
        if (valDropOpt.optIsSome()) {
            var access = new JavaBytecodeBuilder(loc);
            var val = access.invokeInterface(entryClassName, "getValue", objNoArgs, access.load(entrySlot, entryDesc));
            var valDropRes = valDropOpt
                    .optUnwrap()
                    .aspectDropDynamicDrop(xx, loc, DynUtil.regToValue(access.code(), val, type.valueType));
            if (valDropRes.hasErrors()) return valDropRes.reerr();
            body.add(access.code());
            var r = body.addCode(loc, valDropRes.requireValue("map value drop"));
            if (r.optIsSome()) return EResult.errs(r.optUnwrap());
        }

        var mapName = JavaInternalName.fromClass(TOrderedMap.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setup = new JavaBytecodeBuilder(loc);
        var entries = setup.invokeVirtual(mapName, "entrySet", entrySetMethod, DynUtil.sourceReg(setup, vd.source()));
        var iteratorSetup = setup.code(setup.invokeInterface(setName, "iterator", iterMethod, entries));

        var bind = new JavaBytecodeBuilder(loc);
        bind.store(entrySlot, bind.reg(DynUtil.iteratorNext(loc, iteratorSlot, type.entryDecode())));
        var loopRes = DynUtil.foreachSkeleton(loc, iteratorSetup, iteratorSlot, bind.code(), Code.target(body.code()));
        if (loopRes.hasErrors()) return loopRes.reerr();
        return EResult.ok(loopRes.requireValue("loop").code());
    }
}
