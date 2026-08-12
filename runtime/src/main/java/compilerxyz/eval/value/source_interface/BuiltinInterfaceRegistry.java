package compilerxyz.eval.value.source_interface;

import compilerxyz.eval.Type;
import compilerxyz.eval.value.object_.TypeObjectDynamic;
import compilerxyz.util.Opt;
import java.util.concurrent.ConcurrentHashMap;
import compilerxyz.eval.Unique.Builtin;
import compilerxyz.eval.Unique;

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
  }

  public static Opt<BuiltinInterfaceKind> kindForAspectId(Unique id) {
    return switch (id) {
      case Unique.Source s -> Opt.none();
      case Unique.Builtin b -> {
        long v = b.id();
        if (v == 1015L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FIELD);
        if (v == 1037L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FIELD_USE_DYNAMIC);
        if (v == 1039L) yield Opt.some(BuiltinInterfaceKind.ASPECT_ELEMENT_USE_DYNAMIC);
        if (v == 1017L) yield Opt.some(BuiltinInterfaceKind.ASPECT_SET);
        if (v == 1018L) yield Opt.some(BuiltinInterfaceKind.ASPECT_OPERATOR_ADD);
        if (v == 1042L) yield Opt.some(BuiltinInterfaceKind.ASPECT_OPERATOR_MOD);
        if (v == 1022L) yield Opt.some(BuiltinInterfaceKind.ASPECT_BIND);
        if (v == 1023L) yield Opt.some(BuiltinInterfaceKind.ASPECT_BIND_DYNAMIC);
        if (v == 1024L) yield Opt.some(BuiltinInterfaceKind.ASPECT_CONSUME_DYNAMIC);
        if (v == 1032L) yield Opt.some(BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC);
        if (v == 1035L) yield Opt.some(BuiltinInterfaceKind.ASPECT_DROP_DYNAMIC_LIGHT);
        if (v == 1021L) yield Opt.some(BuiltinInterfaceKind.ASPECT_INITIAL_BIND);
        if (v == 1026L) yield Opt.some(BuiltinInterfaceKind.ASPECT_LIFT);
        if (v == 1116L) yield Opt.some(BuiltinInterfaceKind.ASPECT_FUNCTION);
        if (v == 2001L) yield Opt.some(BuiltinInterfaceKind.VALUE);
        if (v == 2002L || v == 6006L) yield Opt.some(BuiltinInterfaceKind.TYPE);
        if (v == 2005L) yield Opt.some(BuiltinInterfaceKind.BINDING);
        if (v == 2006L) yield Opt.some(BuiltinInterfaceKind.TARGET);
        if (v == 2007L) yield Opt.some(BuiltinInterfaceKind.CODE_DYN);
        yield Opt.none();
      }
    };
  }

  public static void clear() {
    CACHE.clear();
  }
}
