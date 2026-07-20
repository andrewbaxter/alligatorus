package compilerxyz.eval.value.option;

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
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.gen.GenAspectArtifact;
import compilerxyz.gen.GenAspectConsumeDynamic;

@compilerxyz.annotations.BuiltinTypeId(7037L)
public record TypeOptionVariantConstructorDynamic(Type elementType, boolean isSome)
    implements
        Type,
        AspectArtifact,
        AspectFunction,
        ExporterAny {

  @Override
  public TypeId hasTypeIdTypeId() { return new TypeId.Composite(GenTypeIds.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID, TList.of(elementType.hasTypeIdTypeId())); }


  @Override
  public String typeDesc() {
    return "option(" + elementType.typeDesc() + ")." + (isSome ? "some" : "none");
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeOptionVariantConstructorDynamic.class, compilerxyz.gen.GenTypeIds.TYPE_OPTION_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID, "option_variant_constructor_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
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
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var consumeRes = compilerxyz.gen.GenAspectConsumeDynamic.get(xx, loc, underlyingType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var underlyingJsRes =
        consumeRes
            .requireValue("TypeOptionVariantConstructorDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs =
        underlyingJsRes.requireValue("TypeOptionVariantConstructorDynamic::call toDynamic");

    Type resultType = new TypeOptionDynamic(compilerxyz.eval.dynamic.DynUtil.substituteBoxed(xx, elementType));
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
      newOpt =
          JavaBytecodeUtils.callStaticInterfaceMethod(
              new compilerxyz.eval.dynamic.CodeLoc.Some(loc),
              JavaInternalName.fromClass(Opt.class),
              "none",
              JavaMethodDescriptor.fromParts(
                  JavaDataDescriptor.fromInternalName(
                      JavaInternalName.fromClass(Opt.class)),
                  TList.of()));
    }
    return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, newOpt, resultType));
  }

  @Override
  public Opt<Type> aspectFunctionExceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(compilerxyz.eval.value.object_.ValueObjectConst.ofType(this));
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
}
