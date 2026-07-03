package compilerxyz.eval.dynamic;

public final class JvmConstants {
  private JvmConstants() {}

  public static final JavaInternalName INTNAME_OBJECT = JavaInternalName.of("java/lang/Object");
  public static final JavaInternalName INTNAME_STRING = JavaInternalName.of("java/lang/String");
  public static final JavaInternalName INTNAME_BOXED_BOOL =
      JavaInternalName.of("java/lang/Boolean");
  public static final JavaInternalName INTNAME_HASH_MAP =
      JavaInternalName.of("java/util/HashMap");
  public static final JavaInternalName INTNAME_MAP = JavaInternalName.of("java/util/Map");

  public static final JavaDataDescriptor DESC_OBJECT =
      JavaDataDescriptor.fromInternalName(INTNAME_OBJECT);
  public static final JavaDataDescriptor DESC_STRING =
      JavaDataDescriptor.fromInternalName(INTNAME_STRING);
  public static final JavaDataDescriptor DESC_BOOL = JavaDataDescriptor.DESC_BOOL;
}
