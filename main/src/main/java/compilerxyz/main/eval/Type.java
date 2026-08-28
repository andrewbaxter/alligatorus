package compilerxyz.main.eval;

import compilerxyz.main.artifact.Artifact;
import compilerxyz.main.artifact.Artifactable;
import compilerxyz.main.artifact.ExporterAny;
import compilerxyz.main.eval.aspect.Aspect;
import compilerxyz.main.gen.AspectArtifact;
import compilerxyz.main.gen.AspectDynamicConst;
import compilerxyz.prelude.annotations.BuiltinFull;
import compilerxyz.prelude.annotations.BuiltinPath;
import compilerxyz.prelude.util.Opt;

@BuiltinFull(uniqueId = 2002L, scopeId = 3002)
@BuiltinPath("eval.type")
public interface Type extends Artifactable {

    static EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc, Type self) {
        if (self instanceof AspectDynamicConst dc && !(self instanceof AspectArtifact)) {
            return dc.aspectDynamicConstConstForm().artifactableExportArtifact(xx, loc);
        }
        if (!(self instanceof AspectArtifact aa)) {
            return EResult.err(loc, self.typeDesc() + " does not implement AspectArtifact");
        }
        if (!(self instanceof ExporterAny ea)) {
            return EResult.err(loc, self.typeDesc() + " does not implement ExporterAny");
        }
        return aa.aspectArtifactExporter().artifactExporterExport(xx, loc, ea);
    }

    boolean typeIsEqual(ExprContext xx, Type other);

    boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

    EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);

    boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

    EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

    String typeDesc();

    Opt<Aspect> typeAspect(ExprContext xx, Unique key);
}
