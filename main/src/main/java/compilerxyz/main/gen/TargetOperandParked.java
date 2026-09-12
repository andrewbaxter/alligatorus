package compilerxyz.main.gen;

import compilerxyz.main.eval.CodeValue;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;

@BuiltinFull(uniqueId = 6420L)
@BuiltinPath("types.target_operand_parked")
public final class TargetOperandParked {
    public final CodeValue value;
    public final Binding binding;

    public TargetOperandParked(CodeValue value, Binding binding) {
        this.value = value;
        this.binding = binding;
    }
}
