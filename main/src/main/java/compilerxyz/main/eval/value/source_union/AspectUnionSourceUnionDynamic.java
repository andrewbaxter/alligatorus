package compilerxyz.main.eval.value.source_union;

import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.CodeDynamicReg;
import compilerxyz.main.eval.dynamic.JavaBytecodeBuilder;
import compilerxyz.main.eval.dynamic.JavaBytecodeReg;
import compilerxyz.main.eval.dynamic.JavaDataDescriptor;
import compilerxyz.main.eval.dynamic.SourceUnionEmitter;
import compilerxyz.main.gen.AspectUnion;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;

public record AspectUnionSourceUnionDynamic(TypeSourceUnionDynamic type) implements AspectUnion {

    @Override
    public TList<AspectUnion.Variant> aspectUnionVariants() {
        var out = new TList<AspectUnion.Variant>();
        for (var v : type.variants) {
            out.add(new AspectUnion.Variant(v.tag, v.type));
        }
        return out;
    }

    @Override
    public Opt<TList<ExprErr>> aspectUnionPrepare(ExprContext xx) {
        return Opt.none();
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionTest(QExprId loc, JavaBytecodeReg base, int variantIndex) {
        var b = new JavaBytecodeBuilder(loc);
        b.defSet(type.set);
        return EResult.ok(b.code(b.instanceOf(type.variantInternal(variantIndex), base)));
    }

    @Override
    public EResult<CodeDynamicReg> aspectUnionExtract(
            ExprContext xx, QExprId loc, JavaBytecodeReg base, int variantIndex, Type variantType) {
        var internal = type.variantInternal(variantIndex);
        var payloadDesc = SourceUnionEmitter.payloadDescriptor(xx, variantType);
        var b = new JavaBytecodeBuilder(loc);
        var variant = b.checkcast(JavaDataDescriptor.fromInternalName(internal), base);
        return EResult.ok(b.code(b.getField(internal, SourceUnionEmitter.PAYLOAD_FIELD, payloadDesc, variant)));
    }
}
