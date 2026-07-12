package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.ImportStateId;
import compilerxyz.gen.GenBuiltinInterfaces;
import java.util.concurrent.ConcurrentHashMap;

public final class BuiltinInterfaceRegistry {
  private static final ConcurrentHashMap<ImportStateId, GenBuiltinInterfaces> CACHE =
      new ConcurrentHashMap<>();

  private BuiltinInterfaceRegistry() {}

  public static GenBuiltinInterfaces bundle(ImportStateId importState) {
    return CACHE.computeIfAbsent(importState, GenBuiltinInterfaces::build);
  }

  public static InterfaceData resolve(
      ImportStateId importState, BuiltinInterfaceKind kind) {
    var ifaces = bundle(importState);
    return switch (kind) {
      case VALUE -> ifaces.value;
      case TYPE -> ifaces.type_;
      case ASPECT_FUNCTION -> ifaces.aspect_function;
      case ASPECT_FIELD -> ifaces.aspect_field;
      case ASPECT_DROP_DYNAMIC -> ifaces.aspect_drop_dynamic;
      case ASPECT_CONSUME_DYNAMIC -> throw new IllegalStateException(
          "BuiltinInterfaceRegistry.resolve: ASPECT_CONSUME_DYNAMIC not registered in GenBuiltinInterfaces");
      case BINDING -> ifaces.binding;
      case TARGET -> ifaces.target;
      case CODE_DYN -> ifaces.code_dyn;
    };
  }

  public static void clear() {
    CACHE.clear();
  }
}
