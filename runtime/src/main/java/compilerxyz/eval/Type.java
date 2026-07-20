package compilerxyz.eval;

import compilerxyz.eval.aspect.Aspect;
import compilerxyz.util.Opt;

@compilerxyz.annotations.Builtin(uniqueId = 2002L, scopeId = 3002)
@compilerxyz.annotations.BuiltinPath("eval.type")
@compilerxyz.annotations.BuiltinTrivialIdentityAspect(6006L)
@compilerxyz.annotations.BuiltinTypeId(7012L)
public interface Type extends HasTypeId, compilerxyz.artifact.Artifactable {

  /**
   * Default: a Type exports itself as an artifact by asking its own AspectArtifact for its
   * exporter and delegating. Requires {@code this} to implement both AspectArtifact and
   * ExporterAny; Types that don't need to be exportable can omit those interfaces and this
   * default will fail at call time.
   */
  @Override
  default EResult<compilerxyz.artifact.Artifact> exportArtifact(ExprContext xx, QExprId loc) {
    if (!(this instanceof compilerxyz.gen.AspectArtifact aa)) {
      return EResult.err(loc, typeDesc() + " does not implement AspectArtifact");
    }
    if (!(this instanceof compilerxyz.artifact.ExporterAny ea)) {
      return EResult.err(loc, typeDesc() + " does not implement ExporterAny");
    }
    return aa.aspectArtifactExporter().artifactExporterExport(xx, loc, ea);
  }

  @compilerxyz.annotations.SourceExposed
  boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType);

  @compilerxyz.annotations.SourceExposed
  EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv);

  @compilerxyz.annotations.SourceExposed
  boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType);

  @compilerxyz.annotations.SourceExposed
  EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType);

  @compilerxyz.annotations.SourceExposed
  String typeDesc();

  @compilerxyz.annotations.SourceExposed
  Opt<Aspect> typeAspect(ExprContext xx, Unique key);
}
