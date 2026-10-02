package compilerxyz.main.eval.dynamic;

import compilerxyz.prelude.annotations.BuiltinFull;
import org.objectweb.asm.Opcodes;

@BuiltinFull(uniqueId = 6014L)
public final class JavaDataDescriptor {
    public static final JavaDataDescriptor DESC_VOID = new JavaDataDescriptor("V");
    public static final JavaDataDescriptor DESC_INT = new JavaDataDescriptor("I");
    public static final JavaDataDescriptor DESC_BYTE = new JavaDataDescriptor("B");
    public static final JavaDataDescriptor DESC_CHAR = new JavaDataDescriptor("C");
    public static final JavaDataDescriptor DESC_SHORT = new JavaDataDescriptor("S");
    public static final JavaDataDescriptor DESC_LONG = new JavaDataDescriptor("J");
    public static final JavaDataDescriptor DESC_BOOL = new JavaDataDescriptor("Z");
    public static final JavaDataDescriptor DESC_FLOAT = new JavaDataDescriptor("F");
    public static final JavaDataDescriptor DESC_DOUBLE = new JavaDataDescriptor("D");

    public final String value;

    public String value() {
        return value;
    }

    public JavaDataDescriptor(String value) {
        this.value = value;
    }

    public static JavaDataDescriptor create(String value) {
        return new JavaDataDescriptor(value);
    }

    public static JavaDataDescriptor fromJVMName(JavaInternalName name) {
        return new JavaDataDescriptor("L" + name.value + ";");
    }

    public static JavaDataDescriptor fromInternalName(JavaInternalName name) {
        return fromJVMName(name);
    }

    public static JavaDataDescriptor fromClass(Class<?> t) {
        if (t.isPrimitive()) {
            return switch (t.getName()) {
                case "int" -> DESC_INT;
                case "byte" -> DESC_BYTE;
                case "char" -> DESC_CHAR;
                case "short" -> DESC_SHORT;
                case "long" -> DESC_LONG;
                case "boolean" -> DESC_BOOL;
                case "float" -> DESC_FLOAT;
                case "double" -> DESC_DOUBLE;
                case "void" -> DESC_VOID;
                default -> throw new AssertionError("Unknown primitive: " + t);
            };
        }
        if (t.isArray()) return new JavaDataDescriptor("[" + fromClass(t.getComponentType()).value);
        return fromJVMName(JavaInternalName.fromClass(t));
    }

    private char sortChar() {
        if (value.isEmpty()) throw new IllegalStateException("JavaDataDescriptor: empty descriptor");
        return value.charAt(0);
    }

    public boolean isWide() {
        return switch (sortChar()) {
            case 'D', 'J' -> true;
            default -> false;
        };
    }

    public int slotWidth() {
        return isWide() ? 2 : 1;
    }

    public boolean isReference() {
        return switch (sortChar()) {
            case 'L', '[' -> true;
            default -> false;
        };
    }

    public int loadOpcode() {
        return switch (sortChar()) {
            case 'D' -> Opcodes.DLOAD;
            case 'J' -> Opcodes.LLOAD;
            case 'F' -> Opcodes.FLOAD;
            case 'I', 'Z', 'B', 'S', 'C' -> Opcodes.ILOAD;
            case 'L', '[' -> Opcodes.ALOAD;
            default -> throw new IllegalStateException("JavaDataDescriptor: no load opcode for (" + value + ")");
        };
    }

    public int storeOpcode() {
        return switch (sortChar()) {
            case 'D' -> Opcodes.DSTORE;
            case 'J' -> Opcodes.LSTORE;
            case 'F' -> Opcodes.FSTORE;
            case 'I', 'Z', 'B', 'S', 'C' -> Opcodes.ISTORE;
            case 'L', '[' -> Opcodes.ASTORE;
            default -> throw new IllegalStateException("JavaDataDescriptor: no store opcode for (" + value + ")");
        };
    }

    public JavaDataDescriptor arrayOf() {
        return new JavaDataDescriptor("[" + value);
    }

    public int arrayLoadOpcode() {
        return switch (sortChar()) {
            case 'D' -> Opcodes.DALOAD;
            case 'J' -> Opcodes.LALOAD;
            case 'F' -> Opcodes.FALOAD;
            case 'I' -> Opcodes.IALOAD;
            case 'Z', 'B' -> Opcodes.BALOAD;
            case 'S' -> Opcodes.SALOAD;
            case 'C' -> Opcodes.CALOAD;
            case 'L', '[' -> Opcodes.AALOAD;
            default -> throw new IllegalStateException("JavaDataDescriptor: no array load opcode for (" + value + ")");
        };
    }

    public int returnOpcode() {
        return switch (sortChar()) {
            case 'V' -> Opcodes.RETURN;
            case 'D' -> Opcodes.DRETURN;
            case 'J' -> Opcodes.LRETURN;
            case 'F' -> Opcodes.FRETURN;
            case 'I', 'Z', 'B', 'S', 'C' -> Opcodes.IRETURN;
            case 'L', '[' -> Opcodes.ARETURN;
            default -> throw new IllegalStateException("JavaDataDescriptor: no return opcode for (" + value + ")");
        };
    }

    @Override
    public String toString() {
        return value;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof JavaDataDescriptor other && other.value.equals(this.value);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }
}
