package compilerxyz.main.eval.value.list;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorIndexed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectIterableListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var slots = mustCast(BindingIteratorIndexed.class, loop);
        var getDesc = JavaMethodDescriptor.fromParts(type.elementDescriptor(xx), TList.of(JavaDataDescriptor.DESC_INT));
        var b = new JavaBytecodeBuilder(loc);
        var list = b.load(slots.listSlot(), type.storeDescriptor());
        var index = b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT);
        var out = b.invokeVirtual(type.storeName, "get", getDesc, list, index);
        return EResult.ok(DynUtil.regToValue(b.code(), out, type.elementType));
    }

    @Override
    public Type aspectIterableElementType() {
        return type.elementType;
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var bodyCodeRes = AspectIterableListBoxedDynamic.foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var bodyCode = bodyCodeRes.requireValue("foreach body");
        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        var sizeDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());
        var slots = mustCast(BindingIteratorIndexed.class, loop);
        var listSlot = slots.listSlot();
        var indexSlot = slots.indexSlot();
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        var b = new JavaBytecodeBuilder(loc);
        b.store(listSlot, b.reg(baseRes.requireValue("base")));
        b.store(indexSlot, b.constInt(0));
        b.land(loopKey);
        var index = b.load(indexSlot, JavaDataDescriptor.DESC_INT);
        var list = b.load(listSlot, type.storeDescriptor());
        var size = b.invokeVirtual(type.storeName, "size", sizeDesc, list);
        b.branch(Opcodes.IF_ICMPGE, doneKey, index, size);
        b.add(elementBindDyn);
        var bodyRes = b.addCode(loc, bodyCode);
        if (bodyRes.hasErrors()) return bodyRes.reerr();
        var next = b.op(
                Opcodes.IADD,
                JavaDataDescriptor.DESC_INT,
                b.load(indexSlot, JavaDataDescriptor.DESC_INT),
                b.constInt(1));
        b.store(indexSlot, next);
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorIndexed(new BindingId(), new BindingId());
    }
}
