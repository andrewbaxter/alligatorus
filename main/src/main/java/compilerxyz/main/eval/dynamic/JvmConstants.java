package compilerxyz.main.eval.dynamic;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class JvmConstants {
    private JvmConstants() {}

    public static final JavaInternalName INTNAME_OBJECT = JavaInternalName.fromClass(Object.class);
    public static final JavaInternalName INTNAME_STRING = JavaInternalName.fromClass(String.class);
    public static final JavaInternalName INTNAME_BOXED_BOOL = JavaInternalName.fromClass(Boolean.class);
    public static final JavaInternalName INTNAME_HASH_MAP = JavaInternalName.fromClass(HashMap.class);
    public static final JavaInternalName INTNAME_LINKED_HASH_MAP = JavaInternalName.fromClass(LinkedHashMap.class);
    public static final JavaInternalName INTNAME_MAP = JavaInternalName.fromClass(Map.class);
    public static final JavaInternalName INTNAME_MAP_ENTRY = JavaInternalName.fromClass(Map.Entry.class);
    public static final JavaInternalName INTNAME_ITERATOR = JavaInternalName.fromClass(Iterator.class);
    public static final JavaInternalName INTNAME_ITERABLE = JavaInternalName.fromClass(Iterable.class);
    public static final JavaInternalName INTNAME_LIST = JavaInternalName.fromClass(List.class);
    public static final JavaInternalName INTNAME_SET = JavaInternalName.fromClass(Set.class);
    public static final JavaInternalName INTNAME_RUNTIME_EXCEPTION = JavaInternalName.fromClass(RuntimeException.class);

    public static final JavaDataDescriptor DESC_OBJECT = JavaDataDescriptor.fromInternalName(INTNAME_OBJECT);
    public static final JavaDataDescriptor DESC_STRING = JavaDataDescriptor.fromInternalName(INTNAME_STRING);
    public static final JavaDataDescriptor DESC_BOOL = JavaDataDescriptor.DESC_BOOL;
}
