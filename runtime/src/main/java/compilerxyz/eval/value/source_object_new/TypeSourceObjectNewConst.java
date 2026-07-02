package compilerxyz.eval.value.source_object_new;

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
import compilerxyz.eval.value.source_object.SourceObjectData;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;

public record TypeSourceObjectNewConst(SourceObjectData data)
    implements Type, AspectArtifact, AspectBind, AspectFunction, ExporterAny {

  @Override
  public String typeDesc() {
    var d = data.description;
    if (d != null) return d;
    return "definition_set_object_new";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceObjectNewConst();
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
    return ExporterRoot.SOURCE_OBJECT_NEW_CONST;
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
              "TypeSourceObjectNewConst::exporterRootDelegateSelfExport: recursive export_artifact + parent "
                  + "TypeSourceObjectConst export deferred");
        }

        @Override
        public EResult<ExporterAny> exporterRootDelegateSelfFromArtifactData(
            ExprContext xx, QExprId loc, Opt<ArtifactIdSource> id, JsonValue data) {
          return EResult.err(
              loc,
              "TypeSourceObjectNewConst::exporterRootDelegateSelfFromArtifactData: recursive "
                  + "import_independent_artifact + parent TypeSourceObjectConst import deferred");
        }
      };

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceObjectNewConst::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceObjectNewConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    var set = data.set;
    if (set == null) {
      return EResult.err(
          loc, "TypeSourceObjectNewConst::call: object has no definition set reference");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    Type underlyingType = data.underlyingType;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceObjectNewConst::call: object underlying type not finalised");
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
    Type resultType = new TypeSourceObjectDynamic(data);

    var ctorArgs = new TList<compilerxyz.eval.dynamic.JavaBytecode.LoweredSequence.Element>();
    ctorArgs.add(new compilerxyz.eval.dynamic.JavaBytecode.LoweredSequence.Code(underlyingJs));

    var objectIndex = set.objects.indexOf(data);
    if (objectIndex < 0) {
      return EResult.err(
          loc,
          "TypeSourceObjectNewConst::call: source object not registered in its owning def-set");
    }
    var objInternalName = SourceObjectEmitter.internalNameFor(set.defSetId, objectIndex);
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(objInternalName);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.of(
                SourceObjectEmitter.DATA_CTOR_DESCRIPTOR));
    var newInstance =
        new CodeDynamic(loc, new JavaBytecode.LoweredSequence(
                compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiate(
                    -1, klass, ctorDesc, ctorArgs)));
    return EResult.ok(DynUtil.dynamicStackToValue(loc, newInstance, resultType));
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

  private record SourceObjectNewConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse_(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
  }
}
