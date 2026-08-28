package compilerxyz.eval.value.source_union;




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
import compilerxyz.eval.BindingId;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.DefinitionSet;
import compilerxyz.eval.EResult;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.ExprErr;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.aspect.AspectLookup;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.value.binding.BindingDynamicBinding;
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectDropDynamicLight;
import compilerxyz.gen.AspectInitialBind;
import compilerxyz.gen.AspectType;
import compilerxyz.gen.AspectUnion;
import compilerxyz.gen.Binding;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.gen.GenAspectDropDynamicLight;
import compilerxyz.gen.GenAspectInitialBind;
import compilerxyz.gen.GenAspectPostDeserialize;
import compilerxyz.gen.GenAspectType;
import compilerxyz.gen.GenAspectUnion;
import compilerxyz.gen.GenTrivialIdentityAspects;
import compilerxyz.gen.GenType;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.Value;
import compilerxyz.gen.codec.ArtifactDataSourceUnionCodec;
import compilerxyz.safejson.JsonValue;
import compilerxyz.safejson.JsonValueNull;
import compilerxyz.util.MustCast;
import compilerxyz.util.Opt;
import compilerxyz.util.Result;
import compilerxyz.util.TList;
import java.util.function.Function;

@BuiltinLight(uniqueId = 6141L)
public record TypeTypeSourceUnionConst(TypeSourceUnionDynamic type_)
    implements
        Type,
        AspectArtifact,
        AspectBind,
        AspectBindDynamic,
        AspectConsumeDynamic,
        AspectDropDynamicLight,
        AspectInitialBind,
        AspectType,
        AspectUnion,
        ExporterAny {


  @Override
  public JavaDataDescriptor aspectConsumeDynamicJvmClass(ExprContext xx) {
    return JavaDataDescriptor.fromInternalName(JvmConstants.INTNAME_OBJECT);
  }

  @Override
  public String typeDesc() {
    return "union";
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_CONST_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(type_.identity)
        || key.equals(GenAspectArtifact.AspectId)
        || key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectConsumeDynamic.AspectId)
        || key.equals(GenAspectDropDynamicLight.AspectId)
        || key.equals(GenAspectInitialBind.AspectId)
        || key.equals(GenAspectType.AspectId)
        || key.equals(GenAspectUnion.AspectId)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectPostDeserialize.AspectId)) {
      return Opt.some(PostDeserializeIdentity.INSTANCE);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.SOURCE_UNION_CONST;
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
          var uc = MustCast.mustCast(TypeTypeSourceUnionConst.class, self);
          var tu = uc.type_;
          var set = tu.set;
          if (set == null) {
            return EResult.err(
                loc,
                "TypeTypeSourceUnionConst::exporterRootDelegateSelfExport: union not attached to a"
                    + " definition set");
          }
          var errors = new TList<ExprErr>();
          var artifact =
              ArtifactSupport.implExportIndependentArtifact(
                  xx.vx.ix.gx.artifacts,
                  tu.artifactIdCell,
                  () -> {
                    var defSetArtRes =
                        ExporterRoot.DEFINITION_SET.artifactExporterExport(xx, loc, set);
                    defSetArtRes.drainInto(errors);
                    var defSetArt =
                        defSetArtRes.valueOrNull() != null
                            ? defSetArtRes.requireValue(
                                "TypeTypeSourceUnionConst::exporterExport defSet")
                            : nullArtifact();
                    var variantsOut = new TList<ArtifactDataSourceUnionVariant>();
                    for (var v : tu.variants) {
                      var tArtRes = v.type.artifactableExportArtifact(xx, loc);
                      tArtRes.drainInto(errors);
                      var tArt =
                          tArtRes.valueOrNull() != null
                              ? tArtRes.requireValue(
                                  "TypeTypeSourceUnionConst::exporterExport variant type")
                              : nullArtifact();
                      variantsOut.add(
                          new ArtifactDataSourceUnionVariant(v.jsTag, v.tag, tArt));
                    }
                    var body =
                        new ArtifactDataSourceUnion(
                            defSetArt,
                            set.defSetId,
                            tu.identity,
                            variantsOut,
                            tu.compiled.baseBinaryName,
                            tu.compiled.variantBinaryNames,
                            tu.compiled.baseBytecode,
                            tu.compiled.variantBytecode);
                    return makeBody.apply(ArtifactDataSourceUnionCodec.toValue(body));
                  });
          if (errors.isEmpty()) return EResult.ok(artifact);
          return EResult.mixed(artifact, errors);
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          if (id.optIsNone()) {
            return EResult.err(
                loc, "TypeTypeSourceUnionConst requires an artifact id to deserialize");
          }
          var idValue = id.optUnwrap();
          return ArtifactSupport.implImportIndependentArtifact(
              xx,
              loc,
              idValue,
              data,
              (raw) -> {
                ArtifactDataSourceUnion body;
                try {
                  body = ArtifactDataSourceUnionCodec.fromValue(raw);
                } catch (RuntimeException e) {
                  return new Result.Err<>(
                      "TypeTypeSourceUnionConst data: failed to parse "
                          + "ArtifactDataSourceUnion: " + e.getMessage());
                }
                return new Result.Ok<>(body);
              },
              (body) -> {
                var tu = new TypeSourceUnionDynamic(body.identity());
                tu.artifactIdCell.set(idValue);
                tu.compiled.baseBinaryName = body.compiledBaseBinaryName();
                tu.compiled.variantBinaryNames = body.compiledVariantBinaryNames();
                tu.compiled.baseBytecode = body.compiledBaseBytecode();
                tu.compiled.variantBytecode = body.compiledVariantBytecode();
                for (var v : body.variants()) {
                  tu.active.add(tu.variants.size());
                  tu.variants.add(
                      new SourceUnionVariant(v.tag(), v.jsTag(), GenType.dynamicType()));
                }
                var imported = new ImportedSourceUnionConst(tu);
                var deps = new TList<Artifact>();
                deps.add(body.defSet());
                for (var v : body.variants()) deps.add(v.type_());
                ArtifactSupport.ImportIndependentResult.Finalizer fin =
                    (xx2, loc2) -> {
                      var defSetAny =
                          ArtifactSupport.independentFinalizeImportArtifact(
                              xx2, loc2, body.defSet());
                      var defSetVal =
                          defSetAny.requireValue(
                              "TypeTypeSourceUnionConst::finalizer defSet artifact");
                      if (!(defSetVal instanceof DefinitionSet set)) {
                        throw new IllegalStateException(
                            "TypeTypeSourceUnionConst finalizer: defSet artifact resolved to "
                                + defSetVal.getClass());
                      }
                      tu.initSetReflectively(set);
                      var variantsIt = body.variants().iterator();
                      for (int i = 0; i < tu.variants.size(); i++) {
                        var v = tu.variants.get(i);
                        var artV = variantsIt.next();
                        var tRes =
                            ArtifactSupport.independentFinalizeImportTypeArtifact(
                                xx2, loc2, artV.type_());
                        var tVal =
                            tRes.requireValue(
                                "TypeTypeSourceUnionConst::finalizer variant type");
                        tu.variants.set(
                            i, new SourceUnionVariant(v.tag, v.jsTag, tVal));
                      }
                    };
                return new Result.Ok<>(
                    new ArtifactSupport.ImportIndependentResult(imported, deps, fin));
              });
        }
      };

  public record ImportedSourceUnionConst(TypeSourceUnionDynamic type_) implements ExporterAny {
    @Override
    public EResult<Type> exporterAnyAsType() {
      return EResult.ok(type_);
    }

    @Override
    public EResult<Value> exporterAnyAsValue() {
      return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), type_));
    }
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindViaTarget(xx, loc, cv);
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeDynamic> aspectConsumeDynamicToDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    var dyn = cv.code().codeDynamic();
    if (dyn != null) return EResult.ok(dyn);
    return EResult.err(
        loc, "TypeTypeSourceUnionConst::toDynamic: CodeValue has no dynamic code");
  }

  @Override
  public Binding aspectInitialBindInitialBind(BindingId dynamicKey) {
    return new BindingDynamicBinding(dynamicKey, new TypeTypeSourceUnionConst(type_));
  }

  @Override
  public Type aspectTypeType(Value value) {
    return new TypeTypeSourceUnionConst(type_);
  }

  @Override
  public TList<AspectUnion.Variant> aspectUnionVariants() {
    var out = new TList<AspectUnion.Variant>();
    for (var v : type_.variants) {
      out.add(new AspectUnion.Variant(v.tag, v.jsTag, v.type));
    }
    return out;
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(new ValueObjectConst(ValueObjectConst.plainTypeType(), this));
  }

  @Override
  public EResult<Code> aspectDropDynamicLightDropDynamicLight(
      ExprContext xx,
      QExprId loc,
      CodeValue cv) {
    return DynUtil.dropDefault(xx, loc, cv);
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_TYPE_SOURCE_UNION_CONST_IDENTITY, TypeTypeSourceUnionConst.class);
    return oOpt.optIsSome() && type_.typeIsEqual(xx, oOpt.optUnwrap().type_);
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
