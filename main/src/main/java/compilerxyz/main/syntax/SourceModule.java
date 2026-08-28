package compilerxyz.main.syntax;

import compilerxyz.main.safejson.SafeJsonSerializable;

public sealed interface SourceModule extends SafeJsonSerializable {
    record V1(SourceExpr expr) implements SourceModule {}
}
