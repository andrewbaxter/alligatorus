package compilerxyz.eval.value.source_union_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import java.util.function.Function;

public record TypeTypeSourceUnionNewConst(TypeSourceUnionDynamic type_, int variantIndex)
    implements Type, AspectArtifact, ExporterAny {

  @Override
  public String typeDesc() {
    return "definition_set_union_new_const";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceUnionNewConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
    return ExporterRoot.SOURCE_UNION_NEW_CONST;
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          return EResult.err(
              loc,
              "TypeTypeSourceUnionNewConst::exporterRootDelegateSelfExport: recursive export_artifact + parent "
                  + "TypeTypeSourceUnionConst export deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeTypeSourceUnionNewConst::exporterRootDelegateSelfFromArtifactData: recursive "
                  + "import_independent_artifact + parent TypeTypeSourceUnionConst import deferred");
        }
      };

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
