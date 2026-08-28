package compilerxyz.main.eval;

public enum NumericKind {
    BYTE,
    SHORT,
    INT,
    LONG,
    FLOAT,
    DOUBLE,
    UBYTE,
    USHORT,
    UINT,
    ULONG;

    public boolean integral() {
        return this != FLOAT && this != DOUBLE;
    }

    public boolean unsigned() {
        return this == UBYTE || this == USHORT || this == UINT || this == ULONG;
    }

    public boolean wide() {
        return this == LONG || this == DOUBLE || this == ULONG;
    }

    public long slots() {
        return wide() ? 2 : 1;
    }

    public int intWidth() {
        return switch (this) {
            case BYTE, UBYTE -> 8;
            case SHORT, USHORT -> 16;
            case INT, UINT -> 32;
            case LONG, ULONG -> 64;
            case FLOAT, DOUBLE -> throw new IllegalStateException("intWidth on floating kind " + this);
        };
    }

    public static NumericKind unaryPromote(NumericKind a) {
        return switch (a) {
            case BYTE, SHORT, INT -> INT;
            case LONG -> LONG;
            case FLOAT -> FLOAT;
            case DOUBLE -> DOUBLE;
            case UBYTE, USHORT, UINT -> UINT;
            case ULONG -> ULONG;
        };
    }
}
