package compilerxyz.main.syntax;

import compilerxyz.main.safejson.SafeJsonStreamable;
import compilerxyz.prelude.util.TList;

public sealed interface SourceModule extends SafeJsonStreamable {
    record V1(TList<SourceExpr> exprs) implements SourceModule {}
}
