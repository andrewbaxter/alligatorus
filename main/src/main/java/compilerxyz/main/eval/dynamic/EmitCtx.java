package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.BindingId;
import compilerxyz.prelude.util.TMap;

public final class EmitCtx {

    public int nextSlot;

    public final String ownerInternalName;

    public final TMap<BindingId, Integer> slots = new TMap<>();

    public final String fieldPrefix;

    public EmitCtx(String ownerInternalName, int nextSlot) {
        this(ownerInternalName, nextSlot, "");
    }

    public EmitCtx(String ownerInternalName, int nextSlot, String fieldPrefix) {
        this.ownerInternalName = ownerInternalName;
        this.nextSlot = nextSlot;
        this.fieldPrefix = fieldPrefix;
    }

    public EmitCtx(String ownerInternalName, int xxSlotUnused, int nextSlot, String fieldPrefix) {
        this(ownerInternalName, nextSlot, fieldPrefix);
    }
}
