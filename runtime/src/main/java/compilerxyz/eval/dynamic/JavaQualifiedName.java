package compilerxyz.eval.dynamic;

import compilerxyz.util.TList;

public class JavaQualifiedName {
    public final TList<String> value;

    public JavaQualifiedName(TList<String> value) {
        this.value = value;
    }

    @Override
    public String toString() {
        var out = new StringBuilder();
        boolean first = true;
        for (var s : value) {
            if (first) first = false;
            else out.append(".");
            out.append(s);
        }
        return out.toString();
    }

    public JavaInternalName asInternalName() {
        var out = new StringBuilder();
        boolean first = true;
        for (var s : value) {
            if (first) first = false;
            else out.append("$");
            out.append(s.replace('.', '/'));
        }
        return JavaInternalName.of(out.toString());
    }
}
