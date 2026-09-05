package compilerxyz.lint.convention;

import java.util.Set;
import java.util.regex.Pattern;

public final class JavaNames {
    private final Set<String> keywords = Set.of(
            "abstract",
            "assert",
            "boolean",
            "break",
            "byte",
            "case",
            "catch",
            "char",
            "class",
            "const",
            "continue",
            "default",
            "do",
            "double",
            "else",
            "enum",
            "extends",
            "final",
            "finally",
            "float",
            "for",
            "goto",
            "if",
            "implements",
            "import",
            "instanceof",
            "int",
            "interface",
            "long",
            "native",
            "new",
            "package",
            "private",
            "protected",
            "public",
            "return",
            "short",
            "static",
            "strictfp",
            "super",
            "switch",
            "synchronized",
            "this",
            "throw",
            "throws",
            "transient",
            "try",
            "void",
            "volatile",
            "while",
            "true",
            "false",
            "null");

    public static final Pattern UPPER_CAMEL = Pattern.compile("[A-Z][A-Za-z0-9]*");
    public static final Pattern LOWER_CAMEL = Pattern.compile("[a-z][A-Za-z0-9]*");
    public static final Pattern UPPER_SNAKE = Pattern.compile("[A-Z][A-Z0-9]*(_[A-Z0-9]+)*");
    public static final Pattern PACKAGE_SEGMENT = Pattern.compile("[a-z]([a-z0-9_]*[a-z0-9])?");

    public static final String UNDERSCORE_RULE =
            "a trailing '_' is only allowed when the bare name is a Java keyword; a leading '_' is never allowed";

    public String bare(String name) {
        if (name.length() > 1 && name.endsWith("_") && keywords.contains(name.substring(0, name.length() - 1))) {
            return name.substring(0, name.length() - 1);
        }
        return name;
    }
}
