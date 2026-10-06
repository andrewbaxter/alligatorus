package compilerxyz.main.eval.value.string;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.builtin.BuiltinBodies;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.array.TypeArrayDynamic;
import compilerxyz.main.eval.value.byte_.TypeByteDynamic;
import compilerxyz.main.gen.GenTrivialIdentityAspects;
import compilerxyz.prelude.util.TList;

final class StringCastBytes {
    private StringCastBytes() {}

    static boolean canCast(ExprContext xx, Type destType) {
        return destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_LIST_BYTE_DYNAMIC_IDENTITY)
                        .optIsSome()
                || destType.typeAspect(xx, GenTrivialIdentityAspects.TYPE_LIST_UBYTE_DYNAMIC_IDENTITY)
                        .optIsSome();
    }

    static EResult<CodeValue> cast(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
        var stringRes = DynUtil.materialize(xx, loc, cv);
        if (stringRes.hasErrors()) return stringRes.reerr();
        var stringDesc = JavaDataDescriptor.fromClass(String.class);
        var b = new JavaBytecodeBuilder(loc);
        var string = b.checkcast(stringDesc, b.reg(stringRes.requireValue("StringCastBytes string")));
        var bytes = b.invokeStatic(
                JavaInternalName.fromClass(BuiltinBodies.class),
                "strUtf8",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.fromClass(byte[].class), TList.of(stringDesc)),
                string);
        return destType.typeCastFrom(
                xx, loc, DynUtil.regToValue(b.code(), bytes, new TypeArrayDynamic<>(new TypeByteDynamic())));
    }
}
