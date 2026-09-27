package compilerxyz.main.eval.value.map;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorSlot;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.list.AspectIterableListBoxedDynamic;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TOrderedMap;
import java.util.Iterator;
import java.util.Set;

public record AspectIterableMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var iteratorSlot = mustCast(BindingIteratorSlot.class, loop).iteratorSlot();
        return EResult.ok(DynUtil.regToValue(
                DynUtil.iteratorNext(loc, iteratorSlot, type.entryDecode()), aspectIterableElementType()));
    }

    @Override
    public Type aspectIterableElementType() {
        return new TypeMapEntryDynamic(type.keyType, type.valueType);
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var mapName = JavaInternalName.fromClass(TOrderedMap.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setup = new JavaBytecodeBuilder(loc);
        var entries = setup.invokeVirtual(mapName, "entrySet", entrySetMethod, setup.reg(baseRes.requireValue("base")));
        var iteratorSetup = setup.code(setup.invokeInterface(setName, "iterator", iterMethod, entries));

        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }

        var bodyCodeRes = AspectIterableListBoxedDynamic.foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var bodyCode = bodyCodeRes.requireValue("foreach body");
        var iteratorSlot = mustCast(BindingIteratorSlot.class, loop).iteratorSlot();
        return DynUtil.foreachSkeleton(loc, iteratorSetup, iteratorSlot, elementBindDyn.optUnwrap(), bodyCode);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorSlot(new BindingId());
    }
}
