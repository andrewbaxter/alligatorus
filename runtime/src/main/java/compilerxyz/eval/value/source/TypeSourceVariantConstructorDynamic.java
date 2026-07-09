package compilerxyz.eval.value.source;

import compilerxyz.artifact.ExporterAny;
import compilerxyz.eval.CodeValue;
import compilerxyz.eval.EResult;
import compilerxyz.eval.Erb;
import compilerxyz.eval.Eval;
import compilerxyz.eval.ExprContext;
import compilerxyz.eval.QExprId;
import compilerxyz.eval.Type;
import compilerxyz.eval.TypeId;
import compilerxyz.eval.Unique;
import compilerxyz.eval.Value;
import compilerxyz.eval.aspect.Aspect;
import compilerxyz.eval.dynamic.CodeDynamic;
import compilerxyz.eval.dynamic.DynUtil;
import compilerxyz.eval.dynamic.JavaBytecodeSequence;
import compilerxyz.eval.dynamic.JavaBytecodeUtils;
import compilerxyz.eval.dynamic.JavaDataDescriptor;
import compilerxyz.eval.dynamic.JavaInternalName;
import compilerxyz.eval.dynamic.JavaMethodDescriptor;
import compilerxyz.eval.value.TypeRoot;
import compilerxyz.eval.value.string.TypeStringDynamic;
import compilerxyz.eval.value.tuple.Tuple;
import compilerxyz.eval.value.tuple.TupleElement;
import compilerxyz.gen.AspectFunction;
import compilerxyz.gen.GenTypeIds;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public record TypeSourceVariantConstructorDynamic(SourceKind kind)
    implements
        Type,
        AspectFunction,
        ExporterAny {

  public enum SourceKind {
    BINDING,
    EXPR,
    FIELD,
    DUP
  }

  @Override
  public TypeId typeId() {
    return GenTypeIds.TYPE_SOURCE_VARIANT_CONSTRUCTOR_DYNAMIC_TYPE_ID;
  }

  @Override
  public String typeDesc() {
    return "source." + kind.name().toLowerCase();
  }

  @Override
  public Type valueTypeOf() {
    return new TypeRoot.SourceVariantConstructorDynamic();
  }

  @Override
  public Opt<Aspect> typeAspect(ExprContext xx, Unique key) {
    if (key.equals(AspectFunction.ID)) {
      return Opt.some(this);
    }
    return Opt.none();
  }

  @Override
  public EResult<CodeValue> call(ExprContext xx, QExprId loc, CodeValue self, CodeValue argument) {
    var erb = new Erb(loc);
    Type resultType = compilerxyz.eval.value.ValueDynamic.sourceDynamicType();
    JavaInternalName sourceName =
        JavaInternalName.of("compilerxyz/eval/value/ValueDynamic$Source");
    switch (kind) {
      case FIELD -> {
        var argTupleType =
            new Tuple(
                Opt.none(),
                TList.of(
                    new TupleElement.Dynamic(compilerxyz.eval.value.ValueDynamic.sourceDynamicType()),
                    new TupleElement.Dynamic(new TypeStringDynamic())));
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, argTupleType, argument));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        var slotsRes = DynUtil.destructurePositional(xx, loc, argTupleType, convertedOpt.get());
        if (slotsRes.hasErrors()) return slotsRes.reerr();
        var slots = slotsRes.requireValue("TypeSourceVariantConstructorDynamic::call FIELD");
        if (slots.size() != 2) {
          throw new AssertionError(
              "TypeSourceVariantConstructorDynamic::call FIELD: expected 2 slots, got "
                  + slots.size());
        }
        var argsSeq = new JavaBytecodeSequence();
        argsSeq.add(slots.get(0));
        argsSeq.add(slots.get(1));
        var newSource =
            JavaBytecodeUtils.instantiate(
                loc,
                JavaInternalName.of("compilerxyz/eval/value/ValueDynamic$Source$Field"),
                JavaMethodDescriptor.fromConstructorParts(
                    TList.of(
                        JavaDataDescriptor.fromInternalName(sourceName),
                        JavaDataDescriptor.fromInternalName(
                            JavaInternalName.of("java/lang/String")))),
                new CodeDynamic(loc, argsSeq));
        return EResult.ok(DynUtil.dynamicStackToValue(loc, newSource, resultType));
      }
      case DUP -> {
        Type baseType = compilerxyz.eval.value.ValueDynamic.sourceDynamicType();
        var convertedOpt = erb.mustEr(Eval.typecheckConvert(xx, loc, baseType, argument));
        if (convertedOpt.isEmpty()) return EResult.errs(erb.errors());
        var baseJsOpt =
            erb.mustEr(DynUtil.destructurePackedObject(xx, loc, baseType, convertedOpt.get()));
        if (baseJsOpt.isEmpty()) return EResult.errs(erb.errors());
        var newSource =
            JavaBytecodeUtils.instantiate(
                loc,
                JavaInternalName.of("compilerxyz/eval/value/ValueDynamic$Source$Dup"),
                JavaMethodDescriptor.fromConstructorParts(
                    TList.of(JavaDataDescriptor.fromInternalName(sourceName))),
                baseJsOpt.get());
        return EResult.ok(DynUtil.dynamicStackToValue(loc, newSource, resultType));
      }
      case BINDING, EXPR ->
          throw new AssertionError(
              "TypeSourceVariantConstructorDynamic::call: "
                  + kind
                  + " is produced via the stack builtin, not a source-visible constructor");
    }
    throw new AssertionError("unreachable");
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
