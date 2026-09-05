package compilerxyz.main.eval.value.list;

import static compilerxyz.prelude.util.MustCast.mustCast;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.binding.BindingIteratorIndexed;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeInstructionObj;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeStoreLoad;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.gen.AspectIterable;
import compilerxyz.main.gen.Binding;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;

public record AspectIterableListSpecializedDynamic<T extends Type>(TypeListSpecializedDynamic<T> type)
        implements AspectIterable {

    @Override
    public EResult<CodeValue> aspectIterableElementAccess(ExprContext xx, QExprId loc, Binding loop) {
        var indexSlot = mustCast(BindingIteratorIndexed.class, loop).indexSlot();
        var getDesc = JavaMethodDescriptor.fromParts(type.elementDescriptor(xx), TList.of(JavaDataDescriptor.DESC_INT));
        var seq = new JavaBytecodeSequence();
        seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        seq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        seq.add(JavaBytecodeUtils.callMethod(loc, type.storeName, "get", getDesc));
        return EResult.ok(DynUtil.dynamicStackToValue(xx, new CodeDynamic(loc, seq), type.elementType));
    }

    @Override
    public Type aspectIterableElementType() {
        return type.elementType;
    }

    @Override
    public EResult<CodeValue> aspectIterableForeach(
            ExprContext xx, QExprId loc, CodeValue base, Binding loop, Code elementBind, CodeValue body) {
        var baseDynRes =
                new AspectConsumeDynamicListSpecializedDynamic<>(type).aspectConsumeDynamicToDynamic(xx, loc, base);
        if (baseDynRes.hasErrors()) return baseDynRes.reerr();
        var baseDyn = baseDynRes.requireValue("aspectConsumeDynamicToDynamic(base)");
        Code bodyCode;
        if (body.value().optIsSome()) {
            var dropRes = xx.target.targetDrop(xx, loc, body);
            if (dropRes.hasErrors()) return dropRes.reerr();
            var dropCode = dropRes.requireValue("TypeListSpecializedDynamic::foreach body drop");
            var bodyDynCast = CodeDynamic.castOpt(loc, body.code());
            if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
            var bodyDyn = bodyDynCast.requireValue("bodyDyn");
            var dropDynCast = CodeDynamic.castOpt(loc, dropCode);
            if (dropDynCast.hasErrors()) return dropDynCast.reerr();
            var dropDyn = dropDynCast.requireValue("dropDyn");
            if (bodyDyn.optIsNone() && dropDyn.optIsNone()) {
                bodyCode = Code.none(loc);
            } else {
                var seq = new JavaBytecodeSequence();
                if (bodyDyn.optIsSome()) seq.add(bodyDyn.optUnwrap());
                if (dropDyn.optIsSome()) seq.add(dropDyn.optUnwrap());
                bodyCode = Code.target(new CodeDynamic(loc, seq));
            }
        } else {
            bodyCode = body.code();
        }
        var elementBindCast = CodeDynamic.castOpt(loc, elementBind);
        if (elementBindCast.hasErrors()) return elementBindCast.reerr();
        var elementBindDyn = elementBindCast.requireValue("foreach element bind");
        var sizeDesc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_INT, TList.of());
        var indexSlot = mustCast(BindingIteratorIndexed.class, loop).indexSlot();
        var loopLabel = new LabelNode();
        var doneLabel = new LabelNode();
        var loopSeq = new JavaBytecodeSequence();
        loopSeq.add(baseDyn);
        loopSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, 0));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ISTORE, indexSlot)));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(loopLabel)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
        loopSeq.add(JavaBytecodeUtils.callMethod(loc, type.storeName, "size", sizeDesc));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.SWAP));
        loopSeq.add(
                new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.IF_ICMPGE, doneLabel))));
        if (elementBindDyn.optIsSome()) loopSeq.add(elementBindDyn.optUnwrap());
        var bodyDynCast = CodeDynamic.castOpt(loc, bodyCode);
        if (bodyDynCast.hasErrors()) return bodyDynCast.reerr();
        var bodyDyn = bodyDynCast.requireValue("bodyDyn");
        if (bodyDyn.optIsSome()) {
            loopSeq.add(bodyDyn.optUnwrap());
        }
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ILOAD, indexSlot)));
        loopSeq.add(JavaBytecodeUtils.literalIntShortByte(loc, 1));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.IADD));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeStoreLoad(Opcodes.ISTORE, indexSlot)));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(new JumpInsnNode(Opcodes.GOTO, loopLabel))));
        loopSeq.add(new CodeDynamic(loc, new JavaBytecodeInstructionObj(doneLabel)));
        loopSeq.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(new CodeValue(Code.target(new CodeDynamic(loc, loopSeq)), EvalOutcome.NO_VALUE));
    }

    @Override
    public Binding aspectIterableLoopBinding(ExprContext xx) {
        return new BindingIteratorIndexed(new BindingId());
    }
}
