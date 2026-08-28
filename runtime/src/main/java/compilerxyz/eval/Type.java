package compilerxyz.eval;

import compilerxyz.annotations.BuiltinFull;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.Artifactable;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectDynamicConst;
import compilerxyz.util.Opt;

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
