package compilerxyz.eval.value.source_union_new;




import compilerxyz.annotations.BuiltinLight;
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
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactDataSourceUnionNewCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6143L)
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
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_NEW_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectFunction.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
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
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
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
                    var tuArtRes = parentConst.artifactableExportArtifact(xx, loc);
                    tuArtRes.drainInto(errors);
                    var tuArt =
                        tuArtRes.valueOrNull() != null
                            ? tuArtRes.requireValue(
                                "TypeTypeSourceUnionNewConst::exporterExport parent union")
                            : nullArtifact();
                    var body = new ArtifactDataSourceUnionNew(tuArt, variantIndex);
                    return makeBody.apply(ArtifactDataSourceUnionNewCodec.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(
                loc, "TypeTypeSourceUnionNewConst requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataSourceUnionNew body;
                try {
                  body = ArtifactDataSourceUnionNewCodec.fromValue(raw);
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
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_NEW_CONST_IDENTITY, TypeTypeSourceUnionNewConst.class);
    return oOpt.optIsSome() && type_.typeIsEqual(xx, oOpt.optUnwrap().type_) && variantIndex == oOpt.optUnwrap().variantIndex;
  }

  @Override
  public boolean typeCanCastFrom(ExprContext xx, QExprId loc, Type sourceType) {
    return false;
  }
  @Override
  public boolean typeCanCastTo(ExprContext xx, QExprId loc, Type destType) {
    return false;
  }
  @Override
  public EResult<CodeValue> typeCastTo(ExprContext xx, QExprId loc, CodeValue cv, Type destType) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastTo unreachable (typeCanCastTo returned false)");
  }

  @Override
  public EResult<Artifact> artifactableExportArtifact(ExprContext xx, QExprId loc) {
    return Type.exportArtifact(xx, loc, this);
  }
}
