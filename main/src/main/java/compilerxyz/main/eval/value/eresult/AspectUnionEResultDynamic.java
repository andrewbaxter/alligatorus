package compilerxyz.main.eval.value.eresult;

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
import compilerxyz.main.eval.value.list.TypeListDynamic;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.main.gen.AspectUnion.Variant;
import compilerxyz.main.gen.GenAspectConsumeDynamic;
import compilerxyz.main.gen.GenCodegenDescriptors;
import compilerxyz.main.gen.GenExprErr;
import compilerxyz.prelude.util.TList;
import org.objectweb.asm.Opcodes;

public record AspectUnionEResultDynamic<T extends Type>(TypeEResultDynamic<T> type) implements AspectUnion {

    @Override
    public TList<Variant> aspectUnionVariants() {
        var out = new TList<Variant>();
        out.add(new Variant(new ConstKey.Str("ok"), type.elementType()));
        out.add(new Variant(new ConstKey.Str("errs"), TypeListDynamic.ofObject(GenExprErr.dynamicType())));
        return out;
    }

    @Override
    public EResult<CodeValue> aspectUnionTest(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionEResultDynamic::test base"));
        var hasErrors = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_HAS_ERRORS, baseReg);
        if (variantIndex == 0) {
            hasErrors = b.op(Opcodes.IXOR, JavaDataDescriptor.DESC_BOOL, hasErrors, b.constBool(true));
        }
        return EResult.ok(DynUtil.regToValue(b.code(), hasErrors, new TypeBoolDynamic()));
    }

    @Override
    public EResult<CodeValue> aspectUnionExtract(ExprContext xx, QExprId loc, CodeValue base, int variantIndex) {
        var baseRes = DynUtil.materialize(xx, loc, base);
        if (baseRes.hasErrors()) return baseRes.reerr();
        var b = new JavaBytecodeBuilder(loc);
        var baseReg = b.reg(baseRes.requireValue("AspectUnionEResultDynamic::extract base"));
        if (variantIndex == 0) {
            var what = b.constString("eresult match ok");
            var value = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_REQUIRE_VALUE, baseReg, what);
            var elemConsumeRes = GenAspectConsumeDynamic.get(xx, loc, type.elementType());
            if (elemConsumeRes.hasErrors()) return elemConsumeRes.reerr();
            var elemDesc = elemConsumeRes
                    .requireValue("AspectUnionEResultDynamic::extract ok consume")
                    .aspectConsumeDynamicJvmClass(xx);
            if (elemDesc.value().startsWith("L") && !elemDesc.value().equals("Ljava/lang/Object;")) {
                value = b.checkcast(elemDesc, value);
            }
            return EResult.ok(DynUtil.regToValue(b.code(), value, type.elementType()));
        }
        var errsOpt = b.invokeVirtual(GenCodegenDescriptors.E_RESULT_ERRORS_OPT, baseReg);
        var errs = b.invokeVirtual(GenCodegenDescriptors.OPT_OPT_UNWRAP, errsOpt);
        var casted = b.checkcast(JavaDataDescriptor.fromInternalName(JavaInternalName.fromClass(TList.class)), errs);
        return EResult.ok(DynUtil.regToValue(b.code(), casted, TypeListDynamic.ofObject(GenExprErr.dynamicType())));
    }
}
