package compilerxyz.eval.value.source_union_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactBodyExporter;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ArtifactSupport;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;

public record TypeTypeSourceUnionNewConst(TypeSourceUnionDynamic type_, int variantIndex)
    implements Type, AspectArtifact, AspectBind, AspectFunction, ExporterAny {

  private TypeSourceUnionNewDynamic asCtor() {
    return new TypeSourceUnionNewDynamic(type_, variantIndex);
  }

  @Override
  public String typeDesc() {
    return "definition_set_union_new_const";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceUnionNewConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID)
        || key.equals(AspectBind.ID)
        || key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    if (key.equals(AspectPostDeserialize.ID)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return asCtor().aspectBindBind(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    return asCtor().aspectFunctionCall(xx, loc, function, argument);
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_UNION_NEW_CONST;
  }

  private static Artifact nullArtifact() {
    return new Artifact.Body(new ArtifactBody(new JsonValueNull(), new ArtifactBodyExporter.Root()));
  }

  public static final ExporterRootDelegate DELEGATE =
      new ExporterRootDelegate() {
        @Override
        public EResult<Artifact> exporterRootDelegateSelfExport(
            ExprContext xx,
            QExprId loc,
            ExporterAny self,
            Function<JsonValue, ArtifactBody> makeBody) {
          if (!(self instanceof TypeTypeSourceUnionNewConst uc)) {
            return EResult.err(
                loc,
                "TypeTypeSourceUnionNewConst::exporterRootDelegateSelfExport: expected"
                    + " TypeTypeSourceUnionNewConst, got " + self.getClass());
          }
          var tu = uc.type_;
          var variantIndex = uc.variantIndex;
          if (variantIndex < 0 || variantIndex >= tu.variants.size()) {
            return EResult.err(
                loc,
                "TypeTypeSourceUnionNewConst::exporterExport: variant index "
                    + variantIndex + " out of range");
          }
          var variant = tu.variants.get(variantIndex);
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  variant.newArtifactIdCell,
                  () -> {
                    var parentConst = new TypeTypeSourceUnionConst(tu);
                    var tuArtRes = ArtifactSupport.exportArtifact(xx, loc, parentConst);
                    tuArtRes.drainInto(errors);
                    var tuArt =
                        tuArtRes.valueOrNull() != null
                            ? tuArtRes.requireValue(
                                "TypeTypeSourceUnionNewConst::exporterExport parent union")
                            : nullArtifact();
                    var body = new ArtifactDataSourceUnionNew(tuArt, variantIndex);
                    return makeBody.apply(JsonValues.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (!(id instanceof Opt.Some<ArtifactIdSource> idSome)) {
            return EResult.err(
                loc, "TypeTypeSourceUnionNewConst requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataSourceUnionNew body;
                try {
                  body = JsonValues.fromValue(ArtifactDataSourceUnionNew.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeTypeSourceUnionNewConst data: failed to parse "
                          + "ArtifactDataSourceUnionNew: " + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var tuArt = body.union();
                var variantIndex = body.variantIndex();
                var holder = new ExporterAny[] {null};
                ExporterAny stub =
                    new ExporterAny() {
                      @Override
                      public EResult<Type> exporterAnyAsType() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeTypeSourceUnionNewConst import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsType();
                      }

                      @Override
                      public EResult<Value> exporterAnyAsValue() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeTypeSourceUnionNewConst import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsValue();
                      }
                    };
                var deps = new TList<Artifact>();
                deps.add(tuArt);
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var parentAny =
                          ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, tuArt);
                      var parentVal =
                          parentAny.requireValue(
                              "TypeTypeSourceUnionNewConst::finalizer parent union");
                      TypeSourceUnionDynamic tuData;
                      if (parentVal instanceof TypeTypeSourceUnionConst pConst) {
                        tuData = pConst.type_();
                      } else if (parentVal
                          instanceof TypeTypeSourceUnionConst.ImportedSourceUnionConst imp) {
                        tuData = imp.type_();
                      } else {
                        throw new IllegalStateException(
                            "TypeTypeSourceUnionNewConst finalizer: parent union artifact resolved to "
                                + parentVal.getClass());
                      }
                      if (variantIndex >= 0 && variantIndex < tuData.variants.size()) {
                        tuData.variants.get(variantIndex).newArtifactIdCell.set(idValue);
                      }
                      holder[0] = new TypeTypeSourceUnionNewConst(tuData, variantIndex);
                    };
                return new Result.Ok<>(new ArtifactSupport.ImportIndependentResult(stub, deps, fin));
              });
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
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeCanCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.Type destType) {
    return false;
  }
  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastTo(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv, compilerxyz.eval.Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }
  @Override
  public compilerxyz.eval.TypeId hasTypeIdTypeId() {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeId not provided");
  }
}
