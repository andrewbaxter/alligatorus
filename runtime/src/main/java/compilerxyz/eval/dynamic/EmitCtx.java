package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.util.TMap;

public final class EmitCtx {

  public int nextSlot;

  public final String ownerInternalName;

  public final int xxSlot;

  public final TMap<BindingId, Integer> slots = new TMap<>();

  public final String fieldPrefix;

  public EmitCtx(String ownerInternalName, int xxSlot, int nextSlot) {
    this(ownerInternalName, xxSlot, nextSlot, "");
  }

  public EmitCtx(String ownerInternalName, int xxSlot, int nextSlot, String fieldPrefix) {
    this.ownerInternalName = ownerInternalName;
    this.xxSlot = xxSlot;
    this.nextSlot = nextSlot;
    this.fieldPrefix = fieldPrefix;
  }

  public int allocSlot(BindingId id) {
    var slot = nextSlot;
    nextSlot += 1;
    slots.put(id, slot);
    return slot;
  }

  public int slotOf(BindingId id) {
    var s = slots.getOrNull(id);
    if (s == null) {
      throw new AssertionError("BindingId not bound: " + id);
    }
    return s;
  }
}
