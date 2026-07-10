package compilerxyz.eval.value.source_object_new;

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
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.target.TargetBindSub;
import compilerxyz.gen.AspectArtifact;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.eval.aspect.PostDeserializeIdentity;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.safejson.JsonValue;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import java.util.function.Function;
import compilerxyz.gen.AspectPostDeserialize;
import compilerxyz.eval.TypeId;
import compilerxyz.gen.GenTypeIds;

public record TypeSourceObjectNewDynamic(TypeSourceObjectDynamic type_)
    implements Type, AspectBind, AspectBindDynamic, AspectFunction, ExporterAny {

  @Override
  public TypeId typeId() {
    return new TypeId.Composite(
        GenTypeIds.TYPE_SOURCE_OBJECT_NEW_CONST_TYPE_ID,
        TList.of(new TypeId.DefsetMember(type_.set.defSetId, uniqueId(type_.identity))));
  }

  private static long uniqueId(Unique u) {
    return switch (u) {
      case Unique.Builtin b -> b.id();
      case Unique.Source s -> s.id();
    };
  }


  @Override
  public String typeDesc() {
    var d = type_.description;
    if (d != null) return d;
    return "definition_set_object_new";
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceObjectNewConst();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectBind.ID)
        || key.equals(AspectBindDynamic.ID)
        || key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<BindResult> bind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceObjectNewConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> call(
      ExprContext xx, QExprId loc, CodeValue function, CodeValue argument) {
    var set = type_.set;
    if (set == null) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::call: object has no definition set reference");
    }
    xx.vx.definitionSets.putIfAbsent(set.defSetId, set);
    Type underlyingType = type_.underlyingType;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::call: object underlying type not finalised");
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
    Type resultType = type_;

    var objectIndex = set.objects.indexOf(type_);
    if (objectIndex < 0) {
      return EResult.err(
          loc,
          "TypeSourceObjectNewDynamic::call: source object not registered in its owning def-set");
    }
    var objInternalName = SourceObjectEmitter.internalNameFor(set.defSetId, objectIndex);
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(objInternalName);
    compilerxyz.eval.dynamic.JavaDataDescriptor underlyingDesc =
        underlyingType instanceof compilerxyz.gen.AspectConsumeDynamic underlyingConsume
            ? underlyingConsume.consumeDynamicJvmClass()
            : compilerxyz.eval.dynamic.JvmConstants.DESC_OBJECT;
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            TList.of(underlyingDesc));
    var newInstance =
        compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
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
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), Opt.some(value));
    }
  }
}
