package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.Opcodes;

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

        var bodySeq = new JavaBytecodeSequence();
        if (keyDropOpt.optIsSome()) {
            var keyDropSome = keyDropOpt.optUnwrap();
            var loadKeySeq = new JavaBytecodeSequence();
            loadKeySeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot)));
            loadKeySeq.add(JavaBytecodeUtils.callInterfaceMethod(
                    loc,
                    entryClassName,
                    "getKey",
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
            var keyCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, loadKeySeq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, type.keyType)));
            var keyDropRes = keyDropSome.aspectDropDynamicDrop(xx, loc, keyCv);
            if (keyDropRes.hasErrors()) return keyDropRes.reerr();
            var keyDropDynCast = CodeDynamic.castOpt(loc, keyDropRes.requireValue("map key drop"));
            if (keyDropDynCast.hasErrors()) return keyDropDynCast.reerr();
            var keyDropDyn = keyDropDynCast.requireValue("keyDropDyn");
            if (keyDropDyn.optIsSome()) bodySeq.add(keyDropDyn.optUnwrap());
        }
        if (valDropOpt.optIsSome()) {
            var valDropSome = valDropOpt.optUnwrap();
            var loadValSeq = new JavaBytecodeSequence();
            loadValSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ALOAD, entrySlot)));
            loadValSeq.add(JavaBytecodeUtils.callInterfaceMethod(
                    loc,
                    entryClassName,
                    "getValue",
                    JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of())));
            var valCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, loadValSeq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, type.valueType)));
            var valDropRes = valDropSome.aspectDropDynamicDrop(xx, loc, valCv);
            if (valDropRes.hasErrors()) return valDropRes.reerr();
            var valDropDynCast = CodeDynamic.castOpt(loc, valDropRes.requireValue("map value drop"));
            if (valDropDynCast.hasErrors()) return valDropDynCast.reerr();
            var valDropDyn = valDropDynCast.requireValue("valDropDyn");
            if (valDropDyn.optIsSome()) bodySeq.add(valDropDyn.optUnwrap());
        }

        var mapName = JavaInternalName.fromClass(Map.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(vd.source().valueDynamicSourceRealize(loc));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var entryStoreSeq = new JavaBytecodeSequence();
        entryStoreSeq.add(DynUtil.iteratorNext(loc, iteratorSlot, type.entryDecode(loc)));
        entryStoreSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ASTORE, entrySlot)));
        var loopRes = DynUtil.foreachSkeleton(
                loc,
                iteratorSetup,
                iteratorSlot,
                new CodeDynamic(loc, entryStoreSeq),
                Code.target(new CodeDynamic(loc, bodySeq)));
        if (loopRes.hasErrors()) return loopRes.reerr();
        var loopCodeCast = CodeDynamic.castOpt(loc, loopRes.requireValue("loop").code());
        if (loopCodeCast.hasErrors()) return loopCodeCast.reerr();
        var loopCode = loopCodeCast.requireValue("loopCode");
        var finalSeq = new JavaBytecodeSequence();
        if (loopCode.optIsSome()) finalSeq.add(loopCode.optUnwrap());
        finalSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, finalSeq)));
    }
}
