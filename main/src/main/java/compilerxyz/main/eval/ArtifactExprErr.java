package compilerxyz.main.eval;

import compilerxyz.main.safejson.SafeJsonSerializable;
import compilerxyz.prelude.util.TList;

public record ArtifactExprErr(ArtifactExpr.ArtifactQExprId loc, TList<String> messages)
        implements SafeJsonSerializable {}
