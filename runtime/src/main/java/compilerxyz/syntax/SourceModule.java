package compilerxyz.syntax;

import compilerxyz.safejson.SafeJsonSerializable;

public sealed interface SourceModule extends SafeJsonSerializable {
    record V1(SourceExpr expr) implements SourceModule {}
}
