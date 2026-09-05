package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalOutcome;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.CodeDynamic;
import compilerxyz.main.eval.dynamic.DynUtil.DropChild;
import compilerxyz.main.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.main.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectDropDynamicMapEntryDynamic(TypeMapEntryDynamic type) implements AspectDropDynamic {

    @Override
    public EResult<Code> aspectDropDynamicDrop(ExprContext xx, QExprId loc, CodeValue cv) {
        if (cv.value().optIsNone()) {
            throw new IllegalStateException("TypeMapEntryDynamic::drop: called on absent CodeValue");
        }
        if (!(cv.value().optUnwrap() instanceof ValueDynamic vd)) {
            throw new IllegalStateException("TypeMapEntryDynamic::drop: expected ValueDynamic, got "
                    + cv.value().optUnwrap().getClass());
        }
        var descObjNoArgs = JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(Object.class), TList.of());
        var children = new TList<DropChild>(2);
        children.add(new DropChild(
                type.keyType(),
                JavaBytecodeUtils.callInterfaceMethod(
                        loc, TypeMapEntryDynamic.MAP_ENTRY_INAME, "getKey", descObjNoArgs)));
        children.add(new DropChild(
                type.valueType(),
                JavaBytecodeUtils.callInterfaceMethod(
                        loc, TypeMapEntryDynamic.MAP_ENTRY_INAME, "getValue", descObjNoArgs)));
        var source = vd.source();
        boolean stackRooted = source.valueDynamicSourceIsStackRooted();
        var childDrops = new TList<CodeDynamic>();
        for (var child : children) {
            var dropOpt = GenAspectDropDynamic.getMaybe(xx, child.childType());
            if (dropOpt.optIsNone()) continue;
            var seq = new JavaBytecodeSequence();
            seq.add(JavaBytecodeUtils.inst(loc, Opcodes.DUP));
            seq.add(child.access());
            var childCv = new CodeValue(
                    Code.target(new CodeDynamic(loc, seq)),
                    EvalOutcome.ofValue(ValueDynamic.stack(xx, child.childType())));
            var childDropRes = dropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, childCv);
            if (childDropRes.hasErrors()) return childDropRes.reerr();
            var childDynCast =
                    CodeDynamic.castOpt(loc, childDropRes.requireValue("dropDestructiveComposite child drop"));
            if (childDynCast.hasErrors()) return childDynCast.reerr();
            var childDyn = childDynCast.requireValue("_ddcChildDyn");
            if (childDyn.optIsSome()) childDrops.add(childDyn.optUnwrap());
        }
        if (childDrops.isEmpty()) return EResult.ok(source.valueDynamicSourceDrop(loc));
        var out = new JavaBytecodeSequence();
        if (!stackRooted) out.add(source.valueDynamicSourceRealize(loc));
        for (var d : childDrops) out.add(d);
        out.add(JavaBytecodeUtils.inst(loc, Opcodes.POP));
        return EResult.ok(Code.target(new CodeDynamic(loc, out)));
    }
}
