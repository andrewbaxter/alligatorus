package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.eval.value.list.TypeListBoxedDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.AspectUnion.Variant;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectUnionEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectUnion {

    @Override
    public TList<Variant> aspectUnionVariants() {
        var out = new TList<Variant>();
        out.add(new Variant(new ConstKey.Str("ok"), type.elementType()));
        out.add(new Variant(new ConstKey.Str("errs"), new TypeListBoxedDynamic(GenExprErr.dynamicType())));
        return out;
    }

    @Override
    public Opt<TList<ExprErr>> aspectUnionPrepare(ExprContext xx) {
        return Opt.none();
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionTest(QExprId loc, JavaBytecodeReg base, int variantIndex) {
        var b = new JavaBytecodeBuilder(loc);
        var hasErrors = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_HAS_ERRORS, base);
        if (variantIndex == 0) {
            hasErrors = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, hasErrors, b.constBool(true));
        }
        return EResult.ok(b.code(hasErrors));
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType) {
        var b = new JavaBytecodeBuilder(loc);
        if (variantIndex == 0) {
            var what = b.constString("eresult match ok");
            var value = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE, base, what);
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("AspectUnionEResultDynamic::Extract ok consume")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                value = b.checkcast(elemDesc, value);
            }
            return EResult.ok(b.code(value));
        }
        var errsOpt = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_ERRORS_OPT, base);
        var errs = b.invokeVirtual(GenCodegenDescriptors.OPT_OPT_UNWRAP, errsOpt);
        var casted = b.checkcast(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class)), errs);
        return EResult.ok(b.code(casted));
    }
}
