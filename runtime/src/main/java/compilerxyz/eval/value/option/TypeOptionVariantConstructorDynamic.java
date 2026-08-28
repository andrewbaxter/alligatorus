package compilerxyz.eval.value.option;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenCodegenDescriptors;
import compilerxyz.gen.GenTrivialIdentityAspects;


import compilerxyz.annotations.BuiltinLight;
import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JvmConstants;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectConsumeDynamic;
import compilerxyz.eval.dynamic.CodeLoc.Some;
import compilerxyz.eval.value.object_.ValueObjectConst;

@BuiltinLight(uniqueId = 6132L)
public record TypeOptionVariantConstructorDynamic(Type elementType, boolean isSome)
    implements
        Type,
        AspectArtifact,
        AspectFunction,
        ExporterAny {


  @Override
  public String typeDesc() {
    return "option(" + elementType.typeDesc() + ")." + (isSome ? "some" : "none");
  }



  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectArtifact.AspectId) || key.equals(GenAspectFunction.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter aspectArtifactExporter() {
    return ExporterRoot.OPTION_VARIANT_CONSTRUCTOR_DYNAMIC;
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
              "TypeOptionVariantConstructorDynamic::exporterRootDelegateSelfExport: recursive export_artifact"
                  + " (eval/artifact.rs) port deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeOptionVariantConstructorDynamic::exporterRootDelegateSelfFromArtifactData: recursive"
                  + " import_type_artifact (eval/artifact.rs) port deferred");
        }
      };

  @Override
  public EResult<CodeValue> aspectFunctionCall(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    Type underlyingType = isSome ? elementType : new TypeNullDynamic();
    var erb = new Erb(loc);
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
    if (convertedOpt.optIsNone()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.optUnwrap();
    var consumeRes = GenAspectConsumeDynamic.get(xx, loc, underlyingType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var underlyingJsRes =
        consumeRes
            .requireValue("TypeOptionVariantConstructorDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs =
        underlyingJsRes.requireValue("TypeOptionVariantConstructorDynamic::call toDynamic");

    Type resultType = new TypeOptionDynamic(DynUtil.substituteBoxed(xx, elementType));
    CodeDynamic newOpt;
    if (isSome) {
      newOpt =
          JavaBytecodeUtils.instantiate(
              loc,
              JavaInternalName.fromClass(Opt.Some.class),
              JavaMethodDescriptor.fromConstructorParts(
                  TList.of(JvmConstants.DESC_OBJECT)),
              underlyingJs);
    } else {
      newOpt = JavaBytecodeUtils.callStaticMethod(new Some(loc), GenCodegenDescriptors.OPT_NONE);
    }
    return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, newOpt, resultType, 1));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType(ExprContext xx, QExprId loc, CodeValue self) {
    return Opt.none();
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
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_IDENTITY, TypeOptionVariantConstructorDynamic.class);
    return oOpt.optIsSome() && elementType.typeIsEqual(xx, oOpt.optUnwrap().elementType) && isSome == oOpt.optUnwrap().isSome;
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
