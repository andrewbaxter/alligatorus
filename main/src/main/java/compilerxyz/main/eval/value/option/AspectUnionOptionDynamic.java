package compilerxyz.main.eval.value.option;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.JavaInternalName;
import compilerxyz.main.eval.value.bool.TypeBoolDynamic;
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
    public EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionOptionDynamic::test base"));
        var isSome = b.instanceOf(JavaInternalName.fromClass(Opt.Some.class), baseReg);
        if (variantIndex == 1) {
            isSome = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, isSome, b.constBool(true));
        }
        return EResult.ok(DynUtil.regToValue(b.code(), isSome, new TypeBoolDynamic()));
    }

    @Override
    public EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionOptionDynamic::extract base"));
        if (variantIndex == 1) {
            return EResult.ok(DynUtil.regToValue(b.code(), b.constNull(), new Tuple(IList.of())));
        }
        var some =
                b.checkcast(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(Opt.Some.class)), baseReg);
        var value = b.invokeVirtual(GenCodegenDescriptors.SOME_VALUE, some);
        var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType);
        if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
        var elemDesc = elemConsumeRes
                .requireValue("AspectUnionOptionDynamic::extract some consume")
                .aspectConsumeDynamicJvmClass(xx);
        if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
            value = b.checkcast(elemDesc, value);
        }
        return EResult.ok(DynUtil.regToValue(b.code(), value, type.elementType));
    }
}
