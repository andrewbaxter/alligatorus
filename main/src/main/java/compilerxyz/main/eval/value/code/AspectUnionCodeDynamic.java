package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public final class AspectUnionCodeDynamic implements AspectUnion {
    public static final AspectUnionCodeDynamic INSTANCE = new AspectUnionCodeDynamic();

    private static final JavaInternalName CODE_INTNAME = JavaInternalName.fromClass(Code.class);

    private AspectUnionCodeDynamic() {}

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        out.add(new AspectUnion.Variant(new ConstKey.Str("none"), new Tuple(IList.of())));
        out.add(new AspectUnion.Variant(new ConstKey.Str("target"), new TypeCodeTargetDynamic()));
        return out;
    }

    @Override
    public EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var desc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of());
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionCodeDynamic::test base"));
        var isNone = b.invokeInterface(CODE_INTNAME, "codeIsNone", desc, baseReg);
        if (variantIndex == 1) {
            isNone = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, isNone, b.constBool(true));
        }
        return EResult.ok(DynUtil.regToValue(b.code(), isNone, new TypeBoolDynamic()));
    }

    @Override
    public EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionCodeDynamic::extract base"));
        if (variantIndex == 0) {
            return EResult.ok(DynUtil.regToValue(b.code(), b.constNull(), new Tuple(IList.of())));
        }
        var target = b.invokeStatic(GenCodegenDescriptors.CODE_TARGET_IMPL, baseReg);
        return EResult.ok(DynUtil.regToValue(b.code(), target, new TypeCodeTargetDynamic()));
    }
}
