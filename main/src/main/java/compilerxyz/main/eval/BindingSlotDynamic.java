package compilerxyz.main.eval;

import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.MustCast;

@BuiltinFull(uniqueId = 6474L)
@BuiltinPath("eval.binding_slot_dynamic")
public record BindingSlotDynamic(BindingId id) implements BindingSlot {
    public static BindingId idOf(BindingSlot slot) {
        return MustCast.mustCast(BindingSlotDynamic.class, slot).id();
    }
}
