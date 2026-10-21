package compilerxyz.main.gen;

import compilerxyz.main.eval.ConstKey;
import compilerxyz.main.eval.Type;
import compilerxyz.main.eval.value.empty.ValueEmpty;
import compilerxyz.main.eval.value.source_union.SourceUnionVariant;
import compilerxyz.main.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.main.eval.value.source_union_new.TypeSourceUnionNewDynamic;
import compilerxyz.prelude.annotations.BuiltinFull;

@BuiltinFull(uniqueId = 6349L)
public final class SourceUnionBuilder {
    private final TypeSourceUnionDynamic union;

    public final Type type;

    public SourceUnionBuilder(TypeSourceUnionDynamic type) {
        this.union = type;
        this.type = type;
    }

    public Value variant(String tag, Type typeRef) {
        if (this.union.set.finished) {
            throw new IllegalStateException("Cannot add a union variant after its definition set is finished");
        }
        var variantIndex = this.union.variants.size();
        this.union.variants.add(new SourceUnionVariant(new ConstKey.Str(tag), typeRef));
        this.union.active.add(variantIndex);
        return new ValueEmpty(new TypeSourceUnionNewDynamic(this.union, variantIndex));
    }
}
