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
import compilerxyz.eval.value.object_.ValueObjectConst;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.gen.GenType;

@BuiltinLight(uniqueId = 6139L)
public record TypeSourceObjectNewDynamic(TypeSourceObjectDynamic type_)
    implements Type, AspectBind, AspectBindDynamic, AspectFunction, ExporterAny {

  public static Type dynamicType() {
    return GenType.dynamicType();
  }



  @Override
  public String typeDesc() {
    return type_.description.optUnwrapOr("definition_set_object_new");
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
    if (cv.value().optIsNone()) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::bind: CodeValue has no compile-time value");
    }
    Value v = cv.value().optUnwrap();
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
    if (type_.underlyingType.optIsNone()) {
      return EResult.err(
          loc, "TypeSourceObjectNewDynamic::call: object underlying type not finalised");
    }
    Type underlyingType = type_.underlyingType.optUnwrap();
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
            .requireValue("TypeSourceObjectNewDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs = underlyingJsRes.requireValue("TypeSourceObjectNewDynamic::call toDynamic");
    Type resultType = type_;

    var objInternalName = SourceObjectEmitter.internalNameFor(set.defSetId, type_.objIdx);
    var klass = JavaInternalName.of(objInternalName);
    var underlyingDesc =
        consumeRes
            .requireValue("TypeSourceObjectNewDynamic::call underlying desc")
            .aspectConsumeDynamicJvmClass(xx);
    var ctorDesc =
        JavaMethodDescriptor.fromConstructorParts(
            TList.of(underlyingDesc));
    var newInstance =
        JavaBytecodeUtils.instantiate(loc, klass, ctorDesc, underlyingJs);
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
    return EResult.ok(new ValueObjectConst(new TypeTypeSourceObjectNewConst(type_()), this));
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
        if (!this.equals(other)) {
          return EResult.err(loc,
              "cannot merge " + getClass().getSimpleName()
                  + " with a differently-typed binding across branches");
        }
      }
      return EResult.ok(this);
    }
  }

  @Override
  public EResult<CodeValue> typeCastFrom(ExprContext xx, QExprId loc, CodeValue cv) {
    throw new UnsupportedOperationException(getClass().getSimpleName() + "::typeCastFrom unreachable (typeCanCastFrom returned false)");
  }
  @Override
  public boolean typeIsEqual(ExprContext xx, Type other) {
    var oOpt = AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_OBJECT_NEW_DYNAMIC_IDENTITY, TypeSourceObjectNewDynamic.class);
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
}
