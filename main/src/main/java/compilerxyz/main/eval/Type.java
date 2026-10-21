package compilerxyz.main.eval;

import compilerxyz.main.artifact.ArtifactExporter;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 2002L, scopeId = 3002)
@BuiltinPath("eval.type")
public interface Type {
    Opt<ArtifactExporter> typeArtifactExporter(ExprContext xx);

    boolean typeIsEqual(ExprContext xx, Type other);

    boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

    EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);

    boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

    EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

    String typeDesc();

    Opt<Aspect> typeAspect(ExprContext xx, Unique key);
}
