package compilerxyz.eval.value.source_union_new;

import compilerxyz.eval.aspect.AspectLookup;

import compilerxyz.gen.GenTrivialIdentityAspects;

import compilerxyz.annotations.BuiltinIdentity;
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
import compilerxyz.gen.AspectBind;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenAspectFunction;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.value.source_union.TypeSourceUnionDynamic;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;
import compilerxyz.gen.GenAspectBind;
import compilerxyz.gen.GenAspectConsumeDynamic;

@BuiltinIdentity(uniqueId = 6142L)
public record TypeSourceUnionNewDynamic(TypeSourceUnionDynamic type_, int variantIndex)
    implements Type, AspectBind, AspectFunction, ExporterAny {

  public static Type dynamicType() {
    return compilerxyz.gen.GenType.dynamicType();
  }


  @Override
  public String typeDesc() {
    if (variantIndex < 0 || variantIndex >= type_.variants.size()) {
      return "union_new(?)";
    }
    return "union_new(" + type_.variants.get(variantIndex).tag.toCanonical() + ")";
  }

  private static final compilerxyz.eval.value.object_.TypeObjectConst META = compilerxyz.eval.value.object_.TypeObjectConst.metaOf(TypeSourceUnionNewDynamic.class, "source_union_new_dynamic");

                public Type valueType() {
    return META;
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY)) {
      return Opt.some(this);
    }
    if (key.equals(GenAspectBind.AspectId) || key.equals(GenAspectFunction.AspectId)) {
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
    Type underlyingType = variant.type;
    if (underlyingType == null) {
      return EResult.err(
          loc, "TypeSourceUnionNewDynamic::call: variant payload type not finalised");
    }
    var unionIndex = type_.set.unions.indexOf(type_);
    if (unionIndex < 0) {
      return EResult.err(
          loc, "TypeSourceUnionNewDynamic::call: union type not in owning DefinitionSet");
    }
    xx.vx.definitionSets.putIfAbsent(type_.set.defSetId, type_.set);
    var variantBinaryName =
        compilerxyz.eval.dynamic.SourceUnionEmitter.variantBinaryNameFor(
            type_.set.defSetId, unionIndex, variantIndex);
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
            .requireValue("TypeSourceUnionNewDynamic::call consume")
            .aspectConsumeDynamicToDynamic(xx, loc, converted);
    if (underlyingJsRes.hasErrors()) return underlyingJsRes.reerr();
    var underlyingJs = underlyingJsRes.requireValue("TypeSourceUnionNewDynamic::call toDynamic");
    Type resultType = type_;
    var variantInternal = JavaInternalName.of(variantBinaryName.replace('.', '/'));
    var payloadDesc =
        compilerxyz.eval.dynamic.SourceUnionEmitter.payloadDescriptor(xx, underlyingType);
    var ctorDesc =
        compilerxyz.eval.dynamic.JavaMethodDescriptor.fromParts(
            compilerxyz.eval.dynamic.JavaDataDescriptor.DESC_VOID, TList.of(payloadDesc));
    var seq = new compilerxyz.eval.dynamic.JavaBytecodeSequence();
    seq.add(JavaBytecodeUtils.newInstance(loc, variantInternal));
    seq.add(JavaBytecodeUtils.inst(loc, org.objectweb.asm.Opcodes.DUP));
    seq.add(underlyingJs);
    seq.add(JavaBytecodeUtils.invokeSpecial(loc, variantInternal, "<init>", ctorDesc));
    var newObject = new CodeDynamic(loc, seq);
    return EResult.ok(DynUtil.dynamicStackToValue(xx, loc, newObject, resultType, 1));
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

  private record SourceUnionNewConstBinding(Value value) implements Binding {
    @Override
    public CodeValue bindingUse(ExprContext xx, QExprId loc, Code code) {
      return new CodeValue(Code.none(loc), EvalOutcome.ofValue(value));
    }

    @Override
    public Binding bindingFork() {
      return this;
    }

    @Override
    public boolean bindingEquals(ExprContext xx, QExprId loc, Binding other) {
      return this == other || this.equals(other);
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
    return AspectLookup.getMaybe(xx, other, GenTrivialIdentityAspects.TYPE_SOURCE_UNION_NEW_DYNAMIC_IDENTITY, TypeSourceUnionNewDynamic.class) instanceof Opt.Some<TypeSourceUnionNewDynamic> oSome
        && type_.typeIsEqual(xx, oSome.value().type_) && variantIndex == oSome.value().variantIndex;
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
