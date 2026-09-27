package compilerxyz.main.eval.value.map;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.ValueDynamic;
import compilerxyz.main.gen.AspectDropDynamic;
import compilerxyz.main.gen.GenAspectDropDynamic;
import compilerxyz.prelude.util.TList;

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
        record Child(Type type, String getter) {}
        var children = TList.of(new Child(type.keyType(), "getKey"), new Child(type.valueType(), "getValue"));
        var b = new JavaBytecodeBuilder(loc);
        boolean any = false;
        for (var child : children) {
            var dropOpt = GenAspectDropDynamic.getMaybe(xx, child.type());
            if (dropOpt.optIsNone()) continue;
            var access = new JavaBytecodeBuilder(loc);
            var raw = access.invokeInterface(
                    TypeMapEntryDynamic.MAP_ENTRY_INAME,
                    child.getter(),
                    descObjNoArgs,
                    DynUtil.sourceReg(access, vd.source()));
            var childCv = DynUtil.regToValue(access.code(), raw, child.type());
            var childDropRes = dropOpt.optUnwrap().aspectDropDynamicDrop(xx, loc, childCv);
            if (childDropRes.hasErrors()) return childDropRes.reerr();
            b.add(access.code());
            var dropRes = b.addCode(loc, childDropRes.requireValue("map entry child drop"));
            if (dropRes.optIsSome()) return EResult.errs(dropRes.optUnwrap());
            any = true;
        }
        if (!any) return EResult.ok(Code.none(loc));
        return EResult.ok(Code.target(b.code()));
    }
}
