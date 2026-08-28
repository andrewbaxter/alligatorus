package compilerxyz.docguard;

public record Violation(String file, int line, String kind, String message) {
    public String render() {
        return file + ":" + line + " [" + kind + "] " + message;
    }
}
