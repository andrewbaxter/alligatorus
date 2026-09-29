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
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.tuple.Tuple;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectUnionOptionDynamic<T extends Type>(TypeOptionDynamic<T> type) implements AspectUnion {

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
        var isSome = b.instanceOf(JavaInternalName.fromClass(Opt.Some.class), base);
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
        var some = b.checkcast(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Opt.Some.class)), base);
        var value = b.invokeVirtual(GenCodegenDescriptors.SOME_VALUE, some);
        var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType);
        if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
        var elemDesc = elemConsumeRes
                .requireValue("AspectUnionOptionDynamic::extract some consume")
                .aspectConsumeDynamicJvmClass(xx);
        if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
            value = b.checkcast(elemDesc, value);
        }
        return EResult.ok(b.code(value));
    }
}
