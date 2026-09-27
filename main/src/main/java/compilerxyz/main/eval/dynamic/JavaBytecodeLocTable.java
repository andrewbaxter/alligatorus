package compilerxyz.main.eval.dynamic;

import compilerxyz.main.eval.QExprId;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;

public final class JavaBytecodeLocTable {
    private static final int LINE_MAX = 0xFFFF;

    private final TList<QExprId> locs = new TList<>();
    private final TMap<QExprId, Integer> lines = new TMap<>();

    public int line(QExprId loc) {
        return lines.computeIfAbsentT(loc, k -> {
            locs.add(k);
            if (locs.size() > LINE_MAX) {
                throw new IllegalStateException("generated class exceeds " + LINE_MAX + " source locations");
            }
            return locs.size();
        });
    }

    public IList<QExprId> locs() {
        return IList.from(locs);
    }
}
