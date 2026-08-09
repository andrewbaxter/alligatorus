package compilerxyz.eval.value.source_object_new;

import compilerxyz.gen.BindingSetter;



import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinLight;
import compilerxyz.eval.EvalOutcome;

import compilerxyz.artifact.ExporterAny;
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
import compilerxyz.gen.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.gen.TargetBindSub;
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectBindDynamic;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.source_object.TypeSourceObjectDynamic;
import compilerxyz.eval.dynamic.SourceObjectEmitter;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectBindDynamic;
import compilerxyz.gen.GenAspectConsumeDynamic;

@BuiltinLight(uniqueId = 6139L)
public record TypeSourceObjectNewDynamic(TypeSourceObjectDynamic type_)
    implements Type, AspectBind, AspectBindDynamic, AspectFunction, ExporterAny {

  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }



  @Override
  public String typeDesc() {
    var d = type_.description;
    if (d != null) return d;
    return "definition_set_object_new";
  }


  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_NEW_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectBind.AspectId)
        || key.equals(GenAspectBindDynamic.AspectId)
        || key.equals(GenAspectFunction.AspectId)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<Opt<TargetBindSub>> aspectBindDynamicBindDynamic(ExprContext xx, QExprId loc, CodeValue cv) {
    return DynUtil.bindDynamicDefault(xx, loc, cv);
  }

  @Override
  public EResult<BindResult> aspectBindBind(ExprContext xx, QExprId loc, CodeValue cv) {
    if (!(cv.value() instanceof Opt.Some<Value> some)) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::bind: CodeValue has no compile-time value");
    }
    Value v = some.value();
    return EResult.ok(new BindResult(Code.none(loc), new SourceObjectNewConstBinding(v)));
  }

  @Override
  public EResult<CodeValue> aspectFunctionCall(
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
    var consumeRes = compilerxyz.gen.GenAspectConsumeDynamic.get(xx, loc, underlyingType);
    if (consumeRes.hasErrors()) return consumeRes.reerr();
    var underlyingJsRes =
        consumeRes
            .requireValue("TypeSourceObjectNewDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs = underlyingJsRes.requireValue("TypeSourceObjectNewDynamic::call toDynamic");
    Type resultType = type_;

    var objectIndex = set.objects.indexOf(type_);
    if (objectIndex < 0) {
      return EResult.err(
          loc,
          "TypeSourceObjectNewDynamic::call: source object not registered in its owning def-set");
    }
    var objInternalName = SourceObjectEmitter.internalNameFor(set.defSetId, objectIndex);
    var klass = compilerxyz.eval.dynamic.JavaInternalName.of(objInternalName);
    var underlyingDesc =
        consumeRes
            .requireValue("TypeSourceObjectNewDynamic::call underlying desc")
            .aspectConsumeDynamicJvmClass(xx);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromConstructorParts(
            TList.of(underlyingDesc));
    var newInstance =
        compilerxyz.eval.dynamic.JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
    return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, newInstance, resultType, 1));
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
    return EResult.ok(new compilerxyz.eval.value.object_.ValueObjectConst(new TypeTypeSourceObjectNewConst(type_()), this));
  }

  private record SourceObjectNewConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Opt<BindingSetter> setter, Code code) {
      return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
    }

    @Override
    public Binding bindingFork() {
      return this;
    }



    @Override
    public EResult<Binding> bindingMerge(ExprContext xx, QExprId loc, TList<Binding> others) {
      for (var other : others) {
        if (other != this && !this.equals(other)) {
          return EResult.err(loc,
              "cannot merge " + getClass().getSimpleName()
                  + " with a differently-typed binding across branches");
        }
      }
      return EResult.ok(this);
    }
  }

  @Override
  public compilerxyz.eval.EResult<compilerxyz.eval.CodeValue> typeCastFrom(compilerxyz.eval.ExprContext xx, compilerxyz.eval.QExprId loc, compilerxyz.eval.CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(compilerxyz.eval.ExprContext xx, compilerxyz.eval.Type other) {
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_NEW_DYNAMIC_IDENTITY, TypeSourceObjectNewDynamic.class) instanceof Opt.Some<TypeSourceObjectNewDynamic> oSome
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
