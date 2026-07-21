package compilerxyz.eval;

import compilerxyz.annotations.Builtin;
import compilerxyz.annotations.BuiltinPath;
import compilerxyz.annotations.BuiltinTrivialIdentityAspect;
import compilerxyz.annotations.BuiltinTypeId;
import compilerxyz.annotations.SourceExposed;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.Artifactable;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.util.Opt;

@Builtin(uniqueId = 2002L, scopeId = 3002)
@BuiltinPath("eval.type")
@BuiltinTrivialIdentityAspect(6006L)
@BuiltinTypeId(7012L)
public interface Type extends HasTypeId, Artifactable {

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

  @SourceExposed
  boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

  @SourceExposed
  EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);

  @SourceExposed
  boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

  @SourceExposed
  EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

  @SourceExposed
  String typeDesc();

  @SourceExposed
  Opt<Aspect> typeAspect(ExprContext xx, Unique key);
}
