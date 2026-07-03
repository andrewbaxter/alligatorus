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
  public static final JavaInternalName INTNAME_MAP_ENTRY =
      JavaInternalName.of("java/util/Map$Entry");
  public static final JavaInternalName INTNAME_ITERATOR =
      JavaInternalName.of("java/util/Iterator");
  public static final JavaInternalName INTNAME_ITERABLE =
      JavaInternalName.of("java/lang/Iterable");
  public static final JavaInternalName INTNAME_LIST = JavaInternalName.of("java/util/List");
  public static final JavaInternalName INTNAME_SET = JavaInternalName.of("java/util/Set");
  public static final JavaInternalName INTNAME_RUNTIME_EXCEPTION =
      JavaInternalName.of("java/lang/RuntimeException");

  public static final JavaDataDescriptor DESC_OBJECT =
      JavaDataDescriptor.fromInternalName(INTNAME_OBJECT);
  public static final JavaDataDescriptor DESC_STRING =
      JavaDataDescriptor.fromInternalName(INTNAME_STRING);
  public static final JavaDataDescriptor DESC_BOOL = JavaDataDescriptor.DESC_BOOL;
}
