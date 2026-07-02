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
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.null_.TypeNullDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;

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
  public Type valueTypeOf() {
    return new TypeRoot.OptionVariantConstructorDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectArtifact.ID) || key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public ArtifactExporter exporter() {
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
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    Type underlyingType = isSome ? elementType : new TypeNullDynamic();
    var erb = new Erb(loc);
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var toDynamicOpt = erb.mustEr(AspectConsumeDynamic.get(xx, loc, underlyingType));
    if (toDynamicOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var underlyingJsOpt = erb.mustEr(toDynamicOpt.get().toDynamic(xx, loc, converted));
    if (underlyingJsOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var underlyingJs = underlyingJsOpt.get();

    Type resultType = new TypeOptionDynamic(elementType);
    TList<JavaBytecode.LoweredSequence.Element> elements;
    if (isSome) {
      elements =
          JavaBytecodeUtils.instantiate(
              -1,
              JavaInternalName.of("compilerxyz/util/Opt$Some"),
              JavaMethodDescriptor.fromConstructorParts(JvmConstants.DESC_OBJECT),
              TList.of(new JavaBytecode.LoweredSequence.Code(underlyingJs)));
    } else {
      elements = new TList<>();
      elements.add(
          new JavaBytecode.LoweredSequence.Bytecode(
              JavaBytecodeUtils.callStaticInterfaceMethod(
                  -1,
                  JavaInternalName.of("compilerxyz/util/Opt"),
                  "none",
                  JavaMethodDescriptor.fromParts(
                      JavaDataDescriptor.fromInternalName(
                          JavaInternalName.of("compilerxyz/util/Opt"))))));
    }
    var newOpt = new CodeDynamic(loc, new JavaBytecode.LoweredSequence(elements));
    return EResult.ok(DynUtil.dynamicStackToValue(loc, newOpt, resultType));
  }

  @Override
  public Opt<Type> exceptionType() {
    return Opt.none();
  }

  @Override
  public EResult<Type> exporterAnyAsType() {
    return EResult.ok(this);
  }

  @Override
  public EResult<Value> exporterAnyAsValue() {
    return EResult.ok(this);
  }
}
