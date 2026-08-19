package compilerxyz.gen;

import compilerxyz.annotations.BuiltinPushStruct;
import compilerxyz.eval.ConstKey;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.SourceId;
import compilerxyz.eval.Type;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.Prefixes;
import compilerxyz.eval.value.source_union.SourceUnionVariant;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union_new.TypeSourceUnionNewDynamic;
import java.lang.String;

@BuiltinPushStruct
public final class SourceUnionBuilder {
    public final TypeSourceUnionDynamic type_;

    public SourceUnionBuilder(TypeSourceUnionDynamic type_) {
        this.type_ = type_;
    }

    public static Type dynamicType() {
        return GenObjectTypes.sourceUnionBuilder();
    }

    public EResult<TypeSourceUnionNewDynamic> variant(ExprContext xx, String tag, Type typeRef) {
        if (this.type_.set.finished) {
            return EResult.err(
                new QExprId(new SourceId(-1L), -1L),
                "Cannot add a union variant after its definition set is finished");
        }
        var jsTag = Prefixes.TAG_PREFIX + this.type_.nextTagId.next();
        var variantIndex = this.type_.variants.size();
        var payloadType = DynUtil.substituteBoxed(xx, typeRef);
        this.type_.variants.add(new SourceUnionVariant(new ConstKey.Str(tag), jsTag, payloadType));
        this.type_.active.add(variantIndex);
        return EResult.ok(new TypeSourceUnionNewDynamic(this.type_, variantIndex));
    }
}
