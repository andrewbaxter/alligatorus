package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import java.util.concurrent.ConcurrentHashMap;

public final class BuiltinInterfaceRegistry {
  private static final ConcurrentHashMap<BuiltinInterfaceKind, InterfaceData> CACHE =
      new ConcurrentHashMap<>();

  private BuiltinInterfaceRegistry() {}

  public static InterfaceData resolve(BuiltinInterfaceKind kind) {
    return CACHE.computeIfAbsent(kind, k -> {
      Type t = switch (k) {
        case VALUE -> compilerxyz.gen.GenValue.dynamicType();
        case TYPE -> compilerxyz.gen.GenType.dynamicType();
        case ASPECT_FUNCTION -> compilerxyz.gen.GenAspectFunction.dynamicType();
        case ASPECT_FIELD -> compilerxyz.gen.GenAspectField.dynamicType();
        case ASPECT_FIELD_USE_DYNAMIC -> compilerxyz.gen.GenAspectFieldUseDynamic.dynamicType();
        case ASPECT_ELEMENT_USE_DYNAMIC -> compilerxyz.gen.GenAspectElementUseDynamic.dynamicType();
        case ASPECT_DROP_DYNAMIC -> compilerxyz.gen.GenAspectDropDynamic.dynamicType();
        case ASPECT_CONSUME_DYNAMIC -> compilerxyz.gen.GenAspectConsumeDynamic.dynamicType();
        case ASPECT_BIND_DYNAMIC -> compilerxyz.gen.GenAspectBindDynamic.dynamicType();
        case ASPECT_BIND -> compilerxyz.gen.GenAspectBind.dynamicType();
        case ASPECT_DROP_DYNAMIC_LIGHT -> compilerxyz.gen.GenAspectDropDynamicLight.dynamicType();
        case ASPECT_INITIAL_BIND -> compilerxyz.gen.GenAspectInitialBind.dynamicType();
        case ASPECT_LIFT -> compilerxyz.gen.GenAspectLift.dynamicType();
        case ASPECT_SET -> compilerxyz.gen.GenAspectSet.dynamicType();
        case ASPECT_OPERATOR_ADD -> compilerxyz.gen.GenAspectOperatorAdd.dynamicType();
        case ASPECT_OPERATOR_MOD -> compilerxyz.gen.GenAspectOperatorMod.dynamicType();
        case BINDING -> compilerxyz.gen.GenBinding.dynamicType();
        case TARGET -> compilerxyz.gen.GenTarget.dynamicType();
        case CODE_DYN -> compilerxyz.gen.GenCodeDyn.dynamicType();
      };
      return ((TypeObjectDynamic) t).getInterfaceData();
    });
  }

}
