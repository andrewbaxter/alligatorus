package compilerxyz.eval.value.source_union_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.eval.Binding;
import compilerxyz.eval.Code;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.ConstKey;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectConsumeDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.source_union.SourceUnionData;
import compilerxyz.eval.value.source_union.TypeSourceUnionConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;

public record TypeSourceUnionNewConst(SourceUnionData data, int variantIndex)
    implements Type, AspectArtifact, AspectBind, AspectFunction, ExporterAny {

  @Override
  public String typeDesc() {
    if (variantIndex < 0 || variantIndex >= data.variants.size()) {
      return "union_new(?)";
    }
    return "union_new(" + data.variants.get(variantIndex).tag.toCanonical() + ")";
  }

  @Override
  public Type valueTypeOf() {
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
              "TypeSourceUnionNewConst::exporterRootDelegateSelfExport: recursive export_artifact + parent "
                  + "TypeSourceUnionConst export deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeSourceUnionNewConst::exporterRootDelegateSelfFromArtifactData: recursive "
                  + "import_independent_artifact + parent TypeSourceUnionConst import deferred");
        }
      };

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceUnionNewConst::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceUnionNewConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    if (variantIndex < 0 || variantIndex >= data.variants.size()) {
      return EResult.err(
          loc, "TypeSourceUnionNewConst::call: variant index " + variantIndex + " out of range");
    }
    var variant = data.variants.get(variantIndex);
    String jsTag = variant.jsTag;
    Type underlyingType = variant.type;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceUnionNewConst::call: variant payload type not finalised");
    }
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
    Type resultType = new TypeSourceUnionConst(data);
    var canonicalTag = new ConstKey.Str(jsTag).toCanonical();
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.literalString(loc, canonicalTag));
    seq.add(underlyingJs);
    var descriptor =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class),
            TList.of(
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(String.class),
                compilerxyz.eval.dynamic.JavaDataDescriptor.fromClass(Object.class)));
    seq.add(
        JavaBytecodeUtils.callStaticMethod(
            loc,
            compilerxyz.eval.dynamic.JavaInternalName.fromClass(
                compilerxyz.eval.dynamic.DynRuntime.class),
            "unionWrap",
            descriptor));
    var newObject = new CodeDynamic(loc, seq);
    return EResult.ok(DynUtil.dynamicStackToValue(loc, newObject, resultType));
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

  private record SourceUnionNewConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
  }
}
