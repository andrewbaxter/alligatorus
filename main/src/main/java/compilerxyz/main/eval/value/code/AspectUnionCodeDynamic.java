package compilerxyz.main.eval.value.code;

import compilerxyz.main.eval.Code;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
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
    public Opt<TList<ExprErr>> aspectUnionPrepare(ExprContext xx) {
        return Opt.none();
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionTest(QExprId loc, JavaBytecodeReg base, int variantIndex) {
        var desc = JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of());
        var b = new JavaBytecodeBuilder(loc);
        var isNone = b.invokeInterface(CODE_INTNAME, "codeIsNone", desc, base);
        if (variantIndex == 1) {
            isNone = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, isNone, b.constBool(true));
        }
        return EResult.ok(b.code(isNone));
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType) {
        var b = new JavaBytecodeBuilder(loc);
        if (variantIndex == 0) {
            return EResult.ok(b.code(b.constNull()));
        }
        return EResult.ok(b.code(b.invokeStatic(GenCodegenDescriptors.CODE_TARGET_IMPL, base)));
    }
}
