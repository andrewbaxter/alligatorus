package compilerxyz.main.eval.dynamic;

import java.util.Iterator;

public final class JvmConstants {
    private JvmConstants() {}

    public static final JavaInternalName INTNAME_OBJECT = JavaInternalName.fromClass(Object.class);
    public static final JavaInternalName INTNAME_STRING = JavaInternalName.fromClass(String.class);
    public static final JavaInternalName INTNAME_ITERATOR = JavaInternalName.fromClass(Iterator.class);

    public static final JavaDataDescriptor DESC_OBJECT = JavaDataDescriptor.fromInternalName(INTNAME_OBJECT);
    public static final JavaDataDescriptor DESC_STRING = JavaDataDescriptor.fromInternalName(INTNAME_STRING);
}
