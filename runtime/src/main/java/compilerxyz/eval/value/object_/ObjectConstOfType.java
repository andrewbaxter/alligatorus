package compilerxyz.eval.value.object_;

import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

/**
 * Shared TypeObjectConst instance representing "a compile-time Type value" —
 * used as the value-type for {@code builtins.types.string},
 * {@code builtins.types.number}, and other builtin bindings whose value is a
 * Type instance. AspectType.type_ casts the value directly to Type.
 *
 * <p>Interfaces (like {@code aspect_bind}) may need a more specialized
 * TypeObjectConst that additionally exposes TypeAspect/AspectAspect; those
 * live elsewhere.
 */
public final class ObjectConstOfType {

  private ObjectConstOfType() {}

  public static Type dynamicType() {
    return Holder.INSTANCE;
  }

  private static final class Holder {
    static final TypeObjectConst INSTANCE = build();

    private static TypeObjectConst build() {
      return new TypeObjectConst(
          GenTypeIds.TYPE_ANY_CONST_TYPE_ID,
          "type",
          Type.class,
          Opt.<Type>none(),
          new TList<Unique>(),
          new TList<TypeObjectCastRule>(),
          new TList<TypeObjectCastFromRule>(),
          Opt.<TypeObjectLift>none(),
          Opt.<ObjectAspectSource>none());
    }
  }
}
