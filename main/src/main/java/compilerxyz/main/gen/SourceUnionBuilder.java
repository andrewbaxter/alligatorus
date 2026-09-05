package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.EResult;
import compilerxyz.main.eval.EvalContext;
import compilerxyz.main.eval.ExprContext;
import compilerxyz.main.eval.QExprId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.dynamic.DynUtil;
import compilerxyz.main.eval.dynamic.Prefixes;
import compilerxyz.main.eval.value.source_union.SourceUnionVariant;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.source_union_new.TypeSourceUnionNewDynamic;
import compilerxyz.prelude.annotations.BuiltinEvalContext;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6349L)
public final class SourceUnionBuilder {
    public final TypeSourceUnionDynamic type;

    public SourceUnionBuilder(TypeSourceUnionDynamic type) {
        this.type = type;
    }

    @BuiltinEvalContext
    public EResult<TypeSourceUnionNewDynamic> variant(EvalContext vx, String tag, Type typeRef) {
        if (this.type.set.finished) {
            return EResult.err(
                    new QExprId(new SourceId(-1L), -1L),
                    "Cannot add a union variant after its definition set is finished");
        }
        var jsTag = Prefixes.TAG_PREFIX + this.type.nextTagId.next();
        var variantIndex = this.type.variants.size();
        var payloadType = DynUtil.substituteBoxed(ExprContext.freshRoot(vx.ix), typeRef);
        this.type.variants.add(new SourceUnionVariant(new ConstKey.Str(tag), jsTag, payloadType));
        this.type.active.add(variantIndex);
        return EResult.ok(new TypeSourceUnionNewDynamic(this.type, variantIndex));
    }
}
