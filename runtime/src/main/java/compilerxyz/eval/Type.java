package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.Artifactable;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 2002L, scopeId = 3002)
@BuiltinPath("eval.type")
public interface Type extends Artifactable {

  /**
   * Default: a Type exports itself as an artifact by asking its own AspectArtifact for its
   * exporter and delegating. Requires {@code this} to implement both AspectArtifact and
   * ExporterAny; Types that don't need to be exportable can omit those interfaces and this
   * default will fail at call time.
   */
  @Override
  default EResult<Artifact> exportArtifact(ExprContext xx, QExprId loc) {
    if (!(this instanceof AspectArtifact aa)) {
      return EResult.err(loc, typeDesc() + " does not implement AspectArtifact");
    }
    if (!(this instanceof ExporterAny ea)) {
      return EResult.err(loc, typeDesc() + " does not implement ExporterAny");
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
