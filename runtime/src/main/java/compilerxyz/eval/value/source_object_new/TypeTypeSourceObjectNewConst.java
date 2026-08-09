package compilerxyz.eval.value.source_object_new;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;


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
import compilerxyz.eval.value.source_object.TypeTypeSourceObjectConst;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.safejson.JsonValues;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.gen.AspectLift;
import org.jspecify.annotations.Nullable;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import java.util.function.Function;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectLift;
import compilerxyz.gen.GenAspectPostDeserialize;

@BuiltinLight(uniqueId = 6140L)
public record TypeTypeSourceObjectNewConst(TypeSourceObjectDynamic type_)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectDropDynamicLight,
        AspectFunction,
        AspectLift,
        ExporterAny {

  private TypeSourceObjectNewDynamic asCtor() {
    return new TypeSourceObjectNewDynamic(type_);
  }

  @Override
  public String typeDesc() {
    if (type_.description != null) return type_.description;
    return "definition_set_object_new_const";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectFunction.AspectId)
        || key.equals(GenAspectLift.AspectId)) {
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
  public EResult<Value> aspectLiftLift(
      ExprContext xx, QExprId loc, @Nullable Object externalHostValue) {
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(
        new TypeSourceObjectNewDynamic(type_), null));
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_OBJECT_NEW_CONST;
  }

  @Override
  public EResult<compilerxyz.eval.Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx, QExprId loc, CodeValue cv) {
    return compilerxyz.eval.dynamic.DynUtil.dropDefault(xx, loc, cv);
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
          if (!(self instanceof TypeTypeSourceObjectNewConst oc)) {
            return EResult.err(
                loc,
                "TypeTypeSourceObjectNewConst::exporterRootDelegateSelfExport: expected"
                    + " TypeTypeSourceObjectNewConst, got " + self.getClass());
          }
          var dt = oc.type_;
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  dt.newArtifactIdCell,
                  () -> {
                    var parentConst = new TypeTypeSourceObjectConst(dt);
                    var objectArtRes = parentConst.exportArtifact(xx, loc);
                    objectArtRes.drainInto(errors);
                    var objectArt =
                        objectArtRes.valueOrNull() != null
                            ? objectArtRes.requireValue(
                                "TypeTypeSourceObjectNewConst::exporterExport parent object")
                            : nullArtifact();
                    var body = new ArtifactDataSourceObjectNew(objectArt);
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
                loc,
                "TypeTypeSourceObjectNewConst requires an artifact id to deserialize");
          }
          var idValue = idSome.value();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataSourceObjectNew body;
                try {
                  body = JsonValues.fromValue(ArtifactDataSourceObjectNew.class, raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeTypeSourceObjectNewConst data: failed to parse "
                          + "ArtifactDataSourceObjectNew: " + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var objectArt = body.object();
                var holder = new ExporterAny[] {null};
                ExporterAny stub =
                    new ExporterAny() {
                      @Override
                      public EResult<Type> exporterAnyAsType() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeTypeSourceObjectNewConst import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsType();
                      }

                      @Override
                      public EResult<compilerxyz.gen.Value> exporterAnyAsValue() {
                        if (holder[0] == null) {
                          throw new IllegalStateException(
                              "TypeTypeSourceObjectNewConst import stub accessed pre-finalize");
                        }
                        return holder[0].exporterAnyAsValue();
                      }
                    };
                var deps = new TList<Artifact>();
                deps.add(objectArt);
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var parentAny =
                          ArtifactSupport.independentFinalizeImportArtifact(xx2, loc2, objectArt);
                      var parentVal =
                          parentAny.requireValue(
                              "TypeTypeSourceObjectNewConst::finalizer parent object");
                      compilerxyz.eval.value.source_object.TypeSourceObjectDynamic dtData;
                      if (parentVal instanceof TypeTypeSourceObjectConst pConst) {
                        dtData = pConst.type_();
                      } else if (parentVal
                          instanceof TypeTypeSourceObjectConst.ImportedSourceObjectConst imp) {
                        dtData = imp.type_();
                      } else {
                        throw new IllegalStateException(
                            "TypeTypeSourceObjectNewConst finalizer: parent object artifact resolved to "
                                + parentVal.getClass());
                      }
                      dtData.newArtifactIdCell.set(idValue);
                      holder[0] = new TypeTypeSourceObjectNewConst(dtData);
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(compilerxyz.eval.value.object_.ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_OBJECT_NEW_CONST_IDENTITY, TypeTypeSourceObjectNewConst.class) instanceof Opt.Some<TypeTypeSourceObjectNewConst> oSome
        && type_.typeIsEqual(xx, oSome.value().type_);
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
}
