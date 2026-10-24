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
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectIterableListDynamic<T extends Type>(TypeListDynamic<T> type) implements AspectIterable {
    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var slots = mustCast(BindingIteratorIndexed.class, loop);
        var b = new JavaBytecodeBuilder(loc);
        var list = b.load(slots.baseSlot(), type.storeDescriptor());
        var index = b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT);
        var raw = b.invokeVirtual(type.storeName, "get", type.getDescriptor(xx), list, index);
        return EResult.ok(DynUtil.regToValue(b.code(), type.elementDecode(xx, b, raw), type.elementType));
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
        var bodyCodeRes = foreachBody(xx, loc, body);
        if (bodyCodeRes.hasErrors()) return bodyCodeRes.reerr();
        var bodyCode = bodyCodeRes.requireValue("foreach body");
        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        var slots = mustCast(BindingIteratorIndexed.class, loop);
        var b = new JavaBytecodeBuilder(loc);
        b.store(slots.baseSlot(), b.reg(baseRes.requireValue("base")));
        var loopRes = indexLoop(loc, type, slots, b, elementBindDyn, bodyCode);
        if (loopRes.hasErrors()) return loopRes.reerr();
        return EResult.ok(new CodeValue(Code.target(b.code()), EvalOutcome.NO_VALUE));
    }

    static EResult<Object> indexLoop(
            QExprId loc,
            TypeListDynamic<?> type,
            BindingIteratorIndexed slots,
            JavaBytecodeBuilder b,
            Opt<CodeDynamic> elementBind,
            Code body) {
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        b.store(slots.indexSlot(), b.constInt(0));
        b.land(loopKey);
        var index = b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT);
        var list = b.load(slots.baseSlot(), type.storeDescriptor());
        var size = b.invokeVirtual(type.storeName, "size", TypeListDynamic.SIZE_DESCRIPTOR, list);
        b.branch(Opcodes.IF_ICMPGE, doneKey, index, size);
        b.add(elementBind);
        var bodyRes = b.addCode(loc, body);
        if (bodyRes.optIsSome()) return EResult.errs(bodyRes.optUnwrap());
        var next = b.op(
                Opcodes.IADD,
                JavaDataDescriptor.DESC_INT,
                b.load(slots.indexSlot(), JavaDataDescriptor.DESC_INT),
                b.constInt(1));
        b.store(slots.indexSlot(), next);
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(new Object());
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorIndexed(new BindingId(), new BindingId());
    }

    public static EResult<Code> foreachBody(ExprContext xx, QExprId loc, CodeValue body) {
        if (body.value().optIsNone()) return EResult.ok(body.code());
        var dropRes = xx.target.targetDrop(xx, loc, body);
        if (dropRes.hasErrors()) return dropRes.reerr();
        return xx.target.targetSeq(loc, TList.of(body.code(), dropRes.requireValue("foreach body drop")));
    }
}
