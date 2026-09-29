package compilerxyz.main.eval.value.option;

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
import compilerxyz.main.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectUnionOptionSpecializedDynamic<T extends Type>(TypeOptionSpecializedDynamic<T> type)
        implements AspectUnion {

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        out.add(new AspectUnion.Variant(new ConstKey.Str("some"), type.elementType));
        out.add(new AspectUnion.Variant(new ConstKey.Str("none"), new Tuple(IList.of())));
        return out;
    }

    @Override
    public Opt<TList<ExprErr>> aspectUnionPrepare(ExprContext xx) {
        return Opt.none();
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionTest(QExprId loc, JavaBytecodeReg base, int variantIndex) {
        var b = new JavaBytecodeBuilder(loc);
        var isSome = b.invokeVirtual(
                type.storeName,
                "isSome",
                JavaMethodDescriptor.fromParts(JavaDataDescriptor.DESC_BOOL, TList.of()),
                base);
        if (variantIndex == 1) {
            isSome = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, isSome, b.constBool(true));
        }
        return EResult.ok(b.code(isSome));
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType) {
        var b = new JavaBytecodeBuilder(loc);
        if (variantIndex == 1) {
            return EResult.ok(b.code(b.constNull()));
        }
        var value = b.invokeVirtual(
                type.storeName, "value", JavaMethodDescriptor.fromParts(type.elementDescriptor(xx), TList.of()), base);
        return EResult.ok(b.code(value));
    }
}
