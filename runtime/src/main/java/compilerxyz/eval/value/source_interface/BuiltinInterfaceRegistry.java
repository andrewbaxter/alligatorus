package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import java.util.concurrent.ConcurrentHashMap;

public final class BuiltinInterfaceRegistry {
  private static final ConcurrentHashMap<BuiltinInterfaceKind, InterfaceData> CACHE =
      new ConcurrentHashMap<>();

  private BuiltinInterfaceRegistry() {}

  public static InterfaceData resolve(BuiltinInterfaceKind kind) {
    return CACHE.computeIfAbsent(kind, BuiltinInterfaceRegistry::build);
  }

  private static InterfaceData build(BuiltinInterfaceKind kind) {
    Type t = switch (kind) {
      case VALUE -> compilerxyz.gen.GenValue.dynamicType();
      case TYPE -> compilerxyz.gen.GenType.dynamicType();
      case ASPECT_FUNCTION -> compilerxyz.gen.GenAspectFunction.dynamicType();
      case ASPECT_FIELD -> compilerxyz.gen.GenAspectField.dynamicType();
      case ASPECT_DROP_DYNAMIC -> compilerxyz.gen.GenAspectDropDynamic.dynamicType();
      case ASPECT_CONSUME_DYNAMIC -> compilerxyz.gen.GenAspectConsumeDynamic.dynamicType();
      case ASPECT_BIND_DYNAMIC -> compilerxyz.gen.GenAspectBindDynamic.dynamicType();
      case ASPECT_BIND -> compilerxyz.gen.GenAspectBind.dynamicType();
      case ASPECT_SET -> compilerxyz.gen.GenAspectSet.dynamicType();
      case ASPECT_OPERATOR_ADD -> compilerxyz.gen.GenAspectOperatorAdd.dynamicType();
      case BINDING -> compilerxyz.gen.GenBinding.dynamicType();
      case TARGET -> compilerxyz.gen.GenTarget.dynamicType();
      case CODE_DYN -> compilerxyz.gen.GenCodeDyn.dynamicType();
    };
    return ((TypeObjectDynamic) t).getInterfaceData();
  }

  public static @org.jspecify.annotations.Nullable BuiltinInterfaceKind kindForAspectId(
      compilerxyz.eval.Unique id) {
    if (!(id instanceof compilerxyz.eval.Unique.Builtin b)) return null;
    long v = b.id();
    if (v == 1015L) return BuiltinInterfaceKind.ASPECT_FIELD;
    if (v == 1017L) return BuiltinInterfaceKind.ASPECT_SET;
    if (v == 1018L) return BuiltinInterfaceKind.ASPECT_OPERATOR_ADD;
    if (v == 1022L) return BuiltinInterfaceKind.ASPECT_BIND;
    if (v == 1023L) return BuiltinInterfaceKind.ASPECT_BIND_DYNAMIC;
    if (v == 1024L) return BuiltinInterfaceKind.ASPECT_CONSUME_DYNAMIC;
    if (v == 1032L) return BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC;
    if (v == 1116L) return BuiltinInterfaceKind.ASPECT_FUNCTION;
    if (v == 2001L) return BuiltinInterfaceKind.VALUE;
    if (v == 2002L || v == 6006L) return BuiltinInterfaceKind.TYPE;
    if (v == 2005L) return BuiltinInterfaceKind.BINDING;
    if (v == 2006L) return BuiltinInterfaceKind.TARGET;
    if (v == 2007L) return BuiltinInterfaceKind.CODE_DYN;
    return null;
  }

  public static void clear() {
    CACHE.clear();
  }
}
