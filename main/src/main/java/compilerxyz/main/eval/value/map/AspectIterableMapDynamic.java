package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorStack;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.prelude.util.TList;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public record AspectIterableMapDynamic<K extends Type, V extends Type>(TypeMapDynamic<K, V> type)
        implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        return EResult.ok(DynUtil.dynamicStackToValue(
                xx, DynUtil.iteratorNext(loc, type.entryDecode(loc)), aspectIterableElementType()));
    }

    @Override
    public Type aspectIterableElementType() {
        return new TypeMapEntryDynamic(type.keyType, type.valueType);
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type);
        if (baseConsumeRes.hasErrors()) return baseConsumeRes.reerr();
        var baseDynRes =
                baseConsumeRes.requireValue("AspectConsumeDynamic.get").aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDynRes.hasErrors()) return baseDynRes.reerr();
        var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");

        var mapName = JavaInternalName.fromClass(Map.class);
        var setName = JavaInternalName.fromClass(Set.class);
        var iteratorName = JavaInternalName.fromClass(Iterator.class);
        var entrySetMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(setName), TList.of());
        var iterMethod = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromJVMName(iteratorName), TList.of());
        var setupSeq = new JavaBytecodeSequence();
        setupSeq.add(baseDyn);
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, mapName, "entrySet", entrySetMethod));
        setupSeq.add(JavaBytecodeUtils.callInterfaceMethod(loc, setName, "iterator", iterMethod));
        var iteratorSetup = new CodeDynamic(loc, setupSeq);

        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        if (elementBindDyn.optIsNone()) {
            return EResult.err(loc, "foreach: element bind produced no code");
        }

        Code bodyCode;
        if (body.value().optIsSome()) {
            var dropRes = xx.target.targetDrop(xx, loc, body);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var dropCode = dropRes.requireValue("TypeMapDynamic::foreach body drop");
            var bodyDynCast = CodeDynamic.castOpt(loc, body.code());
            if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
            var bodyDyn = bodyDynCast.requireValue("bodyDyn");
            var dropDynCast = CodeDynamic.castOpt(loc, dropCode);
            if (dropDynCast.hasErrors()) return dropDynCast.reerr();
            var dropDyn = dropDynCast.requireValue("dropDyn");
            if (bodyDyn.optIsNone() && dropDyn.optIsNone()) {
                bodyCode = Code.none(loc);
            } else {
                var combinedSeq = new JavaBytecodeSequence();
                if (bodyDyn.optIsSome()) combinedSeq.add(bodyDyn.optUnwrap());
                if (dropDyn.optIsSome()) combinedSeq.add(dropDyn.optUnwrap());
                bodyCode = Code.target(new CodeDynamic(loc, combinedSeq));
            }
        } else {
            bodyCode = body.code();
        }
        return DynUtil.foreachSkeleton(loc, iteratorSetup, elementBindDyn.optUnwrap(), bodyCode);
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorStack();
    }
}
