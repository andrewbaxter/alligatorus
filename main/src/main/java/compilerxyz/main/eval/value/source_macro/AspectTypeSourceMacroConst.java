package compilerxyz.main.eval.value.source_macro;

import compilerxyz.main.eval.Type;
import compilerxyz.main.gen.AspectType;
import compilerxyz.main.gen.Value;

public record AspectTypeSourceMacroConst(TypeSourceMacroConst type) implements AspectType {

    @Override
    public Type aspectTypeType(Value value) {
        return new TypeSourceMacroConst(type.data());
    }
}
