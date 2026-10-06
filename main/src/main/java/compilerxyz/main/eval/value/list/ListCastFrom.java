package compilerxyz.main.eval.value.list;

import compilerxyz.main.eval.BindingId;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.JumpKey;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.aspect.AspectLookup;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.dynamic.JvmSort;
import compilerxyz.main.eval.value.array.TypeArrayConst;
import compilerxyz.main.eval.value.array.TypeArrayDynamic;
import compilerxyz.main.gen.GenAspectTuple;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

final class ListCastFrom {
    private ListCastFrom() {}

    record Store(JavaInternalName name, String pushMethod, JavaMethodDescriptor pushDescriptor) {}

    static boolean canCast(ExprContext xx, QExprId loc, Type elementType, Type sourceType) {
        var tupleOpt = GenAspectTuple.getMaybe(xx, sourceType);
        if (tupleOpt.optIsSome()) {
            for (var sourceElement : tupleOpt.optUnwrap().aspectTupleElementTypes()) {
                if (!canConvert(xx, loc, sourceElement, elementType)) return false;
            }
            return true;
        }
        var arrayElement = arrayElementType(xx, sourceType);
        return arrayElement.optIsSome() && canConvert(xx, loc, arrayElement.optUnwrap(), elementType);
    }

    static EResult<CodeValue> cast(
            ExprContext xx, QExprId loc, ListDynamic listType, Type elementType, Store store, CodeValue cv) {
        if (cv.value().optIsNone()) {
            return EResult.err(loc, listType.typeDesc() + "::typeCastFrom: CodeValue has no value");
        }
        var value = cv.value().optUnwrap();
        var sourceType = value.valueType();
        var storeCtor = JavaMethodDescriptor.fromConstructorParts(TList.of());
        var tupleOpt = GenAspectTuple.getMaybe(xx, sourceType);
        if (tupleOpt.optIsSome()) {
            var elements = new TList<CodeDynamicReg>();
            for (var elementRes : tupleOpt.optUnwrap().aspectTupleTakeElements(xx, loc, value)) {
                if (elementRes.hasErrors()) return elementRes.reerr();
                var dynRes = DynUtil.materialize(
                        xx, loc, elementType, elementRes.requireValue("ListCastFrom tuple element"));
                if (dynRes.hasErrors()) return dynRes.reerr();
                elements.add(dynRes.requireValue("ListCastFrom tuple element dynamic"));
            }
            var b = new JavaBytecodeBuilder(loc);
            var carryRes = b.addCode(loc, cv.code());
            if (carryRes.optIsSome()) return EResult.errs(carryRes.optUnwrap());
            var regs = new TList<JavaBytecodeReg>(elements.size());
            for (var element : elements) regs.add(b.reg(element));
            var list = b.newInstance(store.name(), storeCtor);
            for (var reg : regs)
                b.invokeVirtualVoid(store.name(), store.pushMethod(), store.pushDescriptor(), list, reg);
            return EResult.ok(DynUtil.regToValue(b.code(), list, listType));
        }
        var sourceElementType = arrayElementType(xx, sourceType).optUnwrap();
        var sourceElementDesc = JvmSort.descriptor(xx, sourceElementType);
        var arrayDesc = sourceElementDesc.arrayOf();
        var storeDesc = JavaDataDescriptor.fromInternalName(store.name());
        var baseRes = DynUtil.materialize(xx, loc, cv);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var baseSlot = new BindingId();
        var indexSlot = new BindingId();
        var listSlot = new BindingId();
        var loopKey = new JumpKey();
        var doneKey = new JumpKey();
        var b = new JavaBytecodeBuilder(loc);
        b.store(baseSlot, b.reg(baseRes.requireValue("ListCastFrom array")));
        b.store(indexSlot, b.constInt(0));
        b.store(listSlot, b.newInstance(store.name(), storeCtor));
        b.land(loopKey);
        var index = b.load(indexSlot, JavaDataDescriptor.DESC_INT);
        var size = b.op(Opcodes.ARRAYLENGTH, JavaDataDescriptor.DESC_INT, b.load(baseSlot, arrayDesc));
        b.branch(Opcodes.IF_ICMPGE, doneKey, index, size);
        var eb = new JavaBytecodeBuilder(loc);
        var sourceElement = eb.op(
                sourceElementDesc.arrayLoadOpcode(),
                sourceElementDesc,
                eb.load(baseSlot, arrayDesc),
                eb.load(indexSlot, JavaDataDescriptor.DESC_INT));
        var elementRes = DynUtil.materialize(
                xx, loc, elementType, DynUtil.regToValue(eb.code(), sourceElement, sourceElementType));
        if (elementRes.hasErrors()) return elementRes.reerr();
        var list = b.load(listSlot, storeDesc);
        b.invokeVirtualVoid(
                store.name(),
                store.pushMethod(),
                store.pushDescriptor(),
                list,
                b.reg(elementRes.requireValue("ListCastFrom array element")));
        b.store(
                indexSlot,
                b.op(
                        Opcodes.IADD,
                        JavaDataDescriptor.DESC_INT,
                        b.load(indexSlot, JavaDataDescriptor.DESC_INT),
                        b.constInt(1)));
        b.jump(loopKey);
        b.land(doneKey);
        return EResult.ok(DynUtil.regToValue(b.code(), b.load(listSlot, storeDesc), listType));
    }

    private static boolean canConvert(ExprContext xx, QExprId loc, Type source, Type dest) {
        return source.typeCanCastTo(xx, loc, dest) || dest.typeCanCastFrom(xx, loc, source);
    }

    private static Opt<Type> arrayElementType(ExprContext xx, Type sourceType) {
        var dynamicOpt = AspectLookup.getMaybe(
                xx, sourceType, GenTrivialIdentityAspects.TYPE_ARRAY_DYNAMIC_IDENTITY, TypeArrayDynamic.class);
        if (dynamicOpt.optIsSome()) return Opt.some(dynamicOpt.optUnwrap().elementType);
        var constOpt = AspectLookup.getMaybe(
                xx, sourceType, GenTrivialIdentityAspects.TYPE_ARRAY_CONST_IDENTITY, TypeArrayConst.class);
        if (constOpt.optIsSome()) return Opt.some(constOpt.optUnwrap().elementType);
        return Opt.none();
    }
}
