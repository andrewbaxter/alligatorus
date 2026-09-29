package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.source_union.SourceUnionVariant;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.source_union_new.TypeSourceUnionNewDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6349L)
public final class SourceUnionBuilder {
    public final TypeSourceUnionDynamic type;

    public SourceUnionBuilder(TypeSourceUnionDynamic type) {
        this.type = type;
    }

    public TypeSourceUnionNewDynamic variant(String tag, Type typeRef) {
        if (this.type.set.finished) {
            throw new IllegalStateException("Cannot add a union variant after its definition set is finished");
        }
        var variantIndex = this.type.variants.size();
        this.type.variants.add(new SourceUnionVariant(new ConstKey.Str(tag), typeRef));
        this.type.active.add(variantIndex);
        return new TypeSourceUnionNewDynamic(this.type, variantIndex);
    }
}
