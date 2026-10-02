package compilerxyz.main.syntax;

import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public sealed interface SourceModule extends SafeJsonSerializable {
    record V1(TList<SourceExpr> exprs) implements SourceModule {}
}
