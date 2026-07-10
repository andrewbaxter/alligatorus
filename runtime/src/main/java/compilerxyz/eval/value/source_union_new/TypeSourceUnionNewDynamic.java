package compilerxyz.eval.value.source_union_new;

import compilerxyz.artifact.Artifact;
import compilerxyz.artifact.ArtifactBody;
import compilerxyz.artifact.ArtifactExporter;
import compilerxyz.artifact.ArtifactIdSource;
import compilerxyz.artifact.ExporterAny;
import compilerxyz.artifact.ExporterRoot;
import compilerxyz.artifact.ExporterRootDelegate;
import compilerxyz.gen.Binding;
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
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.eval.value.source_union.TypeTypeSourceUnionConst;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeSourceUnionNewDynamic(TypeSourceUnionDynamic type_, int variantIndex)
    implements Type, AspectBind, AspectFunction, ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.Composite(
        GenTypeIds.TYPE_SOURCE_UNION_NEW_CONST_TYPE_ID,
        TList.of(
            new TypeId.DefsetMember(type_.set.defSetId, uniqueId(type_.identity)),
            new TypeId.Builtin(variantIndex)));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public String typeDesc() {
    if (variantIndex < 0 || variantIndex >= type_.variants.size()) {
      return "union_new(?)";
    }
    return "union_new(" + type_.variants.get(variantIndex).tag.toCanonical() + ")";
  }

  @Override
  public Type valueType() {
    return new TypeRoot.SourceUnionNewConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectBind.ID) || key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceUnionNewDynamic::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceUnionNewConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    if (variantIndex < 0 || variantIndex >= type_.variants.size()) {
      return EResult.err(
          loc, "TypeSourceUnionNewDynamic::call: variant index " + variantIndex + " out of range");
    }
    var variant = type_.variants.get(variantIndex);
    String jsTag = variant.jsTag;
    Type underlyingType = variant.type;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceUnionNewDynamic::call: variant payload type not finalised");
    }
    var erb = new Erb(loc);
    var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, underlyingType, argument));
    if (convertedOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var converted = convertedOpt.get();
    var underlyingJsOpt =
        erb.mustEr(
            compilerxyz.eval.dynamic.DynUtil.destructurePackedObject(
                xx, loc, underlyingType, converted));
    if (underlyingJsOpt.isEmpty()) {
      return EResult.errs(erb.errors());
    }
    var underlyingJs = underlyingJsOpt.get();
    Type resultType = new TypeTypeSourceUnionConst(type_);
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
  public Opt<Type> aspectFunctionExceptionType() {
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
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
  }
}
